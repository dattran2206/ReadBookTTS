package com.vietreader.tts.domain.model

import androidx.compose.runtime.Immutable

enum class AmbientIconType {
    RAIN,
    STREAM,
    PIANO,
    CUSTOM
}

enum class AmbientTrackFilter(val label: String) {
    ALL("Tất cả"),
    BUILT_IN("Có sẵn"),
    CUSTOM("Nhạc của bạn")
}

@Immutable
data class AmbientTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val durationLabel: String,
    val filePath: String,
    val isBuiltIn: Boolean = false,
    val iconType: AmbientIconType = AmbientIconType.CUSTOM
)

@Immutable
data class AmbientMusicSettings(
    val isEnabledWhenReading: Boolean = false,
    val selectedTrackId: String = "builtin_rain",
    val volumePercent: Int = 25
)
