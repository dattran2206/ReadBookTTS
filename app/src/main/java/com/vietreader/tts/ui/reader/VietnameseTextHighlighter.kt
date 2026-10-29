package com.vietreader.tts.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.vietreader.tts.domain.model.HighlightLevel

data class TextSentenceSpan(
    val index: Int,
    val start: Int,
    val end: Int,
    val text: String
)

data class TextWordSpan(
    val index: Int,
    val start: Int,
    val end: Int,
    val word: String
)

/**
 * Utility for parsing Vietnamese paragraphs into sentence and word spans
 * and rendering synchronized highlights across Paragraph, Sentence, and Word levels.
 */
object VietnameseTextHighlighter {

    private val SENTENCE_SPLIT_REGEX = Regex("""(?<=[.!?…])\s+(?=[A-ZÀ-Ỹ"“'0-9\p{L}])|(?<=\n)\s*""")
    private val WORD_REGEX = Regex("""\S+""")

    /**
     * Splits paragraph text into sentences with exact character offsets.
     */
    fun getSentenceSpans(text: String): List<TextSentenceSpan> {
        if (text.isBlank()) return emptyList()

        val spans = mutableListOf<TextSentenceSpan>()
        val matches = SENTENCE_SPLIT_REGEX.findAll(text).toList()

        if (matches.isEmpty()) {
            return listOf(TextSentenceSpan(0, 0, text.length, text))
        }

        var currentStart = 0
        var sentenceIdx = 0

        for (match in matches) {
            val end = match.range.first
            if (end > currentStart) {
                val sentenceText = text.substring(currentStart, end).trim()
                if (sentenceText.isNotEmpty()) {
                    spans.add(TextSentenceSpan(sentenceIdx++, currentStart, end, sentenceText))
                }
            }
            currentStart = match.range.last + 1
        }

        if (currentStart < text.length) {
            val tailText = text.substring(currentStart).trim()
            if (tailText.isNotEmpty()) {
                spans.add(TextSentenceSpan(sentenceIdx, currentStart, text.length, tailText))
            }
        }

        return spans.ifEmpty { listOf(TextSentenceSpan(0, 0, text.length, text)) }
    }

    /**
     * Splits text into individual words with exact character offsets.
     */
    fun getWordSpans(text: String): List<TextWordSpan> {
        if (text.isBlank()) return emptyList()
        val spans = mutableListOf<TextWordSpan>()
        var wordIdx = 0
        for (match in WORD_REGEX.findAll(text)) {
            spans.add(
                TextWordSpan(
                    index = wordIdx++,
                    start = match.range.first,
                    end = match.range.last + 1,
                    word = match.value
                )
            )
        }
        return spans
    }

    /**
     * Builds an [AnnotatedString] styled appropriately for the current [HighlightLevel].
     */
    fun buildHighlightedParagraph(
        text: String,
        isActive: Boolean,
        highlightLevel: HighlightLevel,
        activeWordStart: Int,
        activeWordEnd: Int,
        inkColor: Color,
        highlightBgColor: Color,
        highlightWordBgColor: Color,
        primaryColor: Color,
        sentenceSpans: List<TextSentenceSpan>? = null
    ): AnnotatedString {
        if (!isActive) {
            return AnnotatedString(text)
        }

        when (highlightLevel) {
            HighlightLevel.PARAGRAPH -> {
                return buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = inkColor,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append(text)
                    }
                }
            }

            HighlightLevel.SENTENCE -> {
                val sentences = sentenceSpans ?: getSentenceSpans(text)
                if (sentences.isEmpty()) return AnnotatedString(text)

                val activeSentence = if (activeWordStart >= 0) {
                    sentences.find { activeWordStart in it.start..it.end }
                        ?: sentences.firstOrNull { it.end > activeWordStart }
                        ?: sentences.last()
                } else {
                    sentences.first()
                }

                return buildAnnotatedString {
                    var lastIndex = 0
                    for (sentence in sentences) {
                        if (sentence.start > lastIndex) {
                            append(text.substring(lastIndex, sentence.start))
                        }

                        if (sentence == activeSentence) {
                            withStyle(
                                SpanStyle(
                                    background = highlightWordBgColor,
                                    color = inkColor,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(text.substring(sentence.start, sentence.end))
                            }
                        } else {
                            withStyle(
                                SpanStyle(
                                    color = inkColor.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(text.substring(sentence.start, sentence.end))
                            }
                        }
                        lastIndex = sentence.end
                    }
                    if (lastIndex < text.length) {
                        append(text.substring(lastIndex))
                    }
                }
            }

            HighlightLevel.WORD -> {
                val sentences = sentenceSpans ?: getSentenceSpans(text)
                val activeSentence = if (activeWordStart >= 0) {
                    sentences.find { activeWordStart in it.start..it.end }
                        ?: sentences.firstOrNull { it.end > activeWordStart }
                        ?: sentences.lastOrNull()
                } else {
                    sentences.firstOrNull()
                }

                val validWordRange = if (activeWordStart >= 0 && activeWordEnd > activeWordStart && activeWordEnd <= text.length) {
                    activeWordStart until activeWordEnd
                } else if (activeSentence != null) {
                    val words = getWordSpans(activeSentence.text)
                    if (words.isNotEmpty()) {
                        (activeSentence.start + words[0].start) until (activeSentence.start + words[0].end)
                    } else null
                } else null

                return buildAnnotatedString {
                    var lastIndex = 0
                    for (sentence in sentences) {
                        if (sentence.start > lastIndex) {
                            append(text.substring(lastIndex, sentence.start))
                        }

                        val isCurSentence = sentence == activeSentence
                        if (isCurSentence && validWordRange != null) {
                            val wStart = validWordRange.first.coerceIn(sentence.start, sentence.end)
                            val wEnd = (validWordRange.last + 1).coerceIn(wStart, sentence.end)

                            if (wStart > sentence.start) {
                                withStyle(
                                    SpanStyle(
                                        background = highlightBgColor.copy(alpha = 0.35f),
                                        color = inkColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append(text.substring(sentence.start, wStart))
                                }
                            }

                            if (wEnd > wStart) {
                                withStyle(
                                    SpanStyle(
                                        background = highlightWordBgColor,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(text.substring(wStart, wEnd))
                                }
                            }

                            if (wEnd < sentence.end) {
                                withStyle(
                                    SpanStyle(
                                        background = highlightBgColor.copy(alpha = 0.35f),
                                        color = inkColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append(text.substring(wEnd, sentence.end))
                                }
                            }
                        } else {
                            withStyle(
                                SpanStyle(
                                    color = inkColor.copy(alpha = if (isCurSentence) 0.85f else 0.55f),
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(text.substring(sentence.start, sentence.end))
                            }
                        }
                        lastIndex = sentence.end
                    }
                    if (lastIndex < text.length) {
                        append(text.substring(lastIndex))
                    }
                }
            }
        }
    }
}
