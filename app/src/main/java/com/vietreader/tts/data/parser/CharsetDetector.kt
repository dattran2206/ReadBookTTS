package com.vietreader.tts.data.parser

import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Robust Charset Detector for Vietnamese text documents.
 * Correctly identifies UTF-8 (with or without BOM), UTF-16LE, UTF-16BE,
 * with graceful fallback to Windows-1258 (Vietnamese ANSI) and ISO-8859-1.
 */
object CharsetDetector {

    /**
     * Detects the most appropriate [Charset] for a text file by inspecting BOM markers
     * and performing strict UTF-8 validity checks on a sample buffer without boundary truncation.
     */
    fun detectCharset(file: File): Charset {
        if (!file.exists() || file.length() == 0L) return StandardCharsets.UTF_8

        val header = ByteArray(4)
        val bytesRead = FileInputStream(file).use { input ->
            input.read(header, 0, header.size.coerceAtMost(file.length().toInt()))
        }

        // 1. Check Byte Order Mark (BOM)
        if (bytesRead >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytesRead >= 2 && header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }
        if (bytesRead >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }

        // 2. Sample-based UTF-8 validation (up to 64KB)
        val sampleSize = file.length().coerceAtMost(64 * 1024L).toInt()
        val sampleBytes = ByteArray(sampleSize)
        FileInputStream(file).use { input ->
            input.read(sampleBytes, 0, sampleSize)
        }

        // Prevent boundary truncation of multi-byte UTF-8 character sequences
        var validSampleLength = sampleSize
        while (validSampleLength > 0 && (sampleBytes[validSampleLength - 1].toInt() and 0xC0) == 0x80) {
            validSampleLength--
        }
        if (validSampleLength > 0 && (sampleBytes[validSampleLength - 1].toInt() and 0x80) != 0) {
            validSampleLength--
        }

        val checkedBytes = if (validSampleLength < sampleSize) {
            sampleBytes.copyOf(validSampleLength)
        } else {
            sampleBytes
        }

        if (isValidUtf8(checkedBytes)) {
            return StandardCharsets.UTF_8
        }

        // 3. Fallback: Check if Windows-1258 (Vietnamese ANSI) is available, then ISO-8859-1
        return runCatching {
            Charset.forName("windows-1258")
        }.getOrElse {
            StandardCharsets.ISO_8859_1
        }
    }

    /**
     * Reads text from a file, automatically detecting the encoding and stripping any leading BOM character.
     */
    fun readText(file: File): String {
        val charset = detectCharset(file)
        var text = file.readText(charset)
        // Strip UTF-8 / UTF-16 BOM character if present
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1)
        }
        return text
    }

    /**
     * Returns an [InputStreamReader] configured with the detected charset, positioned right after any BOM.
     */
    fun openReader(file: File): InputStreamReader {
        val charset = detectCharset(file)
        val stream = FileInputStream(file)

        // Skip BOM bytes if present
        if (charset == StandardCharsets.UTF_8) {
            val mark = ByteArray(3)
            val read = stream.read(mark)
            if (!(read == 3 && mark[0] == 0xEF.toByte() && mark[1] == 0xBB.toByte() && mark[2] == 0xBF.toByte())) {
                stream.close()
                return InputStreamReader(FileInputStream(file), charset)
            }
        }
        return InputStreamReader(stream, charset)
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes))
            true
        } catch (_: Exception) {
            false
        }
    }
}
