package com.vietreader.tts.ui.reader

import androidx.compose.runtime.Immutable
import com.vietreader.tts.domain.model.BookChapter
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.TranscriptSentence
import com.vietreader.tts.domain.model.VoiceCatalog

@Immutable
data class ReaderUiState(
    val playbackState: PlaybackState = PlaybackState.EMPTY,
    val sentences: List<TranscriptSentence> = emptyList(),
    val availableVoices: List<String> = VoiceCatalog.defaultAvailableVoiceNames,
    val readerMode: ReaderMode = ReaderMode.READ_ALONG,
    val isControlsCollapsed: Boolean = false,
    val isOverflowMenuExpanded: Boolean = false,
    val sleepTimerMinutes: Int = 30,
    val sleepTimerRemainingSeconds: Int = 30 * 60,
    val isAutoScrollEnabled: Boolean = true,
    val stopAtEndOfChapter: Boolean = false,
    val highlightLevel: HighlightLevel = HighlightLevel.SENTENCE,
    val readingThemeMode: ReadingThemeMode = ReadingThemeMode.IVORY,
    val fontSizePx: Int = 17,
    val currentChapterNumber: Int = 1,
    val totalChaptersCount: Int = 1,
    val chapters: List<BookChapter> = emptyList(),
    val isAppearanceSheetVisible: Boolean = false,
    val isTocSheetVisible: Boolean = false,
    val isSleepTimerSheetVisible: Boolean = false
) {
    val chapterHeaderLabel: String
        get() = playbackState.chapterShortLabel.uppercase()

    val chapterSubtitle: String
        get() = playbackState.chapterTitle
            .substringAfter(":", playbackState.chapterTitle)
            .trim()
}
