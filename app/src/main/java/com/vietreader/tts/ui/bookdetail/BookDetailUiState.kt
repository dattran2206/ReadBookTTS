package com.vietreader.tts.ui.bookdetail

import androidx.compose.runtime.Immutable
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookDetailTab
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceOption

@Immutable
data class BookDetailUiState(
    val book: Book = Book.EMPTY,
    val selectedTab: BookDetailTab = BookDetailTab.CHAPTERS,
    val voiceOptions: List<VoiceOption> = VoiceCatalog.defaultVoiceOptions,
    val isVoiceSheetVisible: Boolean = false,
    val isAllChaptersDownloaded: Boolean = false
)
