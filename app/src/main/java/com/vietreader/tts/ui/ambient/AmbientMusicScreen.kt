package com.vietreader.tts.ui.ambient

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vietreader.tts.domain.model.AmbientIconType
import com.vietreader.tts.domain.model.AmbientTrack
import com.vietreader.tts.domain.model.AmbientTrackFilter
import com.vietreader.tts.ui.components.AmbientMusicTopBar
import com.vietreader.tts.ui.components.AppBottomTab
import com.vietreader.tts.ui.components.AppFilterChip
import com.vietreader.tts.ui.components.AudioControllerBar
import com.vietreader.tts.ui.components.BottomNavBar
import com.vietreader.tts.ui.theme.Background
import com.vietreader.tts.ui.theme.ErrorColor
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnPrimaryContainer
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary
import kotlin.math.roundToInt

@Composable
fun AmbientMusicRoute(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTab: (AppBottomTab) -> Unit,
    viewModel: AmbientMusicViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onStopPreview()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onImportTrackFromUri(context, uri)
    }

    AmbientMusicScreen(
        uiState = uiState,
        onSelectFilter = viewModel::onSelectFilter,
        onToggleEnabled = viewModel::onToggleEnabled,
        onVolumeChange = viewModel::onVolumeChange,
        onSelectTrack = viewModel::onSelectTrack,
        onTogglePreviewTrack = viewModel::onTogglePreviewTrack,
        onAddTrackClick = {
            audioPickerLauncher.launch(arrayOf("audio/*"))
        },
        onRequestDeleteTrack = viewModel::onRequestDeleteTrack,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onConfirmDeleteTrack = viewModel::onConfirmDeleteTrack,
        onDismissStatusMessage = viewModel::onDismissStatusMessage,
        onNavigateToNowPlaying = onNavigateToNowPlaying,
        onToggleBookPlayPause = viewModel::onToggleBookPlayPause,
        onNavigateToTab = onNavigateToTab
    )
}

