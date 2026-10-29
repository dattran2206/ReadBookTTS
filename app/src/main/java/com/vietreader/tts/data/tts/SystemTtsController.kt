package com.vietreader.tts.data.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.vietreader.tts.domain.model.PronunciationRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Controller wrapping Android's TextToSpeech engine with support for Vietnamese (vi-VN),
 * sentence-by-sentence utterance callbacks, pitch/speed adjustments, 1-to-1 system voice mapping,
 * and custom pronunciation dictionary rules.
 */
class SystemTtsController(
    private val context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SystemTtsController"
        private const val UTTERANCE_PREFIX = "sentence_"
        val VIETNAMESE_LOCALE = Locale("vi", "VN")
    }

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    var onSentenceStart: ((Int) -> Unit)? = null
    var onSentenceDone: ((Int) -> Unit)? = null
    var onError: ((Int, String) -> Unit)? = null
    var onWordRange: ((sentenceIndex: Int, start: Int, end: Int) -> Unit)? = null
    var onVoicesDiscovered: ((List<Voice>) -> Unit)? = null

    private data class CompiledPronunciationRule(
        val regex: Regex,
        val safeReplacement: String
    )

    private var currentSpeed: Float = 1.0f
    private var currentPitch: Float = 1.0f
    private var activeVoiceName: String = "Giọng hệ thống 1"
    private var pendingSpeakAction: (() -> Unit)? = null
    private val utteranceGenCounter = AtomicLong(0L)
    @Volatile
    private var activeUtteranceGen: Long = 0L
    @Volatile
    private var currentSpeakingIndex: Int = -1
    @Volatile
    private var isCancelledOrStopped: Boolean = false
    @Volatile
    private var compiledRules: List<CompiledPronunciationRule> = emptyList()
    @Volatile
    var isCurrentUtteranceLengthModified: Boolean = false
        private set
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val result = engine.setLanguage(VIETNAMESE_LOCALE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Vietnamese (vi-VN) not directly supported, trying generic 'vi' locale")
                val fallbackResult = engine.setLanguage(Locale("vi"))
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Generic 'vi' not supported, falling back to default device locale: ${Locale.getDefault()}")
                    engine.setLanguage(Locale.getDefault())
                }
            }

            // Setup UtteranceProgressListener
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val (index, genId) = parseUtteranceId(utteranceId) ?: return
                    if (!isCancelledOrStopped && genId == activeUtteranceGen && index == currentSpeakingIndex) {
                        _isSpeaking.value = true
                        onSentenceStart?.invoke(index)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val (index, genId) = parseUtteranceId(utteranceId) ?: return
                    if (!isCancelledOrStopped && genId == activeUtteranceGen && index == currentSpeakingIndex) {
                        _isSpeaking.value = false
                        currentSpeakingIndex = -1
                        onSentenceDone?.invoke(index)
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    val (index, genId) = parseUtteranceId(utteranceId) ?: return
                    if (!isCancelledOrStopped && genId == activeUtteranceGen && index == currentSpeakingIndex) {
                        onWordRange?.invoke(index, start, end)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val (index, genId) = parseUtteranceId(utteranceId) ?: return
                    if (!isCancelledOrStopped && genId == activeUtteranceGen && index == currentSpeakingIndex) {
                        _isSpeaking.value = false
                        currentSpeakingIndex = -1
                        onError?.invoke(index, "Lỗi phát âm câu $index")
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val (index, genId) = parseUtteranceId(utteranceId) ?: return
                    if (!isCancelledOrStopped && genId == activeUtteranceGen && index == currentSpeakingIndex) {
                        _isSpeaking.value = false
                        currentSpeakingIndex = -1
                        val errorMsg = when (errorCode) {
                            TextToSpeech.ERROR_SYNTHESIS -> "Lỗi tổng hợp âm thanh (ERROR_SYNTHESIS)"
                            TextToSpeech.ERROR_SERVICE -> "Lỗi dịch vụ TTS (ERROR_SERVICE)"
                            TextToSpeech.ERROR_OUTPUT -> "Lỗi đầu ra âm thanh (ERROR_OUTPUT)"
                            TextToSpeech.ERROR_NETWORK -> "Lỗi kết nối mạng (ERROR_NETWORK)"
                            TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Hết thời gian chờ mạng (ERROR_NETWORK_TIMEOUT)"
                            TextToSpeech.ERROR_INVALID_REQUEST -> "Yêu cầu không hợp lệ (ERROR_INVALID_REQUEST)"
                            TextToSpeech.ERROR_NOT_INSTALLED_YET -> "Gói giọng nói chưa được cài đặt (ERROR_NOT_INSTALLED_YET)"
                            else -> "Mã lỗi TTS: $errorCode"
                        }
                        Log.e(TAG, "TTS Error: $errorMsg for utterance: $utteranceId")
                        onError?.invoke(index, errorMsg)
                    }
                }
            })

            // Cache available voices
            try {
                val allVoices = engine.voices.orEmpty()
                val viVoices = allVoices.filter { voice ->
                    voice.locale.language.equals("vi", ignoreCase = true)
                }
                val voices = if (viVoices.isNotEmpty()) {
                    viVoices.sortedBy { it.name }
                } else {
                    allVoices.filter { voice ->
                        voice.locale.language.equals(Locale.getDefault().language, ignoreCase = true)
                    }.sortedBy { it.name }
                }
                _availableVoices.value = voices
                onVoicesDiscovered?.invoke(voices)
                Log.d(TAG, "Discovered ${voices.size} TTS voices matching Vietnamese / device locale")
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch voices list", e)
            }

            // Apply current speed, pitch & active voice
            setVoiceByName(activeVoiceName)
            engine.setSpeechRate(currentSpeed)
            engine.setPitch(currentPitch)

            _isReady.value = true
            Log.i(TAG, "System TextToSpeech initialized successfully with language: ${engine.voice?.locale ?: engine.language}")

            // Execute pending action if any
            pendingSpeakAction?.invoke()
            pendingSpeakAction = null
        } else {
            Log.e(TAG, "TextToSpeech init failed with status: $status")
            _isReady.value = false
        }
    }

    fun setPronunciationRules(rules: List<PronunciationRule>) {
        compiledRules = rules.mapNotNull { rule ->
            val word = rule.originalWord.trim()
            val replacement = rule.spokenReplacement.trim()
            if (word.isNotEmpty() && replacement.isNotEmpty()) {
                val escapedWord = Regex.escape(word)
                val regex = Regex("(?<![\\p{L}\\p{N}])$escapedWord(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
                CompiledPronunciationRule(
                    regex = regex,
                    safeReplacement = Regex.escapeReplacement(replacement)
                )
            } else {
                null
            }
        }
    }

    /**
     * Sanitizes speech text by replacing decorative, non-letter characters, noisy punctuation,
     * and symbol chains with spaces.
     * Preserves exact character count and indices so word highlight spans stay perfectly aligned.
     */
    fun sanitizeTextForSpeech(raw: String): String {
        val chars = raw.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            when {
                // Non-vocal symbols, bullets, geometric markers, brackets, code symbols
                c in "*_#~`=|+^@<>{}\\[\\]/\\•·▪▫■□▲▼►◄◆◇★☆✓✔✕✖" -> {
                    chars[i] = ' '
                }
                // Sequences or isolated dashes/hyphens (keep single hyphen in words like Wi-Fi or Covid-19)
                c == '-' || c == '—' || c == '–' -> {
                    val prev = chars.getOrNull(i - 1)
                    val next = chars.getOrNull(i + 1)
                    val isWordInternal = (prev != null && prev.isLetter()) && (next != null && next.isLetter())
                    if (!isWordInternal) {
                        chars[i] = ' '
                    }
                }
                // Excessive punctuation like repeated dots (> 3)
                c == '.' || c == '!' || c == '?' -> {
                    val prev = chars.getOrNull(i - 1)
                    if (prev == c && i >= 3 && chars.getOrNull(i - 2) == c && chars.getOrNull(i - 3) == c) {
                        chars[i] = ' '
                    }
                }
            }
        }
        return String(chars)
    }

    /**
     * Applies user-configured custom pronunciation rules (whole-word, case-insensitive)
     * using pre-compiled regexes and escaped replacement literals.
     */
    fun applyPronunciationRules(text: String): String {
        val rules = compiledRules
        if (rules.isEmpty()) return text
        var result = text
        for (rule in rules) {
            result = rule.regex.replace(result, rule.safeReplacement)
        }
        return result
    }

    /**
     * Speaks the sentence at [sentenceIndex] with the given [text].
     * If TTS is not yet ready, queues the request to be executed once initialized.
     */
    fun speakSentence(
        sentenceIndex: Int,
        text: String,
        speed: Float = currentSpeed,
        pitch: Float = currentPitch
    ) {
        val sanitizedBase = sanitizeTextForSpeech(text)
        val sanitized = applyPronunciationRules(sanitizedBase)
        isCurrentUtteranceLengthModified = sanitized.length != sanitizedBase.length
        val gen = utteranceGenCounter.incrementAndGet()
        activeUtteranceGen = gen

        if (sanitized.isBlank() || sanitized.none { it.isLetter() }) {
            // Paragraph has no readable words (e.g. decorative divider "-------------")
            currentSpeakingIndex = sentenceIndex
            isCancelledOrStopped = false
            onSentenceStart?.invoke(sentenceIndex)
            mainHandler.postDelayed({
                if (activeUtteranceGen == gen && currentSpeakingIndex == sentenceIndex && !isCancelledOrStopped) {
                    onSentenceDone?.invoke(sentenceIndex)
                }
            }, 350)
            return
        }

        currentSpeakingIndex = sentenceIndex
        isCancelledOrStopped = false

        if (!_isReady.value || tts == null) {
            Log.d(TAG, "TTS not ready yet, queuing sentence $sentenceIndex")
            pendingSpeakAction = {
                speakSentence(sentenceIndex, text, speed, pitch)
            }
            return
        }

        val engine = tts ?: return

        currentSpeed = speed.coerceIn(0.5f, 2.5f)
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        engine.setSpeechRate(currentSpeed)
        engine.setPitch(currentPitch)

        val utteranceId = "${UTTERANCE_PREFIX}${sentenceIndex}_gen_${gen}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val speakResult = engine.speak(
            sanitized,
            TextToSpeech.QUEUE_FLUSH,
            params,
            utteranceId
        )

        if (speakResult != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Failed to queue speak for utterance $utteranceId, code: $speakResult")
        }
    }

    /**
     * Stops current speech playback immediately.
     */
    fun stop() {
        pendingSpeakAction = null
        isCancelledOrStopped = true
        activeUtteranceGen = utteranceGenCounter.incrementAndGet()
        currentSpeakingIndex = -1
        mainHandler.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun setSpeechRate(rate: Float) {
        currentSpeed = rate.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(currentSpeed)
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(currentPitch)
    }

    /**
     * Maps [voiceName] (or `systemVoiceName` / variant key) 1-to-1 to the corresponding
     * Android `Voice` object installed on the device.
     */
    fun setVoiceByName(voiceName: String): Boolean {
        activeVoiceName = voiceName
        val engine = tts ?: return false
        val voices = _availableVoices.value
        if (voices.isEmpty()) return false

        val lower = voiceName.lowercase().trim()
        val targetVariantKey = when {
            lower.contains("gft") || lower.endsWith(" 1") || lower.contains(" i ") || lower.endsWith(" i") -> "-gft"
            lower.contains("vie") || lower.endsWith(" 2") || lower.contains(" ii ") || lower.endsWith(" ii") -> "-vie"
            lower.contains("vid") || lower.endsWith(" 3") || lower.contains(" iii ") || lower.endsWith(" iii") -> "-vid"
            lower.contains("vif") || lower.endsWith(" 4") || lower.contains(" iv ") || lower.endsWith(" iv") -> "-vif"
            lower.contains("vig") || lower.endsWith(" 5") || lower.contains(" v ") || lower.endsWith(" v") -> "-vig"
            else -> null
        }

        val localPool = voices.filter { !it.isNetworkConnectionRequired }.ifEmpty { voices }
        val selectedSystemVoice = when {
            targetVariantKey != null -> {
                localPool.find { it.name.contains(targetVariantKey, ignoreCase = true) }
                    ?: voices.find { it.name.contains(targetVariantKey, ignoreCase = true) }
                    ?: localPool.first()
            }
            else -> {
                localPool.find {
                    it.name.equals(voiceName, ignoreCase = true) ||
                        it.name.contains(voiceName, ignoreCase = true) ||
                        voiceName.contains(it.name, ignoreCase = true)
                } ?: localPool.first()
            }
        }

        return try {
            engine.voice = selectedSystemVoice
            Log.d(TAG, "Mapped voice '$voiceName' -> Android Voice '${selectedSystemVoice.name}'")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set voice $voiceName", e)
            false
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.setOnUtteranceProgressListener(null)
            tts?.shutdown()
            tts = null
            _isReady.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }

    private fun parseUtteranceId(utteranceId: String?): Pair<Int, Long>? {
        if (utteranceId == null || !utteranceId.startsWith(UTTERANCE_PREFIX)) return null
        val payload = utteranceId.removePrefix(UTTERANCE_PREFIX)
        val sentencePart = payload.substringBefore("_gen_")
        val genPart = payload.substringAfter("_gen_", "")
        val index = sentencePart.toIntOrNull() ?: return null
        val gen = genPart.toLongOrNull() ?: activeUtteranceGen
        return index to gen
    }
}
