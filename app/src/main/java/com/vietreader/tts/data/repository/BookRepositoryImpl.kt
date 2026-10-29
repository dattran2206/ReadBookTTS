package com.vietreader.tts.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.vietreader.tts.data.cloud.GoogleDriveSyncManager
import com.vietreader.tts.data.local.WordwaveLocalStorage
import com.vietreader.tts.data.parser.EpubAndTxtParser
import com.vietreader.tts.data.tts.AiVoiceModelManager
import com.vietreader.tts.data.tts.BackgroundMusicController
import com.vietreader.tts.data.tts.SherpaOnnxTtsController
import com.vietreader.tts.data.tts.SystemTtsController
import com.vietreader.tts.domain.model.AmbientMusicSettings
import com.vietreader.tts.domain.model.AmbientTrack
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookBookmark
import com.vietreader.tts.domain.model.ChapterStatus
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.PronunciationRule
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceGroup
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.model.VoiceTuningSettings
import com.vietreader.tts.domain.repository.BookRepository
import com.vietreader.tts.service.PlaybackForegroundService
import com.vietreader.tts.service.ReadingReminderScheduler
import com.vietreader.tts.ui.reader.VietnameseTextHighlighter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class BookRepositoryImpl : BookRepository {

    companion object {
        private const val TAG = "BookRepositoryImpl"

        val instance: BookRepositoryImpl by lazy { BookRepositoryImpl() }
    }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var localStorage: WordwaveLocalStorage? = null
    @Volatile
    private var isInitialized = false

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    override val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState?>(null)
    override val playbackState: StateFlow<PlaybackState?> = _playbackState.asStateFlow()

    private val _selectedBookIdForDetail = MutableStateFlow("")
    override val selectedBookIdForDetail: StateFlow<String> = _selectedBookIdForDetail.asStateFlow()

    private val _readerMode = MutableStateFlow(ReaderMode.READ_ALONG)
    override val readerMode: StateFlow<ReaderMode> = _readerMode.asStateFlow()

    private val _voiceItems = MutableStateFlow(VoiceCatalog.defaultVoiceItems)
    override val voiceItems: StateFlow<List<VoiceItem>> = _voiceItems.asStateFlow()

    private val _voiceTuningSettings = MutableStateFlow(VoiceTuningSettings())
    override val voiceTuningSettings: StateFlow<VoiceTuningSettings> = _voiceTuningSettings.asStateFlow()

    private val _pronunciationRules = MutableStateFlow<List<PronunciationRule>>(emptyList())
    override val pronunciationRules: StateFlow<List<PronunciationRule>> = _pronunciationRules.asStateFlow()

    private val _previewingVoiceId = MutableStateFlow<String?>(null)
    override val previewingVoiceId: StateFlow<String?> = _previewingVoiceId.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    override val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(30)
    override val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow(30 * 60)
    override val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _ambientTracks = MutableStateFlow<List<AmbientTrack>>(emptyList())
    override val ambientTracks: StateFlow<List<AmbientTrack>> = _ambientTracks.asStateFlow()

    private val _ambientMusicSettings = MutableStateFlow(AmbientMusicSettings())
    override val ambientMusicSettings: StateFlow<AmbientMusicSettings> = _ambientMusicSettings.asStateFlow()

    private val _previewingAmbientTrackId = MutableStateFlow<String?>(null)
    override val previewingAmbientTrackId: StateFlow<String?> = _previewingAmbientTrackId.asStateFlow()

    private val _showOnboarding = MutableStateFlow(false)
    override val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    private var ttsController: SystemTtsController? = null
    private var aiVoiceModelManager: AiVoiceModelManager? = null
    private var sherpaController: SherpaOnnxTtsController? = null
    private var backgroundMusicController: BackgroundMusicController? = null
    private var googleDriveSyncManager: GoogleDriveSyncManager? = null
    private var sentenceDelayJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var readingTimeTrackerJob: Job? = null
    private var wordPacingJob: Job? = null
    private var playbackTickerJob: Job? = null
    private var persistJob: Job? = null
    private val voiceDownloadJobs = ConcurrentHashMap<String, Job>()
    private val persistMutex = Mutex()
    @Volatile
    private var hasNativeWordRange = false

    override fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            isInitialized = true
            val applicationCtx = context.applicationContext
            appContext = applicationCtx
            val storage = WordwaveLocalStorage(applicationCtx)
            localStorage = storage

            val loadedRules = storage.loadPronunciationRules()
            _pronunciationRules.value = loadedRules
            _voiceItems.value = storage.loadVoiceItems()
            _voiceTuningSettings.value = storage.loadVoiceTuningSettings()

            val loadedSettings = storage.loadAppSettings(
                booksCount = 0,
                dictionaryWordCount = loadedRules.size
            )
            _appSettings.value = loadedSettings
            _sleepTimerMinutes.value = loadedSettings.defaultSleepTimerMinutes
            _sleepTimerRemainingSeconds.value = loadedSettings.defaultSleepTimerMinutes * 60

            ReadingReminderScheduler.scheduleReminder(
                context = applicationCtx,
                enabled = loadedSettings.reminderEnabled,
                hour = loadedSettings.reminderHour,
                minute = loadedSettings.reminderMinute,
                days = loadedSettings.reminderDays
            )

            _showOnboarding.value = !storage.hasCompletedOnboarding()

            val tts = SystemTtsController(applicationCtx)
            tts.setPronunciationRules(loadedRules)

            val modelMgr = AiVoiceModelManager(applicationCtx)
            aiVoiceModelManager = modelMgr
            val bgMusicCtrl = BackgroundMusicController(applicationCtx)
            backgroundMusicController = bgMusicCtrl
            val builtInTracks = bgMusicCtrl.getBuiltInTracks()
            val customTracks = storage.loadCustomAmbientTracks()
            val allAmbientTracks = builtInTracks + customTracks
            _ambientTracks.value = allAmbientTracks
            googleDriveSyncManager = GoogleDriveSyncManager(applicationCtx)

            var loadedAmbientSettings = storage.loadAmbientMusicSettings()
            if (allAmbientTracks.isNotEmpty() && allAmbientTracks.none { it.id == loadedAmbientSettings.selectedTrackId }) {
                loadedAmbientSettings = loadedAmbientSettings.copy(selectedTrackId = allAmbientTracks.first().id)
                storage.saveAmbientMusicSettings(loadedAmbientSettings)
            }
            _ambientMusicSettings.value = loadedAmbientSettings

            repoScope.launch {
                runCatching { bgMusicCtrl.ensureBuiltInTracksGenerated() }
                runCatching { modelMgr.ensureSharedAssetsReady() }
                    .onFailure { Log.w(TAG, "Failed pre-extracting shared AI TTS assets: ${it.message}") }
            }

            val sherpa = SherpaOnnxTtsController(
                modelManager = modelMgr,
                textSanitizer = { raw ->
                    tts.applyPronunciationRules(tts.sanitizeTextForSpeech(raw))
                }
            )
            sherpaController = sherpa

            tts.onVoicesDiscovered = { discoveredVoices ->
                val defaultVoiceId = storage.loadDefaultVoiceId()
                val installedAiIds = modelMgr.getInstalledAiVoiceIds()
                val combinedItems = VoiceCatalog.buildCombinedVoiceItems(
                    discoveredVoices = discoveredVoices,
                    installedAiVoiceIds = installedAiIds,
                    activeDefaultId = defaultVoiceId,
                    existingItems = _voiceItems.value
                )
                _voiceItems.value = combinedItems
                val activeVoice = combinedItems.firstOrNull { it.isDefault } ?: combinedItems.firstOrNull()
                if (activeVoice != null && !activeVoice.isAiVoice) {
                    tts.setVoiceByName(activeVoice.name)
                }
            }
            ttsController = tts

            val handleSentenceStartCallback: (Int) -> Unit = { sentenceIndex ->
                repoScope.launch(Dispatchers.Main) {
                    if (sentenceIndex < 0) return@launch
                    val current = _playbackState.value ?: return@launch
                    if (current.activeSentenceIndex != sentenceIndex) {
                        val book = _books.value.find { it.id == current.bookId } ?: return@launch
                        val sentence = book.sentences.getOrNull(sentenceIndex) ?: return@launch
                        val newSec = sentence.timestampSeconds.coerceIn(0, current.totalSeconds.coerceAtLeast(1))
                        val newProgress = ((newSec.toFloat() / current.totalSeconds.coerceAtLeast(1)) * 100f)
                            .roundToInt().coerceIn(0, 100)
                        _playbackState.value = current.copy(
                            activeSentenceIndex = sentenceIndex,
                            currentTime = formatSeconds(newSec),
                            currentSeconds = newSec,
                            progressPercent = newProgress
                        )
                        updateBookProgress(book.id, newProgress, newSec)
                    }
                }
            }

            val handleSentenceDoneCallback: (Int) -> Unit = { sentenceIndex ->
                repoScope.launch(Dispatchers.Main) {
                    if (sentenceIndex < 0) {
                        _previewingVoiceId.value = null
                        restoreActiveBookVoice()
                    } else {
                        sentenceDelayJob?.cancel()
                        sentenceDelayJob = launch {
                            handleSentenceCompleted(sentenceIndex)
                        }
                    }
                }
            }

            val handleErrorCallback: (Int, String) -> Unit = { sentenceIndex, errorMsg ->
                Log.e(TAG, "TTS error at sentence $sentenceIndex: $errorMsg")
                repoScope.launch(Dispatchers.Main) {
                    if (sentenceIndex < 0) {
                        _previewingVoiceId.value = null
                        restoreActiveBookVoice()
                    } else {
                        sentenceDelayJob?.cancel()
                        sentenceDelayJob = launch {
                            handleSentenceCompleted(sentenceIndex)
                        }
                    }
                }
            }

            tts.onSentenceStart = handleSentenceStartCallback
            tts.onSentenceDone = handleSentenceDoneCallback
            tts.onWordRange = { sentenceIndex, start, end ->
                if (sentenceIndex >= 0 && !tts.isCurrentUtteranceLengthModified) {
                    hasNativeWordRange = true
                    wordPacingJob?.cancel()
                    wordPacingJob = null
                    repoScope.launch(Dispatchers.Main) {
                        val current = _playbackState.value ?: return@launch
                        if (current.activeSentenceIndex == sentenceIndex) {
                            _playbackState.value = current.copy(
                                activeWordStart = start,
                                activeWordEnd = end
                            )
                        }
                    }
                }
            }
            tts.onError = handleErrorCallback

            sherpa.onSentenceStart = handleSentenceStartCallback
            sherpa.onSentenceDone = handleSentenceDoneCallback
            sherpa.onWordRange = { sentenceIndex, start, end ->
                if (sentenceIndex >= 0) {
                    hasNativeWordRange = true
                    wordPacingJob?.cancel()
                    wordPacingJob = null
                    repoScope.launch(Dispatchers.Main) {
                        val current = _playbackState.value ?: return@launch
                        if (current.activeSentenceIndex == sentenceIndex) {
                            _playbackState.value = current.copy(
                                activeWordStart = start,
                                activeWordEnd = end
                            )
                        }
                    }
                }
            }
            sherpa.onError = handleErrorCallback
            sherpa.onReadyForPrefetchNext = { currentSentenceIndex, voiceId, speed ->
                val current = _playbackState.value
                if (current != null && current.isPlaying && currentSentenceIndex >= 0) {
                    val book = _books.value.find { it.id == current.bookId }
                    val nextIndex = currentSentenceIndex + 1
                    val nextSentence = book?.sentences?.getOrNull(nextIndex)
                    if (nextSentence != null) {
                        sherpa.prefetchSentence(
                            sentenceIndex = nextIndex,
                            rawText = nextSentence.text,
                            voiceId = voiceId,
                            speed = speed
                        )
                    }
                }
            }

            repoScope.launch {
                val restoredBooks = storage.loadBooks()
                _books.value = restoredBooks
                _appSettings.value = storage.loadAppSettings(
                    booksCount = restoredBooks.size,
                    dictionaryWordCount = _pronunciationRules.value.size
                )
                val lastId = storage.loadLastSelectedBookId(restoredBooks.firstOrNull()?.id ?: "")
                val activeBook = restoredBooks.find { it.id == lastId } ?: restoredBooks.firstOrNull()
                if (activeBook != null) {
                    _selectedBookIdForDetail.value = activeBook.id
                    selectBookForPlayback(activeBook.id)
                } else {
                    _selectedBookIdForDetail.value = ""
                    _playbackState.value = null
                }
            }
        }
    }

    override suspend fun importBookFromUri(context: Context, uri: Uri): Result<Book> = withContext(Dispatchers.IO) {
        val result = EpubAndTxtParser.importBookFromUri(
            context = context.applicationContext,
            uri = uri
        )
        result.onSuccess { importedBook ->
            _books.update { currentList ->
                val withoutDuplicate = currentList.filterNot {
                    it.isImported && it.title.equals(importedBook.title, ignoreCase = true) &&
                        it.author.equals(importedBook.author, ignoreCase = true)
                }
                listOf(importedBook) + withoutDuplicate
            }
            _selectedBookIdForDetail.value = importedBook.id
            localStorage?.saveLastSelectedBookId(importedBook.id)
            if (_playbackState.value == null) {
                selectBookForPlayback(importedBook.id)
            }
            refreshStats(_books.value.size)
            persistBooksAsync()
        }
        result
    }

    override fun selectBookForDetail(bookId: String) {
        if (_books.value.any { it.id == bookId }) {
            _selectedBookIdForDetail.value = bookId
            localStorage?.saveLastSelectedBookId(bookId)
        }
    }

    override fun setReaderMode(mode: ReaderMode) {
        _readerMode.value = mode
    }

    override fun selectBookForPlayback(bookId: String) {
        _selectedBookIdForDetail.value = bookId
        localStorage?.saveLastSelectedBookId(bookId)
        val currentPlayback = _playbackState.value
        val selected = _books.value.find { it.id == bookId } ?: return

        // If sentences are not loaded yet for a real EPUB book, load chapter sentences directly
        if (selected.sentences.isEmpty() && !selected.epubFilePath.isNullOrBlank()) {
            selectChapterForPlayback(selected.id, selected.currentChapterNumber)
            return
        }

        if (currentPlayback?.bookId == bookId && selected.sentences.isNotEmpty()) return

        val chapterShort = selected.currentChapterShortLabel.ifBlank {
            selected.currentChapterTitle.substringBefore(":").trim()
        }
        val initialIndex = if (selected.sentences.size > 1 && selected.progressPercent > 0) {
            ((selected.progressPercent / 100f) * selected.sentences.lastIndex).roundToInt()
                .coerceIn(0, selected.sentences.lastIndex)
        } else {
            0
        }
        val activeSentence = selected.sentences.getOrNull(initialIndex)
        val currentSec = activeSentence?.timestampSeconds ?: selected.currentPositionSeconds
        val isBookmarked = selected.bookmarks.any {
            it.chapterNumber == selected.currentChapterNumber && it.sentenceIndex == initialIndex
        }

        _playbackState.value = PlaybackState(
            bookId = selected.id,
            title = selected.title,
            author = selected.author,
            chapterTitle = selected.currentChapterTitle,
            chapterShortLabel = chapterShort,
            progressPercent = selected.progressPercent,
            currentTime = formatSeconds(currentSec),
            totalTime = formatSeconds(selected.totalDurationSeconds),
            currentSeconds = currentSec,
            totalSeconds = selected.totalDurationSeconds,
            remainingLabel = selected.remainingTimeLabel,
            voiceName = selected.voiceName,
            isPlaying = false,
            playbackSpeed = currentPlayback?.playbackSpeed ?: _appSettings.value.defaultSpeed,
            activeSentenceIndex = initialIndex,
            isBookmarked = isBookmarked,
            coverGradient = selected.coverGradient,
            coverImagePath = selected.coverImagePath
        )
    }

    override fun selectChapterForPlayback(bookId: String, chapterNumber: Int) {
        val wasPlaying = _playbackState.value?.isPlaying == true
        sentenceDelayJob?.cancel()
        _previewingVoiceId.value = null
        ttsController?.stop()
        sherpaController?.stop()
        sherpaController?.clearPrefetchCache()

        _selectedBookIdForDetail.value = bookId
        localStorage?.saveLastSelectedBookId(bookId)
        val selectedBook = _books.value.find { it.id == bookId } ?: return
        val targetChapter = selectedBook.chapters.find { it.number == chapterNumber } ?: return

        // On-demand load sentences for the selected chapter if this is a real EPUB/TXT book
        val (chapterSentences, chapterDurationSec) = if (
            !selectedBook.epubFilePath.isNullOrBlank() && targetChapter.entryHref.isNotBlank()
        ) {
            EpubAndTxtParser.loadEpubChapterSentences(
                epubFilePath = selectedBook.epubFilePath,
                entryHref = targetChapter.entryHref,
                chapterTitle = targetChapter.title
            )
        } else {
            selectedBook.sentences to selectedBook.totalDurationSeconds
        }

        val durationFormatted = formatSeconds(chapterDurationSec)
        val newChapterFullTitle = targetChapter.fullDisplayTitle
        val newChapterShort = "Chương ${targetChapter.number}"
        val newProgress = if (targetChapter.status == ChapterStatus.PLAYING && selectedBook.currentChapterNumber == chapterNumber) {
            targetChapter.progressPercent
        } else {
            0
        }
        val newRemaining = "Còn $durationFormatted"

        _books.update { list ->
            list.map { book ->
                if (book.id == bookId) {
                    val updatedChapters = book.chapters.map { ch ->
                        when {
                            ch.number < chapterNumber -> ch.copy(
                                status = ChapterStatus.COMPLETED,
                                progressPercent = 100,
                                remainingLabel = ""
                            )
                            ch.number == chapterNumber -> ch.copy(
                                durationLabel = durationFormatted,
                                status = ChapterStatus.PLAYING,
                                progressPercent = newProgress,
                                remainingLabel = newRemaining
                            )
                            else -> ch.copy(
                                status = ChapterStatus.UNPLAYED,
                                progressPercent = 0,
                                remainingLabel = ""
                            )
                        }
                    }
                    book.copy(
                        currentChapterTitle = newChapterFullTitle,
                        currentChapterShortLabel = newChapterShort,
                        currentChapterNumber = chapterNumber,
                        progressPercent = newProgress,
                        remainingTimeLabel = newRemaining,
                        totalDurationSeconds = chapterDurationSec,
                        currentPositionSeconds = 0,
                        lastReadEpochMillis = System.currentTimeMillis(),
                        sentences = chapterSentences,
                        chapters = updatedChapters
                    )
                } else {
                    book
                }
            }
        }

        val currentPlayback = _playbackState.value
        val initialIndex = if (newProgress > 0 && chapterSentences.size > 1) {
            ((newProgress / 100f) * chapterSentences.lastIndex).roundToInt()
                .coerceIn(0, chapterSentences.lastIndex)
        } else {
            0
        }
        val activeSentence = chapterSentences.getOrNull(initialIndex)
        val currentSec = if (newProgress > 0) (activeSentence?.timestampSeconds ?: 0) else 0
        val isBookmarked = selectedBook.bookmarks.any {
            it.chapterNumber == chapterNumber && it.sentenceIndex == initialIndex
        }

        _playbackState.value = PlaybackState(
            bookId = selectedBook.id,
            title = selectedBook.title,
            author = selectedBook.author,
            chapterTitle = newChapterFullTitle,
            chapterShortLabel = newChapterShort,
            progressPercent = newProgress,
            currentTime = formatSeconds(currentSec),
            totalTime = durationFormatted,
            currentSeconds = currentSec,
            totalSeconds = chapterDurationSec,
            remainingLabel = newRemaining,
            voiceName = selectedBook.voiceName,
            isPlaying = wasPlaying,
            playbackSpeed = currentPlayback?.playbackSpeed ?: _appSettings.value.defaultSpeed,
            activeSentenceIndex = initialIndex,
            isBookmarked = isBookmarked,
            coverGradient = selectedBook.coverGradient,
            coverImagePath = selectedBook.coverImagePath
        )

        persistBooksAsync()

        if (wasPlaying) {
            playCurrentSentence()
        }
    }

    override fun togglePlayPause() {
        val current = _playbackState.value ?: run {
            val fallbackId = _selectedBookIdForDetail.value.ifBlank { _books.value.firstOrNull()?.id ?: "" }
            if (fallbackId.isNotBlank()) {
                selectBookForPlayback(fallbackId)
                _playbackState.value
            } else null
        } ?: return

        if (current.isPlaying) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    override fun resumePlayback() {
        _previewingVoiceId.value = null
        val current = _playbackState.value ?: run {
            val fallbackId = _selectedBookIdForDetail.value.ifBlank { _books.value.firstOrNull()?.id ?: "" }
            if (fallbackId.isNotBlank()) {
                selectBookForPlayback(fallbackId)
                _playbackState.value
            } else null
        } ?: return

        if (current.bookId.isBlank()) return
        _previewingAmbientTrackId.value = null
        _playbackState.value = current.copy(isPlaying = true)
        appContext?.let { PlaybackForegroundService.startOrUpdate(it) }
        syncBackgroundMusicPlayback()
        startSleepTimerIfConfigured()
        startReadingTimeTracker()
        startPlaybackTicker()
        playCurrentSentence()
    }

    override fun pausePlayback() {
        sentenceDelayJob?.cancel()
        wordPacingJob?.cancel()
        wordPacingJob = null
        stopPlaybackTicker()
        _playbackState.update { it?.copy(isPlaying = false, activeWordStart = -1, activeWordEnd = -1) }
        ttsController?.stop()
        sherpaController?.stop()
        syncBackgroundMusicPlayback()
        stopReadingTimeTracker()
    }

    override fun stopPlayback() {
        sentenceDelayJob?.cancel()
        wordPacingJob?.cancel()
        wordPacingJob = null
        stopPlaybackTicker()
        _playbackState.update { it?.copy(isPlaying = false, activeWordStart = -1, activeWordEnd = -1) }
        ttsController?.stop()
        sherpaController?.stop()
        sherpaController?.clearPrefetchCache()
        sherpaController?.releaseModel()
        _previewingAmbientTrackId.value = null
        backgroundMusicController?.release()
        stopReadingTimeTracker()
    }

    override fun selectSentence(sentenceIndex: Int) {
        selectSentenceInternal(sentenceIndex, shouldTriggerSpeak = true, isAutoAdvance = false)
    }

    private fun selectSentenceInternal(
        sentenceIndex: Int,
        shouldTriggerSpeak: Boolean,
        isAutoAdvance: Boolean = false
    ) {
        sentenceDelayJob?.cancel()
        wordPacingJob?.cancel()
        wordPacingJob = null
        if (!isAutoAdvance) {
            sherpaController?.clearPrefetchCache()
        }
        val current = _playbackState.value ?: return
        val book = _books.value.find { it.id == current.bookId } ?: return
        val sentence = book.sentences.getOrNull(sentenceIndex) ?: return

        val (newSeconds, newTimeLabel, newProgress) = if (isAutoAdvance) {
            // When automatically advancing paragraph-to-paragraph during continuous reading,
            // maintain the smooth real-time ticking position without jumping forward or backward!
            Triple(current.currentSeconds, current.currentTime, current.progressPercent)
        } else {
            // User manually clicked on a sentence or used previous/next buttons:
            // Seek directly to the exact start timestamp of this sentence.
            val sec = sentence.timestampSeconds.coerceIn(0, current.totalSeconds.coerceAtLeast(1))
            val prog = ((sec.toFloat() / current.totalSeconds.coerceAtLeast(1)) * 100f)
                .roundToInt().coerceIn(0, 100)
            Triple(sec, formatSeconds(sec), prog)
        }

        val isBookmarked = book.bookmarks.any {
            it.chapterNumber == book.currentChapterNumber && it.sentenceIndex == sentenceIndex
        }

        _playbackState.value = current.copy(
            activeSentenceIndex = sentenceIndex,
            currentTime = newTimeLabel,
            currentSeconds = newSeconds,
            progressPercent = newProgress,
            isBookmarked = isBookmarked,
            activeWordStart = -1,
            activeWordEnd = -1
        )
        updateBookProgress(book.id, newProgress, newSeconds)

        if (shouldTriggerSpeak && current.isPlaying) {
            playCurrentSentence()
        }
    }

    private fun playCurrentSentence() {
        sentenceDelayJob?.cancel()
        val current = _playbackState.value ?: return
        if (!current.isPlaying) return
        startPlaybackTicker()

        var book = _books.value.find { it.id == current.bookId } ?: return

        // If book sentences are empty, ensure chapter is loaded
        if (book.sentences.isEmpty() && !book.epubFilePath.isNullOrBlank()) {
            selectChapterForPlayback(book.id, book.currentChapterNumber)
            book = _books.value.find { it.id == current.bookId } ?: return
        }

        if (book.sentences.isEmpty()) {
            Log.w(TAG, "Cannot play: no sentences in chapter")
            _playbackState.update { it?.copy(isPlaying = false) }
            return
        }

        val sentenceIdx = current.activeSentenceIndex.coerceIn(0, book.sentences.lastIndex)
        val sentence = book.sentences.getOrNull(sentenceIdx) ?: return

        val speed = current.playbackSpeed
        val pitchVal = _voiceTuningSettings.value.pitch
        val pitch = 1.0f + (pitchVal * 0.05f).coerceIn(-0.4f, 0.4f)

        wordPacingJob?.cancel()
        hasNativeWordRange = false
        _playbackState.update { it?.copy(activeWordStart = -1, activeWordEnd = -1) }

        appContext?.let { PlaybackForegroundService.startOrUpdate(it) }

        val activeVoiceItem = _voiceItems.value.find { it.name == current.voiceName }
            ?: _voiceItems.value.firstOrNull { it.isDefault }
        val useAiSherpa = activeVoiceItem != null &&
            activeVoiceItem.isAiVoice &&
            aiVoiceModelManager?.isModelInstalled(activeVoiceItem.id) == true

        if (useAiSherpa && activeVoiceItem != null) {
            ttsController?.stop()
            sherpaController?.speakSentence(
                sentenceIndex = sentenceIdx,
                rawText = sentence.text,
                voiceId = activeVoiceItem.id,
                speed = speed,
                pitch = pitch
            )
        } else {
            sherpaController?.stop()
            sherpaController?.releaseModel()
            ttsController?.setVoiceByName(current.voiceName)
            ttsController?.speakSentence(
                sentenceIndex = sentenceIdx,
                text = sentence.text,
                speed = speed,
                pitch = pitch
            )
        }

        // Fallback word tracker in case active engine doesn't emit onRangeStart
        wordPacingJob = repoScope.launch(Dispatchers.Default) {
            delay(280)
            if (hasNativeWordRange) return@launch
            val words = VietnameseTextHighlighter.getWordSpans(sentence.text)
            if (words.isEmpty()) return@launch
            val msPerChar = (45f / speed).toLong().coerceIn(25L, 75L)
            for (w in words) {
                if (hasNativeWordRange) break
                val state = _playbackState.value ?: break
                if (!state.isPlaying || state.activeSentenceIndex != sentenceIdx) break
                _playbackState.update { it?.copy(activeWordStart = w.start, activeWordEnd = w.end) }
                val wordDuration = (w.word.length * msPerChar).coerceIn(120L, 450L)
                delay(wordDuration)
            }
        }
    }

    private suspend fun handleSentenceCompleted(sentenceIndex: Int) {
        wordPacingJob?.cancel()
        wordPacingJob = null
        _playbackState.update { it?.copy(activeWordStart = -1, activeWordEnd = -1) }

        val current = _playbackState.value ?: return
        if (!current.isPlaying) return
        if (current.activeSentenceIndex != sentenceIndex) return

        val pauseSec = _voiceTuningSettings.value.sentencePauseSeconds
        if (pauseSec > 0.05f) {
            delay((pauseSec * 1000).toLong())
        }

        val stateAfterDelay = _playbackState.value ?: return
        if (!stateAfterDelay.isPlaying || stateAfterDelay.activeSentenceIndex != sentenceIndex) return

        val book = _books.value.find { it.id == current.bookId } ?: return
        val nextIndex = sentenceIndex + 1

        if (nextIndex < book.sentences.size) {
            selectSentenceInternal(nextIndex, shouldTriggerSpeak = true, isAutoAdvance = true)
        } else {
            // Reached end of current chapter
            val stopAtEndOfChapter = _appSettings.value.stopAtEndOfChapter
            if (stopAtEndOfChapter) {
                _playbackState.update { it?.copy(isPlaying = false) }
                stopPlaybackTicker()
                ttsController?.stop()
                sherpaController?.stop()
                syncBackgroundMusicPlayback()
                stopReadingTimeTracker()
            } else {
                val nextChapterNum = book.currentChapterNumber + 1
                val nextChapterExists = book.chapters.any { it.number == nextChapterNum }
                if (nextChapterExists) {
                    selectChapterForPlayback(book.id, nextChapterNum)
                } else {
                    // Reached end of the entire book!
                    _playbackState.update { it?.copy(isPlaying = false) }
                    stopPlaybackTicker()
                    ttsController?.stop()
                    sherpaController?.stop()
                    syncBackgroundMusicPlayback()
                    stopReadingTimeTracker()
                }
            }
        }
    }

    override fun skipToPreviousSentence() {
        val current = _playbackState.value ?: return
        val book = _books.value.find { it.id == current.bookId } ?: return
        if (current.activeSentenceIndex > 0) {
            selectSentence(current.activeSentenceIndex - 1)
        } else if (book.currentChapterNumber > 1) {
            val prevChapterNum = book.currentChapterNumber - 1
            if (book.chapters.any { it.number == prevChapterNum }) {
                selectChapterForPlayback(book.id, prevChapterNum)
            }
        }
    }

    override fun skipToNextSentence() {
        val current = _playbackState.value ?: return
        val book = _books.value.find { it.id == current.bookId } ?: return
        if (current.activeSentenceIndex < book.sentences.lastIndex) {
            selectSentence(current.activeSentenceIndex + 1)
        } else {
            val nextChapterNum = book.currentChapterNumber + 1
            if (book.chapters.any { it.number == nextChapterNum }) {
                selectChapterForPlayback(book.id, nextChapterNum)
            }
        }
    }

    override fun seekToSeconds(seconds: Int) {
        sentenceDelayJob?.cancel()
        val current = _playbackState.value ?: return
        val clampedSeconds = seconds.coerceIn(0, current.totalSeconds.coerceAtLeast(1))
        val progress = ((clampedSeconds.toFloat() / current.totalSeconds.coerceAtLeast(1)) * 100f)
            .roundToInt()
            .coerceIn(0, 100)

        val book = _books.value.find { it.id == current.bookId }
        val matchingSentenceIdx = book?.sentences
            ?.indexOfLast { it.timestampSeconds <= clampedSeconds }
            ?.takeIf { it >= 0 }
            ?: current.activeSentenceIndex

        val isBookmarked = book?.bookmarks?.any {
            it.chapterNumber == (book.currentChapterNumber) && it.sentenceIndex == matchingSentenceIdx
        } ?: false

        val wasPlaying = current.isPlaying
        _playbackState.value = current.copy(
            currentSeconds = clampedSeconds,
            currentTime = formatSeconds(clampedSeconds),
            progressPercent = progress,
            activeSentenceIndex = matchingSentenceIdx,
            isBookmarked = isBookmarked
        )
        updateBookProgress(current.bookId, progress, clampedSeconds)
        sherpaController?.clearPrefetchCache()

        if (wasPlaying) {
            ttsController?.stop()
            sherpaController?.stop()
            playCurrentSentence()
        }
    }

    override fun seekByDeltaSeconds(deltaSeconds: Int) {
        val current = _playbackState.value ?: return
        seekToSeconds(current.currentSeconds + deltaSeconds)
    }

    override fun cyclePlaybackSpeed() {
        val nextSpeed = when {
            (_playbackState.value?.playbackSpeed ?: 1.0f) < 1.1f -> 1.2f
            (_playbackState.value?.playbackSpeed ?: 1.0f) < 1.3f -> 1.5f
            else -> 1.0f
        }
        val current = _playbackState.value
        val book = _books.value.find { it.id == current?.bookId }
        val activeSentence = book?.sentences?.getOrNull(current?.activeSentenceIndex ?: 0)

        _playbackState.update { cur ->
            if (cur == null) return@update null
            if (activeSentence != null) {
                val startSec = activeSentence.timestampSeconds.coerceIn(0, cur.totalSeconds.coerceAtLeast(1))
                val startProg = ((startSec.toFloat() / cur.totalSeconds.coerceAtLeast(1)) * 100f).roundToInt().coerceIn(0, 100)
                cur.copy(
                    playbackSpeed = nextSpeed,
                    currentSeconds = startSec,
                    currentTime = formatSeconds(startSec),
                    progressPercent = startProg,
                    activeWordStart = -1,
                    activeWordEnd = -1
                )
            } else {
                cur.copy(playbackSpeed = nextSpeed)
            }
        }
        ttsController?.setSpeechRate(nextSpeed)
        sherpaController?.clearPrefetchCache()
        if (_playbackState.value?.isPlaying == true) {
            playCurrentSentence()
        }
    }

    override fun selectVoice(voiceName: String) {
        val current = _playbackState.value ?: return
        val matchedItem = _voiceItems.value.find { it.name == voiceName }
        _playbackState.value = current.copy(voiceName = voiceName)
        _books.update { list ->
            list.map {
                if (it.id == current.bookId) {
                    it.copy(
                        voiceName = voiceName,
                        voiceRegion = matchedItem?.region ?: it.voiceRegion
                    )
                } else {
                    it
                }
            }
        }
        sherpaController?.clearPrefetchCache()
        if (matchedItem == null || !matchedItem.isAiVoice || sherpaController?.currentLoadedVoiceId != matchedItem.id) {
            sherpaController?.releaseModel()
        }
        if (matchedItem == null || !matchedItem.isAiVoice) {
            ttsController?.setVoiceByName(voiceName)
        }
        if (current.isPlaying) {
            playCurrentSentence()
        }
        persistBooksAsync()
    }

    override fun selectVoiceForBook(bookId: String, voiceName: String, voiceRegion: String) {
        val matchedItem = _voiceItems.value.find { it.name == voiceName }
        _books.update { list ->
            list.map {
                if (it.id == bookId) {
                    it.copy(voiceName = voiceName, voiceRegion = voiceRegion)
                } else {
                    it
                }
            }
        }
        _playbackState.update { current ->
            if (current?.bookId == bookId) {
                current.copy(voiceName = voiceName)
            } else {
                current
            }
        }
        if (_playbackState.value?.bookId == bookId) {
            sherpaController?.clearPrefetchCache()
            if (matchedItem == null || !matchedItem.isAiVoice || sherpaController?.currentLoadedVoiceId != matchedItem.id) {
                sherpaController?.releaseModel()
            }
            if (_playbackState.value?.isPlaying == true) {
                if (matchedItem == null || !matchedItem.isAiVoice) {
                    ttsController?.setVoiceByName(voiceName)
                }
                playCurrentSentence()
            }
        }
        persistBooksAsync()
    }

    override fun setDefaultVoice(voiceId: String) {
        val selectedVoice = _voiceItems.value.find { it.id == voiceId } ?: return
        if (selectedVoice.isDefault && selectedVoice.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT) {
            // Already active default, prevent redundant re-triggers
            return
        }
        _voiceItems.update { list ->
            val updated = list.map { item ->
                when {
                    item.id == voiceId -> item.copy(
                        isDefault = true,
                        installStatus = VoiceInstallStatus.ACTIVE_DEFAULT,
                        downloadProgressPercent = 100
                    )
                    item.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT -> item.copy(
                        isDefault = false,
                        installStatus = VoiceInstallStatus.DOWNLOADED
                    )
                    else -> item.copy(isDefault = false)
                }
            }
            localStorage?.saveVoiceItems(updated)
            updated
        }

        _playbackState.update { current ->
            current?.copy(voiceName = selectedVoice.name)
        }
        sherpaController?.clearPrefetchCache()
        if (!selectedVoice.isAiVoice || sherpaController?.currentLoadedVoiceId != selectedVoice.id) {
            sherpaController?.releaseModel()
        }
        if (!selectedVoice.isAiVoice) {
            ttsController?.setVoiceByName(selectedVoice.name)
        }
        val activeBookId = _playbackState.value?.bookId
        if (activeBookId != null) {
            _books.update { list ->
                list.map {
                    if (it.id == activeBookId) {
                        it.copy(voiceName = selectedVoice.name, voiceRegion = selectedVoice.region)
                    } else {
                        it
                    }
                }
            }
            persistBooksAsync()
        }
        if (_playbackState.value?.isPlaying == true) {
            playCurrentSentence()
        }
    }

    override fun startVoiceDownload(voiceId: String) {
        voiceDownloadJobs[voiceId]?.cancel()
        val targetVoice = _voiceItems.value.find { it.id == voiceId } ?: return
        val modelMgr = aiVoiceModelManager ?: return

        _voiceItems.update { list ->
            list.map { item ->
                if (item.id == voiceId) {
                    item.copy(
                        installStatus = VoiceInstallStatus.DOWNLOADING,
                        downloadProgressPercent = 1,
                        isDownloadPaused = false
                    )
                } else {
                    item
                }
            }
        }

        val job = repoScope.launch {
            val result = modelMgr.downloadVoiceModel(
                voice = targetVoice,
                isPausedProvider = {
                    _voiceItems.value.find { it.id == voiceId }?.isDownloadPaused == true
                },
                onProgress = { progress ->
                    _voiceItems.update { list ->
                        list.map { item ->
                            if (item.id == voiceId && item.installStatus == VoiceInstallStatus.DOWNLOADING) {
                                item.copy(downloadProgressPercent = progress.coerceIn(1, 99))
                            } else {
                                item
                            }
                        }
                    }
                }
            )

            result.onSuccess {
                _voiceItems.update { list ->
                    val nextList = list.map { item ->
                        if (item.id == voiceId) {
                            item.copy(
                                installStatus = VoiceInstallStatus.DOWNLOADED,
                                downloadProgressPercent = 100,
                                isDownloadPaused = false
                            )
                        } else {
                            item
                        }
                    }
                    localStorage?.saveVoiceItems(nextList)
                    nextList
                }
            }.onFailure { err ->
                Log.e(TAG, "AI voice download failed for $voiceId: ${err.message}")
                _voiceItems.update { list ->
                    val nextList = list.map { item ->
                        if (item.id == voiceId) {
                            item.copy(
                                installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
                                downloadProgressPercent = 0,
                                isDownloadPaused = false
                            )
                        } else {
                            item
                        }
                    }
                    localStorage?.saveVoiceItems(nextList)
                    nextList
                }
            }
            voiceDownloadJobs.remove(voiceId)
        }
        voiceDownloadJobs[voiceId] = job
    }

    override fun togglePauseVoiceDownload(voiceId: String) {
        _voiceItems.update { list ->
            list.map { item ->
                if (item.id == voiceId && item.installStatus == VoiceInstallStatus.DOWNLOADING) {
                    item.copy(isDownloadPaused = !item.isDownloadPaused)
                } else {
                    item
                }
            }
        }
    }

    override fun cancelVoiceDownload(voiceId: String) {
        voiceDownloadJobs.remove(voiceId)?.cancel()
        aiVoiceModelManager?.deleteVoiceModel(voiceId)
        _voiceItems.update { list ->
            val updated = list.map { item ->
                if (item.id == voiceId) {
                    item.copy(
                        installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
                        downloadProgressPercent = 0,
                        isDownloadPaused = false
                    )
                } else {
                    item
                }
            }
            localStorage?.saveVoiceItems(updated)
            updated
        }
    }

    override fun deleteDownloadedVoice(voiceId: String) {
        voiceDownloadJobs.remove(voiceId)?.cancel()
        sherpaController?.stop()
        sherpaController?.releaseModel()
        aiVoiceModelManager?.deleteVoiceModel(voiceId)
        _voiceItems.update { list ->
            val target = list.find { it.id == voiceId } ?: return@update list
            if (target.group == VoiceGroup.SYSTEM) return@update list

            val wasDefault = target.isDefault
            val updated = list.map { item ->
                if (item.id == voiceId) {
                    item.copy(
                        installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
                        downloadProgressPercent = 0,
                        isDefault = false
                    )
                } else {
                    item
                }
            }

            val finalVoices = if (wasDefault) {
                val fallbackId = updated.firstOrNull {
                    it.installStatus == VoiceInstallStatus.DOWNLOADED
                }?.id
                updated.map { item ->
                    if (item.id == fallbackId) {
                        item.copy(
                            isDefault = true,
                            installStatus = VoiceInstallStatus.ACTIVE_DEFAULT
                        )
                    } else {
                        item
                    }
                }
            } else {
                updated
            }
            localStorage?.saveVoiceItems(finalVoices)
            finalVoices
        }
    }

    override fun updateVoiceTuning(settings: VoiceTuningSettings) {
        _voiceTuningSettings.value = settings
        localStorage?.saveVoiceTuningSettings(settings)
        val current = _playbackState.value
        val book = _books.value.find { it.id == current?.bookId }
        val activeSentence = book?.sentences?.getOrNull(current?.activeSentenceIndex ?: 0)

        _playbackState.update { cur ->
            if (cur == null) return@update null
            if (cur.isPlaying && activeSentence != null) {
                val startSec = activeSentence.timestampSeconds.coerceIn(0, cur.totalSeconds.coerceAtLeast(1))
                val startProg = ((startSec.toFloat() / cur.totalSeconds.coerceAtLeast(1)) * 100f).roundToInt().coerceIn(0, 100)
                cur.copy(
                    playbackSpeed = settings.speed,
                    currentSeconds = startSec,
                    currentTime = formatSeconds(startSec),
                    progressPercent = startProg,
                    activeWordStart = -1,
                    activeWordEnd = -1
                )
            } else {
                cur.copy(playbackSpeed = settings.speed)
            }
        }
        ttsController?.setSpeechRate(settings.speed)
        val pitchMultiplier = 1.0f + (settings.pitch * 0.05f).coerceIn(-0.4f, 0.4f)
        ttsController?.setPitch(pitchMultiplier)
        sherpaController?.clearPrefetchCache()
        if (_playbackState.value?.isPlaying == true) {
            playCurrentSentence()
        }
    }

    override fun resetVoiceTuning() {
        val defaults = VoiceTuningSettings()
        updateVoiceTuning(defaults)
    }

    override fun addPronunciationRule(originalWord: String, spokenReplacement: String) {
        val word = originalWord.trim()
        val replacement = spokenReplacement.trim()
        if (word.isEmpty() || replacement.isEmpty()) return

        var updatedRules: List<PronunciationRule> = emptyList()
        _pronunciationRules.update { current ->
            val withoutExisting = current.filterNot { it.originalWord.equals(word, ignoreCase = true) }
            val newRule = PronunciationRule(
                id = "rule_${System.currentTimeMillis()}",
                originalWord = word,
                spokenReplacement = replacement
            )
            (listOf(newRule) + withoutExisting).also { updatedRules = it }
        }
        ttsController?.setPronunciationRules(updatedRules)
        sherpaController?.clearPrefetchCache()
        _appSettings.update { it.copy(dictionaryWordCount = updatedRules.size) }
        repoScope.launch(Dispatchers.IO) {
            localStorage?.savePronunciationRules(updatedRules)
        }
    }

    override fun deletePronunciationRule(ruleId: String) {
        var updatedRules: List<PronunciationRule> = emptyList()
        _pronunciationRules.update { current ->
            current.filterNot { it.id == ruleId }.also { updatedRules = it }
        }
        ttsController?.setPronunciationRules(updatedRules)
        sherpaController?.clearPrefetchCache()
        _appSettings.update { it.copy(dictionaryWordCount = updatedRules.size) }
        repoScope.launch(Dispatchers.IO) {
            localStorage?.savePronunciationRules(updatedRules)
        }
    }

    override fun updateAppearanceSettings(themeMode: ReadingThemeMode, fontSizePx: Int) {
        _appSettings.update { current ->
            val next = current.copy(
                themeMode = themeMode,
                fontSizePx = fontSizePx.coerceIn(14, 24)
            )
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun updateHighlightLevel(level: HighlightLevel) {
        _appSettings.update { current ->
            val next = current.copy(highlightLevel = level)
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun updateAudioPlaybackSettings(autoScrollEnabled: Boolean, stopAtEndOfChapter: Boolean) {
        _appSettings.update { current ->
            val next = current.copy(
                autoScrollEnabled = autoScrollEnabled,
                stopAtEndOfChapter = stopAtEndOfChapter
            )
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun updateDefaultReadingSpeed(speed: Float) {
        val clamped = ((speed * 20f).roundToInt() / 20f).coerceIn(0.75f, 2.0f)
        _appSettings.update { current ->
            val next = current.copy(defaultSpeed = clamped)
            localStorage?.saveAppSettings(next)
            next
        }
        _voiceTuningSettings.update { current ->
            val next = current.copy(speed = clamped)
            localStorage?.saveVoiceTuningSettings(next)
            next
        }
        val current = _playbackState.value
        val book = _books.value.find { it.id == current?.bookId }
        val activeSentence = book?.sentences?.getOrNull(current?.activeSentenceIndex ?: 0)

        _playbackState.update { cur ->
            if (cur == null) return@update null
            if (cur.isPlaying && activeSentence != null) {
                val startSec = activeSentence.timestampSeconds.coerceIn(0, cur.totalSeconds.coerceAtLeast(1))
                val startProg = ((startSec.toFloat() / cur.totalSeconds.coerceAtLeast(1)) * 100f).roundToInt().coerceIn(0, 100)
                cur.copy(
                    playbackSpeed = clamped,
                    currentSeconds = startSec,
                    currentTime = formatSeconds(startSec),
                    progressPercent = startProg,
                    activeWordStart = -1,
                    activeWordEnd = -1
                )
            } else {
                cur.copy(playbackSpeed = clamped)
            }
        }
        ttsController?.setSpeechRate(clamped)
        sherpaController?.clearPrefetchCache()
        if (_playbackState.value?.isPlaying == true) {
            playCurrentSentence()
        }
    }

    override fun toggleCloudSync() {
        _appSettings.update { current ->
            val next = current.copy(cloudSyncEnabled = !current.cloudSyncEnabled)
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun updateReadingReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        days: Set<Int>
    ) {
        val clampedHour = hour.coerceIn(0, 23)
        val clampedMinute = minute.coerceIn(0, 59)
        _appSettings.update { current ->
            val next = current.copy(
                reminderEnabled = enabled,
                reminderHour = clampedHour,
                reminderMinute = clampedMinute,
                reminderDays = days
            )
            localStorage?.saveAppSettings(next)
            next
        }
        appContext?.let { ctx ->
            ReadingReminderScheduler.scheduleReminder(
                context = ctx,
                enabled = enabled,
                hour = clampedHour,
                minute = clampedMinute,
                days = days
            )
        }
    }

    override fun updateReaderProfile(userName: String, userRoleBadge: String) {
        val trimmedName = userName.trim().ifBlank { "Độc giả" }
        val trimmedRole = userRoleBadge.trim().ifBlank { "Độc giả" }
        _appSettings.update { current ->
            val next = current.copy(
                userName = trimmedName,
                userRoleBadge = trimmedRole
            )
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun completeOnboarding(userName: String, userRoleBadge: String) {
        updateReaderProfile(userName, userRoleBadge)
        localStorage?.setCompletedOnboarding(true)
        _showOnboarding.value = false
    }

    override fun cycleReminderTime() {
        _appSettings.update { current ->
            val nextLabel = when (current.reminderTimeLabel) {
                "Nhắc đọc 21:00" -> "Nhắc đọc 22:00"
                "Nhắc đọc 22:00" -> "Nhắc đọc 07:00"
                "Nhắc đọc 07:00" -> "Tắt nhắc nhở"
                else -> "Nhắc đọc 21:00"
            }
            val next = current.copy(reminderTimeLabel = nextLabel)
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun cycleDefaultSleepTimer() {
        _appSettings.update { current ->
            val nextMinutes = when (current.defaultSleepTimerMinutes) {
                15 -> 30
                30 -> 45
                45 -> 60
                60 -> 0
                else -> 15
            }
            val next = current.copy(defaultSleepTimerMinutes = nextMinutes)
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun updateDefaultSleepTimer(minutes: Int) {
        val clamped = minutes.coerceIn(0, 480)
        _appSettings.update { current ->
            val next = current.copy(defaultSleepTimerMinutes = clamped)
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun toggleBookmark() {
        val current = _playbackState.value ?: return
        val book = _books.value.find { it.id == current.bookId } ?: return
        val sentenceIdx = current.activeSentenceIndex
        val sentence = book.sentences.getOrNull(sentenceIdx) ?: return
        val nextBookmarked = !current.isBookmarked

        _playbackState.value = current.copy(isBookmarked = nextBookmarked)
        _books.update { list ->
            list.map { item ->
                if (item.id == book.id) {
                    val updatedBookmarks = if (nextBookmarked) {
                        val newBm = BookBookmark(
                            id = "bm_${System.currentTimeMillis()}",
                            chapterTitle = current.chapterTitle,
                            quoteText = "\"${sentence.text}\"",
                            timestampLabel = sentence.timestampLabel,
                            chapterNumber = item.currentChapterNumber,
                            sentenceIndex = sentenceIdx
                        )
                        listOf(newBm) + item.bookmarks
                    } else {
                        item.bookmarks.filterNot {
                            it.chapterNumber == item.currentChapterNumber && it.sentenceIndex == sentenceIdx
                        }
                    }
                    item.copy(bookmarks = updatedBookmarks)
                } else {
                    item
                }
            }
        }
        persistBooksAsync()
    }

    override fun deleteBookmark(bookId: String, bookmarkId: String) {
        _books.update { list ->
            list.map { book ->
                if (book.id == bookId) {
                    book.copy(bookmarks = book.bookmarks.filterNot { it.id == bookmarkId })
                } else {
                    book
                }
            }
        }
        val currentPlayback = _playbackState.value
        if (currentPlayback?.bookId == bookId) {
            val currentBook = _books.value.find { it.id == bookId }
            val stillBookmarked = currentBook?.bookmarks?.any {
                it.chapterNumber == currentBook.currentChapterNumber && it.sentenceIndex == currentPlayback.activeSentenceIndex
            } ?: false
            if (currentPlayback.isBookmarked != stillBookmarked) {
                _playbackState.value = currentPlayback.copy(isBookmarked = stillBookmarked)
            }
        }
        persistBooksAsync()
    }

    override fun deleteBook(bookId: String) {
        val targetBook = _books.value.find { it.id == bookId } ?: return

        if (_playbackState.value?.bookId == bookId) {
            sentenceDelayJob?.cancel()
            ttsController?.stop()
            sherpaController?.stop()
            sherpaController?.clearPrefetchCache()
            stopReadingTimeTracker()
            stopPlaybackTicker()
        }

        // Delete physical files
        targetBook.epubFilePath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
                val idxFile = File("$path.chapters.v2.idx")
                if (idxFile.exists()) idxFile.delete()
            }
        }
        targetBook.coverImagePath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }

        val remainingBooks = _books.value.filterNot { it.id == bookId }
        _books.value = remainingBooks

        if (_selectedBookIdForDetail.value == bookId) {
            val nextBook = remainingBooks.firstOrNull()
            if (nextBook != null) {
                _selectedBookIdForDetail.value = nextBook.id
                localStorage?.saveLastSelectedBookId(nextBook.id)
                if (_playbackState.value?.bookId == bookId) {
                    selectBookForPlayback(nextBook.id)
                }
            } else {
                _selectedBookIdForDetail.value = ""
                localStorage?.saveLastSelectedBookId("")
                _playbackState.value = null
            }
        } else if (_playbackState.value?.bookId == bookId) {
            val nextBook = remainingBooks.firstOrNull()
            if (nextBook != null) {
                selectBookForPlayback(nextBook.id)
            } else {
                _playbackState.value = null
            }
        }

        refreshStats(remainingBooks.size)
        persistBooksAsync()
    }

    override fun recordReadingTime(seconds: Long) {
        if (seconds <= 0) return
        localStorage?.recordReadingSession(seconds)
        refreshStats(_books.value.size)
    }

    private fun refreshStats(booksCount: Int) {
        val storage = localStorage ?: return
        val (streak, timeLabel, totalBooks) = storage.getReadingStats(booksCount)
        _appSettings.update {
            it.copy(
                streakDays = streak,
                totalListeningTimeLabel = timeLabel,
                totalBooksRead = totalBooks
            )
        }
    }

    private fun updateBookProgress(bookId: String, progressPercent: Int, currentSeconds: Int) {
        _books.update { list ->
            list.map { book ->
                if (book.id == bookId) {
                    val updatedChapters = book.chapters.map { ch ->
                        if (ch.number == book.currentChapterNumber) {
                            ch.copy(
                                status = ChapterStatus.PLAYING,
                                progressPercent = progressPercent
                            )
                        } else {
                            ch
                        }
                    }
                    book.copy(
                        progressPercent = progressPercent,
                        currentPositionSeconds = currentSeconds,
                        lastReadEpochMillis = System.currentTimeMillis(),
                        chapters = updatedChapters
                    )
                } else {
                    book
                }
            }
        }
        persistBooksAsync()
    }

    private fun persistBooksAsync() {
        persistJob?.cancel()
        persistJob = repoScope.launch {
            delay(150L)
            val snapshot = _books.value
            persistMutex.withLock {
                localStorage?.saveBooks(snapshot)
            }
        }
    }

    override fun cycleSleepTimer() {
        val nextMinutes = when (_sleepTimerMinutes.value) {
            15 -> 30
            30 -> 45
            45 -> 60
            60 -> 0
            else -> 15
        }
        setSleepTimer(nextMinutes)
    }

    override fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTimerJob?.cancel()
        if (minutes > 0 && _playbackState.value?.isPlaying == true) {
            startSleepTimerCountdown()
        }
    }

    private fun startSleepTimerIfConfigured() {
        if (_sleepTimerMinutes.value > 0) {
            if (_sleepTimerRemainingSeconds.value <= 0) {
                _sleepTimerRemainingSeconds.value = _sleepTimerMinutes.value * 60
            }
            startSleepTimerCountdown()
        }
    }

    private fun startSleepTimerCountdown() {
        sleepTimerJob?.cancel()
        sleepTimerJob = repoScope.launch {
            while (isActive && _sleepTimerRemainingSeconds.value > 0) {
                delay(1000L)
                if (_playbackState.value?.isPlaying == true) {
                    val rem = _sleepTimerRemainingSeconds.value - 1
                    _sleepTimerRemainingSeconds.value = rem
                    if (rem <= 0) {
                        Log.i(TAG, "Sleep timer expired: pausing playback and releasing native AI model")
                        _playbackState.update { it?.copy(isPlaying = false) }
                        sentenceDelayJob?.cancel()
                        stopPlaybackTicker()
                        ttsController?.stop()
                        sherpaController?.stop()
                        sherpaController?.releaseModel()
                        _previewingAmbientTrackId.value = null
                        backgroundMusicController?.release()
                        stopReadingTimeTracker()
                        _sleepTimerMinutes.value = 0
                        break
                    }
                }
            }
        }
    }

    private fun startReadingTimeTracker() {
        readingTimeTrackerJob?.cancel()
        readingTimeTrackerJob = repoScope.launch {
            while (isActive && _playbackState.value?.isPlaying == true) {
                delay(15_000L)
                if (_playbackState.value?.isPlaying == true) {
                    recordReadingTime(15L)
                }
            }
        }
    }

    private fun stopReadingTimeTracker() {
        readingTimeTrackerJob?.cancel()
        readingTimeTrackerJob = null
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = repoScope.launch {
            while (isActive) {
                val state = _playbackState.value ?: break
                if (!state.isPlaying) break

                val speed = state.playbackSpeed.coerceIn(0.5f, 2.5f)
                val delayInterval = (1000f / speed).toLong().coerceIn(200L, 2000L)
                delay(delayInterval)

                val cur = _playbackState.value ?: break
                if (!cur.isPlaying) break

                val nextSec = (cur.currentSeconds + 1).coerceAtMost(cur.totalSeconds)
                val progress = ((nextSec.toFloat() / cur.totalSeconds.coerceAtLeast(1)) * 100f).roundToInt().coerceIn(0, 100)
                val timeLabel = formatSeconds(nextSec)

                _playbackState.value = cur.copy(
                    currentSeconds = nextSec,
                    currentTime = timeLabel,
                    progressPercent = progress
                )
            }
        }
    }

    private fun stopPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = null
    }

    override fun playVoicePreview(
        sampleQuote: String,
        voiceName: String,
        speed: Float,
        pitch: Int,
        voiceId: String?
    ) {
        sentenceDelayJob?.cancel()
        wordPacingJob?.cancel()
        wordPacingJob = null
        _playbackState.update { it?.copy(isPlaying = false, activeWordStart = -1, activeWordEnd = -1) }
        stopPlaybackTicker()
        stopReadingTimeTracker()
        ttsController?.stop()
        sherpaController?.stop()
        _previewingVoiceId.value = voiceId

        val targetItem = if (voiceId != null) {
            _voiceItems.value.find { it.id == voiceId }
        } else {
            _voiceItems.value.find { it.name == voiceName }
        }
        val pitchMultiplier = 1.0f + (pitch * 0.05f).coerceIn(-0.4f, 0.4f)

        if (targetItem != null && targetItem.isAiVoice && aiVoiceModelManager?.isModelInstalled(targetItem.id) == true) {
            sherpaController?.speakSentence(
                sentenceIndex = -1,
                rawText = sampleQuote,
                voiceId = targetItem.id,
                speed = speed,
                pitch = pitchMultiplier
            )
        } else {
            ttsController?.setVoiceByName(voiceName)
            ttsController?.speakSentence(
                sentenceIndex = -1,
                text = sampleQuote,
                speed = speed,
                pitch = pitchMultiplier
            )
        }
    }

    override fun stopVoicePreview() {
        _previewingVoiceId.value = null
        ttsController?.stop()
        sherpaController?.stop()
        restoreActiveBookVoice()
    }

    private fun restoreActiveBookVoice() {
        val activeBookVoice = _playbackState.value?.voiceName
            ?: _voiceItems.value.firstOrNull { it.isDefault }?.name
            ?: "Giọng hệ thống 1"
        val voiceItem = _voiceItems.value.find { it.name == activeBookVoice }
        if (voiceItem == null || !voiceItem.isAiVoice) {
            sherpaController?.releaseModel()
            ttsController?.setVoiceByName(activeBookVoice)
        } else if (sherpaController?.currentLoadedVoiceId != null &&
            sherpaController?.currentLoadedVoiceId != voiceItem.id
        ) {
            sherpaController?.releaseModel()
        }
    }

    override fun isTtsSpeaking(): Boolean {
        return ttsController?.isSpeaking?.value == true || sherpaController?.isSpeaking?.value == true
    }

    override fun releaseAiVoiceEngine() {
        sherpaController?.releaseModel()
        _previewingAmbientTrackId.value = null
        backgroundMusicController?.release()
    }

    override fun setAmbientMusicEnabled(enabled: Boolean) {
        _ambientMusicSettings.update { current ->
            val next = current.copy(isEnabledWhenReading = enabled)
            localStorage?.saveAmbientMusicSettings(next)
            next
        }
        syncBackgroundMusicPlayback()
    }

    private var ambientVolumePersistJob: Job? = null

    override fun setAmbientMusicVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        _ambientMusicSettings.update { current ->
            current.copy(volumePercent = clamped)
        }
        backgroundMusicController?.updateVolume(clamped)

        // Debounce writing volume changes to persistent storage to avoid high-frequency disk I/O while dragging slider
        ambientVolumePersistJob?.cancel()
        ambientVolumePersistJob = repoScope.launch {
            delay(300L)
            localStorage?.saveAmbientMusicSettings(_ambientMusicSettings.value)
        }
    }

    override fun selectAmbientTrack(trackId: String) {
        val exists = _ambientTracks.value.any { it.id == trackId }
        if (!exists) return
        if (_ambientMusicSettings.value.selectedTrackId == trackId) {
            // Already selected, avoid redundant persistence
            return
        }
        _ambientMusicSettings.update { current ->
            val next = current.copy(selectedTrackId = trackId)
            localStorage?.saveAmbientMusicSettings(next)
            next
        }
        if (_previewingAmbientTrackId.value != null && _previewingAmbientTrackId.value != trackId) {
            _previewingAmbientTrackId.value = null
        }
        syncBackgroundMusicPlayback()
    }

    override fun togglePreviewAmbientTrack(trackId: String) {
        val currentPreview = _previewingAmbientTrackId.value
        if (currentPreview == trackId) {
            _previewingAmbientTrackId.value = null
            syncBackgroundMusicPlayback()
        } else {
            _previewingAmbientTrackId.value = trackId
            syncBackgroundMusicPlayback()
        }
    }

    override fun stopAmbientPreview() {
        if (_previewingAmbientTrackId.value != null) {
            _previewingAmbientTrackId.value = null
            syncBackgroundMusicPlayback()
        }
    }

    override suspend fun importAmbientTrackFromUri(context: Context, uri: Uri): Result<AmbientTrack> {
        val bgCtrl = backgroundMusicController ?: BackgroundMusicController(context.applicationContext).also {
            backgroundMusicController = it
        }
        val result = bgCtrl.importCustomTrack(uri)
        result.onSuccess { newTrack ->
            _ambientTracks.update { list ->
                val builtIns = list.filter { it.isBuiltIn }
                val customs = listOf(newTrack) + list.filterNot { it.isBuiltIn }
                localStorage?.saveCustomAmbientTracks(customs)
                builtIns + customs
            }
            selectAmbientTrack(newTrack.id)
        }
        return result
    }

    override fun deleteAmbientTrack(trackId: String) {
        val target = _ambientTracks.value.find { it.id == trackId } ?: return
        if (target.isBuiltIn) return
        if (_previewingAmbientTrackId.value == trackId) {
            _previewingAmbientTrackId.value = null
        }
        backgroundMusicController?.deleteCustomTrackFile(target)
        _ambientTracks.update { list ->
            val updated = list.filterNot { it.id == trackId }
            localStorage?.saveCustomAmbientTracks(updated.filterNot { it.isBuiltIn })
            updated
        }
        if (_ambientMusicSettings.value.selectedTrackId == trackId) {
            val fallbackId = _ambientTracks.value.firstOrNull()?.id ?: "builtin_piano"
            _ambientMusicSettings.update { current ->
                val next = current.copy(selectedTrackId = fallbackId)
                localStorage?.saveAmbientMusicSettings(next)
                next
            }
        }
        syncBackgroundMusicPlayback()
    }

    private fun syncBackgroundMusicPlayback() {
        val bgCtrl = backgroundMusicController ?: return
        val settings = _ambientMusicSettings.value
        val previewId = _previewingAmbientTrackId.value
        val isBookPlaying = _playbackState.value?.isPlaying == true

        if (previewId != null) {
            val previewTrack = _ambientTracks.value.find { it.id == previewId }
            bgCtrl.syncPlaybackState(
                shouldPlay = previewTrack != null,
                track = previewTrack,
                volumePercent = settings.volumePercent
            )
            return
        }

        val activeTrack = _ambientTracks.value.find { it.id == settings.selectedTrackId }
            ?: _ambientTracks.value.firstOrNull()
        val shouldPlayWithBook = isBookPlaying && settings.isEnabledWhenReading && activeTrack != null
        bgCtrl.syncPlaybackState(
            shouldPlay = shouldPlayWithBook,
            track = activeTrack,
            volumePercent = settings.volumePercent
        )
    }

    private fun formatSeconds(totalSec: Int): String {
        val mins = totalSec / 60
        val secs = totalSec % 60
        return "%02d:%02d".format(mins, secs)
    }

    override fun updateBackupAccountEmail(email: String) {
        val trimmed = email.trim()
        _appSettings.update { current ->
            val next = current.copy(backupAccountEmail = trimmed)
            localStorage?.saveAppSettings(next)
            next
        }
    }

    override fun getBackupStats(): Triple<Int, Int, Int> {
        val booksCount = _books.value.size
        val bookmarksCount = _books.value.sumOf { it.bookmarks.size }
        val rulesCount = _pronunciationRules.value.size
        return Triple(booksCount, bookmarksCount, rulesCount)
    }

    override fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("appName", "Wordwave")
        root.put("version", 1)
        val now = System.currentTimeMillis()
        root.put("backupTimestamp", now)
        val settings = _appSettings.value
        root.put("backupAccountEmail", settings.backupAccountEmail)

        // 1. Settings
        val settingsObj = JSONObject().apply {
            put("userName", settings.userName)
            put("userRoleBadge", settings.userRoleBadge)
            put("themeMode", settings.themeMode.name)
            put("fontSizePx", settings.fontSizePx)
            put("autoScrollEnabled", settings.autoScrollEnabled)
            put("stopAtEndOfChapter", settings.stopAtEndOfChapter)
            put("defaultSpeed", settings.defaultSpeed.toDouble())
            put("reminderEnabled", settings.reminderEnabled)
            put("reminderHour", settings.reminderHour)
            put("reminderMinute", settings.reminderMinute)
            val daysArr = JSONArray()
            settings.reminderDays.forEach { daysArr.put(it) }
            put("reminderDays", daysArr)
            put("defaultSleepTimerMinutes", settings.defaultSleepTimerMinutes)
            put("highlightLevel", settings.highlightLevel.name)
            put("backupAccountEmail", settings.backupAccountEmail)
        }
        root.put("appSettings", settingsObj)

        // 2. Voice Tuning
        val tuning = _voiceTuningSettings.value
        val tuningObj = JSONObject().apply {
            put("speed", tuning.speed.toDouble())
            put("pitch", tuning.pitch)
            put("sentencePauseSeconds", tuning.sentencePauseSeconds.toDouble())
            put("defaultVoiceId", localStorage?.loadDefaultVoiceId().orEmpty())
        }
        root.put("voiceTuning", tuningObj)

        // 3. Pronunciation Rules
        val rulesArray = JSONArray()
        for (r in _pronunciationRules.value) {
            rulesArray.put(JSONObject().apply {
                put("id", r.id)
                put("originalWord", r.originalWord)
                put("spokenReplacement", r.spokenReplacement)
            })
        }
        root.put("pronunciationRules", rulesArray)

        // 4. Ambient Settings
        val ambient = _ambientMusicSettings.value
        val ambientObj = JSONObject().apply {
            put("isEnabledWhenReading", ambient.isEnabledWhenReading)
            put("selectedTrackId", ambient.selectedTrackId)
            put("volumePercent", ambient.volumePercent)
        }
        root.put("ambientSettings", ambientObj)

        // 5. Books state & progress & bookmarks
        val booksArray = JSONArray()
        for (b in _books.value) {
            val bObj = JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("author", b.author)
                put("epubFilePath", b.epubFilePath.orEmpty())
                put("coverImagePath", b.coverImagePath.orEmpty())
                put("currentChapterNumber", b.currentChapterNumber)
                put("currentChapterTitle", b.currentChapterTitle)
                put("currentChapterShortLabel", b.currentChapterShortLabel)
                put("progressPercent", b.progressPercent)
                put("currentPositionSeconds", b.currentPositionSeconds)
                put("remainingTimeLabel", b.remainingTimeLabel)
                put("voiceName", b.voiceName)
                put("isCompleted", b.isCompleted)
                put("isNew", b.isNew)
                put("lastReadEpochMillis", b.lastReadEpochMillis)
                put("categoryBadge", b.categoryBadge)
                put("genreLabel", b.genreLabel)
                put("totalDurationFormatted", b.totalDurationFormatted)
                put("totalChaptersCount", b.totalChaptersCount)
                put("synopsis", b.synopsis)
                put("format", b.format.name)
                put("gradStart", b.coverGradient.startColorHex)
                put("gradCenter", b.coverGradient.centerColorHex)
                put("gradEnd", b.coverGradient.endColorHex)

                val bmArray = JSONArray()
                for (bm in b.bookmarks) {
                    bmArray.put(JSONObject().apply {
                        put("id", bm.id)
                        put("chapterTitle", bm.chapterTitle)
                        put("quoteText", bm.quoteText)
                        put("timestampLabel", bm.timestampLabel)
                        put("chapterNumber", bm.chapterNumber)
                        put("sentenceIndex", bm.sentenceIndex)
                    })
                }
                put("bookmarks", bmArray)
            }
            booksArray.put(bObj)
        }
        root.put("books", booksArray)

        // Update last backup timestamp
        val updatedSettings = settings.copy(lastBackupTimestamp = now)
        _appSettings.value = updatedSettings
        localStorage?.saveAppSettings(updatedSettings)

        return root.toString(2)
    }

    override fun restoreBackupFromJson(jsonString: String): Result<String> {
        return runCatching {
            val root = JSONObject(jsonString)
            val appName = root.optString("appName")
            if (appName != "Wordwave" && !root.has("books") && !root.has("appSettings")) {
                throw IllegalArgumentException("Tệp tin không đúng định dạng sao lưu của Wordwave")
            }

            var restoredBooksCount = 0
            var restoredRulesCount = 0

            // 1. Restore AppSettings
            root.optJSONObject("appSettings")?.let { sObj ->
                val themeName = sObj.optString("themeMode", ReadingThemeMode.IVORY.name)
                val theme = runCatching { ReadingThemeMode.valueOf(themeName) }.getOrDefault(ReadingThemeMode.IVORY)
                val highlightName = sObj.optString("highlightLevel", HighlightLevel.SENTENCE.name)
                val highlight = runCatching { HighlightLevel.valueOf(highlightName) }.getOrDefault(HighlightLevel.SENTENCE)
                val reminderDaysArray = sObj.optJSONArray("reminderDays")
                val daysSet = if (reminderDaysArray != null && reminderDaysArray.length() > 0) {
                    (0 until reminderDaysArray.length()).map { reminderDaysArray.getInt(it) }.toSet()
                } else {
                    setOf(1, 2, 3, 4, 5, 6, 7)
                }
                val email = sObj.optString("backupAccountEmail", root.optString("backupAccountEmail", ""))

                val updated = _appSettings.value.copy(
                    userName = sObj.optString("userName", _appSettings.value.userName),
                    userRoleBadge = sObj.optString("userRoleBadge", _appSettings.value.userRoleBadge),
                    themeMode = theme,
                    fontSizePx = sObj.optInt("fontSizePx", 17),
                    autoScrollEnabled = sObj.optBoolean("autoScrollEnabled", true),
                    stopAtEndOfChapter = sObj.optBoolean("stopAtEndOfChapter", false),
                    defaultSpeed = sObj.optDouble("defaultSpeed", 1.15).toFloat(),
                    reminderEnabled = sObj.optBoolean("reminderEnabled", true),
                    reminderHour = sObj.optInt("reminderHour", 21),
                    reminderMinute = sObj.optInt("reminderMinute", 0),
                    reminderDays = daysSet,
                    defaultSleepTimerMinutes = sObj.optInt("defaultSleepTimerMinutes", 30),
                    highlightLevel = highlight,
                    backupAccountEmail = email.ifBlank { _appSettings.value.backupAccountEmail }
                )
                _appSettings.value = updated
                localStorage?.saveAppSettings(updated)

                appContext?.let { ctx ->
                    ReadingReminderScheduler.scheduleReminder(
                        context = ctx,
                        enabled = updated.reminderEnabled,
                        hour = updated.reminderHour,
                        minute = updated.reminderMinute,
                        days = updated.reminderDays
                    )
                }
            }

            // 2. Restore Voice Tuning
            root.optJSONObject("voiceTuning")?.let { tObj ->
                val tuning = VoiceTuningSettings(
                    speed = tObj.optDouble("speed", 1.15).toFloat(),
                    pitch = tObj.optInt("pitch", 0),
                    sentencePauseSeconds = tObj.optDouble("sentencePauseSeconds", 0.4).toFloat()
                )
                _voiceTuningSettings.value = tuning
                localStorage?.saveVoiceTuningSettings(tuning)
                val defVoiceId = tObj.optString("defaultVoiceId")
                if (defVoiceId.isNotBlank()) {
                    localStorage?.saveDefaultVoiceId(defVoiceId)
                }
            }

            // 3. Restore Pronunciation Rules
            root.optJSONArray("pronunciationRules")?.let { rArray ->
                val list = mutableListOf<PronunciationRule>()
                for (i in 0 until rArray.length()) {
                    val rObj = rArray.getJSONObject(i)
                    val orig = rObj.optString("originalWord").trim()
                    val repl = rObj.optString("spokenReplacement").trim()
                    if (orig.isNotBlank() && repl.isNotBlank()) {
                        list.add(
                            PronunciationRule(
                                id = rObj.optString("id", "rule_$i"),
                                originalWord = orig,
                                spokenReplacement = repl
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _pronunciationRules.value = list
                    localStorage?.savePronunciationRules(list)
                    ttsController?.setPronunciationRules(list)
                    restoredRulesCount = list.size
                }
            }

            // 4. Restore Ambient Settings
            root.optJSONObject("ambientSettings")?.let { aObj ->
                val ambient = AmbientMusicSettings(
                    isEnabledWhenReading = aObj.optBoolean("isEnabledWhenReading", false),
                    selectedTrackId = aObj.optString("selectedTrackId", "builtin_piano"),
                    volumePercent = aObj.optInt("volumePercent", 25)
                )
                _ambientMusicSettings.value = ambient
                localStorage?.saveAmbientMusicSettings(ambient)
                syncBackgroundMusicPlayback()
            }

            // 5. Restore Books progress & bookmarks
            root.optJSONArray("books")?.let { bArray ->
                val currentBooks = _books.value.toMutableList()
                var booksModified = false
                for (i in 0 until bArray.length()) {
                    val bObj = bArray.getJSONObject(i)
                    val bookId = bObj.optString("id")
                    if (bookId.isBlank()) continue

                    val bmArray = bObj.optJSONArray("bookmarks")
                    val restoredBookmarks = mutableListOf<BookBookmark>()
                    if (bmArray != null) {
                        for (j in 0 until bmArray.length()) {
                            val bmObj = bmArray.getJSONObject(j)
                            restoredBookmarks.add(
                                BookBookmark(
                                    id = bmObj.optString("id", "bm_$j"),
                                    chapterTitle = bmObj.optString("chapterTitle", "Chương 1"),
                                    quoteText = bmObj.optString("quoteText", ""),
                                    timestampLabel = bmObj.optString("timestampLabel", "00:00"),
                                    chapterNumber = bmObj.optInt("chapterNumber", 1),
                                    sentenceIndex = bmObj.optInt("sentenceIndex", 0)
                                )
                            )
                        }
                    }

                    val existingIdx = currentBooks.indexOfFirst { it.id == bookId }
                    if (existingIdx >= 0) {
                        val book = currentBooks[existingIdx]
                        currentBooks[existingIdx] = book.copy(
                            currentChapterNumber = bObj.optInt("currentChapterNumber", book.currentChapterNumber),
                            currentChapterTitle = bObj.optString("currentChapterTitle", book.currentChapterTitle),
                            currentPositionSeconds = bObj.optInt("currentPositionSeconds", book.currentPositionSeconds),
                            progressPercent = bObj.optInt("progressPercent", book.progressPercent),
                            bookmarks = if (restoredBookmarks.isNotEmpty()) restoredBookmarks else book.bookmarks
                        )
                        booksModified = true
                        restoredBooksCount++
                    }
                }
                if (booksModified) {
                    _books.value = currentBooks
                    localStorage?.saveBooks(currentBooks)
                }
            }

            "Khôi phục thành công! Đã đồng bộ $restoredBooksCount sách, $restoredRulesCount từ điển và toàn bộ cài đặt."
        }
    }

    override fun getGoogleDriveSyncManager(): GoogleDriveSyncManager? = googleDriveSyncManager

    override suspend fun syncBackupToGoogleDrive(): Result<String> {
        val manager = googleDriveSyncManager
            ?: return Result.failure(IllegalStateException("Dịch vụ Google Drive chưa sẵn sàng"))
        val backupJson = exportBackupJson()
        val uploadResult = manager.uploadBackupToDrive(backupJson)
        return uploadResult.map {
            val now = System.currentTimeMillis()
            val accountEmail = manager.getSignedInAccount()?.email.orEmpty()
            _appSettings.update { cur ->
                val next = cur.copy(
                    lastBackupTimestamp = now,
                    backupAccountEmail = accountEmail.ifBlank { cur.backupAccountEmail }
                )
                localStorage?.saveAppSettings(next)
                next
            }
            "Đã sao lưu thành công lên Google Drive lúc %02d:%02d".format(
                java.time.LocalTime.now().hour,
                java.time.LocalTime.now().minute
            )
        }
    }

    override suspend fun restoreBackupFromGoogleDrive(): Result<String> {
        val manager = googleDriveSyncManager
            ?: return Result.failure(IllegalStateException("Dịch vụ Google Drive chưa sẵn sàng"))
        val downloadResult = manager.downloadBackupFromDrive()
        return downloadResult.fold(
            onSuccess = { json ->
                restoreBackupFromJson(json)
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }
}
