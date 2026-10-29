package com.vietreader.tts.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ReaderViewModel(
    private val repository: BookRepository = BookRepositoryImpl.instance
) : ViewModel() {

    private data class ReaderViewControls(
        val isControlsCollapsed: Boolean = false,
        val isOverflowMenuExpanded: Boolean = false,
        val isAppearanceSheetVisible: Boolean = false,
        val isTocSheetVisible: Boolean = false,
        val isSleepTimerSheetVisible: Boolean = false
    )

    private val viewControls = MutableStateFlow(ReaderViewControls())

    val uiState: StateFlow<ReaderUiState> = combine(
        repository.books,
        repository.playbackState,
        repository.appSettings,
        repository.sleepTimerMinutes,
        repository.sleepTimerRemainingSeconds,
        repository.readerMode,
        repository.voiceItems,
        viewControls
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val books = flows[0] as List<Book>
        val playbackState = flows[1] as PlaybackState?
        val appSettings = flows[2] as AppSettings
        val sleepTimerMinutes = flows[3] as Int
        val sleepTimerRemainingSeconds = flows[4] as Int
        val readerMode = flows[5] as ReaderMode
        @Suppress("UNCHECKED_CAST")
        val voiceItems = flows[6] as List<VoiceItem>
        val controls = flows[7] as ReaderViewControls

        val activePlayback = playbackState ?: PlaybackState.EMPTY
        val currentBook = books.find { it.id == activePlayback.bookId }
        val sentences = currentBook?.sentences.orEmpty()
        val availableVoices = voiceItems
            .filter {
                it.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT ||
                    it.installStatus == VoiceInstallStatus.DOWNLOADED
            }
            .map { it.name }
            .ifEmpty { VoiceCatalog.defaultAvailableVoiceNames }

        ReaderUiState(
            playbackState = activePlayback,
            sentences = sentences,
            availableVoices = availableVoices,
            readerMode = readerMode,
            isControlsCollapsed = controls.isControlsCollapsed,
            isOverflowMenuExpanded = controls.isOverflowMenuExpanded,
            sleepTimerMinutes = sleepTimerMinutes,
            sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
            isAutoScrollEnabled = appSettings.autoScrollEnabled,
            stopAtEndOfChapter = appSettings.stopAtEndOfChapter,
            highlightLevel = appSettings.highlightLevel,
            readingThemeMode = appSettings.themeMode,
            fontSizePx = appSettings.fontSizePx,
            currentChapterNumber = currentBook?.currentChapterNumber ?: 1,
            totalChaptersCount = currentBook?.totalChaptersCount ?: (currentBook?.chapters?.size ?: 1),
            chapters = currentBook?.chapters.orEmpty(),
            isAppearanceSheetVisible = controls.isAppearanceSheetVisible,
            isTocSheetVisible = controls.isTocSheetVisible,
            isSleepTimerSheetVisible = controls.isSleepTimerSheetVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = run {
            val activePlayback = repository.playbackState.value ?: PlaybackState.EMPTY
            val currentBook = repository.books.value.find { it.id == activePlayback.bookId }
            val sentences = currentBook?.sentences.orEmpty()
            val settings = repository.appSettings.value
            ReaderUiState(
                playbackState = activePlayback,
                sentences = sentences,
                availableVoices = VoiceCatalog.defaultAvailableVoiceNames,
                readerMode = repository.readerMode.value,
                sleepTimerMinutes = repository.sleepTimerMinutes.value,
                sleepTimerRemainingSeconds = repository.sleepTimerRemainingSeconds.value,
                isAutoScrollEnabled = settings.autoScrollEnabled,
                stopAtEndOfChapter = settings.stopAtEndOfChapter,
                highlightLevel = settings.highlightLevel,
                readingThemeMode = settings.themeMode,
                fontSizePx = settings.fontSizePx,
                currentChapterNumber = currentBook?.currentChapterNumber ?: 1,
                totalChaptersCount = currentBook?.totalChaptersCount ?: (currentBook?.chapters?.size ?: 1),
                chapters = currentBook?.chapters.orEmpty()
            )
        }
    )

    fun onPreviousChapterClick() {
        val currentBookId = uiState.value.playbackState.bookId
        val prevChap = (uiState.value.currentChapterNumber - 1).coerceAtLeast(1)
        if (prevChap != uiState.value.currentChapterNumber) {
            repository.selectChapterForPlayback(currentBookId, prevChap)
        }
    }

    fun onNextChapterClick() {
        val currentBookId = uiState.value.playbackState.bookId
        val nextChap = (uiState.value.currentChapterNumber + 1).coerceAtMost(uiState.value.totalChaptersCount)
        if (nextChap != uiState.value.currentChapterNumber) {
            repository.selectChapterForPlayback(currentBookId, nextChap)
        }
    }

    fun onSelectBook(bookId: String) {
        repository.selectBookForPlayback(bookId)
    }

    fun onModeChange(mode: ReaderMode) {
        repository.setReaderMode(mode)
    }

    fun onTogglePlayPause() {
        repository.togglePlayPause()
    }

    fun onSentenceClick(index: Int) {
        repository.selectSentence(index)
    }

    fun onPrevSentenceClick() {
        repository.skipToPreviousSentence()
    }

    fun onNextSentenceClick() {
        repository.skipToNextSentence()
    }

    fun onRewind15Click() {
        repository.seekByDeltaSeconds(-15)
    }

    fun onForward15Click() {
        repository.seekByDeltaSeconds(15)
    }

    fun onSeekToSeconds(seconds: Int) {
        repository.seekToSeconds(seconds)
    }

    fun onCycleSpeedClick() {
        repository.cyclePlaybackSpeed()
    }

    fun onSelectVoice(voiceName: String) {
        repository.selectVoice(voiceName)
    }

    fun onToggleBookmark() {
        repository.toggleBookmark()
    }

    fun onToggleControlsCollapsed() {
        viewControls.update { it.copy(isControlsCollapsed = !it.isControlsCollapsed) }
    }

    fun onToggleOverflowMenu() {
        viewControls.update { it.copy(isOverflowMenuExpanded = !it.isOverflowMenuExpanded) }
    }

    fun onDismissOverflowMenu() {
        viewControls.update { it.copy(isOverflowMenuExpanded = false) }
    }

    fun onOpenSleepTimerSheet() {
        viewControls.update { it.copy(isSleepTimerSheetVisible = true, isOverflowMenuExpanded = false) }
    }

    fun onDismissSleepTimerSheet() {
        viewControls.update { it.copy(isSleepTimerSheetVisible = false) }
    }

    fun onSelectSleepTimerMinutes(minutes: Int) {
        repository.setSleepTimer(minutes)
        viewControls.update { it.copy(isSleepTimerSheetVisible = false) }
    }

    fun onToggleStopAtEndOfChapter(stop: Boolean) {
        val currentSettings = repository.appSettings.value
        repository.updateAudioPlaybackSettings(
            autoScrollEnabled = currentSettings.autoScrollEnabled,
            stopAtEndOfChapter = stop
        )
    }

    fun onCycleSleepTimer() {
        onOpenSleepTimerSheet()
    }

    fun onToggleAutoScroll() {
        val currentSettings = repository.appSettings.value
        repository.updateAudioPlaybackSettings(
            autoScrollEnabled = !currentSettings.autoScrollEnabled,
            stopAtEndOfChapter = currentSettings.stopAtEndOfChapter
        )
    }

    fun onHighlightLevelChange(level: HighlightLevel) {
        repository.updateHighlightLevel(level)
    }

    fun onOpenAppearanceSheet() {
        viewControls.update { it.copy(isAppearanceSheetVisible = true, isOverflowMenuExpanded = false) }
    }

    fun onDismissAppearanceSheet() {
        viewControls.update { it.copy(isAppearanceSheetVisible = false) }
    }

    fun onOpenTocSheet() {
        viewControls.update { it.copy(isTocSheetVisible = true, isOverflowMenuExpanded = false) }
    }

    fun onDismissTocSheet() {
        viewControls.update { it.copy(isTocSheetVisible = false) }
    }

    fun onSelectChapter(chapterNumber: Int) {
        val currentBookId = uiState.value.playbackState.bookId
        repository.selectChapterForPlayback(currentBookId, chapterNumber)
        viewControls.update { it.copy(isTocSheetVisible = false) }
    }

    fun onUpdateAppearance(themeMode: ReadingThemeMode, fontSizePx: Int) {
        repository.updateAppearanceSettings(themeMode, fontSizePx)
    }
}
