package com.vietreader.tts.data.parser

import android.text.Html
import com.vietreader.tts.domain.model.TranscriptSentence
import kotlin.math.roundToInt

/**
 * Universal Vietnamese EPUB/TXT paragraph & sentence tokenizer.
 *
 * Handles:
 * - Calibre & Microsoft Word nested `<head>/<style>/<!--...-->` blocks
 * - Split Drop-Cap paragraphs (`<p>T</p><p>háng 4/2007...</p>` -> `"Tháng 4/2007..."`)
 * - Duplicate chapter heading removal at the top of chapters
 * - Balanced paragraph splitting for comfortable reading & synchronized highlighting
 */
object VietnameseSentenceTokenizer {

    private val HTML_COMMENT_REGEX = Regex("<!--[\\s\\S]*?-->")
    private val HEAD_REGEX = Regex("<head[^>]*>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE)
    private val STYLE_SCRIPT_REGEX = Regex("<(style|script)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE)
    private val CHAPTER_HEADER_BLOCK_REGEX = Regex(
        "<div[^>]*class=[\"'][^\"']*chapter-block[^\"']*[\"'][^>]*>[\\s\\S]*?</div>",
        RegexOption.IGNORE_CASE
    )
    private val HEADING_TAG_REGEX = Regex("<h[1-4][^>]*>([\\s\\S]*?)</h[1-4]>", RegexOption.IGNORE_CASE)
    private val BR_TAG_REGEX = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val ANY_TAG_REGEX = Regex("<[^>]+>")
    private val MULTI_SPACE_REGEX = Regex("[ \\t\\x0B\\f\\r]+")

    /**
     * Parses one or more concatenated EPUB chapter XHTML/HTML segments into a list of [TranscriptSentence]s
     * and returns the pair of (sentences, estimatedTotalDurationSeconds).
     */
    fun tokenizeHtmlChapter(
        rawHtml: String,
        chapterTitleToStrip: String = ""
    ): Pair<List<TranscriptSentence>, Int> {
        val cleanedHtml = rawHtml
            .replace(HTML_COMMENT_REGEX, "")
            .replace(HEAD_REGEX, "")
            .replace(STYLE_SCRIPT_REGEX, "")
            .replace(CHAPTER_HEADER_BLOCK_REGEX, "")
            .replace(HEADING_TAG_REGEX, "\n")
            .replace(BR_TAG_REGEX, "\n")

        val rawParagraphs = mutableListOf<String>()

        val pMatches = Regex("<p[^>]*>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE).findAll(cleanedHtml).toList()
        if (pMatches.isNotEmpty()) {
            for (match in pMatches) {
                val inner = cleanHtmlFragment(match.groupValues[1])
                if (inner.isNotBlank()) {
                    rawParagraphs.addAll(
                        inner.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    )
                }
            }
        } else {
            val plain = cleanHtmlFragment(cleanedHtml)
            rawParagraphs.addAll(
                plain.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            )
        }

        val mergedDropCaps = mergeSplitDropCaps(rawParagraphs)
        return buildTranscriptSentences(mergedDropCaps, chapterTitleToStrip)
    }

    /**
     * Parses plain text content of a chapter into [TranscriptSentence]s.
     */
    fun tokenizePlainTextChapter(
        rawText: String,
        chapterTitleToStrip: String = ""
    ): Pair<List<TranscriptSentence>, Int> {
        val paragraphs = rawText
            .replace("\r\n", "\n")
            .split(Regex("\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val mergedDropCaps = mergeSplitDropCaps(paragraphs)
        return buildTranscriptSentences(mergedDropCaps, chapterTitleToStrip)
    }

    /**
     * Merges Calibre / PRC-to-EPUB split drop-cap paragraphs where a single leading capital letter
     * is placed in its own `<p>` tag (e.g. `<p>T</p>` + `<p>háng 4/2007...</p>` -> `"Tháng 4/2007..."`).
     */
    private fun mergeSplitDropCaps(paragraphs: List<String>): List<String> {
        if (paragraphs.size < 2) return paragraphs
        val result = ArrayList<String>(paragraphs.size)
        var i = 0
        while (i < paragraphs.size) {
            val current = paragraphs[i].trim()
            if (
                current.length == 1 &&
                current[0].isLetter() &&
                i + 1 < paragraphs.size
            ) {
                val next = paragraphs[i + 1].trim()
                val nextFirstWord = next.substringBefore(' ')
                val nextFirstChar = nextFirstWord.firstOrNull()
                if (
                    nextFirstChar != null &&
                    nextFirstChar.isLetter() &&
                    (nextFirstChar.isLowerCase() || nextFirstWord.all { !it.isLetter() || it.isUpperCase() })
                ) {
                    result.add(current + next)
                    i += 2
                    continue
                }
            }
            result.add(current)
            i++
        }
        return result
    }

    private fun buildTranscriptSentences(
        rawParagraphs: List<String>,
        chapterTitleToStrip: String
    ): Pair<List<TranscriptSentence>, Int> {
        val normTitle = normalizeForComparison(chapterTitleToStrip)
        val normTitleCore = normalizeForComparison(
            chapterTitleToStrip.substringAfter(":", chapterTitleToStrip)
        )

        val filtered = rawParagraphs.filterIndexed { index, text ->
            if (text.length <= 1) return@filterIndexed false
            if (index < 3) {
                val normText = normalizeForComparison(text)
                if (normTitle.isNotBlank() && (normText == normTitle || normText == normTitleCore)) {
                    return@filterIndexed false
                }
                if (normText.matches(Regex("^(?:chương|phần|hồi|quyển)\\s*\\d+(?:\\s*\\(tt\\))?.*$")) && text.length < 90) {
                    return@filterIndexed false
                }
            }
            true
        }

        val segments = mutableListOf<String>()
        for (para in filtered) {
            if (para.length <= 450) {
                segments.add(para)
            } else {
                segments.addAll(splitLongParagraph(para))
            }
        }

        if (segments.isEmpty()) {
            val fallbackText = rawParagraphs.firstOrNull { it.length > 1 }
                ?: "Chương này không có nội dung văn bản."
            val fallback = TranscriptSentence(
                id = 1,
                text = fallbackText,
                timestampLabel = "00:00",
                timestampSeconds = 0,
                progressPercent = 100f
            )
            return listOf(fallback) to 10
        }

        val wordCounts = segments.map { countWords(it).coerceAtLeast(3) }
        val totalWords = wordCounts.sum().coerceAtLeast(1)
        val totalDurationSeconds = (totalWords / 3.2f).roundToInt().coerceAtLeast(segments.size * 3)

        var cumulativeWords = 0
        val result = segments.mapIndexed { idx, segmentText ->
            val startRatio = cumulativeWords.toFloat() / totalWords.toFloat()
            val startSeconds = (startRatio * totalDurationSeconds).roundToInt()
            cumulativeWords += wordCounts[idx]
            val endRatio = (cumulativeWords.toFloat() / totalWords.toFloat()) * 100f

            TranscriptSentence(
                id = idx + 1,
                text = segmentText,
                timestampLabel = formatTimestamp(startSeconds),
                timestampSeconds = startSeconds,
                progressPercent = endRatio.coerceIn(1f, 100f)
            )
        }

        return result to totalDurationSeconds
    }

    private fun normalizeForComparison(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[\"'“”‘’.:\\-–—…()]"), " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    private fun splitLongParagraph(paragraph: String): List<String> {
        val sentences = paragraph.split(Regex("(?<=[.!?…])\\s+"))
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            if (current.length + sentence.length + 1 > 420 && current.isNotEmpty()) {
                chunks.add(current.toString().trim())
                current.clear()
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(sentence.trim())
        }
        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }

    fun cleanHtmlFragment(html: String): String {
        val withoutComments = html
            .replace(HTML_COMMENT_REGEX, " ")
            .replace(STYLE_SCRIPT_REGEX, " ")
            .replace(ANY_TAG_REGEX, " ")
        val decoded = try {
            Html.fromHtml(withoutComments, Html.FROM_HTML_MODE_LEGACY).toString()
        } catch (_: Throwable) {
            withoutComments
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
        }
        return decoded
            .replace('\u00A0', ' ')
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    private fun countWords(text: String): Int {
        return text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun formatTimestamp(totalSeconds: Int): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "%02d:%02d".format(mins, secs)
    }
}
