package com.vietreader.tts.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.vietreader.tts.domain.model.AmbientIconType
import com.vietreader.tts.domain.model.AmbientTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Manages looping background ambient music mixed alongside TTS book reading,
 * as well as built-in relaxing audio synthesis and custom audio file imports.
 */
class BackgroundMusicController(private val context: Context) {

    companion object {
        private const val TAG = "BackgroundMusicCtrl"
        private const val SAMPLE_RATE = 22050
        private const val ASSETS_AMBIENT_FOLDER = "ambient_music"
        private const val ASSET_PREFIX = "asset://"
        private val SUPPORTED_AUDIO_EXTS = setOf("mp3", "wav", "m4a", "ogg", "aac", "flac")
    }

    private val rootDir = File(context.filesDir, "ambient_music").apply { mkdirs() }
    private val builtInDir = File(rootDir, "builtin").apply { mkdirs() }
    private val customDir = File(rootDir, "custom").apply { mkdirs() }

    private val playerLock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var currentLoadedPath: String? = null
    private var currentVolumePercent: Int = 25

    fun getBuiltInTracks(): List<AmbientTrack> {
        val assetTracks = discoverAssetAmbientTracks()
        if (assetTracks.isNotEmpty()) {
            return assetTracks
        }
        return listOf(
            AmbientTrack(
                id = "builtin_piano",
                title = "Giai điệu Tĩnh tâm",
                subtitle = "Hòa âm Ambient ấm áp • Đọc sách đêm",
                category = "Tích hợp sẵn",
                durationLabel = "Lặp vô tận",
                filePath = File(builtInDir, "builtin_piano.wav").absolutePath,
                isBuiltIn = true,
                iconType = AmbientIconType.PIANO
            ),
            AmbientTrack(
                id = "builtin_rain",
                title = "Mưa đêm hiên nhà",
                subtitle = "Tiếng mưa rơi êm dịu • Tập trung sâu",
                category = "Tích hợp sẵn",
                durationLabel = "Lặp vô tận",
                filePath = File(builtInDir, "builtin_rain.wav").absolutePath,
                isBuiltIn = true,
                iconType = AmbientIconType.RAIN
            ),
            AmbientTrack(
                id = "builtin_stream",
                title = "Suối rừng tĩnh lặng",
                subtitle = "Âm thanh thiên nhiên • Thư giãn tâm trí",
                category = "Tích hợp sẵn",
                durationLabel = "Lặp vô tận",
                filePath = File(builtInDir, "builtin_stream.wav").absolutePath,
                isBuiltIn = true,
                iconType = AmbientIconType.STREAM
            )
        )
    }

    private fun discoverAssetAmbientTracks(): List<AmbientTrack> {
        val assetEntries = runCatching {
            context.assets.list(ASSETS_AMBIENT_FOLDER)?.toList().orEmpty()
        }.getOrDefault(emptyList())

        val audioFiles = assetEntries
            .filter { fileName ->
                val ext = fileName.substringAfterLast('.', "").lowercase()
                ext in SUPPORTED_AUDIO_EXTS
            }
            .sorted()

        return audioFiles.map { fileName ->
            val baseName = fileName.substringBeforeLast('.')
            val safeIdKey = baseName.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
            val displayTitle = baseName
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .ifBlank { fileName }

            val lowerTitle = displayTitle.lowercase()
            val detectedIcon = when {
                lowerTitle.contains("mưa") || lowerTitle.contains("rain") -> AmbientIconType.RAIN
                lowerTitle.contains("suối") || lowerTitle.contains("stream") ||
                    lowerTitle.contains("nước") || lowerTitle.contains("biển") ||
                    lowerTitle.contains("rừng") || lowerTitle.contains("forest") ||
                    lowerTitle.contains("moon") || lowerTitle.contains("night") -> AmbientIconType.STREAM
                else -> AmbientIconType.PIANO
            }

            val assetRelPath = "$ASSETS_AMBIENT_FOLDER/$fileName"
            val (sizeLabel, durationLabel) = inspectAssetMetadata(assetRelPath)

            AmbientTrack(
                id = "asset_$safeIdKey",
                title = displayTitle,
                subtitle = if (sizeLabel.isNotBlank()) "Nhạc nền mặc định • $sizeLabel" else "Nhạc nền mặc định • Tích hợp sẵn",
                category = "Tích hợp sẵn",
                durationLabel = durationLabel,
                filePath = "$ASSET_PREFIX$assetRelPath",
                isBuiltIn = true,
                iconType = detectedIcon
            )
        }
    }

