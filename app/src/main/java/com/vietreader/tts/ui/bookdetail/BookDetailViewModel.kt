package com.vietreader.tts.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookBookmark
import com.vietreader.tts.domain.model.BookDetailTab
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceOption
import com.vietreader.tts.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class BookDetailViewModel(
    private val repository: BookRepository = BookRepositoryImpl.instance
) : ViewModel() {

    private data class DetailControls(
        val selectedTab: BookDetailTab = BookDetailTab.CHAPTERS,
        val isVoiceSheetVisible: Boolean = false,
        val isAllChaptersDownloaded: Boolean = false
    )

    private val controls = MutableStateFlow(DetailControls())

    val uiState: StateFlow<BookDetailUiState> = combine(
        repository.books,
        repository.selectedBookIdForDetail,
        repository.voiceItems,
        controls
    ) { books, selectedBookId, voices, ctrl ->
        val book = books.find { it.id == selectedBookId }
            ?: books.firstOrNull()
            ?: Book.EMPTY

        val installedVoiceOptions = voices
            .filter {
                it.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT ||
                    it.installStatus == VoiceInstallStatus.DOWNLOADED
            }
            .map { it.toVoiceOption() }
            .ifEmpty { VoiceCatalog.defaultVoiceOptions }

        BookDetailUiState(
            book = book,
            selectedTab = ctrl.selectedTab,
            voiceOptions = installedVoiceOptions,
            isVoiceSheetVisible = ctrl.isVoiceSheetVisible,
            isAllChaptersDownloaded = ctrl.isAllChaptersDownloaded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookDetailUiState(
            book = repository.books.value.find { it.id == repository.selectedBookIdForDetail.value }
                ?: repository.books.value.firstOrNull()
                ?: Book.EMPTY
        )
    )

    fun onSelectBook(bookId: String) {
        repository.selectBookForDetail(bookId)
    }

    fun onSelectTab(tab: BookDetailTab) {
        controls.update { it.copy(selectedTab = tab) }
    }

    fun onOpenVoiceSheet() {
        controls.update { it.copy(isVoiceSheetVisible = true) }
    }

    fun onDismissVoiceSheet() {
        controls.update { it.copy(isVoiceSheetVisible = false) }
    }

    fun onSelectVoiceOption(option: VoiceOption) {
        val currentBookId = repository.selectedBookIdForDetail.value
        repository.selectVoiceForBook(
            bookId = currentBookId,
            voiceName = option.name,
            voiceRegion = option.regionTag
        )
        controls.update { it.copy(isVoiceSheetVisible = false) }
    }

    fun onPlayContinueClick() {
        val currentBookId = repository.selectedBookIdForDetail.value
        repository.setReaderMode(ReaderMode.LISTEN)
        repository.selectBookForPlayback(currentBookId)
    }

    fun onReadBookClick() {
        val currentBookId = repository.selectedBookIdForDetail.value
        repository.setReaderMode(ReaderMode.READ_ALONG)
        repository.selectBookForPlayback(currentBookId)
    }

    fun onChapterClick(chapterNumber: Int) {
        val currentBookId = repository.selectedBookIdForDetail.value
        repository.setReaderMode(ReaderMode.READ_ALONG)
        repository.selectChapterForPlayback(currentBookId, chapterNumber)
    }

    fun onBookmarkClick(bookmark: BookBookmark) {
        val currentBookId = repository.selectedBookIdForDetail.value
        repository.setReaderMode(ReaderMode.READ_ALONG)
        repository.selectChapterForPlayback(currentBookId, bookmark.chapterNumber)
        repository.selectSentence(bookmark.sentenceIndex)
    }

    fun onDeleteBookmark(bookmarkId: String) {
        val currentBookId = repository.selectedBookIdForDetail.value
        repository.deleteBookmark(currentBookId, bookmarkId)
    }

    fun onDeleteBook() {
        val currentBookId = repository.selectedBookIdForDetail.value
        if (currentBookId.isNotBlank()) {
            repository.deleteBook(currentBookId)
        }
    }

    fun onDownloadAllClick() {
        controls.update { it.copy(isAllChaptersDownloaded = !it.isAllChaptersDownloaded) }
    }
}
