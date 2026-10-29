package com.vietreader.tts.data.tts

import android.content.Context
import android.util.Log
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * Manages storage, extraction of shared `espeak-ng-data` + `tokens.txt`, real HTTP downloading
 * (with Pause/Resume & redirect handling from Hugging Face CDN), and file integrity validation
 * for Sherpa-ONNX Vietnamese VITS models.
 */
class AiVoiceModelManager(private val context: Context) {

    companion object {
        private const val TAG = "AiVoiceModelManager"
        private const val AI_VOICES_DIR_NAME = "ai_voices"
        private const val ESPEAK_DIR_NAME = "espeak-ng-data"
        private const val ESPEAK_MARKER_FILE = ".extracted_v2"
        private const val TOKENS_FILE_NAME = "tokens.txt"
        private const val MODEL_FILE_NAME = "model.onnx"
        private const val MODEL_PART_FILE_NAME = "model.onnx.part"
        private const val DEFAULT_EXPECTED_BYTES = 63_516_173L
    }

    private val extractMutex = Mutex()

    val baseDir: File
        get() = File(context.filesDir, AI_VOICES_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    val espeakDataDir: File
        get() = File(baseDir, ESPEAK_DIR_NAME)

    val sharedTokensFile: File
        get() = File(baseDir, TOKENS_FILE_NAME)

    fun getVoiceDir(voiceId: String): File {
        return File(baseDir, voiceId).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getModelFile(voiceId: String): File {
        return File(getVoiceDir(voiceId), MODEL_FILE_NAME)
    }

    fun getPartFile(voiceId: String): File {
        return File(getVoiceDir(voiceId), MODEL_PART_FILE_NAME)
    }

    /**
     * Checks whether the `.onnx` model for [voiceId] is completely downloaded and passes
     * size + ONNX Protobuf header integrity checks.
     * Automatically cleans up any corrupted `model.onnx` file so JNI never attempts to load it.
     */
    fun isModelInstalled(voiceId: String, minBytes: Long = 50_000_000L): Boolean {
        val modelFile = getModelFile(voiceId)
        if (!modelFile.exists() || !modelFile.isFile) return false
        if (!isValidOnnxModelFile(modelFile, minBytes)) {
            Log.w(TAG, "Deleting corrupted or incomplete ONNX model file: ${modelFile.absolutePath}")
            runCatching { modelFile.delete() }
            return false
        }
        return true
    }

    /**
     * Verifies that [file] meets [minBytes] and starts with a valid ONNX `ModelProto` protobuf tag
     * (`0x08` = field 1 `ir_version`, varint) rather than an HTML error page or zeroed block.
     */
    private fun isValidOnnxModelFile(file: File, minBytes: Long): Boolean {
        if (!file.exists() || !file.isFile || file.length() < minBytes) return false
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val firstByte = raf.read()
                val secondByte = raf.read()
                // Standard ONNX ModelProto begins with 0x08 (field 1: ir_version, wire type 0)
                // followed by a small positive IR version (1..20), never '<' (0x3C HTML) or 0x00
                firstByte == 0x08 && secondByte in 1..32
            }
        }.getOrDefault(false)
    }

    fun getInstalledAiVoiceIds(): Set<String> {
        return VoiceCatalog.defaultAiVoiceItems
            .filter { isModelInstalled(it.id, it.expectedMinBytes) }
            .map { it.id }
            .toSet()
    }

    fun getTotalDownloadedSizeMb(): Int {
        var totalBytes = 0L
        for (item in VoiceCatalog.defaultAiVoiceItems) {
            val file = getModelFile(item.id)
            if (file.exists()) {
                totalBytes += file.length()
            }
        }
        return (totalBytes / (1024.0 * 1024.0)).roundToInt()
    }