    private fun inspectAssetMetadata(assetRelPath: String): Pair<String, String> {
        var sizeBytes = 0L
        var durationSec = 0
        runCatching {
            context.assets.openFd(assetRelPath).use { afd ->
                sizeBytes = afd.length.coerceAtLeast(0L)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    durationSec = (durMs / 1000L).toInt().coerceAtLeast(0)
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }
        val sizeLabel = when {
            sizeBytes >= 1024L * 1024L -> "%.1f MB".format(sizeBytes / (1024f * 1024f))
            sizeBytes > 0L -> "${(sizeBytes / 1024L).coerceAtLeast(1L)} KB"
            else -> ""
        }
        val durationLabel = if (durationSec > 0) {
            val mins = durationSec / 60
            val secs = durationSec % 60
            "%02d:%02d • Lặp vô tận".format(mins, secs)
        } else {
            "Lặp vô tận"
        }
        return sizeLabel to durationLabel
    }

    suspend fun ensureBuiltInTracksGenerated() = withContext(Dispatchers.IO) {
        runCatching {
            val assetEntries = runCatching {
                context.assets.list(ASSETS_AMBIENT_FOLDER)?.toList().orEmpty()
            }.getOrDefault(emptyList())
            val hasAssetAudio = assetEntries.any {
                it.substringAfterLast('.', "").lowercase() in SUPPORTED_AUDIO_EXTS
            }
            if (hasAssetAudio) {
                return@runCatching
            }

            // Fallback: generate synthesized built-in tracks only if assets/ambient_music is empty
            val pianoFile = File(builtInDir, "builtin_piano.wav")
            if (!pianoFile.exists() || pianoFile.length() < 10_000L) {
                generateWarmAmbientChordsWav(pianoFile, durationSeconds = 16)
            }
            val rainFile = File(builtInDir, "builtin_rain.wav")
            if (!rainFile.exists() || rainFile.length() < 10_000L) {
                generateGentleRainWav(rainFile, durationSeconds = 12)
            }
            val streamFile = File(builtInDir, "builtin_stream.wav")
            if (!streamFile.exists() || streamFile.length() < 10_000L) {
                generateForestStreamWav(streamFile, durationSeconds = 12)
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed preparing built-in ambient tracks: ${e.message}")
        }
    }

    fun syncPlaybackState(
        shouldPlay: Boolean,
        track: AmbientTrack?,
        volumePercent: Int
    ) {
        synchronized(playerLock) {
            currentVolumePercent = volumePercent.coerceIn(0, 100)
            if (!shouldPlay || track == null) {
                pauseInternal()
                return
            }

            val sourceKey = track.filePath
            try {
                if (mediaPlayer == null || currentLoadedPath != sourceKey) {
                    releaseInternal()
                    val mp = MediaPlayer()
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )

                    if (sourceKey.startsWith(ASSET_PREFIX)) {
                        val assetRelPath = sourceKey.removePrefix(ASSET_PREFIX)
                        val configured = runCatching {
                            val afd = context.assets.openFd(assetRelPath)
                            try {
                                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                mp.isLooping = true
                                mp.prepare()
                            } finally {
                                afd.close()
                            }
                            true
                        }.getOrDefault(false)

                        if (!configured) {
                            val fallbackFile = getOrExtractAssetFallback(assetRelPath)
                            mp.reset()
                            mp.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            mp.setDataSource(fallbackFile.absolutePath)
                            mp.isLooping = true
                            mp.prepare()
                        }
                    } else {
                        val file = File(sourceKey)
                        if (!file.exists()) {
                            Log.w(TAG, "Ambient track file does not exist yet: $sourceKey")
                            mp.release()
                            return
                        }
                        mp.setDataSource(file.absolutePath)
                        mp.isLooping = true
                        mp.prepare()
                    }

                    val vol = computeScalarVolume(currentVolumePercent)
                    mp.setVolume(vol, vol)
                    mp.start()
                    mediaPlayer = mp
                    currentLoadedPath = sourceKey
                } else {
                    val mp = mediaPlayer ?: return
                    val vol = computeScalarVolume(currentVolumePercent)
                    mp.setVolume(vol, vol)
                    if (!mp.isPlaying) {
                        mp.start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing ambient track ${track.title}: ${e.message}")
                releaseInternal()
            }
        }
    }

    fun updateVolume(volumePercent: Int) {
        synchronized(playerLock) {
            currentVolumePercent = volumePercent.coerceIn(0, 100)
            val vol = computeScalarVolume(currentVolumePercent)
            runCatching {
                mediaPlayer?.setVolume(vol, vol)
            }
        }
    }

    fun pause() {
        synchronized(playerLock) {
            pauseInternal()
        }
    }

    fun release() {
        synchronized(playerLock) {
            releaseInternal()
        }
    }

    private fun pauseInternal() {
        runCatching {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
    }

    private fun releaseInternal() {
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        currentLoadedPath = null
    }

    private fun getOrExtractAssetFallback(assetRelPath: String): File {
        val fileName = assetRelPath.substringAfterLast('/')
        val destFile = File(builtInDir, "fallback_$fileName")
        if (!destFile.exists() || destFile.length() == 0L) {
            context.assets.open(assetRelPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            }
        }
        return destFile
    }

    /**
     * Converts 0..100% UI slider into a perceptually smooth scalar volume (0.0f..0.75f)
     * so background music never overpowers the spoken TTS voice even at higher settings.
     */
    private fun computeScalarVolume(percent: Int): Float {
        if (percent <= 0) return 0f
        val normalized = (percent.coerceIn(0, 100) / 100f)
        return (normalized.pow(1.6f) * 0.75f).coerceIn(0f, 0.75f)
    }

    suspend fun importCustomTrack(uri: Uri): Result<AmbientTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val rawName = queryDisplayName(uri) ?: "Nhạc nền_${System.currentTimeMillis()}"
            val cleanTitle = rawName.substringBeforeLast(".").trim().ifBlank { "Bản nhạc nền" }
            val ext = rawName.substringAfterLast(".", "mp3").lowercase()
                .takeIf { it in setOf("mp3", "wav", "m4a", "ogg", "aac", "flac") } ?: "mp3"

            val trackId = "custom_${System.currentTimeMillis()}"
            val destFile = File(customDir, "$trackId.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            } ?: error("Không thể đọc file âm thanh đã chọn")

            if (!destFile.exists() || destFile.length() < 512L) {
                destFile.delete()
                error("File âm thanh không hợp lệ hoặc bị trống")
            }

            val durationSec = extractDurationSeconds(destFile)
            val durationLabel = if (durationSec > 0) {
                val mins = durationSec / 60
                val secs = durationSec % 60
                "%02d:%02d • Lặp lại".format(mins, secs)
            } else {
                "Lặp vô tận"
            }

            val sizeKb = (destFile.length() / 1024L).coerceAtLeast(1L)
            val sizeLabel = if (sizeKb >= 1024L) {
                "%.1f MB".format(sizeKb / 1024f)
            } else {
                "$sizeKb KB"
            }

            AmbientTrack(
                id = trackId,
                title = cleanTitle,
                subtitle = "Nhạc từ thiết bị • $sizeLabel",
                category = "Nhạc của bạn",
                durationLabel = durationLabel,
                filePath = destFile.absolutePath,
                isBuiltIn = false,
                iconType = AmbientIconType.CUSTOM
            )
        }
    }

    fun deleteCustomTrackFile(track: AmbientTrack) {
        if (track.isBuiltIn) return
        synchronized(playerLock) {
            if (currentLoadedPath == track.filePath) {
                releaseInternal()
            }
        }
        runCatching {
            val f = File(track.filePath)
            if (f.exists()) f.delete()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    private fun extractDurationSeconds(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            (durMs / 1000L).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Generates a warm, relaxing 4-chord ambient Rhodes/pad progression (Fmaj7 -> Em7 -> Dm7 -> Cmaj7)
     * with seamless loop crossfading.
     */
    private fun generateWarmAmbientChordsWav(destFile: File, durationSeconds: Int) {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val samples = ShortArray(totalSamples)

        val chords = arrayOf(
            floatArrayOf(174.61f, 220.00f, 261.63f, 329.63f), // Fmaj7
            floatArrayOf(164.81f, 196.00f, 246.94f, 293.66f), // Em7
            floatArrayOf(146.83f, 174.61f, 220.00f, 261.63f), // Dm7
            floatArrayOf(130.81f, 164.81f, 196.00f, 246.94f)  // Cmaj7
        )
        val chordDurationSec = durationSeconds / chords.size.toFloat()

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val chordIdx = ((t / chordDurationSec).toInt()).coerceIn(0, chords.lastIndex)
            val localT = t - (chordIdx * chordDurationSec)
            val chord = chords[chordIdx]

            // Smooth fade-in and fade-out per chord so transitions are silky smooth
            val env = sin((localT / chordDurationSec) * PI).toFloat().pow(0.45f)
            val lfo = 0.90f + 0.10f * sin(2.0 * PI * 0.25 * t).toFloat()

            var signal = 0f
            for (freq in chord) {
                signal += sin(2.0 * PI * freq * t).toFloat() * 0.22f
                signal += sin(2.0 * PI * (freq * 2f) * t).toFloat() * 0.04f
            }
            val value = (signal * env * lfo * 0.55f).coerceIn(-0.95f, 0.95f)
            samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        writeWavFile(destFile, samples)
    }

    /**
     * Generates a warm, soothing night rain sound using filtered brown/pink noise + gentle pattering.
     */
    private fun generateGentleRainWav(destFile: File, durationSeconds: Int) {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val samples = ShortArray(totalSamples)
        val rng = Random(42L)
        var lp1 = 0f
        var lp2 = 0f

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val white = (rng.nextFloat() * 2f - 1f)
            // Two-pole low-pass filter for warm rain texture without harsh hiss
            lp1 += 0.12f * (white - lp1)
            lp2 += 0.08f * (lp1 - lp2)

            // Seamless loop envelope at start/end 0.5s
            val loopEnv = when {
                t < 0.5f -> t / 0.5f
                t > durationSeconds - 0.5f -> (durationSeconds - t) / 0.5f
                else -> 1f
            }.coerceIn(0f, 1f)

            val slowBreeze = 0.85f + 0.15f * cos(2.0 * PI * (t / durationSeconds.toDouble())).toFloat()
            val sampleVal = (lp2 * slowBreeze * loopEnv * 0.65f).coerceIn(-0.95f, 0.95f)
            samples[i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()
        }
        writeWavFile(destFile, samples)
    }

    /**
     * Generates a calm forest stream & warm harmonic drone.
     */
    private fun generateForestStreamWav(destFile: File, durationSeconds: Int) {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val samples = ShortArray(totalSamples)
        val rng = Random(108L)
        var band = 0f

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val white = (rng.nextFloat() * 2f - 1f)
            band += 0.05f * (white - band)

            val ripple = 0.80f + 0.20f * sin(2.0 * PI * 0.5 * t).toFloat()
            val warmDrone = (
                sin(2.0 * PI * 130.81 * t).toFloat() * 0.12f +
                    sin(2.0 * PI * 196.00 * t).toFloat() * 0.09f +
                    sin(2.0 * PI * 261.63 * t).toFloat() * 0.06f
                )

            val loopEnv = when {
                t < 0.5f -> t / 0.5f
                t > durationSeconds - 0.5f -> (durationSeconds - t) / 0.5f
                else -> 1f
            }.coerceIn(0f, 1f)

            val combined = ((band * ripple * 0.55f) + warmDrone) * loopEnv
            samples[i] = (combined.coerceIn(-0.95f, 0.95f) * Short.MAX_VALUE).toInt().toShort()
        }
        writeWavFile(destFile, samples)
    }

    private fun writeWavFile(destFile: File, pcmSamples: ShortArray) {
        val byteDataSize = pcmSamples.size * 2
        val totalDataLen = byteDataSize + 36
        val byteRate = SAMPLE_RATE * 2

        BufferedOutputStream(FileOutputStream(destFile)).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // Subchunk1Size (PCM)
            header.putShort(1) // AudioFormat = 1 (PCM)
            header.putShort(1) // NumChannels = 1 (Mono)
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(byteDataSize)
            out.write(header.array())

            val chunkBuf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmSamples) {
                if (!chunkBuf.hasRemaining()) {
                    out.write(chunkBuf.array(), 0, chunkBuf.position())
                    chunkBuf.clear()
                }
                chunkBuf.putShort(s)
            }
            if (chunkBuf.position() > 0) {
                out.write(chunkBuf.array(), 0, chunkBuf.position())
            }
            out.flush()
        }
    }
}
