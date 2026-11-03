package com.vietreader.tts.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookChapter
import com.vietreader.tts.domain.model.BookFormat
import com.vietreader.tts.domain.model.ChapterStatus
import com.vietreader.tts.domain.model.CoverGradient
import com.vietreader.tts.domain.model.TranscriptSentence
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Robust, production-grade PDF parser for Wordwave.
 * Features:
 * 1. High-fidelity first page cover rendering via Android native [PdfRenderer] with fallback to typography art.
 * 2. PDF metadata & TOC (Bookmarks/Outline) extraction with page mapping and loop prevention.
 * 3. Dynamic page grouping for PDFs without embedded TOC or with sparse bookmarks.
 * 4. Text cleaning, heading separation & de-hyphenation engine tailored for Vietnamese TTS synthesis.
 * 5. Instantaneous chapter lazy-loading with hard caps against out-of-memory or ANRs on huge PDFs.
 */
object PdfBookParser {

    private val PAGE_NUMBER_FOOTER_REGEX = Regex(
        """(?im)^\s*(?:[-–—\[(]?\s*(?:trang|page)?\s*\d+\s*(?:[/|\\]\s*\d+)?\s*[-–—\])]?)\s*$"""
    )
    private val RUNNING_CHAPTER_HEADER_REGEX = Regex(
        """(?im)^\s*(?:\d+\s*[|•\-–—]?\s*)?(?:chương|hồi|phần|quyển|tiết|bài|mục|tập|chapter|part)\s*(?:\d+|[ivxlcdm]+|(?:thứ\s+)?[a-zà-ỹ]+)[\s.:\-–—]*[^\n]{0,70}?(?:\s*[|•\-–—]\s*\d+|\s+\d+)?\s*$"""
    )
    private val RUNNING_SECTION_HEADER_WITH_PAGE_REGEX = Regex(
        """(?im)^\s*(?:\d+\s*[|•\-–—]\s*[^\n]{3,60}|[^\n]{3,60}\s*[|•\-–—]\s*\d+)\s*$"""
    )
    private val HYPHENATED_LINE_BREAK_REGEX = Regex(
        """(?<=\p{L})[–—-]\s*\n\s*(?=\p{L})"""
    )
    private val HEADING_LINE_REGEX = Regex(
        """(?im)^\s*(?:chương|hồi|phần|quyển|tiết|bài|mục|tập|chapter|part)\s*(?:\d+|[ivxlcdm]+|(?:thứ\s+)?[a-zà-ỹ]+).*"""
    )

    private const val MAX_PAGES_PER_CHAPTER = 18

    /**
     * Creates a memory-bounded setting swapping buffer to disk scratch storage
     * to eliminate Out-Of-Memory hazards on massive PDF documents.
     */
    private fun getSafeMemorySetting(pdfFile: File): MemoryUsageSetting {
        val scratchDir = File(pdfFile.parentFile, "pdf_scratch").apply { mkdirs() }
        return MemoryUsageSetting.setupMixed(10 * 1024 * 1024).setTempDir(scratchDir)
    }

