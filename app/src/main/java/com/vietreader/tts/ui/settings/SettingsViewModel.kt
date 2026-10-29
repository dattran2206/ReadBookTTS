package com.vietreader.tts.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.PronunciationRule
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.SettingsSheetType
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: BookRepository = BookRepositoryImpl.instance
) : ViewModel() {

    private val activeSheetFlow = MutableStateFlow(SettingsSheetType.NONE)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.appSettings,
        repository.voiceItems,
        repository.pronunciationRules,
        repository.playbackState,
        activeSheetFlow
    ) { flows ->
        val settings = flows[0] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val voices = flows[1] as List<VoiceItem>
        @Suppress("UNCHECKED_CAST")
        val rules = flows[2] as List<PronunciationRule>
        val playbackState = flows[3] as PlaybackState?
        val sheet = flows[4] as SettingsSheetType

        val storageSummary = "${voices.size} gói giọng hệ thống có sẵn"

        SettingsUiState(
            settings = settings.copy(dictionaryWordCount = rules.size),
            pronunciationRules = rules,
            offlineStorageSummary = storageSummary,
            activeSheet = sheet,
            playbackState = playbackState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onOpenSheet(sheet: SettingsSheetType) {
        activeSheetFlow.value = sheet
    }

    fun onDismissSheet() {
        repository.stopVoicePreview()
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onUpdateAppearance(themeMode: ReadingThemeMode, fontSizePx: Int) {
        repository.updateAppearanceSettings(themeMode, fontSizePx)
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onUpdateAudioPlayback(autoScrollEnabled: Boolean, stopAtEndOfChapter: Boolean) {
        repository.updateAudioPlaybackSettings(autoScrollEnabled, stopAtEndOfChapter)
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onUpdateReadingSpeed(speed: Float) {
        repository.updateDefaultReadingSpeed(speed)
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onAddPronunciationRule(originalWord: String, spokenReplacement: String) {
        repository.addPronunciationRule(originalWord, spokenReplacement)
    }

    fun onDeletePronunciationRule(ruleId: String) {
        repository.deletePronunciationRule(ruleId)
    }

    fun onTestPronunciationRule(rule: PronunciationRule) {
        val defaultVoice = repository.voiceItems.value.firstOrNull { it.isDefault }?.name ?: "Giọng hệ thống 1"
        repository.playVoicePreview(
            sampleQuote = rule.originalWord,
            voiceName = defaultVoice,
            speed = repository.appSettings.value.defaultSpeed,
            pitch = repository.voiceTuningSettings.value.pitch,
            voiceId = "dict_${rule.id}"
        )
    }

    fun onStopPreview() {
        repository.stopVoicePreview()
    }

    fun onToggleCloudSync() {
        repository.toggleCloudSync()
    }

    fun onCycleReminderTime() {
        activeSheetFlow.value = SettingsSheetType.REMINDER_SETTINGS
    }

    fun onUpdateReadingReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        days: Set<Int>
    ) {
        repository.updateReadingReminder(enabled, hour, minute, days)
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onCycleDefaultSleepTimer() {
        activeSheetFlow.value = SettingsSheetType.DEFAULT_SLEEP_TIMER
    }

    fun onUpdateDefaultSleepTimer(minutes: Int) {
        repository.updateDefaultSleepTimer(minutes)
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onUpdateProfile(name: String, role: String) {
        repository.updateReaderProfile(name, role)
        activeSheetFlow.value = SettingsSheetType.NONE
    }

    fun onUpdateBackupAccountEmail(email: String) {
        repository.updateBackupAccountEmail(email)
    }

    fun exportBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupFromJson(json: String): Result<String> = repository.restoreBackupFromJson(json)

    fun getBackupStats(): Triple<Int, Int, Int> = repository.getBackupStats()

    fun onTogglePlayPause() {
        repository.togglePlayPause()
    }

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudSyncMessage = MutableStateFlow<String?>(null)
    val cloudSyncMessage: StateFlow<String?> = _cloudSyncMessage.asStateFlow()

    fun getGoogleSignedInAccount(): GoogleSignInAccount? {
        return repository.getGoogleDriveSyncManager()?.getSignedInAccount()
    }

    fun getGoogleSignInIntent(): Intent? {
        return repository.getGoogleDriveSyncManager()?.getSignInIntent()
    }

    fun handleGoogleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        val result = repository.getGoogleDriveSyncManager()?.handleSignInResult(data)
            ?: Result.failure(IllegalStateException("Google Drive chưa sẵn sàng"))
        result.onSuccess { account ->
            account.email?.let { email ->
                repository.updateBackupAccountEmail(email)
            }
        }
        return result
    }

    fun onGoogleSignOut(onComplete: () -> Unit = {}) {
        repository.getGoogleDriveSyncManager()?.signOut {
            repository.updateBackupAccountEmail("")
            onComplete()
        }
    }

    fun onSyncBackupToGoogleDrive(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudSyncMessage.value = "Đang tải lên Google Drive..."
            val result = repository.syncBackupToGoogleDrive()
            _isCloudSyncing.value = false
            result.onSuccess { msg ->
                _cloudSyncMessage.value = msg
            }.onFailure { err ->
                _cloudSyncMessage.value = "Lỗi sao lưu: ${err.localizedMessage}"
            }
            onResult(result)
        }
    }

    fun onRestoreBackupFromGoogleDrive(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudSyncMessage.value = "Đang khôi phục từ Google Drive..."
            val result = repository.restoreBackupFromGoogleDrive()
            _isCloudSyncing.value = false
            result.onSuccess { msg ->
                _cloudSyncMessage.value = msg
            }.onFailure { err ->
                _cloudSyncMessage.value = "Lỗi khôi phục: ${err.localizedMessage}"
            }
            onResult(result)
        }
    }

    override fun onCleared() {
        repository.stopVoicePreview()
        super.onCleared()
    }
}
