package com.vietreader.tts.ui.ambient

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.AmbientMusicSettings
import com.vietreader.tts.domain.model.AmbientTrack
import com.vietreader.tts.domain.model.AmbientTrackFilter
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AmbientMusicViewModel(
    private val repository: BookRepository = BookRepositoryImpl.instance
) : ViewModel() {

    private data class ScreenLocalState(
        val selectedFilter: AmbientTrackFilter = AmbientTrackFilter.ALL,
        val trackPendingDelete: AmbientTrack? = null,
        val isImporting: Boolean = false,
        val importStatusMessage: String? = null
    )

    private val localState = MutableStateFlow(ScreenLocalState())

    val uiState: StateFlow<AmbientMusicUiState> = combine(
        repository.ambientMusicSettings,
        repository.ambientTracks,
        repository.previewingAmbientTrackId,
        repository.playbackState,
        localState
    ) { flows ->
        val settings = flows[0] as AmbientMusicSettings
        @Suppress("UNCHECKED_CAST")
        val tracks = flows[1] as List<AmbientTrack>
        val previewId = flows[2] as String?
        val playbackState = flows[3] as PlaybackState?
        val local = flows[4] as ScreenLocalState

        AmbientMusicUiState(
            settings = settings,
            builtInTracks = tracks.filter { it.isBuiltIn },
            customTracks = tracks.filterNot { it.isBuiltIn },
            selectedFilter = local.selectedFilter,
            previewingTrackId = previewId,
            trackPendingDelete = local.trackPendingDelete,
            isImporting = local.isImporting,
            importStatusMessage = local.importStatusMessage,
            playbackState = playbackState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AmbientMusicUiState()
    )

    fun onSelectFilter(filter: AmbientTrackFilter) {
        localState.update { it.copy(selectedFilter = filter) }
    }

    fun onToggleEnabled(enabled: Boolean) {
        repository.setAmbientMusicEnabled(enabled)
    }

    fun onVolumeChange(volumePercent: Int) {
        repository.setAmbientMusicVolume(volumePercent)
    }

    fun onSelectTrack(trackId: String) {
        repository.selectAmbientTrack(trackId)
    }

    fun onTogglePreviewTrack(trackId: String) {
        repository.togglePreviewAmbientTrack(trackId)
    }

    fun onStopPreview() {
        repository.stopAmbientPreview()
    }

    fun onImportTrackFromUri(context: Context, uri: Uri?) {
        if (uri == null) return
        localState.update { it.copy(isImporting = true, importStatusMessage = null) }
        viewModelScope.launch {
            val result = repository.importAmbientTrackFromUri(context, uri)
            result.onSuccess { track ->
                localState.update {
                    it.copy(
                        isImporting = false,
                        importStatusMessage = "Đã thêm nhạc nền \"${track.title}\""
                    )
                }
            }.onFailure { err ->
                localState.update {
                    it.copy(
                        isImporting = false,
                        importStatusMessage = "Lỗi thêm nhạc: ${err.message ?: "Không thể đọc file"}"
                    )
                }
            }
        }
    }

    fun onDismissStatusMessage() {
        localState.update { it.copy(importStatusMessage = null) }
    }

    fun onRequestDeleteTrack(track: AmbientTrack) {
        if (track.isBuiltIn) return
        localState.update { it.copy(trackPendingDelete = track) }
    }

    fun onDismissDeleteDialog() {
        localState.update { it.copy(trackPendingDelete = null) }
    }

    fun onConfirmDeleteTrack() {
        val target = localState.value.trackPendingDelete ?: return
        repository.deleteAmbientTrack(target.id)
        localState.update {
            it.copy(
                trackPendingDelete = null,
                importStatusMessage = "Đã xóa nhạc nền \"${target.title}\""
            )
        }
    }

    fun onToggleBookPlayPause() {
        repository.togglePlayPause()
    }

    override fun onCleared() {
        repository.stopAmbientPreview()
        super.onCleared()
    }
}
