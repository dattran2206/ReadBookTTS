package com.vietreader.tts.ui.voices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vietreader.tts.domain.model.VoiceDownloadFilter
import com.vietreader.tts.domain.model.VoiceGroup
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.model.VoiceRegionFilter
import com.vietreader.tts.domain.model.VoiceTuningSettings
import com.vietreader.tts.ui.components.AppBottomTab
import com.vietreader.tts.ui.components.AppFilterChip
import com.vietreader.tts.ui.components.AudioControllerBar
import com.vietreader.tts.ui.components.BottomNavBar
import com.vietreader.tts.ui.components.VoiceManagementTopBar
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
import com.vietreader.tts.ui.theme.SurfaceContainerHighest
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary
import com.vietreader.tts.ui.theme.VietReaderTheme
import kotlin.math.roundToInt

/**
 * Stateful Route for Screen 4 (`stitch/04-quan-ly-giong-doc.html`).
 */
@Composable
fun VoiceManagementRoute(
    viewModel: VoiceManagementViewModel = viewModel(),
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToTab: (AppBottomTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onStopPreview()
        }
    }

    VoiceManagementScreen(
        uiState = uiState,
        onSelectDownloadFilter = viewModel::onSelectDownloadFilter,
        onSelectRegionFilter = viewModel::onSelectRegionFilter,
        onTogglePreviewVoice = viewModel::onTogglePreviewVoice,
        onUseVoiceNow = viewModel::onUseVoiceNow,
        onStartDownload = viewModel::onStartDownload,
        onTogglePauseDownload = viewModel::onTogglePauseDownload,
        onCancelDownload = viewModel::onCancelDownload,
        onRequestDeleteVoice = viewModel::onRequestDeleteVoice,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onConfirmDeleteVoice = viewModel::onConfirmDeleteVoice,
        onOpenTuningSheet = viewModel::onOpenTuningSheet,
        onDismissTuningSheet = viewModel::onDismissTuningSheet,
        onApplyTuningSettings = viewModel::onApplyTuningSettings,
        onPreviewTuningSettings = viewModel::onPreviewTuningSettings,
        onResetTuningSettings = viewModel::onResetTuningSettings,
        onTogglePlayPause = viewModel::onTogglePlayPause,
        onMiniPlayerClick = onNavigateToNowPlaying,
        onTabSelected = onNavigateToTab
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementScreen(
    uiState: VoiceManagementUiState,
    onSelectDownloadFilter: (VoiceDownloadFilter) -> Unit,
    onSelectRegionFilter: (VoiceRegionFilter) -> Unit,
    onTogglePreviewVoice: (String) -> Unit,
    onUseVoiceNow: (String) -> Unit,
    onStartDownload: (String) -> Unit,
    onTogglePauseDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRequestDeleteVoice: (VoiceItem) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteVoice: () -> Unit,
    onOpenTuningSheet: () -> Unit,
    onDismissTuningSheet: () -> Unit,
    onApplyTuningSettings: (VoiceTuningSettings) -> Unit,
    onResetTuningSettings: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    onPreviewTuningSettings: (VoiceTuningSettings) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            VoiceManagementTopBar(
                onOpenTuningSheet = onOpenTuningSheet
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.playbackState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp)
                    ) {
                        AudioControllerBar(
                            playbackState = uiState.playbackState,
                            onTogglePlayPause = onTogglePlayPause,
                            onBarClick = onMiniPlayerClick
                        )
                    }
                }

                BottomNavBar(
                    selectedTab = AppBottomTab.VOICES,
                    onTabSelected = onTabSelected
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
            // 1. System TTS Engine Status Header
            item {
                OfflineStorageHeaderCard(
                    usedStorageMb = uiState.usedStorageMb,
                    freeStorageLabel = uiState.freeStorageLabel,
                    usageRatio = uiState.storageUsageRatio
                )
            }

            // 1b. Download Status Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VoiceDownloadFilter.entries) { filter ->
                        AppFilterChip(
                            label = filter.label,
                            selected = uiState.selectedDownloadFilter == filter,
                            onClick = { onSelectDownloadFilter(filter) }
                        )
                    }
                }
            }

            // 2. Grouped Voice List (Hiển thị trực tiếp các gói giọng hệ thống từ Android SDK)
            if (uiState.groupedVoices.isEmpty()) {
                item(key = "empty_voices") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainer,
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Chưa có giọng đọc phù hợp bộ lọc",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = OnSurface
                            )
                            Text(
                                text = "Engine TTS hệ thống trên thiết bị hiện chỉ cung cấp các gói giọng chuẩn Miền Bắc (có sẵn ngoại tuyến).",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                uiState.groupedVoices.forEach { (group, voices) ->
                    item(key = "header_${group.name}") {
                        VoiceGroupHeader(
                            group = group,
                            count = voices.size,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(
                        items = voices,
                        key = { it.id }
                    ) { voice ->
                        VoiceItemCard(
                            voice = voice,
                            isPreviewPlaying = uiState.previewingVoiceId == voice.id,
                            onTogglePreview = { onTogglePreviewVoice(voice.id) },
                            onUseNow = { onUseVoiceNow(voice.id) },
                            onStartDownload = { onStartDownload(voice.id) },
                            onTogglePauseDownload = { onTogglePauseDownload(voice.id) },
                            onCancelDownload = { onCancelDownload(voice.id) },
                            onDeleteVoice = { onRequestDeleteVoice(voice) }
                        )
                    }
                }
            }
        }
    }

    // 4. Voice Tuning Modal Bottom Sheet
    if (uiState.isTuningSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissTuningSheet,
            sheetState = sheetState,
            containerColor = SurfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            VoiceTuningSheetContent(
                initialSettings = uiState.tuningSettings,
                isPreviewPlaying = uiState.previewingVoiceId == "tuning_preview",
                onPreview = onPreviewTuningSettings,
                onApply = onApplyTuningSettings,
                onReset = onResetTuningSettings,
                onClose = onDismissTuningSheet
            )
        }
    }

    // 5. Delete Confirmation Dialog
    val pendingDelete = uiState.voicePendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            containerColor = SurfaceContainerLowest,
            title = {
                Text(
                    text = "Thu hồi bộ nhớ giọng đọc",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = "Thu hồi ${pendingDelete.sizeMb} MB bộ nhớ cho giọng đọc ${pendingDelete.name}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteVoice) {
                    Text(
                        text = "Xóa tải về",
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
private fun OfflineStorageHeaderCard(
    usedStorageMb: Int,
    freeStorageLabel: String,
    usageRatio: Float,
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
                    text = "Giọng đọc AI & Hệ thống",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = if (usedStorageMb > 0) "Đã tải $usedStorageMb MB • $freeStorageLabel" else freeStorageLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Tertiary
                )
            }
            Text(
                text = "Tải giọng AI chất lượng cao về máy để nghe ngoại tuyến hoặc dùng ngay giọng hệ thống",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun VoiceGroupHeader(
    group: VoiceGroup,
    count: Int,
    modifier: Modifier = Modifier
) {
    val groupIcon = when (group) {
        VoiceGroup.AI_HIGH_QUALITY -> Icons.Outlined.AutoAwesome
        VoiceGroup.SYSTEM -> Icons.Outlined.Memory
    }

    val badgeBgColor = when (group) {
        VoiceGroup.AI_HIGH_QUALITY -> PrimaryContainer.copy(alpha = 0.16f)
        VoiceGroup.SYSTEM -> SurfaceContainerHigh
    }

    val badgeTextColor = when (group) {
        VoiceGroup.AI_HIGH_QUALITY -> Primary
        VoiceGroup.SYSTEM -> OnSurfaceVariant
    }

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
                    imageVector = groupIcon,
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
                        text = group.title,
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
                    text = group.subtitle,
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
                text = group.badgeText,
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
private fun VoiceItemCard(
    voice: VoiceItem,
    isPreviewPlaying: Boolean,
    onTogglePreview: () -> Unit,
    onUseNow: () -> Unit,
    onStartDownload: () -> Unit,
    onTogglePauseDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActiveDefault = voice.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT
    val isDownloading = voice.installStatus == VoiceInstallStatus.DOWNLOADING

    val avatarBg = when {
        isActiveDefault -> PrimaryContainer.copy(alpha = 0.15f)
        isDownloading -> Primary.copy(alpha = 0.10f)
        else -> SurfaceContainerHigh
    }
    val avatarTextColor = when {
        isActiveDefault || isDownloading -> Primary
        else -> OnSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActiveDefault) Primary.copy(alpha = 0.35f) else OutlineVariant.copy(alpha = 0.4f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Initials Avatar + Name & Meta + Status Badge
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
                        Text(
                            text = voice.initials,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = avatarTextColor
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = voice.name,
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
                            text = voice.metaLine,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Status Badge
                when (voice.installStatus) {
                    VoiceInstallStatus.ACTIVE_DEFAULT -> {
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
                    }

                    VoiceInstallStatus.DOWNLOADED -> {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Tertiary.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (voice.group == VoiceGroup.SYSTEM) "Có sẵn" else "Đã tải",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Tertiary
                            )
                        }
                    }

                    VoiceInstallStatus.DOWNLOADING -> {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (voice.isDownloadPaused) {
                                    "Tạm dừng ${voice.downloadProgressPercent}%"
                                } else {
                                    "Đang tải ${voice.downloadProgressPercent}%"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Primary
                            )
                        }
                    }

                    VoiceInstallStatus.NOT_DOWNLOADED -> {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Chưa tải",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = SurfaceContainer
            )

            // Bottom Action Row by Install Status
            when (voice.installStatus) {
                VoiceInstallStatus.ACTIVE_DEFAULT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PreviewSampleButton(
                                label = if (isPreviewPlaying) "Đang phát..." else "Nghe thử",
                                isPlaying = isPreviewPlaying,
                                onClick = onTogglePreview
                            )

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
                        }

                        if (voice.group != VoiceGroup.SYSTEM) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onDeleteVoice),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Xóa tải về",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                VoiceInstallStatus.DOWNLOADED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PreviewSampleButton(
                                label = if (isPreviewPlaying) "Đang phát..." else "Nghe thử",
                                isPlaying = isPreviewPlaying,
                                onClick = onTogglePreview
                            )

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

                        if (voice.group != VoiceGroup.SYSTEM) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onDeleteVoice),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Xóa tải về",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                VoiceInstallStatus.DOWNLOADING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainer)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((voice.downloadProgressPercent / 100f).coerceIn(0f, 1f))
                                        .clip(CircleShape)
                                        .background(PrimaryContainer)
                                )
                            }

                            Text(
                                text = voice.downloadedMbFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onTogglePauseDownload),
                                shape = CircleShape,
                                color = SurfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (voice.isDownloadPaused) {
                                            Icons.Outlined.PlayArrow
                                        } else {
                                            Icons.Outlined.Pause
                                        },
                                        contentDescription = null,
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (voice.isDownloadPaused) "Tiếp tục" else "Tạm dừng",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = OnSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onCancelDownload),
                                shape = CircleShape,
                                color = SurfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Hủy",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                VoiceInstallStatus.NOT_DOWNLOADED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tải mô hình AI về máy để nghe thử & đọc sách",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = OnSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Surface(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onStartDownload),
                            shape = CircleShape,
                            color = Primary,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = OnPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Tải về (${voice.sizeMb} MB)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = OnPrimary
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
private fun PreviewSampleButton(
    label: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (isPlaying) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isPlaying) Primary else OnSurface
            )
        }
    }
}