@Composable
fun AmbientMusicScreen(
    uiState: AmbientMusicUiState,
    onSelectFilter: (AmbientTrackFilter) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSelectTrack: (String) -> Unit,
    onTogglePreviewTrack: (String) -> Unit,
    onAddTrackClick: () -> Unit,
    onRequestDeleteTrack: (AmbientTrack) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteTrack: () -> Unit,
    onDismissStatusMessage: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onToggleBookPlayPause: () -> Unit,
    onNavigateToTab: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val showBuiltIn = uiState.selectedFilter == AmbientTrackFilter.ALL ||
        uiState.selectedFilter == AmbientTrackFilter.BUILT_IN
    val showCustom = uiState.selectedFilter == AmbientTrackFilter.ALL ||
        uiState.selectedFilter == AmbientTrackFilter.CUSTOM

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            AmbientMusicTopBar(onAddMusicClick = onAddTrackClick)
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.playbackState != null && uiState.playbackState.bookId.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp)
                    ) {
                        AudioControllerBar(
                            playbackState = uiState.playbackState,
                            onTogglePlayPause = onToggleBookPlayPause,
                            onBarClick = onNavigateToNowPlaying
                        )
                    }
                }

                BottomNavBar(
                    selectedTab = AppBottomTab.AMBIENT_MUSIC,
                    onTabSelected = onNavigateToTab
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Summary Card (matching VoiceManagementScreen's OfflineStorageHeaderCard)
            item(key = "ambient_summary_header") {
                AmbientSummaryHeaderCard(
                    totalTracksCount = uiState.builtInTracks.size + uiState.customTracks.size,
                    customTracksCount = uiState.customTracks.size,
                    activeTrackTitle = uiState.activeTrack?.title ?: "Giai điệu Tĩnh tâm"
                )
            }

            if (!uiState.importStatusMessage.isNullOrBlank()) {
                item(key = "status_banner") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryContainer.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismissStatusMessage)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.importStatusMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Đóng",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
                                )
                            )
                        }
                    }
                }
            }

            // 2. Master Toggle & Volume Control Card (matching SurfaceContainerLowest card style)
            item(key = "ambient_master_card") {
                AmbientMasterControlCard(
                    isEnabled = uiState.settings.isEnabledWhenReading,
                    volumePercent = uiState.settings.volumePercent,
                    activeTrackTitle = uiState.activeTrack?.title ?: "Giai điệu Tĩnh tâm",
                    isPreviewingAny = uiState.previewingTrackId != null,
                    onToggleEnabled = onToggleEnabled,
                    onVolumeChange = onVolumeChange
                )
            }

            // 3. Filter Chips Row (matching VoiceManagementScreen & LibraryScreen)
            item(key = "ambient_filter_chips") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AmbientTrackFilter.entries) { filter ->
                        AppFilterChip(
                            label = filter.label,
                            selected = uiState.selectedFilter == filter,
                            onClick = { onSelectFilter(filter) }
                        )
                    }
                }
            }

            // 4. Group 1: Built-in Relaxing Tracks (matching VoiceGroupHeader + VoiceItemCard)
            if (showBuiltIn && uiState.builtInTracks.isNotEmpty()) {
                item(key = "header_builtin") {
                    AmbientGroupHeader(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Âm thanh thư giãn có sẵn",
                        subtitle = "Chạy ngoại tuyến • Tự động lặp mượt mà khi đọc sách",
                        badgeText = "Có sẵn",
                        count = uiState.builtInTracks.size,
                        isHighlightedGroup = false,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                items(
                    items = uiState.builtInTracks,
                    key = { it.id }
                ) { track ->
                    AmbientTrackItemCard(
                        track = track,
                        isSelected = track.id == uiState.settings.selectedTrackId,
                        isPreviewPlaying = track.id == uiState.previewingTrackId,
                        onTogglePreview = { onTogglePreviewTrack(track.id) },
                        onUseNow = { onSelectTrack(track.id) },
                        onDeleteTrack = null
                    )
                }
            }

            // 5. Group 2: User's Custom Tracks
            if (showCustom) {
                item(key = "header_custom") {
                    AmbientGroupHeader(
                        icon = Icons.Outlined.MusicNote,
                        title = "Nhạc nền từ thiết bị",
                        subtitle = "Hỗ trợ file MP3, WAV, M4A, OGG lưu trên máy của bạn",
                        badgeText = "Cá nhân",
                        count = uiState.customTracks.size,
                        isHighlightedGroup = true,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                if (uiState.customTracks.isEmpty()) {
                    item(key = "empty_custom_tracks") {
                        EmptyCustomTracksCard(
                            isImporting = uiState.isImporting,
                            onAddClick = onAddTrackClick
                        )
                    }
                } else {
                    items(
                        items = uiState.customTracks,
                        key = { it.id }
                    ) { track ->
                        AmbientTrackItemCard(
                            track = track,
                            isSelected = track.id == uiState.settings.selectedTrackId,
                            isPreviewPlaying = track.id == uiState.previewingTrackId,
                            onTogglePreview = { onTogglePreviewTrack(track.id) },
                            onUseNow = { onSelectTrack(track.id) },
                            onDeleteTrack = { onRequestDeleteTrack(track) }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog (matching VoiceManagementScreen dialog style)
    val pendingDelete = uiState.trackPendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            containerColor = SurfaceContainerLowest,
            title = {
                Text(
                    text = "Xóa nhạc nền",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa bản nhạc \"${pendingDelete.title}\" khỏi danh sách nhạc nền?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteTrack) {
                    Text(
                        text = "Xóa nhạc",
                        color = ErrorColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text(
                        text = "Hủy",
                        color = OnSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun AmbientSummaryHeaderCard(
    totalTracksCount: Int,
    customTracksCount: Int,
    activeTrackTitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainer,
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Không gian Nhạc nền & Thư giãn",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = if (customTracksCount > 0) {
                        "Đã thêm $customTracksCount bài • $totalTracksCount bản nhạc"
                    } else {
                        "$totalTracksCount bản nhạc"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Tertiary
                )
            }
            Text(
                text = "Đang chọn mặc định: $activeTrackTitle • Phát lặp êm dịu song song cùng giọng đọc sách",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun AmbientMasterControlCard(
    isEnabled: Boolean,
    volumePercent: Int,
    activeTrackTitle: String,
    isPreviewingAny: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onVolumeChange: (Int) -> Unit
) {
    val presetVolumes = listOf(
        15 to "15% Nhẹ",
        25 to "25% Chuẩn",
        45 to "45% Vừa",
        70 to "70% Rõ"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) Primary.copy(alpha = 0.35f) else OutlineVariant.copy(alpha = 0.4f)
        ),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isEnabled) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LibraryMusic,
                            contentDescription = null,
                            tint = if (isEnabled) Primary else OnSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Phát nhạc nền khi đọc sách",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isPreviewingAny -> "Đang phát nghe thử..."
                                isEnabled -> "Tự động phát \"$activeTrackTitle\" khi đọc"
                                else -> "Đang tắt • Bật để hòa âm cùng giọng đọc"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (isEnabled || isPreviewingAny) Primary else OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnPrimary,
                        checkedTrackColor = PrimaryContainer,
                        uncheckedThumbColor = OnSurfaceVariant,
                        uncheckedTrackColor = SurfaceContainerHigh
                    )
                )
            }

            HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)

            // Volume Slider Control
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Âm lượng nhạc nền (Volume)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "$volumePercent%",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeDown,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = volumePercent.toFloat(),
                        onValueChange = { onVolumeChange(it.roundToInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = PrimaryContainer,
                            inactiveTrackColor = SurfaceContainerHigh
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetVolumes.forEach { (presetVal, label) ->
                        val isPresetSelected = volumePercent == presetVal
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(CircleShape)
                                .clickable { onVolumeChange(presetVal) },
                            shape = CircleShape,
                            color = if (isPresetSelected) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isPresetSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (isPresetSelected) Primary else OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientGroupHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    count: Int,
    isHighlightedGroup: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeBgColor = if (isHighlightedGroup) PrimaryContainer.copy(alpha = 0.16f) else SurfaceContainerHigh
    val badgeTextColor = if (isHighlightedGroup) Primary else OnSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "($count)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeBgColor)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = badgeTextColor
            )
        }
    }
}

@Composable
private fun AmbientTrackItemCard(
    track: AmbientTrack,
    isSelected: Boolean,
    isPreviewPlaying: Boolean,
    onTogglePreview: () -> Unit,
    onUseNow: () -> Unit,
    onDeleteTrack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val iconVector = when (track.iconType) {
        AmbientIconType.PIANO -> Icons.Outlined.Piano
        AmbientIconType.RAIN -> Icons.Outlined.WaterDrop
        AmbientIconType.STREAM -> Icons.Outlined.CloudQueue
        AmbientIconType.CUSTOM -> Icons.Outlined.AudioFile
    }

    val avatarBg = if (isSelected) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainerHigh
    val avatarIconColor = if (isSelected) Primary else OnSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Primary.copy(alpha = 0.35f) else OutlineVariant.copy(alpha = 0.4f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Avatar + Title & Meta + Status Badge (identical to VoiceItemCard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = avatarIconColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${track.subtitle} • ${track.durationLabel}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isSelected) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryContainer.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Mặc định",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Primary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Tertiary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (track.isBuiltIn) "Có sẵn" else "Đã thêm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Tertiary
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = SurfaceContainer
            )

            // Bottom Action Row (identical to VoiceItemCard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview Button
                    Surface(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePreview),
                        shape = CircleShape,
                        color = if (isPreviewPlaying) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isPreviewPlaying) "Đang phát..." else "Nghe thử",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isPreviewPlaying) Primary else OnSurface
                            )
                        }
                    }

                    // Active Status or "Dùng ngay" Button
                    if (isSelected) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Tertiary.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Tertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Đang dùng",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Tertiary
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onUseNow),
                            shape = CircleShape,
                            color = PrimaryContainer.copy(alpha = 0.14f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Dùng ngay",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Primary
                                )
                            }
                        }
                    }
                }

                if (onDeleteTrack != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDeleteTrack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Xóa nhạc nền",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCustomTracksCard(
    isImporting: Boolean,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Chưa có bản nhạc cá nhân nào",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = OnSurface
            )
            Text(
                text = "Bạn có thể thêm nhạc không lời (.mp3, .wav, .m4a, .ogg) từ điện thoại để phát nền khi đọc sách.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OnSurfaceVariant
            )
            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isImporting, onClick = onAddClick),
                shape = CircleShape,
                color = PrimaryContainer,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = OnPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isImporting) "Đang thêm..." else "Thêm nhạc từ máy",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnPrimaryContainer
                    )
                }
            }
        }
    }
}
