package com.vietreader.tts.ui.settings

import androidx.compose.runtime.Immutable
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.PronunciationRule
import com.vietreader.tts.domain.model.SettingsSheetType

@Immutable
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val pronunciationRules: List<PronunciationRule> = emptyList(),
    val offlineStorageSummary: String = "Đã dùng 100 MB / Còn trống 1.9 GB",
    val activeSheet: SettingsSheetType = SettingsSheetType.NONE,
    val playbackState: PlaybackState? = null
)
