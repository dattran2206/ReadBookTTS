package com.vietreader.tts.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.PronunciationRule
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.SettingsSheetType
import com.vietreader.tts.ui.components.AppBottomTab
import com.vietreader.tts.ui.components.AudioControllerBar
import com.vietreader.tts.ui.components.BackupRestoreSheetContent
import com.vietreader.tts.ui.components.BottomNavBar
import com.vietreader.tts.ui.components.EditProfileSheetContent
import com.vietreader.tts.ui.components.ReadingReminderSheetContent
import com.vietreader.tts.ui.components.SettingsTopBar
import com.vietreader.tts.ui.components.SleepTimerSheetContent
import com.vietreader.tts.ui.theme.Amber200
import com.vietreader.tts.ui.theme.Background
import com.vietreader.tts.ui.theme.HeadlineSerifFamily
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnPrimaryContainer
import com.vietreader.tts.ui.theme.OnPrimaryFixed
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.PrimaryFixed
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLow
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary
import com.vietreader.tts.ui.theme.VietReaderTheme
import kotlin.math.roundToInt

/**
 * Stateful Route for Screen 6 (`stitch/06-cai-dat.html`).
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToVoices: () -> Unit = {},
    onNavigateToTab: (AppBottomTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val cloudSyncMessage by viewModel.cloudSyncMessage.collectAsState()
    val context = LocalContext.current

    var googleAccount by remember { mutableStateOf(viewModel.getGoogleSignedInAccount()) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val signInResult = viewModel.handleGoogleSignInResult(result.data)
        signInResult.onSuccess { account ->
            googleAccount = account
            Toast.makeText(context, "Đã kết nối: ${account.email}", Toast.LENGTH_SHORT).show()
        }.onFailure { err ->
            Toast.makeText(context, "Đăng nhập Google thất bại: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                Toast.makeText(context, "Xuất tệp sao lưu thành công!", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Toast.makeText(context, "Lỗi sao lưu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }.orEmpty()
                val result = viewModel.restoreBackupFromJson(json)
                result.onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Lỗi khôi phục: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }.onFailure { e ->
                Toast.makeText(context, "Không thể đọc tệp: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val backupStats = remember(uiState.settings, uiState.pronunciationRules) {
        viewModel.getBackupStats()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onStopPreview()
        }
    }

    SettingsScreen(
        uiState = uiState,
        onOpenSheet = viewModel::onOpenSheet,
        onDismissSheet = viewModel::onDismissSheet,
        onUpdateAppearance = viewModel::onUpdateAppearance,
        onUpdateAudioPlayback = viewModel::onUpdateAudioPlayback,
        onUpdateReadingSpeed = viewModel::onUpdateReadingSpeed,
        onAddPronunciationRule = viewModel::onAddPronunciationRule,
        onDeletePronunciationRule = viewModel::onDeletePronunciationRule,
        onTestPronunciationRule = viewModel::onTestPronunciationRule,
        onUpdateReadingReminder = viewModel::onUpdateReadingReminder,
        onUpdateProfile = viewModel::onUpdateProfile,
        onUpdateDefaultSleepTimer = viewModel::onUpdateDefaultSleepTimer,
        backupStats = backupStats,
        isSignedInGoogle = googleAccount != null,
        googleAccountEmail = googleAccount?.email,
        googleAccountName = googleAccount?.displayName,
        isSyncing = isCloudSyncing,
        syncMessage = cloudSyncMessage,
        onSignInGoogleClick = {
            viewModel.getGoogleSignInIntent()?.let { intent ->
                googleSignInLauncher.launch(intent)
            }
        },
        onSignOutGoogleClick = {
            viewModel.onGoogleSignOut {
                googleAccount = null
                Toast.makeText(context, "Đã đăng xuất Google", Toast.LENGTH_SHORT).show()
            }
        },
        onTriggerCloudBackup = {
            viewModel.onSyncBackupToGoogleDrive { result ->
                result.onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        },
        onTriggerCloudRestore = {
            viewModel.onRestoreBackupFromGoogleDrive { result ->
                result.onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        },
        onTriggerLocalExport = {
            val dateStr = java.time.LocalDate.now().toString().replace("-", "")
            exportLauncher.launch("wordwave_backup_$dateStr.json")
        },
        onTriggerLocalImport = {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onOfflineStorageClick = onNavigateToVoices,
        onTogglePlayPause = viewModel::onTogglePlayPause,
        onMiniPlayerClick = onNavigateToNowPlaying,
        onTabSelected = onNavigateToTab
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenSheet: (SettingsSheetType) -> Unit,
    onDismissSheet: () -> Unit,
    onUpdateAppearance: (ReadingThemeMode, Int) -> Unit,
    onUpdateAudioPlayback: (Boolean, Boolean) -> Unit,
    onUpdateReadingSpeed: (Float) -> Unit,
    onOfflineStorageClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateDefaultSleepTimer: (Int) -> Unit = {},
    onUpdateReadingReminder: (Boolean, Int, Int, Set<Int>) -> Unit = { _, _, _, _ -> },
    onUpdateProfile: (String, String) -> Unit = { _, _ -> },
    backupStats: Triple<Int, Int, Int> = Triple(0, 0, 0),
    isSignedInGoogle: Boolean = false,
    googleAccountEmail: String? = null,
    googleAccountName: String? = null,
    isSyncing: Boolean = false,
    syncMessage: String? = null,
    onSignInGoogleClick: () -> Unit = {},
    onSignOutGoogleClick: () -> Unit = {},
    onTriggerCloudBackup: () -> Unit = {},
    onTriggerCloudRestore: () -> Unit = {},
    onTriggerLocalExport: () -> Unit = {},
    onTriggerLocalImport: () -> Unit = {},
    onAddPronunciationRule: (String, String) -> Unit = { _, _ -> },
    onDeletePronunciationRule: (String) -> Unit = {},
    onTestPronunciationRule: (PronunciationRule) -> Unit = {}
) {
    val settings = uiState.settings

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            SettingsTopBar()
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
                    selectedTab = AppBottomTab.SETTINGS,
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
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. User Profile Card
            item {
                ReaderProfileCard(
                    settings = settings,
                    onClick = { onOpenSheet(SettingsSheetType.EDIT_PROFILE) }
                )
            }

            // 2. Reading & Listening Statistics Card (`Thống kê nghe đọc`)
            item {
                ReadingStatsCard(
                    settings = settings,
                    onClick = { onOpenSheet(SettingsSheetType.READING_STATS) }
                )
            }

            // 3. Section: TRẢI NGHIỆM ĐỌC & NGHE
            item {
                SettingsGroupSection(title = "TRẢI NGHIỆM ĐỌC & NGHE") {
                    SettingsRowItem(
                        icon = Icons.Outlined.FormatSize,
                        title = "Giao diện & Chữ",
                        valueText = settings.appearanceSummary,
                        onClick = { onOpenSheet(SettingsSheetType.APPEARANCE_AND_FONT) },
                        showBottomDivider = true
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.PlayCircleOutline,
                        title = "Phát âm thanh",
                        valueText = settings.audioPlaybackSummary,
                        onClick = { onOpenSheet(SettingsSheetType.AUDIO_PLAYBACK) },
                        showBottomDivider = false
                    )
                }
            }

            // 4. Section: ÂM THANH & BỘ NHỚ
            item {
                SettingsGroupSection(title = "ÂM THANH & BỘ NHỚ") {
                    SettingsRowItem(
                        icon = Icons.Outlined.RecordVoiceOver,
                        title = "Giọng đọc & phát âm",
                        valueText = settings.voiceAndSpeedSummary,
                        onClick = { onOpenSheet(SettingsSheetType.SPEED_AND_PRONUNCIATION) },
                        showBottomDivider = true
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.SdStorage,
                        title = "Lưu trữ offline",
                        valueText = uiState.offlineStorageSummary,
                        onClick = onOfflineStorageClick,
                        showBottomDivider = false
                    )
                }
            }

            // 5. Section: HỆ THỐNG & ĐỒNG BỘ
            item {
                SettingsGroupSection(title = "HỆ THỐNG & ĐỒNG BỘ") {
                    SettingsRowItem(
                        icon = Icons.Outlined.CloudSync,
                        title = "Sao lưu & Dữ liệu",
                        valueText = settings.cloudSyncSummary,
                        valueTextColor = if (settings.cloudSyncEnabled) Tertiary else OnSurfaceVariant,
                        valueFontWeight = if (settings.cloudSyncEnabled) FontWeight.Medium else FontWeight.Normal,
                        onClick = { onOpenSheet(SettingsSheetType.BACKUP_RESTORE) },
                        showBottomDivider = true
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Thông báo",
                        valueText = settings.reminderSummary,
                        onClick = { onOpenSheet(SettingsSheetType.REMINDER_SETTINGS) },
                        showBottomDivider = true
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.Bedtime,
                        title = "Hẹn giờ ngủ mặc định",
                        valueText = settings.sleepTimerSummary,
                        onClick = { onOpenSheet(SettingsSheetType.DEFAULT_SLEEP_TIMER) },
                        showBottomDivider = false
                    )
                }
            }

            // 6. App Footer (`Wordwave • Phiên bản 1.2.0`)
            item {
                SettingsAppFooter(version = settings.appVersion)
            }
        }
    }

    // Interactive Bottom Sheets for Settings Options
    if (uiState.activeSheet != SettingsSheetType.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState = sheetState,
            containerColor = SurfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            when (uiState.activeSheet) {
                SettingsSheetType.APPEARANCE_AND_FONT -> {
                    AppearanceAndFontSheetContent(
                        currentTheme = settings.themeMode,
                        currentFontSizePx = settings.fontSizePx,
                        onApply = onUpdateAppearance,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.SPEED_AND_PRONUNCIATION -> {
                    SpeedAndPronunciationSheetContent(
                        currentSpeed = settings.defaultSpeed,
                        pronunciationRules = uiState.pronunciationRules,
                        onApplySpeed = onUpdateReadingSpeed,
                        onAddRule = onAddPronunciationRule,
                        onDeleteRule = onDeletePronunciationRule,
                        onTestRule = onTestPronunciationRule,
                        onManageVoicesClick = {
                            onDismissSheet()
                            onOfflineStorageClick()
                        },
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.AUDIO_PLAYBACK -> {
                    AudioPlaybackSheetContent(
                        initialAutoScroll = settings.autoScrollEnabled,
                        initialStopAtChapterEnd = settings.stopAtEndOfChapter,
                        onApply = onUpdateAudioPlayback,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.READING_STATS -> {
                    ReadingStatsSheetContent(
                        settings = settings,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.DEFAULT_SLEEP_TIMER -> {
                    SleepTimerSheetContent(
                        title = "Hẹn giờ ngủ mặc định",
                        subtitle = "Tự động tắt audio sau khoảng thời gian cài đặt",
                        currentMinutes = settings.defaultSleepTimerMinutes,
                        showEndOfChapterOption = false,
                        onSelectMinutes = onUpdateDefaultSleepTimer,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.REMINDER_SETTINGS -> {
                    ReadingReminderSheetContent(
                        initialEnabled = settings.reminderEnabled,
                        initialHour = settings.reminderHour,
                        initialMinute = settings.reminderMinute,
                        initialDays = settings.reminderDays,
                        onApply = onUpdateReadingReminder,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.EDIT_PROFILE -> {
                    EditProfileSheetContent(
                        currentName = settings.userName,
                        currentRoleBadge = settings.userRoleBadge,
                        onApply = onUpdateProfile,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.BACKUP_RESTORE -> {
                    BackupRestoreSheetContent(
                        isSignedInGoogle = isSignedInGoogle,
                        googleAccountEmail = googleAccountEmail,
                        googleAccountName = googleAccountName,
                        isSyncing = isSyncing,
                        syncMessage = syncMessage,
                        lastBackupTimestamp = settings.lastBackupTimestamp,
                        booksCount = backupStats.first,
                        bookmarksCount = backupStats.second,
                        rulesCount = backupStats.third,
                        onSignInGoogleClick = onSignInGoogleClick,
                        onSignOutGoogleClick = onSignOutGoogleClick,
                        onTriggerCloudBackup = onTriggerCloudBackup,
                        onTriggerCloudRestore = onTriggerCloudRestore,
                        onTriggerLocalExport = onTriggerLocalExport,
                        onTriggerLocalImport = onTriggerLocalImport,
                        onClose = onDismissSheet
                    )
                }

                SettingsSheetType.NONE -> {}
            }
        }
    }
}

@Composable
private fun ReaderProfileCard(
    settings: AppSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLow,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Warm Reader Avatar (`w-14 h-14 rounded-full`)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7A3700),
                                Color(0xFF451E03)
                            )
                        )
                    )
                    .border(1.dp, Amber200.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = settings.userName,
                    tint = Amber200,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = settings.userName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = HeadlineSerifFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryFixed)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = settings.userRoleBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = OnPrimaryFixed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${settings.streakDays} ngày liên tục",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Chỉnh sửa hồ sơ",
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ReadingStatsCard(
    settings: AppSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryFixed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thống kê nghe đọc",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${settings.totalListeningTimeLabel} • ${settings.totalBooksRead} tác phẩm • Thói quen đọc",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsGroupSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = HeadlineSerifFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            ),
            color = OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    valueText: String,
    onClick: () -> Unit,
    showBottomDivider: Boolean,
    modifier: Modifier = Modifier,
    valueTextColor: Color = OnSurfaceVariant,
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = OnSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = valueFontWeight
                    ),
                    color = valueTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showBottomDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = SurfaceContainerHigh.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SettingsAppFooter(
    version: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFlorist,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Wordwave • Phiên bản $version",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = HeadlineSerifFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = OnSurface
        )
    }
}

@Composable
private fun AppearanceAndFontSheetContent(
    currentTheme: ReadingThemeMode,
    currentFontSizePx: Int,
    onApply: (ReadingThemeMode, Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedTheme by remember(currentTheme) { mutableStateOf(currentTheme) }
    var selectedFontSize by remember(currentFontSizePx) { mutableIntStateOf(currentFontSizePx) }
    val fontSizes = listOf(15, 17, 19, 21)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetHeader(
            title = "Giao diện & Cỡ chữ",
            subtitle = "Tùy chỉnh màu nền trang sách và kích thước chữ đọc",
            onClose = onClose
        )

        // Theme Mode Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Màu nền giao diện đọc",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = OnSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReadingThemeMode.entries.forEach { mode ->
                    val isSelected = mode == selectedTheme
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTheme = mode },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(mode.previewBgHex),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Primary else OutlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Aa",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = HeadlineSerifFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(mode.previewTextHex)
                            )
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = Color(mode.previewTextHex),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Font Size Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cỡ chữ đọc",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = "${selectedFontSize}px",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fontSizes.forEach { sizePx ->
                    val isSelected = sizePx == selectedFontSize
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFontSize = sizePx },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryContainer else SurfaceContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${sizePx}px",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) OnPrimaryContainer else OnSurface
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .clickable { onApply(selectedTheme, selectedFontSize) },
            shape = CircleShape,
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Áp dụng thay đổi",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun SpeedAndPronunciationSheetContent(
    currentSpeed: Float,
    pronunciationRules: List<PronunciationRule>,
    onApplySpeed: (Float) -> Unit,
    onAddRule: (String, String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onTestRule: (PronunciationRule) -> Unit,
    onManageVoicesClick: () -> Unit,
    onClose: () -> Unit
) {
    var speed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
    var newWordInput by remember { mutableStateOf("") }
    var newPronunciationInput by remember { mutableStateOf("") }
    val presets = listOf(0.75f, 1.0f, 1.2f, 1.5f, 1.75f, 2.0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SheetHeader(
            title = "Giọng đọc & Từ điển phát âm",
            subtitle = "Tùy chỉnh tốc độ đọc mặc định và cách phát âm từ viết tắt/tên riêng",
            onClose = onClose
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tốc độ đọc mặc định",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = "${"%.2f".format(speed)}x",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }

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
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = kotlin.math.abs(speed - preset) < 0.03f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { speed = preset },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryContainer else SurfaceContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${preset}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) OnPrimaryContainer else OnSurface
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SurfaceContainer)

        // Custom Pronunciation Dictionary Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Từ điển phát âm tùy chỉnh (${pronunciationRules.size} từ)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = OnSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newWordInput,
                    onValueChange = { newWordInput = it },
                    placeholder = {
                        Text(
                            text = "Từ gốc (VD: AI)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(0.9f)
                )

                OutlinedTextField(
                    value = newPronunciationInput,
                    onValueChange = { newPronunciationInput = it },
                    placeholder = {
                        Text(
                            text = "Đọc là (VD: ây ai)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1.1f)
                )

                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (newWordInput.isNotBlank() && newPronunciationInput.isNotBlank()) {
                                onAddRule(newWordInput, newPronunciationInput)
                                newWordInput = ""
                                newPronunciationInput = ""
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryContainer
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Thêm",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnPrimaryContainer
                        )
                    }
                }
            }

            if (pronunciationRules.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pronunciationRules.forEach { rule ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceContainerLow,
                            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = rule.originalWord,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "→",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = OnSurfaceVariant
                                    )
                                    Text(
                                        text = rule.spokenReplacement,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = Primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { onTestRule(rule) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Nghe thử từ",
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { onDeleteRule(rule.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Xóa từ",
                                            tint = OnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Manager Shortcut
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onManageVoicesClick),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quản lý Giọng đọc hệ thống",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Mở danh sách các giọng đọc hệ thống có sẵn trên thiết bị",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = OnSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .clickable { onApplySpeed(speed) },
            shape = CircleShape,
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Lưu tốc độ đọc",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun AudioPlaybackSheetContent(
    initialAutoScroll: Boolean,
    initialStopAtChapterEnd: Boolean,
    onApply: (Boolean, Boolean) -> Unit,
    onClose: () -> Unit
) {
    var autoScroll by remember(initialAutoScroll) { mutableStateOf(initialAutoScroll) }
    var stopAtChapterEnd by remember(initialStopAtChapterEnd) { mutableStateOf(initialStopAtChapterEnd) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SheetHeader(
            title = "Cài đặt phát âm thanh",
            subtitle = "Tùy chỉnh hành vi tự động cuộn và chuyển chương",
            onClose = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tự động cuộn theo giọng đọc",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
                Text(
                    text = "Giữ câu đang phát luôn nằm ở trung tâm màn hình",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
            }
            Switch(
                checked = autoScroll,
                onCheckedChange = { autoScroll = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = Primary
                )
            )
        }

        HorizontalDivider(color = SurfaceContainer)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tự dừng khi hết chương",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
                Text(
                    text = "Tạm dừng phát âm thanh sau khi hoàn tất mỗi chương sách",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
            }
            Switch(
                checked = stopAtChapterEnd,
                onCheckedChange = { stopAtChapterEnd = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = Primary
                )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .clickable { onApply(autoScroll, stopAtChapterEnd) },
            shape = CircleShape,
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Áp dụng",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun ReadingStatsSheetContent(
    settings: AppSettings,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SheetHeader(
            title = "Thống kê nghe đọc",
            subtitle = "Hồ sơ độc giả ${settings.userName}",
            onClose = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                label = "Tổng thời gian",
                value = settings.totalListeningTimeLabel,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                label = "Tác phẩm",
                value = "${settings.totalBooksRead} cuốn",
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                label = "Chuỗi ngày",
                value = "${settings.streakDays} ngày",
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            shape = CircleShape,
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Đóng",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = HeadlineSerifFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = HeadlineSerifFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = subtitle,
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
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    VietReaderTheme {
        SettingsScreen(
            uiState = SettingsUiState(),
            onOpenSheet = {},
            onDismissSheet = {},
            onUpdateAppearance = { _, _ -> },
            onUpdateAudioPlayback = { _, _ -> },
            onUpdateReadingSpeed = {},
            onOfflineStorageClick = {},
            onTogglePlayPause = {},
            onMiniPlayerClick = {},
            onTabSelected = {}
        )
    }
}
