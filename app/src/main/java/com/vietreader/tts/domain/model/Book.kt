package com.vietreader.tts.domain.model

import androidx.compose.runtime.Immutable

enum class BookFormat(val label: String) {
    EPUB("EPUB"),
    TXT("TXT"),
    PDF("PDF")
}

@Immutable
data class CoverGradient(
    val startColorHex: Long,
    val centerColorHex: Long,
    val endColorHex: Long
)

@Immutable
data class TranscriptSentence(
    val id: Int,
    val text: String,
    val timestampLabel: String,
    val timestampSeconds: Int,
    val progressPercent: Float
)

enum class ChapterStatus {
    COMPLETED,
    PLAYING,
    UNPLAYED
}

@Immutable
data class BookChapter(
    val number: Int,
    val title: String,
    val durationLabel: String,
    val status: ChapterStatus,
    val progressPercent: Int = 0,
    val remainingLabel: String = "",
    val entryHref: String = ""
) {
    val fullDisplayTitle: String
        get() {
            val trimmed = title.trim()
            val lower = trimmed.lowercase()
            val hasOwnPrefix = lower.startsWith("chương ") ||
                lower.startsWith("phần ") ||
                lower.startsWith("quyển ") ||
                lower.startsWith("hồi ") ||
                lower.startsWith("lời ") ||
                lower.startsWith("lá thư") ||
                lower.startsWith("mở đầu") ||
                lower.startsWith("phụ lục") ||
                lower.startsWith("chú thích") ||
                lower.startsWith("về các")
            return if (hasOwnPrefix) trimmed else "Chương $number: $trimmed"
        }
}

@Immutable
data class BookBookmark(
    val id: String,
    val chapterTitle: String,
    val quoteText: String,
    val timestampLabel: String,
    val chapterNumber: Int = 1,
    val sentenceIndex: Int = 0
)

@Immutable
data class VoiceOption(
    val id: String,
    val name: String,
    val regionTag: String,
    val description: String,
    val isAiVoice: Boolean = false
) {
    val displayWithRegion: String
        get() = "$name ($regionTag)"
}

enum class BookDetailTab(val label: String) {
    CHAPTERS("Mục lục chương"),
    BOOKMARKS("Đánh dấu (4)"),
    INFO("Thông tin sách")
}

@Immutable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val categoryBadge: String = "Tác phẩm kinh điển",
    val genreLabel: String = "Truyện dài",
    val totalDurationFormatted: String = "3g 15p",
    val totalChaptersCount: Int = 10,
    val synopsis: String = "",
    val currentChapterTitle: String,
    val currentChapterShortLabel: String,
    val progressPercent: Int,
    val remainingTimeLabel: String,
    val voiceName: String,
    val voiceRegion: String = "Miền Bắc",
    val format: BookFormat = BookFormat.EPUB,
    val coverGradient: CoverGradient,
    val isNew: Boolean = false,
    val isImported: Boolean = true,
    val isCompleted: Boolean = false,
    val addedAtEpochMillis: Long = 0L,
    val lastReadEpochMillis: Long = 0L,
    val totalDurationSeconds: Int = 1590,
    val currentPositionSeconds: Int = 765,
    val sentences: List<TranscriptSentence> = emptyList(),
    val chapters: List<BookChapter> = emptyList(),
    val bookmarks: List<BookBookmark> = emptyList(),
    val epubFilePath: String? = null,
    val coverImagePath: String? = null,
    val currentChapterNumber: Int = 1
) {
    companion object {
        val EMPTY = Book(
            id = "",
            title = "Chưa có sách được chọn",
            author = "Vui lòng nhập sách từ Thư viện",
            categoryBadge = "Thư viện trống",
            genreLabel = "EPUB / TXT",
            totalDurationFormatted = "00:00",
            totalChaptersCount = 0,
            synopsis = "Hãy quay lại màn hình Thư viện và nhấn nút \"+ Thêm\" để nhập tệp sách .epub hoặc .txt từ thiết bị của bạn.",
            currentChapterTitle = "Chưa có chương",
            currentChapterShortLabel = "Chương 0",
            progressPercent = 0,
            remainingTimeLabel = "00:00",
            voiceName = "Giọng hệ thống 1",
            voiceRegion = "Ngoại tuyến",
            format = BookFormat.EPUB,
            coverGradient = CoverGradient(
                startColorHex = 0xFF5D2A0C,
                centerColorHex = 0xFF3E1B07,
                endColorHex = 0xFF210D03
            ),
            isNew = false,
            isImported = true,
            isCompleted = false,
            totalDurationSeconds = 0,
            currentPositionSeconds = 0,
            sentences = emptyList(),
            chapters = emptyList(),
            bookmarks = emptyList(),
            currentChapterNumber = 1
        )
    }
}

@Immutable
data class PlaybackState(
    val bookId: String,
    val title: String,
    val author: String,
    val chapterTitle: String,
    val chapterShortLabel: String,
    val progressPercent: Int,
    val currentTime: String,
    val totalTime: String,
    val currentSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val remainingLabel: String,
    val voiceName: String,
    val isPlaying: Boolean,
    val playbackSpeed: Float = 1.0f,
    val activeSentenceIndex: Int = 0,
    val isBookmarked: Boolean = false,
    val coverGradient: CoverGradient,
    val coverImagePath: String? = null,
    val activeWordStart: Int = -1,
    val activeWordEnd: Int = -1
) {
    companion object {
        val EMPTY = PlaybackState(
            bookId = "",
            title = "Chưa có sách đang phát",
            author = "Wordwave",
            chapterTitle = "Chưa chọn chương",
            chapterShortLabel = "Chương 0",
            progressPercent = 0,
            currentTime = "00:00",
            totalTime = "00:00",
            currentSeconds = 0,
            totalSeconds = 0,
            remainingLabel = "00:00",
            voiceName = "Giọng hệ thống 1",
            isPlaying = false,
            playbackSpeed = 1.0f,
            activeSentenceIndex = 0,
            isBookmarked = false,
            coverGradient = CoverGradient(
                startColorHex = 0xFF5D2A0C,
                centerColorHex = 0xFF3E1B07,
                endColorHex = 0xFF210D03
            )
        )
    }
}

enum class ReaderMode(val label: String) {
    LISTEN("Lái xe"),
    READ_ALONG("Đọc đồng bộ")
}

enum class HighlightLevel(val label: String) {
    PARAGRAPH("Đoạn"),
    SENTENCE("Câu"),
    WORD("Từ")
}

enum class LibraryFilter(val id: String) {
    ALL("all"),
    PLAYING("playing"),
    COMPLETED("completed"),
    IMPORTED("imported")
}

enum class LibraryViewMode(val label: String) {
    GRID("Dạng lưới"),
    LIST("Dạng danh sách")
}

enum class LibrarySortOption(val label: String) {
    RECENTLY_ADDED("Mới thêm"),
    RECENTLY_READ("Đọc gần nhất"),
    TITLE("Tên sách"),
    AUTHOR("Tác giả"),
    PROGRESS("Tiến độ")
}