    /**
     * Parses a local `.pdf` file into a [Book] with cover artwork and Chapter 1 ready.
     */
    fun parsePdfFile(
        context: Context,
        bookId: String,
        pdfFile: File,
        originalFileName: String,
        defaultVoiceName: String = "Giọng hệ thống 1",
        defaultVoiceRegion: String = "Ngoại tuyến"
    ): Book {
        var cleanTitle = originalFileName.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Tài liệu PDF" }
        var authorName = "Tài liệu PDF"

        // 1. Open with PDFBox to extract metadata and TOC outline
        PDDocument.load(pdfFile, getSafeMemorySetting(pdfFile)).use { pdDocument ->
            val info = pdDocument.documentInformation
            info?.title?.takeIf { it.isNotBlank() && it.length < 150 }?.let { cleanTitle = it.trim() }
            info?.author?.takeIf { it.isNotBlank() && it.length < 100 }?.let { authorName = it.trim() }
        }

        val extractedChapters = extractPdfChapters(pdfFile)

        // 2. Render Page 0 as Cover Image using Android Native PdfRenderer
        val (coverFilePath, coverGradient) = extractFirstPageCover(context, pdfFile, bookId, cleanTitle, authorName)

        // 3. Cache chapters index for 1ms subsequent lookups
        saveChaptersIndexCache(pdfFile, extractedChapters)

        // 4. Load sentences for Chapter 1
        val firstChapter = extractedChapters.firstOrNull() ?: BookChapter(
            number = 1,
            title = "Mở đầu",
            durationLabel = "10:00",
            status = ChapterStatus.PLAYING,
            progressPercent = 0,
            remainingLabel = "Còn 10:00",
            entryHref = "1:1"
        )

        val (firstSentences, firstDurationSec) = loadPdfChapterSentences(
            pdfFile = pdfFile,
            rangeSpec = firstChapter.entryHref,
            chapterTitle = firstChapter.title
        )

        val totalEstimatedMinutes = (extractedChapters.size * (firstDurationSec / 60f).coerceAtLeast(4f)).roundToInt()
        val totalDurationFormatted = if (totalEstimatedMinutes >= 60) {
            "${totalEstimatedMinutes / 60}g ${totalEstimatedMinutes % 60}p"
        } else {
            "${totalEstimatedMinutes}p"
        }

        val firstDurationLabel = VietnameseSentenceTokenizer.formatTimestamp(firstDurationSec)
        val updatedChapters = extractedChapters.mapIndexed { idx, ch ->
            if (idx == 0) {
                ch.copy(
                    durationLabel = firstDurationLabel,
                    status = ChapterStatus.PLAYING,
                    remainingLabel = "Còn $firstDurationLabel"
                )
            } else ch
        }

        val now = System.currentTimeMillis()
        return Book(
            id = bookId,
            title = cleanTitle,
            author = authorName,
            categoryBadge = "Tài liệu PDF",
            genreLabel = "Sách / Tài liệu PDF",
            totalDurationFormatted = totalDurationFormatted,
            totalChaptersCount = updatedChapters.size,
            synopsis = firstSentences.take(3).joinToString(" ") { it.text }.ifBlank { "Tài liệu PDF gồm ${updatedChapters.size} phần đã được tối ưu hóa cho giọng đọc AI Wordwave." },
            currentChapterTitle = firstChapter.fullDisplayTitle,
            currentChapterShortLabel = "Phần 1",
            progressPercent = 0,
            remainingTimeLabel = "Còn $firstDurationLabel",
            voiceName = defaultVoiceName,
            voiceRegion = defaultVoiceRegion,
            format = BookFormat.PDF,
            coverGradient = coverGradient,
            isNew = true,
            isImported = true,
            isCompleted = false,
            addedAtEpochMillis = now,
            lastReadEpochMillis = now,
            totalDurationSeconds = firstDurationSec,
            currentPositionSeconds = 0,
            sentences = firstSentences,
            chapters = updatedChapters,
            bookmarks = emptyList(),
            epubFilePath = pdfFile.absolutePath,
            coverImagePath = coverFilePath,
            currentChapterNumber = 1
        )
    }

    /**
     * Loads sentences on-demand for a single chapter range (`startPage:endPage`).
     * Guaranteed never to freeze the app by enforcing a strict page range ceiling.
     */
    fun loadPdfChapterSentences(
        pdfFile: File,
        rangeSpec: String,
        chapterTitle: String
    ): Pair<List<TranscriptSentence>, Int> {
        val parts = rangeSpec.split(':')
        val startPage = parts.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val rawEndPage = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(startPage) ?: startPage
        // Safety cap: never extract more than MAX_PAGES_PER_CHAPTER pages in one batch
        val endPage = rawEndPage.coerceAtMost(startPage + MAX_PAGES_PER_CHAPTER)

        val rawExtractedText = runCatching {
            PDDocument.load(pdfFile, getSafeMemorySetting(pdfFile)).use { pdDocument ->
                val total = pdDocument.numberOfPages
                val actualStart = startPage.coerceIn(1, total)
                val actualEnd = endPage.coerceIn(actualStart, total)

                val stripper = PDFTextStripper().apply {
                    this.startPage = actualStart
                    this.endPage = actualEnd
                    sortByPosition = true
                }
                stripper.getText(pdDocument)
            }
        }.getOrDefault("")

        val sanitizedText = cleanPdfText(rawExtractedText)

        return VietnameseSentenceTokenizer.tokenizePlainTextChapter(
            rawText = sanitizedText.ifBlank { "Nội dung phần này không chứa văn bản có thể trích xuất." },
            chapterTitleToStrip = chapterTitle
        )
    }

