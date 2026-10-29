package com.vietreader.tts.ui.voices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.VoiceDownloadFilter
import com.vietreader.tts.domain.model.VoiceGroup
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.model.VoiceRegionFilter
import com.vietreader.tts.domain.model.VoiceTuningSettings
import com.vietreader.tts.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class VoiceManagementViewModel(
    private val repository: BookRepository = BookRepositoryImpl.instance
) : ViewModel() {

    private data class VoiceScreenControls(
        val selectedDownloadFilter: VoiceDownloadFilter = VoiceDownloadFilter.ALL,
        val selectedRegionFilter: VoiceRegionFilter = VoiceRegionFilter.ALL,
        val isTuningSheetVisible: Boolean = false,
        val voicePendingDelete: VoiceItem? = null
    )

    private val controls = MutableStateFlow(VoiceScreenControls())

    val uiState: StateFlow<VoiceManagementUiState> = combine(
        repository.voiceItems,
        repository.voiceTuningSettings,
        repository.playbackState,
        repository.previewingVoiceId,
        controls
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val voices = flows[0] as List<VoiceItem>
        val tuningSettings = flows[1] as VoiceTuningSettings
        val playbackState = flows[2] as PlaybackState?
        val previewingVoiceId = flows[3] as String?
        val ctrl = flows[4] as VoiceScreenControls

        val filteredVoices = voices.filter { item ->
            when (ctrl.selectedDownloadFilter) {
                VoiceDownloadFilter.ALL -> true
                VoiceDownloadFilter.DOWNLOADED ->
                    item.installStatus == VoiceInstallStatus.DOWNLOADED ||
                        item.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT
                VoiceDownloadFilter.NOT_DOWNLOADED ->
                    item.installStatus == VoiceInstallStatus.NOT_DOWNLOADED ||
                        item.installStatus == VoiceInstallStatus.DOWNLOADING
            }
        }

        val grouped = linkedMapOf<VoiceGroup, List<VoiceItem>>()
        VoiceGroup.entries.forEach { group ->
            val groupItems = filteredVoices
                .filter { it.group == group }
                .let { items ->
                    if (group == VoiceGroup.AI_HIGH_QUALITY) {
                        items.sortedWith(
                            compareBy<VoiceItem> { voice ->
                                when (voice.installStatus) {
                                    VoiceInstallStatus.ACTIVE_DEFAULT -> 0
                                    VoiceInstallStatus.DOWNLOADED -> 1
                                    VoiceInstallStatus.DOWNLOADING -> 2
                                    VoiceInstallStatus.NOT_DOWNLOADED -> 3
                                }
                            }
                        )
                    } else {
                        items
                    }
                }
            if (groupItems.isNotEmpty()) {
                grouped[group] = groupItems
            }
        }

        val usedMb = voices
            .filter {
                it.isAiVoice && (
                    it.installStatus == VoiceInstallStatus.DOWNLOADED ||
                        it.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT
                    )
            }
            .sumOf { it.sizeMb }

        VoiceManagementUiState(
            groupedVoices = grouped,
            selectedDownloadFilter = ctrl.selectedDownloadFilter,
            selectedRegionFilter = ctrl.selectedRegionFilter,
            previewingVoiceId = previewingVoiceId,
            usedStorageMb = usedMb,
            freeStorageLabel = "${voices.size} giọng đọc",
            storageUsageRatio = (usedMb / 500f).coerceIn(0f, 1f),
            isTuningSheetVisible = ctrl.isTuningSheetVisible,
            tuningSettings = tuningSettings,
            voicePendingDelete = ctrl.voicePendingDelete,
            playbackState = playbackState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VoiceManagementUiState()
    )

    fun onSelectDownloadFilter(filter: VoiceDownloadFilter) {
        controls.update { it.copy(selectedDownloadFilter = filter) }
    }

    fun onSelectRegionFilter(filter: VoiceRegionFilter) {
        controls.update { it.copy(selectedRegionFilter = filter) }
    }

    fun onTogglePreviewVoice(voiceId: String) {
        val voice = repository.voiceItems.value.find { it.id == voiceId }
        val currentPreviewId = repository.previewingVoiceId.value
        if (currentPreviewId == voiceId) {
            repository.stopVoicePreview()
        } else {
            if (voice != null) {
                val sampleQuote = "Xin chào, bạn đang nghe thử giọng đọc ${voice.name}. Chúc bạn có những giờ phút nghe sách thật thư giãn và trọn vẹn."
                val tuning = repository.voiceTuningSettings.value
                repository.playVoicePreview(
                    sampleQuote = sampleQuote,
                    voiceName = voice.name,
                    speed = tuning.speed,
                    pitch = tuning.pitch,
                    voiceId = voiceId
                )
            }
        }
    }

    fun onPreviewTuningSettings(settings: VoiceTuningSettings) {
        val defaultVoice = repository.voiceItems.value.firstOrNull { it.isDefault }
            ?: repository.voiceItems.value.firstOrNull()
        val voiceName = defaultVoice?.name ?: "Giọng hệ thống 1"
        repository.playVoicePreview(
            sampleQuote = "Đang kiểm tra tốc độ đọc ${"%.2f".format(settings.speed)} lần và độ cao âm thanh của giọng đọc.",
            voiceName = voiceName,
            speed = settings.speed,
            pitch = settings.pitch,
            voiceId = "tuning_preview"
        )
    }

    fun onStopPreview() {
        repository.stopVoicePreview()
    }

    fun onUseVoiceNow(voiceId: String) {
        repository.setDefaultVoice(voiceId)
    }

    fun onStartDownload(voiceId: String) {
        repository.startVoiceDownload(voiceId)
    }

    fun onTogglePauseDownload(voiceId: String) {
        repository.togglePauseVoiceDownload(voiceId)
    }

    fun onCancelDownload(voiceId: String) {
        repository.cancelVoiceDownload(voiceId)
    }

    fun onRequestDeleteVoice(voice: VoiceItem) {
        controls.update { it.copy(voicePendingDelete = voice) }
    }

    fun onDismissDeleteDialog() {
        controls.update { it.copy(voicePendingDelete = null) }
    }

    fun onConfirmDeleteVoice() {
        val target = controls.value.voicePendingDelete ?: return
        repository.deleteDownloadedVoice(target.id)
        controls.update { it.copy(voicePendingDelete = null) }
    }

    fun onOpenTuningSheet() {
        controls.update { it.copy(isTuningSheetVisible = true) }
    }

    fun onDismissTuningSheet() {
        repository.stopVoicePreview()
        controls.update { it.copy(isTuningSheetVisible = false) }
    }

    fun onApplyTuningSettings(settings: VoiceTuningSettings) {
        repository.stopVoicePreview()
        repository.updateVoiceTuning(settings)
        controls.update { it.copy(isTuningSheetVisible = false) }
    }

    fun onResetTuningSettings() {
        repository.stopVoicePreview()
        repository.resetVoiceTuning()
        controls.update { it.copy(isTuningSheetVisible = false) }
    }

    fun onTogglePlayPause() {
        repository.togglePlayPause()
    }

    override fun onCleared() {
        repository.stopVoicePreview()
        super.onCleared()
    }
}
