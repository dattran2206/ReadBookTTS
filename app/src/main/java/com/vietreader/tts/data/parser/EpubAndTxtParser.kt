package com.vietreader.tts.data.parser

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookChapter
import com.vietreader.tts.domain.model.BookFormat
import com.vietreader.tts.domain.model.ChapterStatus
import com.vietreader.tts.domain.model.CoverGradient
import com.vietreader.tts.domain.model.TranscriptSentence
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipFile
import kotlin.math.roundToInt

/**
 * Universal high-performance EPUB & TXT book parser supporting:
 * 1. Huge web novels (2,000+ chapters like "Tiên Nghịch") via random-access [ZipFile] lazy loading
 * 2. Split-file & anchor-based commercial/PRC-converted EPUBs (like "Điệp Viên Hoàn Hảo X6")
 *    via TOC-driven spine-range merging and `#anchor` boundary slicing.
 */
object EpubAndTxtParser {

    private val CHAPTER_PREFIX_REGEX = Regex(
        "^(?:chương|phần|hồi|quyển|chapter)\\s*(\\d+)\\s*[:.\\-–—]?\\s*(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val CONTINUATION_CHAPTER_REGEX = Regex(
        "^.*\\(\\s*(?:tt|tiếp|tiếp theo)\\s*\\).*$",
        RegexOption.IGNORE_CASE
    )

    private data class TocEntry(
        val label: String,
        val zipHref: String,
        val anchorId: String?
    )

    data class ParsedOpfData(
        val title: String,
        val author: String,
        val description: String,
        val seriesGenre: String,
        val coverHref: String?,
        val chapters: List<BookChapter>
    )

