package com.vietreader.tts.ui.voices

import androidx.compose.runtime.Immutable
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceDownloadFilter
import com.vietreader.tts.domain.model.VoiceGroup
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.model.VoiceRegionFilter
import com.vietreader.tts.domain.model.VoiceTuningSettings

@Immutable
data class VoiceManagementUiState(
    val groupedVoices: Map<VoiceGroup, List<VoiceItem>> = VoiceCatalog.defaultVoiceItems.groupBy { it.group },
    val selectedDownloadFilter: VoiceDownloadFilter = VoiceDownloadFilter.ALL,
    val selectedRegionFilter: VoiceRegionFilter = VoiceRegionFilter.ALL,
    val previewingVoiceId: String? = null,
    val usedStorageMb: Int = 100,
    val freeStorageLabel: String = "1.9 GB",
    val storageUsageRatio: Float = 0.05f,
    val isTuningSheetVisible: Boolean = false,
    val tuningSettings: VoiceTuningSettings = VoiceTuningSettings(),
    val voicePendingDelete: VoiceItem? = null,
    val playbackState: PlaybackState? = null
)
