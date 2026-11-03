package com.vietreader.tts.data.parser

import android.text.Html
import android.util.Log
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
 * - Pre-compiled static regexes for zero-overhead, 120fps parsing throughput
 */
object VietnameseSentenceTokenizer {

    private const val TAG = "VietSentenceTokenizer"

    private val HTML_COMMENT_REGEX = Regex("<!--[\\s\\S]*?-->")
    private val HEAD_REGEX = Regex("<head[^>]*>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE)
    private val STYLE_SCRIPT_REGEX = Regex("<(style|script)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE)
    private val CHAPTER_HEADER_BLOCK_REGEX = Regex(
        "<div[^>]*class=[\"'][^\"']*chapter-block[^\"']*[\"'][^>]*>[\\s\\S]*?</div>",
        RegexOption.IGNORE_CASE
    )
    private val HEADING_TAG_REGEX = Regex("<h[1-4][^>]*>([\\s\\S]*?)</h[1-4]>", RegexOption.IGNORE_CASE)
    private val BR_TAG_REGEX = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val BLOCK_TAG_BREAK_REGEX = Regex("(?i)<(?:div|h[1-6]|li|section|article|blockquote|table|tr)[^>]*>")
    private val BLOCK_TAG_CLOSE_BREAK_REGEX = Regex("(?i)</(?:div|h[1-6]|li|section|article|blockquote|table|tr)>")
    private val ANY_TAG_REGEX = Regex("<[^>]+>")
    private val MULTI_SPACE_REGEX = Regex("[ \\t\\x0B\\f\\r]+")
    private val P_TAG_REGEX = Regex("<p[^>]*>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE)
    private val NEWLINE_SPLIT_REGEX = Regex("\\n+")
    private val STANDALONE_HEADING_REGEX = Regex(
        """^(?:chương|phần|hồi|quyển|tiết|bài|mục|tập|chapter|part)\s*(?:\d+|[ivxlcdm]+|(?:thứ\s+)?[a-zà-ỹ]+)[\s.:\-–—]*[^\n]{0,70}$""",
        RegexOption.IGNORE_CASE
    )
    private val CHAPTER_PREFIX_REGEX = Regex(
        """^(?:(?:chương|hồi|phần|quyển|tiết|bài|mục|tập|chapter|part)\s*(?:\d+|[ivxlcdm]+|(?:thứ\s+)?[a-zà-ỹ]+)[\s.:\-–—]*)+""",
        RegexOption.IGNORE_CASE
    )
    private val ALL_CAPS_HEADING_REGEX = Regex(
        """^([A-ZÀ-Ỹ0-9\s.:\-–—]{2,60}?)(?=[A-ZÀ-Ỹa-zà-ỹ]*[a-zà-ỹ]+|\s*$)"""
    )
    private val CHAPTER_HEADING_MATCH_REGEX = Regex(
        """^(?:chương|phần|hồi|quyển|tiết|bài|mục|tập|chapter|part)\s*(?:\d+|[ivxlcdm]+|(?:thứ\s+)?[a-zà-ỹ]+).*$""",
        RegexOption.IGNORE_CASE
    )
    private val NORMALIZE_PUNCTUATION_REGEX = Regex("[\"'“”‘’.:\\-–—…()]")
    private val SENTENCE_BREAK_REGEX = Regex("(?<=[.!?…])\\s+")
    private val WHITESPACE_SPLIT_REGEX = Regex("\\s+")

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

        val pMatches = P_TAG_REGEX.findAll(cleanedHtml).toList()
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
            .split(NEWLINE_SPLIT_REGEX)
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
        val filtered = rawParagraphs.mapIndexed { index, text ->
            val trimmed = text.trim()
            if (index < 3) {
                stripLeadingChapterHeading(trimmed, chapterTitleToStrip)
            } else if (trimmed.length < 80 && STANDALONE_HEADING_REGEX.matches(trimmed)) {
                // Residual page header from subsequent pages in the PDF/TXT
                ""
            } else {
                trimmed
            }
        }.filter { it.length > 1 }

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

    /**
     * Strips leading duplicated chapter headings (e.g. "CHƯƠNG 1. GIỚI THIỆU Từ rất lâu...")
     * from the very first paragraph/sentence of a chapter so it isn't spoken or rendered twice.
     */
    fun cleanLeadingHeadingFromSentence(text: String, chapterTitle: String = ""): String {
        var clean = text.trim()
        if (clean.length < 5) return clean

        val normTitleCore = normalizeForComparison(
            chapterTitle.substringAfter(':', chapterTitle).substringAfter('-', chapterTitle)
        )

        // 1. Check if string starts with Chapter / Hồi / Phần / Tập / Quyển prefix
        val prefixMatch = CHAPTER_PREFIX_REGEX.find(clean)
        if (prefixMatch != null && prefixMatch.range.first == 0) {
            val remainder = clean.substring(prefixMatch.range.last + 1).trim()
            if (remainder.length > 3) {
                // Check if remainder starts with chapter title (e.g. "GIỚI THIỆU Từ rất lâu...")
                if (normTitleCore.isNotBlank()) {
                    val normRem = normalizeForComparison(remainder)
                    if (normRem.startsWith(normTitleCore)) {
                        val afterTitle = remainder.substring(normTitleCore.length.coerceAtMost(remainder.length))
                            .trimStart(' ', '.', ':', '-', '–', '—', ',', ';')
                        if (afterTitle.length > 5) {
                            return afterTitle
                        }
                    }
                }

                // Check if remainder starts with ALL CAPS heading words (e.g. "GIỚI THIỆU Từ rất lâu...")
                val allCapsMatch = ALL_CAPS_HEADING_REGEX.find(remainder)
                if (allCapsMatch != null && allCapsMatch.range.first == 0) {
                    val after = remainder.substring(allCapsMatch.range.last + 1).trim()
                    if (after.length > 5) {
                        return after
                    }
                }

                return remainder
            }
        }

        // 2. If no chapter prefix, check if sentence starts directly with chapterTitle
        if (normTitleCore.isNotBlank() && clean.length > normTitleCore.length + 5) {
            val normClean = normalizeForComparison(clean)
            if (normClean.startsWith(normTitleCore)) {
                val afterTitle = clean.substring(normTitleCore.length.coerceAtMost(clean.length))
                    .trimStart(' ', '.', ':', '-', '–', '—', ',', ';')
                if (afterTitle.length > 5) {
                    return afterTitle
                }
            }
        }

        return clean
    }

    private fun stripLeadingChapterHeading(raw: String, chapterTitle: String): String {
        var text = raw.trim()
        val normTitle = normalizeForComparison(chapterTitle)
        val normTitleCore = normalizeForComparison(chapterTitle.substringAfter(':', chapterTitle).substringAfter('-', chapterTitle))

        // 1. If the whole paragraph is just the title or a short chapter prefix, discard it entirely
        val norm = normalizeForComparison(text)
        if (norm == normTitle || norm == normTitleCore) return ""
        if (norm.matches(CHAPTER_HEADING_MATCH_REGEX) && text.length < 90) {
            return ""
        }

        // 2. Strip leading "CHƯƠNG 1. GIỚI THIỆU " prefix
        text = cleanLeadingHeadingFromSentence(text, chapterTitle)

        // 3. If still starting with chapterTitle, strip it
        if (normTitleCore.isNotBlank() && text.length >= normTitleCore.length) {
            val potentialTitle = text.take(normTitleCore.length + 5)
            val normPotential = normalizeForComparison(potentialTitle)
            if (normPotential.startsWith(normTitleCore)) {
                val afterTitle = text.substring(normTitleCore.length.coerceAtMost(text.length))
                    .trimStart(' ', '.', ':', '-', '–', '—', ',', ';')
                if (afterTitle.isNotBlank()) {
                    text = afterTitle.trim()
                }
            }
        }

        return text
    }

    private fun normalizeForComparison(input: String): String {
        return input
            .lowercase()
            .replace(NORMALIZE_PUNCTUATION_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    private fun splitLongParagraph(paragraph: String): List<String> {
        val sentences = paragraph.split(SENTENCE_BREAK_REGEX)
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
        val withBlockBreaks = html
            .replace(HTML_COMMENT_REGEX, " ")
            .replace(STYLE_SCRIPT_REGEX, " ")
            .replace(BR_TAG_REGEX, "\n")
            .replace(BLOCK_TAG_BREAK_REGEX, "\n")
            .replace(BLOCK_TAG_CLOSE_BREAK_REGEX, "\n")
            .replace(ANY_TAG_REGEX, " ")
        val decoded = try {
            Html.fromHtml(withBlockBreaks, Html.FROM_HTML_MODE_LEGACY).toString()
        } catch (e: Exception) {
            Log.w(TAG, "HTML fromHtml legacy fallback: ${e.message}")
            withBlockBreaks
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
        return text.trim().split(WHITESPACE_SPLIT_REGEX).count { it.isNotBlank() }
    }

    fun formatTimestamp(totalSeconds: Int): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "%02d:%02d".format(mins, secs)
    }
}
