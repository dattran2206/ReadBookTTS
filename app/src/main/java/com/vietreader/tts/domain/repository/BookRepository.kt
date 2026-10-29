package com.vietreader.tts.domain.repository

import android.content.Context
import android.net.Uri
import com.vietreader.tts.domain.model.AmbientMusicSettings
import com.vietreader.tts.domain.model.AmbientTrack
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.PronunciationRule
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.model.VoiceTuningSettings
import kotlinx.coroutines.flow.StateFlow

interface BookRepository {
    val books: StateFlow<List<Book>>
    val playbackState: StateFlow<PlaybackState?>
    val selectedBookIdForDetail: StateFlow<String>
    val readerMode: StateFlow<ReaderMode>
    val voiceItems: StateFlow<List<VoiceItem>>
    val voiceTuningSettings: StateFlow<VoiceTuningSettings>
    val pronunciationRules: StateFlow<List<PronunciationRule>>
    val previewingVoiceId: StateFlow<String?>
    val appSettings: StateFlow<AppSettings>
    val sleepTimerMinutes: StateFlow<Int>
    val sleepTimerRemainingSeconds: StateFlow<Int>
    val ambientTracks: StateFlow<List<AmbientTrack>>
    val ambientMusicSettings: StateFlow<AmbientMusicSettings>
    val previewingAmbientTrackId: StateFlow<String?>
    val showOnboarding: StateFlow<Boolean>

    fun initialize(context: Context)
    suspend fun importBookFromUri(context: Context, uri: Uri): Result<Book>
    fun selectBookForDetail(bookId: String)
    fun setReaderMode(mode: ReaderMode)
    fun selectBookForPlayback(bookId: String)
    fun selectChapterForPlayback(bookId: String, chapterNumber: Int)
    fun togglePlayPause()
    fun resumePlayback()
    fun pausePlayback()
    fun stopPlayback()
    fun selectSentence(sentenceIndex: Int)
    fun skipToPreviousSentence()
    fun skipToNextSentence()
    fun seekToSeconds(seconds: Int)
    fun seekByDeltaSeconds(deltaSeconds: Int)
    fun cyclePlaybackSpeed()
    fun selectVoice(voiceName: String)
    fun selectVoiceForBook(bookId: String, voiceName: String, voiceRegion: String)
    fun setDefaultVoice(voiceId: String)
    fun startVoiceDownload(voiceId: String)
    fun togglePauseVoiceDownload(voiceId: String)
    fun cancelVoiceDownload(voiceId: String)
    fun deleteDownloadedVoice(voiceId: String)
    fun updateVoiceTuning(settings: VoiceTuningSettings)
    fun resetVoiceTuning()
    fun addPronunciationRule(originalWord: String, spokenReplacement: String)
    fun deletePronunciationRule(ruleId: String)
    fun updateAppearanceSettings(themeMode: ReadingThemeMode, fontSizePx: Int)
    fun updateHighlightLevel(level: HighlightLevel)
    fun updateAudioPlaybackSettings(autoScrollEnabled: Boolean, stopAtEndOfChapter: Boolean)
    fun updateDefaultReadingSpeed(speed: Float)
    fun toggleCloudSync()
    fun cycleReminderTime()
    fun updateReadingReminder(enabled: Boolean, hour: Int, minute: Int, days: Set<Int>)
    fun updateReaderProfile(userName: String, userRoleBadge: String)
    fun completeOnboarding(userName: String, userRoleBadge: String)
    fun cycleDefaultSleepTimer()
    fun updateDefaultSleepTimer(minutes: Int)
    fun cycleSleepTimer()
    fun setSleepTimer(minutes: Int)
    fun playVoicePreview(
        sampleQuote: String,
        voiceName: String,
        speed: Float = 1.0f,
        pitch: Int = 0,
        voiceId: String? = null
    )
    fun stopVoicePreview()
    fun isTtsSpeaking(): Boolean
    fun releaseAiVoiceEngine()
    fun toggleBookmark()
    fun deleteBookmark(bookId: String, bookmarkId: String)
    fun deleteBook(bookId: String)
    fun recordReadingTime(seconds: Long)
    fun setAmbientMusicEnabled(enabled: Boolean)
    fun setAmbientMusicVolume(volumePercent: Int)
    fun selectAmbientTrack(trackId: String)
    fun togglePreviewAmbientTrack(trackId: String)
    fun stopAmbientPreview()
    suspend fun importAmbientTrackFromUri(context: Context, uri: Uri): Result<AmbientTrack>
    fun deleteAmbientTrack(trackId: String)
    fun exportBackupJson(): String
    fun restoreBackupFromJson(jsonString: String): Result<String>
    fun updateBackupAccountEmail(email: String)
    fun getBackupStats(): Triple<Int, Int, Int>
    suspend fun syncBackupToGoogleDrive(): Result<String>
    suspend fun restoreBackupFromGoogleDrive(): Result<String>
    fun getGoogleDriveSyncManager(): com.vietreader.tts.data.cloud.GoogleDriveSyncManager?
}