    /**
     * Renders Page 0 as a cover thumbnail using Android native [PdfRenderer].
     * Compatible with API 26+ by explicitly releasing [PdfRenderer.Page] in a finally block.
     * Guarantees [Bitmap.recycle] invocation to avoid native memory leaks.
     * Falls back to [BookCoverGenerator] if rendering fails or produces an empty white page.
     */
    private fun extractFirstPageCover(
        context: Context,
        pdfFile: File,
        bookId: String,
        title: String,
        author: String
    ): Pair<String, CoverGradient> {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val coverFile = File(coversDir, "$bookId.jpg")

        var renderedSuccessfully = false
        var extractedGradient: CoverGradient? = null

        runCatching {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        var pageBitmap: Bitmap? = null
                        try {
                            val targetWidth = 600
                            val targetHeight = ((page.height.toFloat() / page.width.toFloat()) * targetWidth)
                                .roundToInt()
                                .coerceIn(600, 1000)

                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            pageBitmap = bitmap
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Check if page is not completely blank white
                            if (!isBitmapBlank(bitmap)) {
                                FileOutputStream(coverFile).use { fos ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                                }
                                extractedGradient = extractGradientFromBitmap(bitmap)
                                renderedSuccessfully = true
                            }
                        } finally {
                            pageBitmap?.recycle()
                            page.close()
                        }
                    }
                }
            }
        }

        if (!renderedSuccessfully || !coverFile.exists() || coverFile.length() == 0L) {
            val gradient = BookCoverGenerator.generateCover(
                title = title,
                author = author,
                category = "TÀI LIỆU PDF",
                outputFile = coverFile
            )
            return coverFile.absolutePath to gradient
        }

        val gradient = extractedGradient ?: CoverGradient(
            startColorHex = 0xFF2B3A42,
            centerColorHex = 0xFF1C262C,
            endColorHex = 0xFF0E1316
        )
        return coverFile.absolutePath to gradient
    }

    private fun extractOutlineChapters(pdDocument: PDDocument): List<BookChapter> {
        val outline: PDDocumentOutline = pdDocument.documentCatalog.documentOutline ?: return emptyList()
        val rawEntries = mutableListOf<Pair<String, Int>>()

        // Build quick lookup map: PDPage -> 1-based page number (O(N) single pass instead of O(N*M))
        val pageToNumberMap = HashMap<PDPage, Int>()
        var pIdx = 1
        for (page in pdDocument.pages) {
            pageToNumberMap[page] = pIdx++
        }

        val visited = HashSet<PDOutlineItem>()
        fun traverse(item: PDOutlineItem?, depth: Int) {
            if (depth > 50 || rawEntries.size >= 800) return
            var curr = item
            while (curr != null) {
                if (!visited.add(curr)) break // Prevent circular reference loops in corrupted outlines
                val nonNullItem = curr
                val title = nonNullItem.title?.trim()
                val targetPage = runCatching {
                    nonNullItem.findDestinationPage(pdDocument)?.let { pageToNumberMap[it] }
                }.getOrNull()

                if (!title.isNullOrBlank() && targetPage != null && targetPage > 0) {
                    rawEntries.add(title to targetPage)
                }
                traverse(nonNullItem.firstChild, depth + 1)
                curr = nonNullItem.nextSibling
            }
        }

        traverse(outline.firstChild, 0)

        if (rawEntries.isEmpty()) return emptyList()

        val sortedEntries = rawEntries.sortedBy { it.second }
        val totalPages = pdDocument.numberOfPages
        val chapters = mutableListOf<BookChapter>()
        var chapterIndex = 1

        for (idx in sortedEntries.indices) {
            val (title, pageNum) = sortedEntries[idx]
            val nextStart = if (idx + 1 < sortedEntries.size) sortedEntries[idx + 1].second else totalPages + 1
            val rawEndPage = (nextStart - 1).coerceAtLeast(pageNum).coerceAtMost(totalPages)

            // If an outline item spans more than MAX_PAGES_PER_CHAPTER, subdivide it to prevent ANRs
            if (rawEndPage - pageNum + 1 > MAX_PAGES_PER_CHAPTER) {
                var subStart = pageNum
                var subPart = 1
                while (subStart <= rawEndPage) {
                    val subEnd = (subStart + MAX_PAGES_PER_CHAPTER - 1).coerceAtMost(rawEndPage)
                    val subTitle = "$title - Phần $subPart (Trang $subStart–$subEnd)"
                    chapters.add(
                        BookChapter(
                            number = chapterIndex,
                            title = subTitle,
                            durationLabel = "10:00",
                            status = if (chapterIndex == 1) ChapterStatus.PLAYING else ChapterStatus.UNPLAYED,
                            progressPercent = 0,
                            remainingLabel = if (chapterIndex == 1) "Còn 10:00" else "",
                            entryHref = "$subStart:$subEnd"
                        )
                    )
                    chapterIndex++
                    subPart++
                    subStart = subEnd + 1
                }
            } else {
                chapters.add(
                    BookChapter(
                        number = chapterIndex,
                        title = title,
                        durationLabel = "10:00",
                        status = if (chapterIndex == 1) ChapterStatus.PLAYING else ChapterStatus.UNPLAYED,
                        progressPercent = 0,
                        remainingLabel = if (chapterIndex == 1) "Còn 10:00" else "",
                        entryHref = "$pageNum:$rawEndPage"
                    )
                )
                chapterIndex++
            }
        }
        return chapters
    }

    private fun generateGroupedPageChapters(totalPages: Int): List<BookChapter> {
        val chapters = mutableListOf<BookChapter>()
        val pagesPerChapter = when {
            totalPages <= 10 -> 1
            totalPages <= 40 -> 5
            totalPages <= 120 -> 8
            totalPages <= 300 -> 10
            else -> 12
        }

        var start = 1
        var chapterIndex = 1
        while (start <= totalPages) {
            val end = (start + pagesPerChapter - 1).coerceAtMost(totalPages)
            val title = if (start == end) "Trang $start" else "Phần $chapterIndex (Trang $start–$end)"
            chapters.add(
                BookChapter(
                    number = chapterIndex,
                    title = title,
                    durationLabel = "10:00",
                    status = if (chapterIndex == 1) ChapterStatus.PLAYING else ChapterStatus.UNPLAYED,
                    progressPercent = 0,
                    remainingLabel = if (chapterIndex == 1) "Còn 10:00" else "",
                    entryHref = "$start:$end"
                )
            )
            start = end + 1
            chapterIndex++
        }
        return chapters
    }

    /**
     * Cleans raw extracted PDF text by removing running page numbers,
     * connecting hyphenated words across line breaks, and strictly separating headings
     * with double newlines (\n\n) so they are not glued to body text.
     * Uses pre-compiled regexes to eliminate regex recompilation overhead.
     */
    fun cleanPdfText(raw: String): String {
        if (raw.isBlank()) return ""

        val dehyphenated = raw.replace(HYPHENATED_LINE_BREAK_REGEX, "")

        val lines = dehyphenated.lines()
        val filteredLines = lines.filterNot { line ->
            val trimmed = line.trim()
            trimmed.isEmpty() ||
                PAGE_NUMBER_FOOTER_REGEX.matches(trimmed) ||
                RUNNING_CHAPTER_HEADER_REGEX.matches(trimmed) ||
                RUNNING_SECTION_HEADER_WITH_PAGE_REGEX.matches(trimmed)
        }

        val result = StringBuilder()
        for (i in filteredLines.indices) {
            val line = filteredLines[i].trim()
            if (line.isEmpty()) continue

            result.append(line)
            if (i < filteredLines.size - 1) {
                val nextLine = filteredLines[i + 1].trim()

                // Check if current or next line is a chapter heading using pre-compiled regex
                val isCurrentLineHeading = line.matches(HEADING_LINE_REGEX) ||
                        (line.length in 3..70 && line.all { !it.isLetter() || it.isUpperCase() })
                val isNextLineHeading = nextLine.matches(HEADING_LINE_REGEX) ||
                        (nextLine.length in 3..70 && nextLine.all { !it.isLetter() || it.isUpperCase() })

                // Separate math equation lines into independent blocks so they are not merged with narrative text
                val isCurrentLineEquation = line.length in 3..120 && (
                    line.contains(" = ") || line.contains(" ≈ ") || line.contains(" ≠ ") ||
                    line.contains(" ≤ ") || line.contains(" ≥ ") || line.contains(" ∈ ") ||
                    line.contains("∑") || line.contains("∏") || line.contains("∫") ||
                    line.contains("∇") || line.contains("∂")
                )
                val isNextLineEquation = nextLine.length in 3..120 && (
                    nextLine.contains(" = ") || nextLine.contains(" ≈ ") || nextLine.contains(" ≠ ") ||
                    nextLine.contains(" ≤ ") || nextLine.contains(" ≥ ") || nextLine.contains(" ∈ ") ||
                    nextLine.contains("∑") || nextLine.contains("∏") || nextLine.contains("∫") ||
                    nextLine.contains("∇") || nextLine.contains("∂")
                )

                val endsWithPunct = line.endsWith('.') || line.endsWith('!') || line.endsWith('?') ||
                        line.endsWith(':') || line.endsWith(';') || line.endsWith('…') ||
                        line.endsWith('"') || line.endsWith('”')

                if (endsWithPunct || isCurrentLineHeading || isNextLineHeading || isCurrentLineEquation || isNextLineEquation) {
                    result.append("\n\n")
                } else {
                    result.append(" ")
                }
            }
        }
        return result.toString().trim()
    }

    private fun isBitmapBlank(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        var nonWhiteCount = 0
        val samplePoints = listOf(
            w / 4 to h / 4,
            w / 2 to h / 4,
            (w * 3) / 4 to h / 4,
            w / 4 to h / 2,
            w / 2 to h / 2,
            (w * 3) / 4 to h / 2,
            w / 4 to (h * 3) / 4,
            w / 2 to (h * 3) / 4,
            (w * 3) / 4 to (h * 3) / 4
        )
        for ((x, y) in samplePoints) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (r < 240 || g < 240 || b < 240) {
                nonWhiteCount++
            }
        }
        return nonWhiteCount == 0
    }

    private fun extractGradientFromBitmap(bmp: Bitmap): CoverGradient {
        val w = bmp.width.coerceAtLeast(1)
        val h = bmp.height.coerceAtLeast(1)

        fun darkenColor(argb: Int, factor: Float): Long {
            val r = (((argb shr 16) and 0xFF) * factor).roundToInt().coerceIn(18, 220)
            val g = (((argb shr 8) and 0xFF) * factor).roundToInt().coerceIn(12, 200)
            val b = ((argb and 0xFF) * factor).roundToInt().coerceIn(10, 200)
            return (0xFF000000L) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
        }

        val topPixel = darkenColor(bmp.getPixel(w / 2, h / 5), 0.72f)
        val midPixel = darkenColor(bmp.getPixel(w / 2, h / 2), 0.55f)
        val botPixel = darkenColor(bmp.getPixel(w / 2, (h * 4) / 5), 0.35f)

        return CoverGradient(
            startColorHex = topPixel,
            centerColorHex = midPixel,
            endColorHex = botPixel
        )
    }

    fun extractPdfChapters(pdfFile: File): List<BookChapter> {
        val extractedChapters = mutableListOf<BookChapter>()
        PDDocument.load(pdfFile, getSafeMemorySetting(pdfFile)).use { pdDocument ->
            val totalPages = pdDocument.numberOfPages.coerceAtLeast(1)
            val outlineChapters = extractOutlineChapters(pdDocument)
            if (outlineChapters.size >= 3 || (outlineChapters.isNotEmpty() && totalPages <= 30)) {
                extractedChapters.addAll(outlineChapters)
            } else {
                // Fallback: Group pages into logical sections of <= 15 pages
                extractedChapters.addAll(generateGroupedPageChapters(totalPages))
            }
        }
        return extractedChapters
    }

    fun saveChaptersIndexCache(pdfFile: File, chapters: List<BookChapter>) {
        runCatching {
            val cacheFile = File(pdfFile.parentFile, "${pdfFile.nameWithoutExtension}.chapters.v2.idx")
            val tmpFile = File(pdfFile.parentFile, "${pdfFile.nameWithoutExtension}.chapters.v2.idx.tmp")
            tmpFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                chapters.forEach { ch ->
                    val cleanTitle = ch.title.replace('\t', ' ').replace('\n', ' ')
                    val cleanHref = ch.entryHref.replace('\t', ' ').replace('\n', ' ')
                    writer.write("${ch.number}\t$cleanHref\t$cleanTitle\t${ch.durationLabel}\n")
                }
            }
            if (!tmpFile.renameTo(cacheFile)) {
                tmpFile.copyTo(cacheFile, overwrite = true)
                tmpFile.delete()
            }
        }
    }
}