    /**
     * Imports a `.epub` or `.txt` document from a content [Uri] into app-private storage
     * and returns a fully populated [Book] with Chapter 1 loaded.
     */
    fun importBookFromUri(
        context: Context,
        uri: Uri,
        defaultVoiceName: String = "Giọng hệ thống 1",
        defaultVoiceRegion: String = "Ngoại tuyến"
    ): Result<Book> = runCatching {
        val displayName = resolveDisplayName(context, uri) ?: "Sach_moi.epub"
        val isTxt = displayName.lowercase().endsWith(".txt")
        val bookId = "book_${System.currentTimeMillis()}"

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val extension = if (isTxt) "txt" else "epub"
        val destFile = File(booksDir, "$bookId.$extension")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, bufferSize = 32 * 1024)
            }
        } ?: error("Không thể mở tệp sách từ bộ nhớ thiết bị")

        if (isTxt) {
            parseLocalTxtFile(
                bookId = bookId,
                txtFile = destFile,
                originalFileName = displayName,
                defaultVoiceName = defaultVoiceName,
                defaultVoiceRegion = defaultVoiceRegion
            )
        } else {
            parseLocalEpubFile(
                context = context,
                bookId = bookId,
                epubFile = destFile,
                originalFileName = displayName,
                defaultVoiceName = defaultVoiceName,
                defaultVoiceRegion = defaultVoiceRegion
            )
        }
    }.onFailure { err ->
        if (err is kotlin.coroutines.cancellation.CancellationException) throw err
    }

    /**
     * Parses a local `.epub` file into a [Book], extracting cover image and chapter index cache.
     */
    fun parseLocalEpubFile(
        context: Context,
        bookId: String,
        epubFile: File,
        originalFileName: String,
        defaultVoiceName: String = "Giọng hệ thống 1",
        defaultVoiceRegion: String = "Ngoại tuyến"
    ): Book {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        var coverFilePath: String? = null
        var coverGradient = CoverGradient(
            startColorHex = 0xFF5D2A0C,
            centerColorHex = 0xFF3E1B07,
            endColorHex = 0xFF210D03
        )

        val opfData = ZipFile(epubFile).use { zip ->
            val opfEntryPath = findOpfPath(zip) ?: "metadata.opf"
            val opfBaseDir = opfEntryPath.substringBeforeLast('/', "").let {
                if (it.isEmpty()) "" else "$it/"
            }

            val parsed = parseOpfAndToc(zip, opfEntryPath, opfBaseDir, originalFileName)

            val coverCandidatePath = parsed.coverHref?.let { resolveZipPath(opfBaseDir, it) }
                ?: findFallbackCoverEntry(zip)
            if (coverCandidatePath != null) {
                val coverEntry = findZipEntryCompat(zip, coverCandidatePath)
                if (coverEntry != null) {
                    val outCoverFile = File(coversDir, "$bookId.jpg")
                    zip.getInputStream(coverEntry).use { input ->
                        FileOutputStream(outCoverFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (outCoverFile.exists() && outCoverFile.length() > 0) {
                        coverFilePath = outCoverFile.absolutePath
                        extractGradientFromImage(outCoverFile)?.let { coverGradient = it }
                    }
                }
            }

            parsed
        }

        saveChaptersIndexCache(epubFile, opfData.chapters)

        val firstChapter = opfData.chapters.firstOrNull() ?: BookChapter(
            number = 1,
            title = "Mở đầu",
            durationLabel = "15:00",
            status = ChapterStatus.PLAYING,
            progressPercent = 0,
            remainingLabel = "Còn 15:00",
            entryHref = ""
        )

        val (firstSentences, firstDurationSec) = if (firstChapter.entryHref.isNotBlank()) {
            loadEpubChapterSentences(
                epubFilePath = epubFile.absolutePath,
                entryHref = firstChapter.entryHref,
                chapterTitle = firstChapter.title
            )
        } else {
            VietnameseSentenceTokenizer.tokenizePlainTextChapter(
                rawText = opfData.description.ifBlank { opfData.title },
                chapterTitleToStrip = firstChapter.title
            )
        }

        val totalChapters = opfData.chapters.size.coerceAtLeast(1)
        val estimatedTotalMinutes = (totalChapters * (firstDurationSec / 60f).coerceAtLeast(6f)).roundToInt()
        val totalDurationFormatted = if (estimatedTotalMinutes >= 60) {
            "${estimatedTotalMinutes / 60}g ${estimatedTotalMinutes % 60}p"
        } else {
            "${estimatedTotalMinutes}p"
        }

        val firstDurationLabel = VietnameseSentenceTokenizer.formatTimestamp(firstDurationSec)
        val updatedChapters = opfData.chapters.mapIndexed { idx, ch ->
            if (idx == 0) {
                ch.copy(
                    durationLabel = firstDurationLabel,
                    status = ChapterStatus.PLAYING,
                    progressPercent = 0,
                    remainingLabel = "Còn $firstDurationLabel"
                )
            } else {
                ch
            }
        }

        val now = System.currentTimeMillis()
        return Book(
            id = bookId,
            title = opfData.title,
            author = opfData.author,
            categoryBadge = if (opfData.seriesGenre.isNotBlank()) opfData.seriesGenre else "Sách EPUB",
            genreLabel = if (opfData.seriesGenre.isNotBlank()) opfData.seriesGenre else "Tiểu thuyết / Ký sự",
            totalDurationFormatted = totalDurationFormatted,
            totalChaptersCount = totalChapters,
            synopsis = opfData.description.ifBlank {
                "Tác phẩm ${opfData.title} của tác giả ${opfData.author}, gồm $totalChapters chương/phần."
            },
            currentChapterTitle = firstChapter.fullDisplayTitle,
            currentChapterShortLabel = "Chương ${firstChapter.number}",
            progressPercent = 0,
            remainingTimeLabel = "Còn $firstDurationLabel",
            voiceName = defaultVoiceName,
            voiceRegion = defaultVoiceRegion,
            format = BookFormat.EPUB,
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
            epubFilePath = epubFile.absolutePath,
            coverImagePath = coverFilePath,
            currentChapterNumber = firstChapter.number
        )
    }

    /**
     * Loads and tokenizes a single chapter on-demand from `.epub` or `.txt`.
     * Supports multi-file spine ranges and `#startAnchor~endAnchor` slicing (`file1#a~b;file2`).
     */
    fun loadEpubChapterSentences(
        epubFilePath: String,
        entryHref: String,
        chapterTitle: String = ""
    ): Pair<List<TranscriptSentence>, Int> {
        val file = File(epubFilePath)
        if (!file.exists()) {
            return VietnameseSentenceTokenizer.tokenizePlainTextChapter(
                "Không tìm thấy tệp sách gốc trên thiết bị.",
                chapterTitle
            )
        }

        if (file.extension.equals("txt", ignoreCase = true)) {
            return loadTxtChapterSentences(file, entryHref, chapterTitle)
        }

        return runCatching {
            ZipFile(file).use { zip ->
                val segmentSpecs = entryHref.split(';').filter { it.isNotBlank() }
                val combinedHtml = StringBuilder()

                for (spec in segmentSpecs) {
                    val filePart = spec.substringBefore('#')
                    val anchorPart = spec.substringAfter('#', "")
                    val startAnchor = anchorPart.substringBefore('~', "").takeIf { it.isNotBlank() }
                    val endAnchor = anchorPart.substringAfter('~', "").takeIf { it.isNotBlank() }

                    val entry = findZipEntryCompat(zip, filePart) ?: continue
                    val rawHtml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val slicedHtml = sliceHtmlByAnchors(rawHtml, startAnchor, endAnchor)
                    combinedHtml.append(slicedHtml).append("\n")
                }

                if (combinedHtml.isEmpty()) {
                    error("Không tìm thấy nội dung chương $entryHref")
                }

                VietnameseSentenceTokenizer.tokenizeHtmlChapter(combinedHtml.toString(), chapterTitle)
            }
        }.getOrElse {
            VietnameseSentenceTokenizer.tokenizePlainTextChapter(
                "Không thể đọc nội dung chương này (${it.message}).",
                chapterTitle
            )
        }
    }

    /**
     * Slices an HTML document string between optional `id="<startAnchor>"` and `id="<endAnchor>"` markers.
     */
    private fun sliceHtmlByAnchors(
        rawHtml: String,
        startAnchor: String?,
        endAnchor: String?
    ): String {
        val bodyStartIdx = rawHtml.indexOf("<body", ignoreCase = true).let { idx ->
            if (idx >= 0) rawHtml.indexOf('>', idx).takeIf { it >= 0 }?.plus(1) ?: 0 else 0
        }
        val bodyEndIdx = rawHtml.lastIndexOf("</body>", ignoreCase = true).takeIf { it > bodyStartIdx }
            ?: rawHtml.length

        var sliceStart = bodyStartIdx
        var sliceEnd = bodyEndIdx

        if (!startAnchor.isNullOrBlank()) {
            val anchorPos = findAnchorPosition(rawHtml, startAnchor, bodyStartIdx)
            if (anchorPos in bodyStartIdx until bodyEndIdx) {
                sliceStart = anchorPos
            }
        }

        if (!endAnchor.isNullOrBlank()) {
            val anchorPos = findAnchorPosition(rawHtml, endAnchor, sliceStart)
            if (anchorPos in sliceStart..bodyEndIdx) {
                sliceEnd = anchorPos
            }
        }

        return if (sliceStart < sliceEnd) {
            rawHtml.substring(sliceStart, sliceEnd)
        } else {
            rawHtml
        }
    }

    private fun findAnchorPosition(html: String, anchorId: String, startFrom: Int): Int {
        val pattern = Regex("""id\s*=\s*["']${Regex.escape(anchorId)}["']""", RegexOption.IGNORE_CASE)
        val match = pattern.find(html, startFrom) ?: return -1
        // Walk backwards to the opening `<` of the element carrying this id
        val tagStart = html.lastIndexOf('<', match.range.first)
        return if (tagStart >= 0) tagStart else match.range.first
    }

    /**
     * Reloads the cached [BookChapter] list for an imported EPUB/TXT book in a few milliseconds.
     */
    fun loadCachedChapters(
        epubFilePath: String,
        currentChapterNumber: Int,
        currentProgressPercent: Int
    ): List<BookChapter> {
        val bookFile = File(epubFilePath)
        val cacheFile = File(bookFile.parentFile, "${bookFile.nameWithoutExtension}.chapters.v2.idx")
        if (!cacheFile.exists() && bookFile.exists() && bookFile.extension.equals("epub", true)) {
            // Automatically rebuild v2 chapter cache for previously imported EPUBs
            runCatching {
                ZipFile(bookFile).use { zip ->
                    val opfEntryPath = findOpfPath(zip) ?: "metadata.opf"
                    val opfBaseDir = opfEntryPath.substringBeforeLast('/', "").let {
                        if (it.isEmpty()) "" else "$it/"
                    }
                    val parsed = parseOpfAndToc(zip, opfEntryPath, opfBaseDir, bookFile.name)
                    saveChaptersIndexCache(bookFile, parsed.chapters)
                }
            }
        }
        if (!cacheFile.exists()) return emptyList()

        return runCatching {
            cacheFile.useLines(Charsets.UTF_8) { lines ->
                lines.mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size < 4) return@mapNotNull null
                    val num = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val href = parts[1]
                    val title = parts[2]
                    val durationLabel = parts[3]

                    val status = when {
                        num < currentChapterNumber -> ChapterStatus.COMPLETED
                        num == currentChapterNumber -> ChapterStatus.PLAYING
                        else -> ChapterStatus.UNPLAYED
                    }
                    val progress = when {
                        num < currentChapterNumber -> 100
                        num == currentChapterNumber -> currentProgressPercent
                        else -> 0
                    }
                    val remaining = if (num == currentChapterNumber) "Còn $durationLabel" else ""

                    BookChapter(
                        number = num,
                        title = title,
                        durationLabel = durationLabel,
                        status = status,
                        progressPercent = progress,
                        remainingLabel = remaining,
                        entryHref = href
                    )
                }.toList()
            }
        }.getOrDefault(emptyList())
    }

    private fun saveChaptersIndexCache(bookFile: File, chapters: List<BookChapter>) {
        runCatching {
            val cacheFile = File(bookFile.parentFile, "${bookFile.nameWithoutExtension}.chapters.v2.idx")
            cacheFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (ch in chapters) {
                    val safeTitle = ch.title.replace('\t', ' ').replace('\n', ' ')
                    val safeHref = ch.entryHref.replace('\t', ' ').replace('\n', ' ')
                    writer.write("${ch.number}\t$safeHref\t$safeTitle\t${ch.durationLabel}\n")
                }
            }
        }
    }

    private fun parseOpfAndToc(
        zip: ZipFile,
        opfEntryPath: String,
        opfBaseDir: String,
        fallbackFileName: String
    ): ParsedOpfData {
        var title = ""
        var author = ""
        var description = ""
        var seriesGenre = ""
        var coverMetaId: String? = null
        var tocItemId: String? = null

        val manifestHrefById = LinkedHashMap<String, String>()
        val spineIds = ArrayList<String>(2200)

        val opfEntry = findZipEntryCompat(zip, opfEntryPath)
        if (opfEntry != null) {
            zip.getInputStream(opfEntry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")

                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val tagName = parser.name.lowercase()
                        when {
                            tagName == "dc:title" || tagName.endsWith(":title") -> {
                                val text = parser.nextText().trim()
                                if (title.isBlank() && text.isNotBlank()) title = text
                            }
                            tagName == "dc:creator" || tagName.endsWith(":creator") -> {
                                val text = parser.nextText().trim()
                                if (author.isBlank() && text.isNotBlank()) author = text
                            }
                            tagName == "dc:description" || tagName.endsWith(":description") -> {
                                val rawDesc = parser.nextText().trim()
                                if (description.isBlank() && rawDesc.isNotBlank()) {
                                    description = cleanBookDescription(rawDesc)
                                }
                            }
                            tagName == "meta" -> {
                                val metaName = parser.getAttributeValue(null, "name")?.lowercase()
                                val metaContent = parser.getAttributeValue(null, "content")?.trim()
                                if (metaName == "cover" && !metaContent.isNullOrBlank()) {
                                    coverMetaId = metaContent
                                } else if (metaName == "calibre:series" && !metaContent.isNullOrBlank()) {
                                    seriesGenre = metaContent
                                }
                            }
                            tagName == "item" -> {
                                val id = parser.getAttributeValue(null, "id")
                                val href = parser.getAttributeValue(null, "href")
                                val properties = parser.getAttributeValue(null, "properties") ?: ""
                                if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                                    manifestHrefById[id] = decodeHref(href)
                                    if (properties.contains("cover-image") && coverMetaId == null) {
                                        coverMetaId = id
                                    }
                                }
                            }
                            tagName == "spine" -> {
                                tocItemId = parser.getAttributeValue(null, "toc")
                            }
                            tagName == "itemref" -> {
                                val idref = parser.getAttributeValue(null, "idref")
                                if (!idref.isNullOrBlank()) {
                                    spineIds.add(idref)
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        }

        val orderedSpineHrefs = if (spineIds.isNotEmpty()) {
            spineIds.mapNotNull { id ->
                manifestHrefById[id]?.substringBefore('#')?.let { resolveZipPath(opfBaseDir, it) }
            }
        } else {
            manifestHrefById.values
                .filter { it.endsWith(".html", true) || it.endsWith(".xhtml", true) || it.endsWith(".htm", true) }
                .map { resolveZipPath(opfBaseDir, it.substringBefore('#')) }
        }

        val tocHref = tocItemId?.let { manifestHrefById[it] } ?: "toc.ncx"
        val tocEntries = parseTocNcxEntries(zip, resolveZipPath(opfBaseDir, tocHref), opfBaseDir)

        val chapters = buildUniversalChapters(orderedSpineHrefs, tocEntries)

        val coverHref = coverMetaId?.let { idOrHref ->
            manifestHrefById[idOrHref] ?: idOrHref
        } ?: manifestHrefById.entries.firstOrNull { (id, href) ->
            id.contains("cover", ignoreCase = true) &&
                (href.endsWith(".jpg", true) || href.endsWith(".jpeg", true) || href.endsWith(".png", true))
        }?.value

        val fallbackCleanTitle = fallbackFileName
            .substringBeforeLast('.')
            .split(" - ")
            .firstOrNull()
            ?.trim()
            .orEmpty()
            .ifBlank { "Sách EPUB" }

        val fallbackAuthor = fallbackFileName
            .substringBeforeLast('.')
            .split(" - ")
            .getOrNull(1)
            ?.trim()
            .orEmpty()
            .ifBlank { "Tác giả chưa rõ" }

        return ParsedOpfData(
            title = title.ifBlank { fallbackCleanTitle },
            author = author.ifBlank { fallbackAuthor },
            description = description,
            seriesGenre = seriesGenre,
            coverHref = coverHref,
            chapters = chapters
        )
    }

    /**
     * Universal Chapter Builder:
     * - When `toc.ncx` defines structured chapters (even when split across `index_split_XXX.html` files
     *   or anchored via `#filepos...`), builds chapters from `toc.ncx` and assigns every intermediate
     *   `<spine>` file and anchor boundary to the owning chapter, while automatically merging `(tt)` continuations!
     * - When `toc.ncx` is absent, falls back to 1 chapter per `<spine>` HTML item.
     */
    private fun buildUniversalChapters(
        orderedSpineHrefs: List<String>,
        rawTocEntries: List<TocEntry>
    ): List<BookChapter> {
        val spineIndexByHref = HashMap<String, Int>(orderedSpineHrefs.size * 2)
        orderedSpineHrefs.forEachIndexed { idx, href ->
            spineIndexByHref[href] = idx
        }

        // Filter TOC entries that actually exist in spine and merge "(tt)" continuation entries
        val validToc = rawTocEntries.filter { spineIndexByHref.containsKey(it.zipHref) }
        val mergedToc = ArrayList<TocEntry>(validToc.size)
        for (entry in validToc) {
            if (mergedToc.isNotEmpty() && CONTINUATION_CHAPTER_REGEX.matches(entry.label.trim())) {
                // Continuation of previous chapter (e.g. "Chương 1 (tt)") -> merge into previous chapter's range!
                continue
            }
            mergedToc.add(entry)
        }

        if (mergedToc.size >= 2) {
            val result = ArrayList<BookChapter>(mergedToc.size)
            for (i in mergedToc.indices) {
                val current = mergedToc[i]
                val next = mergedToc.getOrNull(i + 1)

                val startSpineIdx = spineIndexByHref[current.zipHref] ?: continue
                val endSpineIdx = if (next != null) {
                    val nextIdx = spineIndexByHref[next.zipHref] ?: orderedSpineHrefs.lastIndex
                    // If next chapter starts at an anchor inside `nextIdx`, `nextIdx` is shared!
                    if (!next.anchorId.isNullOrBlank()) nextIdx else (nextIdx - 1).coerceAtLeast(startSpineIdx)
                } else {
                    orderedSpineHrefs.lastIndex
                }

                val segmentSpecs = mutableListOf<String>()
                for (sIdx in startSpineIdx..endSpineIdx) {
                    val fileHref = orderedSpineHrefs[sIdx]
                    val startAnchor = if (sIdx == startSpineIdx) current.anchorId else null
                    val endAnchor = if (next != null && sIdx == spineIndexByHref[next.zipHref] && !next.anchorId.isNullOrBlank()) {
                        next.anchorId
                    } else {
                        null
                    }

                    if (startAnchor != null || endAnchor != null) {
                        segmentSpecs.add("$fileHref#${startAnchor.orEmpty()}~${endAnchor.orEmpty()}")
                    } else {
                        segmentSpecs.add(fileHref)
                    }
                }

                val chapterNum = i + 1
                val cleanTitle = cleanChapterTitle(current.label, chapterNum)
                result.add(
                    BookChapter(
                        number = chapterNum,
                        title = cleanTitle,
                        durationLabel = "15:00",
                        status = if (chapterNum == 1) ChapterStatus.PLAYING else ChapterStatus.UNPLAYED,
                        progressPercent = 0,
                        remainingLabel = if (chapterNum == 1) "Còn 15:00" else "",
                        entryHref = segmentSpecs.joinToString(";")
                    )
                )
            }
            if (result.isNotEmpty()) return result
        }

        // Fallback: 1 chapter per spine HTML entry
        val chapters = ArrayList<BookChapter>(orderedSpineHrefs.size)
        var chapterCounter = 1
        for (fullZipHref in orderedSpineHrefs) {
            val baseFileName = fullZipHref.substringAfterLast('/').substringBeforeLast('.')
            if (orderedSpineHrefs.size > 1 &&
                (baseFileName.equals("cover", true) || baseFileName.equals("titlepage", true) || baseFileName.equals("nav", true))
            ) {
                continue
            }
            val cleanTitle = cleanChapterTitle(baseFileName, chapterCounter)
            chapters.add(
                BookChapter(
                    number = chapterCounter,
                    title = cleanTitle,
                    durationLabel = "14:30",
                    status = if (chapterCounter == 1) ChapterStatus.PLAYING else ChapterStatus.UNPLAYED,
                    progressPercent = 0,
                    remainingLabel = if (chapterCounter == 1) "Còn 14:30" else "",
                    entryHref = fullZipHref
                )
            )
            chapterCounter++
        }
        return chapters
    }

    private fun parseTocNcxEntries(
        zip: ZipFile,
        tocZipPath: String,
        opfBaseDir: String
    ): List<TocEntry> {
        val tocEntry = findZipEntryCompat(zip, tocZipPath) ?: return emptyList()
        val result = ArrayList<TocEntry>(2200)

        runCatching {
            zip.getInputStream(tocEntry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")

                var currentLabel = ""
                var insideNavLabel = false
                var event = parser.eventType

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name.lowercase()) {
                                "navlabel" -> insideNavLabel = true
                                "text" -> if (insideNavLabel) {
                                    currentLabel = parser.nextText().trim()
                                }
                                "content" -> {
                                    val src = parser.getAttributeValue(null, "src")
                                    if (!src.isNullOrBlank() && currentLabel.isNotBlank()) {
                                        val decodedSrc = decodeHref(src)
                                        val filePart = decodedSrc.substringBefore('#')
                                        val anchorPart = decodedSrc.substringAfter('#', "").takeIf { it.isNotBlank() }
                                        result.add(
                                            TocEntry(
                                                label = currentLabel,
                                                zipHref = resolveZipPath(opfBaseDir, filePart),
                                                anchorId = anchorPart
                                            )
                                        )
                                        currentLabel = ""
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name.equals("navLabel", ignoreCase = true)) {
                                insideNavLabel = false
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        }
        return result
    }

    /**
     * Normalizes chapter titles across both web-novels ("Chương 1 Xa Nhà") and commercial books
     * ("Lời Nhà xuất bản", "Chương mở đầu: ...", "CHƯƠNG KẾT: MỘT CUỘC ĐỜI HAI MẶT PHI THƯỜNG").
     */
    fun cleanChapterTitle(rawTitle: String, chapterNumber: Int): String {
        val trimmed = rawTitle.trim().replace(Regex("\\s+"), " ")
        val match = CHAPTER_PREFIX_REGEX.matchEntire(trimmed)
        if (match != null) {
            val rest = match.groupValues[2].trim()
            return normalizeAllCapsTitle(rest.ifBlank { "Phần $chapterNumber" })
        }
        return normalizeAllCapsTitle(trimmed.ifBlank { "Phần $chapterNumber" })
    }

    /**
     * Converts shouty ALL-CAPS Vietnamese titles (e.g. "MỘT CUỘC ĐỜI HAI MẶT PHI THƯỜNG")
     * into readable sentence/title case while keeping short acronyms (like "X6", "CIA") intact.
     */
    private fun normalizeAllCapsTitle(text: String): String {
        val letters = text.filter { it.isLetter() }
        if (letters.length > 6 && letters.all { it.isUpperCase() }) {
            val lower = text.lowercase(Locale("vi", "VN"))
            return lower.replaceFirstChar { it.titlecase(Locale("vi", "VN")) }
        }
        return text
    }

    /**
     * Cleans `<dc:description>` HTML, removing Vinabook / store boilerplate lines if present.
     */
    private fun cleanBookDescription(rawDescHtml: String): String {
        val withLineBreaks = rawDescHtml
            .replace(Regex("<(br|p|div)[^>]*>", RegexOption.IGNORE_CASE), "\n")
        val plain = VietnameseSentenceTokenizer.cleanHtmlFragment(withLineBreaks)
        val lines = plain.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("Tác giả:", ignoreCase = true) &&
                    !line.startsWith("Người dịch:", ignoreCase = true) &&
                    !line.startsWith("Nhà xuất bản:", ignoreCase = true) &&
                    !line.startsWith("Nhà phát hành:", ignoreCase = true) &&
                    !line.startsWith("Khổ sách:", ignoreCase = true) &&
                    !line.startsWith("Số trang:", ignoreCase = true)
            }
        return lines.joinToString("\n\n").ifBlank { plain }
    }

    private fun parseLocalTxtFile(
        bookId: String,
        txtFile: File,
        originalFileName: String,
        defaultVoiceName: String,
        defaultVoiceRegion: String
    ): Book {
        val rawText = txtFile.readText(Charsets.UTF_8)
        val cleanBookTitle = originalFileName
            .substringBeforeLast('.')
            .replace('_', ' ')
            .trim()
            .ifBlank { "Tệp văn bản TXT" }

        val chapterBlocks = splitTxtIntoChapters(rawText)
        val chapters = chapterBlocks.mapIndexed { idx, (title, rangeSpec) ->
            val chapterNum = idx + 1
            BookChapter(
                number = chapterNum,
                title = title,
                durationLabel = "12:00",
                status = if (chapterNum == 1) ChapterStatus.PLAYING else ChapterStatus.UNPLAYED,
                progressPercent = 0,
                remainingLabel = if (chapterNum == 1) "Còn 12:00" else "",
                entryHref = rangeSpec
            )
        }
        saveChaptersIndexCache(txtFile, chapters)

        val firstChapter = chapters.first()
        val (firstSentences, firstDurationSec) = loadTxtChapterSentences(
            txtFile = txtFile,
            rangeSpec = firstChapter.entryHref,
            chapterTitle = firstChapter.title
        )
        val now = System.currentTimeMillis()
        return Book(
            id = bookId,
            title = cleanBookTitle,
            author = "Tệp TXT cá nhân",
            categoryBadge = "Văn bản TXT",
            genreLabel = "Tài liệu / Truyện",
            totalDurationFormatted = "${(chapters.size * 12) / 60}g ${(chapters.size * 12) % 60}p",
            totalChaptersCount = chapters.size,
            synopsis = firstSentences.take(3).joinToString(" ") { it.text },
            currentChapterTitle = firstChapter.fullDisplayTitle,
            currentChapterShortLabel = "Chương 1",
            progressPercent = 0,
            remainingTimeLabel = "Còn ${VietnameseSentenceTokenizer.formatTimestamp(firstDurationSec)}",
            voiceName = defaultVoiceName,
            voiceRegion = defaultVoiceRegion,
            format = BookFormat.TXT,
            coverGradient = CoverGradient(0xFF2B4C3F, 0xFF1B3329, 0xFF0E1C16),
            isNew = true,
            isImported = true,
            isCompleted = false,
            addedAtEpochMillis = now,
            lastReadEpochMillis = now,
            totalDurationSeconds = firstDurationSec,
            currentPositionSeconds = 0,
            sentences = firstSentences,
            chapters = chapters,
            bookmarks = emptyList(),
            epubFilePath = txtFile.absolutePath,
            coverImagePath = null,
            currentChapterNumber = 1
        )
    }

    private fun splitTxtIntoChapters(rawText: String): List<Pair<String, String>> {
        val normalized = rawText.replace("\r\n", "\n")
        val headingRegex = Regex("(?m)^\\s*(?:Chương|Phần|Hồi|Chapter)\\s+\\d+[^\\n]*$")
        val matches = headingRegex.findAll(normalized).toList()

        if (matches.size >= 2) {
            return matches.mapIndexed { idx, match ->
                val startOffset = match.range.first
                val endOffset = if (idx + 1 < matches.size) matches[idx + 1].range.first else normalized.length
                val rawHeading = match.value.trim()
                val cleanedTitle = cleanChapterTitle(rawHeading, idx + 1)
                cleanedTitle to "$startOffset:$endOffset"
            }
        }

        val chunks = mutableListOf<Pair<String, String>>()
        var start = 0
        var chapterIdx = 1
        while (start < normalized.length) {
            var end = (start + 8000).coerceAtMost(normalized.length)
            if (end < normalized.length) {
                val nextNewline = normalized.indexOf('\n', end)
                if (nextNewline in end..(end + 1500)) {
                    end = nextNewline
                }
            }
            chunks.add("Phần $chapterIdx" to "$start:$end")
            start = end
            chapterIdx++
        }
        if (chunks.isEmpty()) {
            chunks.add("Mở đầu" to "0:${normalized.length}")
        }
        return chunks
    }

    private fun loadTxtChapterSentences(
        txtFile: File,
        rangeSpec: String,
        chapterTitle: String
    ): Pair<List<TranscriptSentence>, Int> {
        val normalized = txtFile.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val parts = rangeSpec.split(':')
        val start = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, normalized.length) ?: 0
        val end = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(start, normalized.length) ?: normalized.length
        val slice = normalized.substring(start, end)
        return VietnameseSentenceTokenizer.tokenizePlainTextChapter(slice, chapterTitle)
    }

    private fun findOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        return runCatching {
            zip.getInputStream(containerEntry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name.endsWith("rootfile")) {
                        val fullPath = parser.getAttributeValue(null, "full-path")
                        if (!fullPath.isNullOrBlank()) return@use fullPath
                    }
                    event = parser.next()
                }
                null
            }
        }.getOrNull()
    }

    private fun findZipEntryCompat(zip: ZipFile, candidatePath: String) =
        zip.getEntry(candidatePath)
            ?: zip.getEntry(decodeHref(candidatePath))
            ?: zip.getEntry(candidatePath.removePrefix("/"))

    private fun findFallbackCoverEntry(zip: ZipFile): String? {
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val lower = entry.name.lowercase()
            if (lower.contains("cover") && (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
                return entry.name
            }
        }
        return null
    }

    private fun extractGradientFromImage(imageFile: File): CoverGradient? = runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath, opts) ?: return null
        val w = bmp.width.coerceAtLeast(1)
        val h = bmp.height.coerceAtLeast(1)

        val topPixel = darkenColor(bmp.getPixel(w / 2, h / 5), 0.72f)
        val midPixel = darkenColor(bmp.getPixel(w / 2, h / 2), 0.55f)
        val botPixel = darkenColor(bmp.getPixel(w / 2, (h * 4) / 5), 0.35f)
        bmp.recycle()

        CoverGradient(
            startColorHex = topPixel,
            centerColorHex = midPixel,
            endColorHex = botPixel
        )
    }.getOrNull()

    private fun darkenColor(argb: Int, factor: Float): Long {
        val r = (((argb shr 16) and 0xFF) * factor).roundToInt().coerceIn(18, 220)
        val g = (((argb shr 8) and 0xFF) * factor).roundToInt().coerceIn(12, 200)
        val b = ((argb and 0xFF) * factor).roundToInt().coerceIn(10, 200)
        return (0xFF000000L) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    private fun resolveZipPath(baseDir: String, relativeHref: String): String {
        if (baseDir.isEmpty() || relativeHref.startsWith(baseDir)) return relativeHref
        return "$baseDir${relativeHref.removePrefix("/")}"
    }

    private fun decodeHref(raw: String): String = try {
        URLDecoder.decode(raw, "UTF-8")
    } catch (_: Throwable) {
        raw
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return@use cursor.getString(idx)
            }
            null
        } ?: uri.lastPathSegment?.substringAfterLast('/')
    }.getOrNull()
}
