package com.vietreader.tts.ui.ambient

import androidx.compose.runtime.Immutable
import com.vietreader.tts.domain.model.AmbientMusicSettings
import com.vietreader.tts.domain.model.AmbientTrack
import com.vietreader.tts.domain.model.AmbientTrackFilter
import com.vietreader.tts.domain.model.PlaybackState

@Immutable
data class AmbientMusicUiState(
    val settings: AmbientMusicSettings = AmbientMusicSettings(),
    val builtInTracks: List<AmbientTrack> = emptyList(),
    val customTracks: List<AmbientTrack> = emptyList(),
    val selectedFilter: AmbientTrackFilter = AmbientTrackFilter.ALL,
    val previewingTrackId: String? = null,
    val trackPendingDelete: AmbientTrack? = null,
    val isImporting: Boolean = false,
    val importStatusMessage: String? = null,
    val playbackState: PlaybackState? = null
) {
    val activeTrack: AmbientTrack?
        get() = (builtInTracks + customTracks).find { it.id == settings.selectedTrackId }
            ?: builtInTracks.firstOrNull()
}