@Composable
private fun VoiceTuningSheetContent(
    initialSettings: VoiceTuningSettings,
    isPreviewPlaying: Boolean,
    onPreview: (VoiceTuningSettings) -> Unit,
    onApply: (VoiceTuningSettings) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    var speed by remember(initialSettings) { mutableFloatStateOf(initialSettings.speed) }
    var pitch by remember(initialSettings) { mutableIntStateOf(initialSettings.pitch) }
    var pauseSec by remember(initialSettings) { mutableFloatStateOf(initialSettings.sentencePauseSeconds) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tinh chỉnh giọng đọc",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = "Áp dụng cho giọng đang phát",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = OnSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Đóng",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = SurfaceContainer)

        // 1. Speed Slider (0.75x - 2.0x)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tốc độ đọc (Speed)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
                Text(
                    text = "%.2fx".format(speed),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "0.75x",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
                Slider(
                    value = speed,
                    onValueChange = { raw ->
                        speed = ((raw * 20f).roundToInt() / 20f).coerceIn(0.75f, 2.0f)
                    },
                    valueRange = 0.75f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceContainerHigh
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "2.0x",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
            }
        }

        // 2. Pitch Slider (-4 to +4)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val pitchLabel = when {
                pitch == 0 -> "+0 (Tự nhiên)"
                pitch > 0 -> "+$pitch"
                else -> "$pitch"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cao độ (Pitch)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
                Text(
                    text = pitchLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Trầm",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
                Slider(
                    value = pitch.toFloat(),
                    onValueChange = { pitch = it.roundToInt().coerceIn(-4, 4) },
                    valueRange = -4f..4f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceContainerHigh
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Bổng",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
            }
        }

        // 3. Sentence Pause Slider (0.1s - 1.0s)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ngắt nghỉ câu (Sentence Pause)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
                Text(
                    text = "%.2fs".format(pauseSec),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "0.1s",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
                Slider(
                    value = pauseSec,
                    onValueChange = { raw ->
                        pauseSec = ((raw * 20f).roundToInt() / 20f).coerceIn(0.1f, 1.0f)
                    },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceContainerHigh
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "1.0s",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
            }
        }

        // Live Preview Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(CircleShape)
                .clickable {
                    onPreview(
                        VoiceTuningSettings(
                            speed = speed,
                            pitch = pitch,
                            sentencePauseSeconds = pauseSec
                        )
                    )
                },
            shape = CircleShape,
            color = if (isPreviewPlaying) PrimaryContainer.copy(alpha = 0.18f) else SurfaceContainer,
            border = BorderStroke(1.dp, if (isPreviewPlaying) Primary else OutlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPreviewPlaying) "Đang phát mẫu thử..." else "Nghe thử cấu hình hiện tại",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
        }

        // Action Buttons (Mặc định | Áp dụng)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onReset),
                shape = CircleShape,
                color = SurfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Mặc định",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurface
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        onApply(
                            VoiceTuningSettings(
                                speed = speed,
                                pitch = pitch,
                                sentencePauseSeconds = pauseSec
                            )
                        )
                    },
                shape = CircleShape,
                color = Primary,
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Áp dụng",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceManagementScreenPreview() {
    VietReaderTheme {
        VoiceManagementScreen(
            uiState = VoiceManagementUiState(),
            onSelectDownloadFilter = {},
            onSelectRegionFilter = {},
            onTogglePreviewVoice = {},
            onUseVoiceNow = {},
            onStartDownload = {},
            onTogglePauseDownload = {},
            onCancelDownload = {},
            onRequestDeleteVoice = {},
            onDismissDeleteDialog = {},
            onConfirmDeleteVoice = {},
            onOpenTuningSheet = {},
            onDismissTuningSheet = {},
            onApplyTuningSettings = {},
            onResetTuningSettings = {},
            onTogglePlayPause = {},
            onMiniPlayerClick = {},
            onTabSelected = {}
        )
    }
}