    /**
     * Ensures `espeak-ng-data` and `tokens.txt` are extracted from `assets/` into `filesDir/ai_voices/`.
     */
    suspend fun ensureSharedAssetsReady(): Result<Pair<File, File>> = withContext(Dispatchers.IO) {
        extractMutex.withLock {
            runCatching {
                // 1. Extract tokens.txt
                if (!sharedTokensFile.exists() || sharedTokensFile.length() < 100L) {
                    runCatching {
                        context.assets.open(TOKENS_FILE_NAME).use { input ->
                            FileOutputStream(sharedTokensFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }.onFailure {
                        downloadSmallFile(VoiceCatalog.SHARED_TOKENS_URL, sharedTokensFile)
                    }
                }

                // 2. Extract espeak-ng-data.zip
                val marker = File(espeakDataDir, ESPEAK_MARKER_FILE)
                val phontab = File(espeakDataDir, "phontab")
                if (!marker.exists() || !phontab.exists()) {
                    Log.i(TAG, "Extracting espeak-ng-data.zip from assets...")
                    if (espeakDataDir.exists()) {
                        espeakDataDir.deleteRecursively()
                    }
                    espeakDataDir.mkdirs()

                    context.assets.open("espeak-ng-data.zip").use { rawIn ->
                        ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                            var entry = zis.nextEntry
                            val buffer = ByteArray(32 * 1024)
                            while (entry != null) {
                                val rawName = entry.name.replace('\\', '/').trimStart('/')
                                val isDirEntry = entry.isDirectory || rawName.endsWith("/")
                                val cleanRelPath = rawName.trimEnd('/')
                                if (cleanRelPath.isNotEmpty()) {
                                    val relativePath = if (
                                        cleanRelPath == ESPEAK_DIR_NAME ||
                                        cleanRelPath.startsWith("$ESPEAK_DIR_NAME/")
                                    ) {
                                        cleanRelPath
                                    } else {
                                        "$ESPEAK_DIR_NAME/$cleanRelPath"
                                    }
                                    val outFile = File(baseDir, relativePath)
                                    if (isDirEntry) {
                                        if (outFile.exists() && !outFile.isDirectory) {
                                            outFile.delete()
                                        }
                                        outFile.mkdirs()
                                    } else {
                                        val parent = outFile.parentFile
                                        if (parent != null) {
                                            if (parent.exists() && !parent.isDirectory) {
                                                parent.delete()
                                            }
                                            parent.mkdirs()
                                        }
                                        if (outFile.exists() && outFile.isDirectory) {
                                            outFile.deleteRecursively()
                                        }
                                        FileOutputStream(outFile).use { fos ->
                                            var count: Int
                                            while (zis.read(buffer).also { count = it } != -1) {
                                                fos.write(buffer, 0, count)
                                            }
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                    marker.writeText("ok")
                    Log.i(TAG, "espeak-ng-data extracted successfully to ${espeakDataDir.absolutePath}")
                }

                espeakDataDir to sharedTokensFile
            }
        }
    }

    /**
     * Downloads the `.onnx` voice model for [voice] from Hugging Face with Pause/Resume support,
     * real-time progress reporting, and file integrity verification.
     */
    suspend fun downloadVoiceModel(
        voice: VoiceItem,
        isPausedProvider: () -> Boolean,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val downloadUrl = voice.modelDownloadUrl
                ?: error("Không tìm thấy đường dẫn tải cho giọng ${voice.name}")

            // Ensure shared espeak-ng-data and tokens.txt are ready first
            ensureSharedAssetsReady().getOrThrow()

            val targetFile = getModelFile(voice.id)
            if (isModelInstalled(voice.id, voice.expectedMinBytes)) {
                onProgress(100)
                return@runCatching targetFile
            }

            val partFile = getPartFile(voice.id)
            var existingBytes = if (partFile.exists()) partFile.length() else 0L

            val connection = openConnectionFollowingRedirects(downloadUrl, existingBytes)
            try {
                val responseCode = connection.responseCode
                val isResumeSupported = responseCode == HttpURLConnection.HTTP_PARTIAL
                if (!isResumeSupported) {
                    existingBytes = 0L
                    if (partFile.exists()) partFile.delete()
                }

                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    error("Máy chủ trả về mã lỗi HTTP $responseCode khi tải giọng ${voice.name}")
                }

                val contentLength = connection.contentLengthLong
                val totalBytes = when {
                    contentLength > 0L -> existingBytes + contentLength
                    else -> DEFAULT_EXPECTED_BYTES
                }

                var downloadedBytes = existingBytes
                var lastReportedProgress = ((downloadedBytes * 100L) / totalBytes)
                    .toInt()
                    .coerceIn(1, 99)
                onProgress(lastReportedProgress)

                RandomAccessFile(partFile, "rw").use { raf ->
                    if (existingBytes > 0L) {
                        raf.seek(existingBytes)
                    } else {
                        raf.setLength(0L)
                    }

                    BufferedInputStream(connection.inputStream, 64 * 1024).use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()

                            // Wait while download is paused by user
                            while (isPausedProvider()) {
                                coroutineContext.ensureActive()
                                delay(200L)
                            }

                            val read = input.read(buffer)
                            if (read == -1) break

                            raf.write(buffer, 0, read)
                            downloadedBytes += read

                            val currentProgress = ((downloadedBytes * 100L) / totalBytes)
                                .toInt()
                                .coerceIn(1, 99)
                            if (currentProgress != lastReportedProgress) {
                                lastReportedProgress = currentProgress
                                onProgress(currentProgress)
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            // Integrity check (size + ONNX Protobuf magic header) before committing model.onnx
            if (!isValidOnnxModelFile(partFile, voice.expectedMinBytes)) {
                val actualLen = if (partFile.exists()) partFile.length() else 0L
                runCatching { partFile.delete() }
                error("File mô hình tải về bị hỏng hoặc chưa hoàn chỉnh ($actualLen bytes)")
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = partFile.renameTo(targetFile)
            if (!renamed) {
                partFile.copyTo(targetFile, overwrite = true)
                partFile.delete()
            }

            onProgress(100)
            Log.i(TAG, "Successfully downloaded AI voice '${voice.name}' (${targetFile.length()} bytes)")
            targetFile
        }
    }

    fun deleteVoiceModel(voiceId: String): Boolean {
        return runCatching {
            val dir = getVoiceDir(voiceId)
            if (dir.exists()) {
                dir.deleteRecursively()
            } else {
                true
            }
        }.getOrDefault(false)
    }

    private fun openConnectionFollowingRedirects(
        urlStr: String,
        rangeStartBytes: Long
    ): HttpURLConnection {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Wordwave-Android-TTS/1.2")
                if (rangeStartBytes > 0L) {
                    setRequestProperty("Range", "bytes=$rangeStartBytes-")
                }
            }
            val code = conn.responseCode
            if (code in 301..308) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) break
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects++
            } else {
                return conn
            }
        }
        error("Quá nhiều lần chuyển hướng (redirect) khi kết nối tới $urlStr")
    }

    private fun downloadSmallFile(urlStr: String, targetFile: File) {
        val conn = openConnectionFollowingRedirects(urlStr, 0L)
        try {
            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
