package com.vietreader.tts.data.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.vietreader.tts.ui.reader.VietnameseTextHighlighter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * Offline Neural AI TTS Controller powered by `k2-fsa/sherpa-onnx` (Piper VITS `.onnx` models)
 * and `android.media.AudioTrack`.
 *
 * Features:
 * - Single-instance native model management with strict `release()` on voice switch / shutdown to prevent OOM
 * - Background `AudioTrack` streaming in 40ms PCM frames for instant pause/stop responsiveness
 * - Look-ahead **Pre-fetching** (`prefetchSentence`) synthesizing sentence `N + 1` into RAM while
 *   sentence `N` is playing on `AudioTrack` for zero-latency transitions between sentences
 * - Synchronized word-span highlighting (`onWordRange`) aligned to actual PCM sample playback progress.
 */
class SherpaOnnxTtsController(
    private val modelManager: AiVoiceModelManager,
    private val textSanitizer: (String) -> String
) {

    companion object {
        private const val TAG = "SherpaOnnxTtsController"
        private const val MAX_PREFETCH_CACHE_SIZE = 2
    }

    private data class SynthesizedAudio(
        val samples: FloatArray,
        val sampleRate: Int
    )

    private val synthExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SherpaOnnx-SynthThread").apply {
            priority = Thread.NORM_PRIORITY
        }
    }
    private val synthDispatcher = synthExecutor.asCoroutineDispatcher()
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val synthMutex = Mutex()

    private var offlineTts: OfflineTts? = null
    @Volatile
    private var loadedVoiceId: String? = null
    val currentLoadedVoiceId: String?
        get() = loadedVoiceId

    private val prefetchCache = ConcurrentHashMap<String, SynthesizedAudio>()
    private val utteranceGenCounter = AtomicLong(0L)
    private val prefetchGenCounter = AtomicLong(0L)
    @Volatile
    private var activeUtteranceGen: Long = 0L
    @Volatile
    private var activePrefetchGen: Long = 0L
    @Volatile
    private var activeAudioTrack: AudioTrack? = null
    private var activeSpeakJob: Job? = null
    private var activePrefetchJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var onSentenceStart: ((Int) -> Unit)? = null
    var onSentenceDone: ((Int) -> Unit)? = null
    var onError: ((Int, String) -> Unit)? = null
    var onWordRange: ((sentenceIndex: Int, start: Int, end: Int) -> Unit)? = null
    var onReadyForPrefetchNext: ((currentSentenceIndex: Int, voiceId: String, speed: Float) -> Unit)? = null

    /**
     * Speaks [rawText] using the downloaded AI model [voiceId].
     * Automatically uses pre-fetched audio from RAM if sentence [sentenceIndex] was pre-synthesized.
     */
    fun speakSentence(
        sentenceIndex: Int,
        rawText: String,
        voiceId: String,
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        activeSpeakJob?.cancel()
        activeSpeakJob = null
        stopActiveAudioOnly()
        val gen = utteranceGenCounter.incrementAndGet()
        activeUtteranceGen = gen

        val sanitized = textSanitizer(rawText).trim()
        if (sanitized.isBlank() || sanitized.none { it.isLetter() }) {
            onSentenceStart?.invoke(sentenceIndex)
            controllerScope.launch {
                kotlinx.coroutines.delay(300L)
                if (activeUtteranceGen == gen) {
                    onSentenceDone?.invoke(sentenceIndex)
                }
            }
            return
        }

        val clampedSpeed = speed.coerceIn(0.5f, 2.5f)
        val clampedPitch = pitch.coerceIn(0.6f, 1.6f)
        val cacheKey = buildCacheKey(voiceId, clampedSpeed, sanitized)

        activeSpeakJob = controllerScope.launch {
            try {
                // 1. Check RAM prefetch cache first; otherwise synthesize on single-thread ONNX dispatcher
                // (synthesizeInternal also re-checks prefetchCache inside synthMutex so an in-flight
                // prefetch that finishes while we wait on synthMutex is reused without double synthesis!)
                val audio = prefetchCache.remove(cacheKey) ?: synthesizeInternal(
                    voiceId = voiceId,
                    sanitizedText = sanitized,
                    speed = clampedSpeed,
                    cacheKey = cacheKey,
                    isCancelledCheck = { activeUtteranceGen != gen }
                ) ?: return@launch

                ensureActive()
                if (activeUtteranceGen != gen) return@launch

                // 2. As soon as current sentence is synthesized and about to play on AudioTrack,
                // trigger pre-fetching of sentence N + 1 on the background ONNX thread!
                if (sentenceIndex >= 0) {
                    onReadyForPrefetchNext?.invoke(sentenceIndex, voiceId, clampedSpeed)
                }

                // 3. Stream PCM samples through AudioTrack while emitting word highlight ranges
                playPcmSamples(
                    sentenceIndex = sentenceIndex,
                    originalText = rawText,
                    audio = audio,
                    pitch = clampedPitch,
                    gen = gen
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Error in AI TTS speakSentence($sentenceIndex, $voiceId)", e)
                _isSpeaking.value = false
                if (activeUtteranceGen == gen) {
                    onError?.invoke(sentenceIndex, e.message ?: "Lỗi tổng hợp giọng AI")
                }
            }
        }
    }

    /**
     * Pre-synthesizes the upcoming sentence (`N + 1`) in the background while sentence `N` is
     * playing through `AudioTrack`, eliminating inter-sentence synthesis latency.
     */
    fun prefetchSentence(
        sentenceIndex: Int,
        rawText: String,
        voiceId: String,
        speed: Float
    ) {
        val sanitized = textSanitizer(rawText).trim()
        if (sanitized.isBlank() || sanitized.none { it.isLetter() }) return

        val clampedSpeed = speed.coerceIn(0.5f, 2.5f)
        val cacheKey = buildCacheKey(voiceId, clampedSpeed, sanitized)
        if (prefetchCache.containsKey(cacheKey)) return

        activePrefetchJob?.cancel()
        val pfGen = prefetchGenCounter.incrementAndGet()
        activePrefetchGen = pfGen
        activePrefetchJob = controllerScope.launch {
            runCatching {
                val audio = synthesizeInternal(
                    voiceId = voiceId,
                    sanitizedText = sanitized,
                    speed = clampedSpeed,
                    cacheKey = cacheKey,
                    isCancelledCheck = { activePrefetchGen != pfGen },
                    storeInPrefetchCache = true
                )
                if (audio != null && audio.samples.isNotEmpty()) {
                    Log.d(TAG, "Prefetched sentence $sentenceIndex (${audio.samples.size} samples) into RAM")
                }
            }
        }
    }

    private suspend fun synthesizeInternal(
        voiceId: String,
        sanitizedText: String,
        speed: Float,
        cacheKey: String,
        isCancelledCheck: () -> Boolean,
        storeInPrefetchCache: Boolean = false
    ): SynthesizedAudio? = withContext(synthDispatcher) {
        synthMutex.withLock {
            if (isCancelledCheck()) return@withLock null
            // Single-flight deduplication: if an in-flight prefetch just completed for this exact cacheKey,
            // return it immediately without running ONNX inference a second time!
            if (!storeInPrefetchCache) {
                prefetchCache.remove(cacheKey)?.let { return@withLock it }
            } else if (prefetchCache.containsKey(cacheKey)) {
                return@withLock prefetchCache[cacheKey]
            }

            val engine = ensureModelLoadedLocked(voiceId) ?: return@withLock null
            if (isCancelledCheck()) return@withLock null

            val normalizedForVits = ensureTerminalPunctuation(sanitizedText)
            val generated = engine.generate(
                text = normalizedForVits,
                sid = 0,
                speed = speed
            )
            if (generated.samples.isEmpty()) {
                null
            } else {
                val result = SynthesizedAudio(
                    samples = generated.samples,
                    sampleRate = generated.sampleRate.coerceAtLeast(16000)
                )
                if (storeInPrefetchCache && !isCancelledCheck()) {
                    if (prefetchCache.size >= MAX_PREFETCH_CACHE_SIZE) {
                        prefetchCache.keys.firstOrNull()?.let { prefetchCache.remove(it) }
                    }
                    prefetchCache[cacheKey] = result
                }
                result
            }
        }
    }

    /**
     * Ensures the input text ends with a sentence-ending punctuation mark so `espeak-ng` /
     * `piper-phonemize` flushes the final phoneme clause completely without dropping trailing words.
     */
    private fun ensureTerminalPunctuation(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed
        val lastChar = trimmed.last()
        return if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
            "$trimmed "
        } else {
            "$trimmed. "
        }
    }

    private suspend fun ensureModelLoadedLocked(voiceId: String): OfflineTts? {
        if (loadedVoiceId == voiceId && offlineTts != null) {
            return offlineTts
        }

        // Release previously loaded model first so RAM never holds 2 ONNX models simultaneously
        releaseNativeTtsLocked()

        if (!modelManager.isModelInstalled(voiceId)) {
            Log.e(TAG, "AI Model file missing or failed integrity check for $voiceId")
            return null
        }
        val modelFile = modelManager.getModelFile(voiceId)

        val (espeakDir, tokensFile) = modelManager.ensureSharedAssetsReady().getOrElse { err ->
            Log.e(TAG, "Failed to prepare shared espeak-ng-data/tokens.txt", err)
            return null
        }

        return runCatching {
            val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelFile.absolutePath,
                lexicon = "",
                tokens = tokensFile.absolutePath,
                dataDir = espeakDir.absolutePath,
                dictDir = "",
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )
            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = numThreads,
                debug = false,
                provider = "cpu"
            )
            val config = OfflineTtsConfig(
                model = modelConfig,
                ruleFsts = "",
                ruleFars = "",
                maxNumSentences = 1,
                silenceScale = 0.2f
            )
            Log.i(TAG, "Loading Sherpa-ONNX model '$voiceId' (${modelFile.length() / 1048576} MB) with $numThreads threads...")
            val ttsInstance = OfflineTts(assetManager = null, config = config)
            offlineTts = ttsInstance
            loadedVoiceId = voiceId
            prefetchCache.clear()
            ttsInstance
        }.onFailure { err ->
            Log.e(TAG, "Failed to instantiate OfflineTts for $voiceId", err)
        }.getOrNull()
    }

    private suspend fun playPcmSamples(
        sentenceIndex: Int,
        originalText: String,
        audio: SynthesizedAudio,
        pitch: Float,
        gen: Long
    ) = withContext(Dispatchers.IO) {
        val samples = audio.samples
        val sampleRate = audio.sampleRate
        val totalSamples = samples.size
        if (totalSamples == 0) {
            if (activeUtteranceGen == gen) {
                onSentenceDone?.invoke(sentenceIndex)
            }
            return@withContext
        }

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(sampleRate / 5)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeAudioTrack = track

        try {
            if (kotlin.math.abs(pitch - 1.0f) > 0.03f) {
                runCatching {
                    track.playbackParams = PlaybackParams()
                        .setPitch(pitch)
                        .setSpeed(1.0f)
                }
            }

            track.play()
            _isSpeaking.value = true
            if (activeUtteranceGen == gen) {
                onSentenceStart?.invoke(sentenceIndex)
            }

            // Prepare proportional word spans for smooth word-level highlighting
            val wordSpans = if (sentenceIndex >= 0) {
                VietnameseTextHighlighter.getWordSpans(originalText)
            } else {
                emptyList()
            }
            val totalWordChars = wordSpans.sumOf { it.word.length.coerceAtLeast(1) }.coerceAtLeast(1)
            val wordSampleThresholds = IntArray(wordSpans.size)
            var cumulativeChars = 0
            for (i in wordSpans.indices) {
                wordSampleThresholds[i] = ((cumulativeChars.toFloat() / totalWordChars) * totalSamples).toInt()
                cumulativeChars += wordSpans[i].word.length.coerceAtLeast(1)
            }
            var currentWordIdx = -1

            // Write in ~40ms chunks so stop/pause interrupts within 40ms
            val chunkSamples = (sampleRate * 0.04f).toInt().coerceAtLeast(512)
            var offset = 0

            while (offset < totalSamples && activeUtteranceGen == gen) {
                ensureActive()

                if (wordSpans.isNotEmpty()) {
                    var nextWord = currentWordIdx
                    while (nextWord + 1 < wordSpans.size && offset >= wordSampleThresholds[nextWord + 1]) {
                        nextWord++
                    }
                    if (nextWord != currentWordIdx && nextWord in wordSpans.indices) {
                        currentWordIdx = nextWord
                        val span = wordSpans[currentWordIdx]
                        onWordRange?.invoke(sentenceIndex, span.start, span.end)
                    }
                }

                val toWrite = minOf(chunkSamples, totalSamples - offset)
                val written = track.write(samples, offset, toWrite, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }

            // CRITICAL FIX: `track.write` returns as soon as the final PCM samples enter Android's
            // AudioTrack / AudioFlinger ring buffer (~350-600ms before the speaker finishes playing them).
            // If playback completed normally, push a short silence tail and wait until the hardware
            // playback head finishes playing all `totalSamples` so the last words are never cut off!
            if (offset >= totalSamples && activeUtteranceGen == gen) {
                val tailSilenceSamples = (sampleRate * 0.45f).toInt().coerceAtLeast(2048)
                val silenceChunk = FloatArray(chunkSamples)
                var silenceWritten = 0
                while (silenceWritten < tailSilenceSamples && activeUtteranceGen == gen) {
                    ensureActive()
                    val toWrite = minOf(chunkSamples, tailSilenceSamples - silenceWritten)
                    val written = track.write(silenceChunk, 0, toWrite, AudioTrack.WRITE_BLOCKING)
                    if (written <= 0) break
                    silenceWritten += written
                }

                // Wait for AudioTrack hardware playbackHeadPosition to reach totalSamples (bounded timeout)
                val targetFrames = (totalSamples / pitch.coerceIn(0.6f, 1.6f)).toInt()
                var waitedMs = 0L
                val maxWaitMs = 900L
                while (activeUtteranceGen == gen && waitedMs < maxWaitMs) {
                    ensureActive()
                    val headPos = runCatching { track.playbackHeadPosition }.getOrDefault(targetFrames)
                    if (headPos >= targetFrames) break
                    kotlinx.coroutines.delay(25L)
                    waitedMs += 25L
                }

                // Allow hardware DAC / mixer pipeline to finish outputting the final phonemes
                if (activeUtteranceGen == gen) {
                    kotlinx.coroutines.delay(180L)
                }
            }
        } finally {
            _isSpeaking.value = false
            runCatching {
                if (activeUtteranceGen == gen) {
                    track.stop()
                } else {
                    track.pause()
                    track.flush()
                }
                track.release()
            }
            if (activeAudioTrack === track) {
                activeAudioTrack = null
            }
        }

        if (activeUtteranceGen == gen) {
            onSentenceDone?.invoke(sentenceIndex)
        }
    }

    /**
     * Stops current `AudioTrack` playback and cancels in-flight utterance jobs without unloading
     * the ONNX model from RAM (used for fast pause/resume or sentence skipping).
     */
    fun stop() {
        activeUtteranceGen = utteranceGenCounter.incrementAndGet()
        activePrefetchGen = prefetchGenCounter.incrementAndGet()
        activeSpeakJob?.cancel()
        activeSpeakJob = null
        activePrefetchJob?.cancel()
        activePrefetchJob = null
        stopActiveAudioOnly()
    }

    private fun stopActiveAudioOnly() {
        _isSpeaking.value = false
        val track = activeAudioTrack
        if (track != null) {
            activeAudioTrack = null
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
        }
    }

    fun clearPrefetchCache() {
        activePrefetchGen = prefetchGenCounter.incrementAndGet()
        activePrefetchJob?.cancel()
        activePrefetchJob = null
        prefetchCache.clear()
    }

    /**
     * Releases the native `OfflineTts` ONNX model from RAM and clears prefetch buffers.
     * Call this when switching to a System TTS voice or deleting an AI voice model.
     */
    fun releaseModel() {
        stop()
        prefetchCache.clear()
        controllerScope.launch(synthDispatcher) {
            synthMutex.withLock {
                releaseNativeTtsLocked()
            }
        }
    }

    private fun releaseNativeTtsLocked() {
        val current = offlineTts
        offlineTts = null
        val prevId = loadedVoiceId
        loadedVoiceId = null
        if (current != null) {
            runCatching {
                Log.i(TAG, "Releasing Sherpa-ONNX native model '$prevId' from RAM")
                current.release()
            }.onFailure { e ->
                Log.w(TAG, "Error releasing OfflineTts", e)
            }
        }
    }

    fun shutdown() {
        stop()
        prefetchCache.clear()
        runCatching {
            kotlinx.coroutines.runBlocking(synthDispatcher) {
                synthMutex.withLock {
                    releaseNativeTtsLocked()
                }
            }
            synthExecutor.shutdown()
        }
        controllerScope.cancel()
    }

    private fun buildCacheKey(voiceId: String, speed: Float, sanitizedText: String): String {
        val speedKey = (speed * 20f).roundToInt()
        return "$voiceId|$speedKey|${sanitizedText.hashCode()}"
    }
}
