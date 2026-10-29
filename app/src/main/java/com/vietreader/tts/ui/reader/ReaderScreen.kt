package com.vietreader.tts.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.TranscriptSentence
import com.vietreader.tts.ui.components.ExpandedAudioControllerBar
import com.vietreader.tts.ui.components.ReaderTopBar
import com.vietreader.tts.ui.components.SleepTimerSheetContent
import com.vietreader.tts.ui.theme.HeadlineSerifFamily
import com.vietreader.tts.ui.theme.HighlightBg
import com.vietreader.tts.ui.theme.HighlightBorder
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnPrimaryContainer
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.PrimaryFixed
import com.vietreader.tts.ui.theme.ReadingInk
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLow
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.VietReaderTheme

/**
 * Stateful Route for the Synchronized Reader & Audio Player Screen (`02-dang-nghe-doc-dong-bo.html`).
 */
@Composable
fun ReaderRoute(
    bookId: String? = null,
    initialMode: ReaderMode? = null,
    viewModel: ReaderViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onOpenBookDetailToc: (String) -> Unit = {},
    onNavigateToVoiceManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    LaunchedEffect(bookId, initialMode) {
        if (!bookId.isNullOrBlank()) {
            viewModel.onSelectBook(bookId)
        }
        if (initialMode != null) {
            viewModel.onModeChange(initialMode)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    ReaderScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onModeChange = viewModel::onModeChange,
        onSentenceClick = viewModel::onSentenceClick,
        onTogglePlayPause = viewModel::onTogglePlayPause,
        onPrevSentenceClick = viewModel::onPrevSentenceClick,
        onNextSentenceClick = viewModel::onNextSentenceClick,
        onRewind15Click = viewModel::onRewind15Click,
        onForward15Click = viewModel::onForward15Click,
        onSeekToSeconds = viewModel::onSeekToSeconds,
        onCycleSpeedClick = viewModel::onCycleSpeedClick,
        onSelectVoice = viewModel::onSelectVoice,
        onTocClick = viewModel::onOpenTocSheet,
        onOpenAppearanceSheet = viewModel::onOpenAppearanceSheet,
        onDismissTocSheet = viewModel::onDismissTocSheet,
        onDismissAppearanceSheet = viewModel::onDismissAppearanceSheet,
        onSelectChapter = viewModel::onSelectChapter,
        onUpdateTheme = { mode -> viewModel.onUpdateAppearance(mode, uiState.fontSizePx) },
        onUpdateFontSize = { sizePx -> viewModel.onUpdateAppearance(uiState.readingThemeMode, sizePx) },
        onBookmarkClick = viewModel::onToggleBookmark,
        onToggleControlsCollapsed = viewModel::onToggleControlsCollapsed,
        onToggleOverflowMenu = viewModel::onToggleOverflowMenu,
        onDismissOverflowMenu = viewModel::onDismissOverflowMenu,
        onCycleSleepTimer = viewModel::onCycleSleepTimer,
        onOpenSleepTimerSheet = viewModel::onOpenSleepTimerSheet,
        onDismissSleepTimerSheet = viewModel::onDismissSleepTimerSheet,
        onSelectSleepTimerMinutes = viewModel::onSelectSleepTimerMinutes,
        onToggleStopAtEndOfChapter = viewModel::onToggleStopAtEndOfChapter,
        onToggleAutoScroll = viewModel::onToggleAutoScroll,
        onHighlightLevelChange = viewModel::onHighlightLevelChange,
        onNavigateToVoiceManagement = onNavigateToVoiceManagement,
        onNavigateToSettings = onNavigateToSettings,
        onPrevChapterClick = viewModel::onPreviousChapterClick,
        onNextChapterClick = viewModel::onNextChapterClick
    )
}

/**
 * Stateless Composable for Screen 2: "Đang nghe & Đọc đồng bộ - Văn Âm Mobile" (`02-dang-nghe-doc-dong-bo.html`).
 *
 * Uses GPU-accelerated `graphicsLayer` transitions for collapsing/expanding the bottom audio dock
 * so there is zero layout re-measurement or composition stutter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onBackClick: () -> Unit,
    onModeChange: (ReaderMode) -> Unit,
    onSentenceClick: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevSentenceClick: () -> Unit,
    onNextSentenceClick: () -> Unit,
    onRewind15Click: () -> Unit,
    onForward15Click: () -> Unit,
    onSeekToSeconds: (Int) -> Unit,
    onCycleSpeedClick: () -> Unit,
    onSelectVoice: (String) -> Unit,
    onTocClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onToggleControlsCollapsed: () -> Unit,
    onToggleOverflowMenu: () -> Unit,
    onDismissOverflowMenu: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onHighlightLevelChange: (HighlightLevel) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSleepTimerSheet: () -> Unit = {},
    onDismissSleepTimerSheet: () -> Unit = {},
    onSelectSleepTimerMinutes: (Int) -> Unit = {},
    onToggleStopAtEndOfChapter: (Boolean) -> Unit = {},
    onOpenAppearanceSheet: () -> Unit = {},
    onDismissTocSheet: () -> Unit = {},
    onDismissAppearanceSheet: () -> Unit = {},
    onSelectChapter: (Int) -> Unit = {},
    onUpdateTheme: (ReadingThemeMode) -> Unit = {},
    onUpdateFontSize: (Int) -> Unit = {},
    onNavigateToVoiceManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onPrevChapterClick: () -> Unit = {},
    onNextChapterClick: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Scroll to top when chapter changes, or restore position if opening existing progress
    LaunchedEffect(uiState.currentChapterNumber, uiState.playbackState.bookId) {
        val targetSentence = uiState.playbackState.activeSentenceIndex
        if (targetSentence > 0) {
            listState.scrollToItem((targetSentence + 1).coerceAtLeast(0))
        } else {
            listState.scrollToItem(0)
        }
    }

    // Re-sync scroll position immediately when returning to the app from background / Lock Screen
    DisposableEffect(
        lifecycleOwner,
        uiState.isAutoScrollEnabled,
        uiState.readerMode
    ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                uiState.isAutoScrollEnabled &&
                uiState.readerMode == ReaderMode.READ_ALONG
            ) {
                val targetIdx = (uiState.playbackState.activeSentenceIndex + 1).coerceAtLeast(0)
                coroutineScope.launch {
                    runCatching { listState.scrollToItem(targetIdx) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val context = LocalContext.current
    // In Driving Mode (ReaderMode.LISTEN), keep the screen awake so user can safely glance while driving
    DisposableEffect(uiState.readerMode) {
        val window = context.findActivity()?.window
        if (uiState.readerMode == ReaderMode.LISTEN) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 0f = Dock fully expanded, 1f = Dock fully collapsed into floating FAB
    val collapseProgress by animateFloatAsState(
        targetValue = if (uiState.isControlsCollapsed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 240,
            easing = FastOutSlowInEasing
        ),
        label = "controls_collapse_progress"
    )

    // Auto-scroll to active sentence when auto-scroll is enabled
    LaunchedEffect(
        uiState.playbackState.activeSentenceIndex,
        uiState.isAutoScrollEnabled,
        uiState.readerMode
    ) {
        if (uiState.isAutoScrollEnabled && uiState.readerMode == ReaderMode.READ_ALONG) {
            val targetItemIndex = (uiState.playbackState.activeSentenceIndex + 1)
                .coerceAtLeast(0)
            listState.animateScrollToItem(targetItemIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ReaderTopBar(
                bookTitle = uiState.playbackState.title,
                readerMode = uiState.readerMode,
                isOverflowMenuExpanded = uiState.isOverflowMenuExpanded,
                sleepTimerMinutes = uiState.sleepTimerMinutes,
                isAutoScrollEnabled = uiState.isAutoScrollEnabled,
                highlightLevel = uiState.highlightLevel,
                onBackClick = onBackClick,
                onModeChange = onModeChange,
                onToggleOverflowMenu = onToggleOverflowMenu,
                onDismissOverflowMenu = onDismissOverflowMenu,
                onCycleSleepTimer = onCycleSleepTimer,
                onOpenSleepTimerSheet = onOpenSleepTimerSheet,
                onToggleAutoScroll = onToggleAutoScroll,
                onHighlightLevelChange = onHighlightLevelChange,
                onOpenTocClick = onTocClick,
                onOpenAppearanceClick = onOpenAppearanceSheet,
                onNavigateToVoiceManagement = onNavigateToVoiceManagement,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (uiState.readerMode) {
                ReaderMode.READ_ALONG -> {
                    ReadAlongCanvas(
                        chapterLabel = uiState.chapterHeaderLabel,
                        chapterTitle = uiState.chapterSubtitle,
                        sentences = uiState.sentences,
                        activeSentenceIndex = uiState.playbackState.activeSentenceIndex,
                        highlightLevel = uiState.highlightLevel,
                        activeWordStart = uiState.playbackState.activeWordStart,
                        activeWordEnd = uiState.playbackState.activeWordEnd,
                        readingThemeMode = uiState.readingThemeMode,
                        fontSizePx = uiState.fontSizePx,
                        currentChapterNumber = uiState.currentChapterNumber,
                        totalChaptersCount = uiState.totalChaptersCount,
                        onSentenceClick = onSentenceClick,
                        onPrevChapterClick = onPrevChapterClick,
                        onNextChapterClick = onNextChapterClick,
                        onTocClick = onTocClick,
                        listState = listState
                    )

                    // Bottom Audio Dock: slides down only the lower controls so Timeline remains visible when collapsed
                    ExpandedAudioControllerBar(
                        playbackState = uiState.playbackState,
                        availableVoices = uiState.availableVoices,
                        collapseProgress = collapseProgress,
                        isCollapsed = uiState.isControlsCollapsed,
                        onToggleCollapseControls = onToggleControlsCollapsed,
                        onSeekToSeconds = onSeekToSeconds,
                        onPrevSentenceClick = onPrevSentenceClick,
                        onRewind15Click = onRewind15Click,
                        onTogglePlayPause = onTogglePlayPause,
                        onForward15Click = onForward15Click,
                        onNextSentenceClick = onNextSentenceClick,
                        onCycleSpeedClick = onCycleSpeedClick,
                        onSelectVoice = onSelectVoice,
                        onTocClick = onTocClick,
                        onBookmarkClick = onBookmarkClick,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Floating Pure Reading Mode Un-hide Button (#btn-show-controls)
                    val fabProgress = ((collapseProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 20.dp, bottom = 96.dp)
                            .size(44.dp)
                            .graphicsLayer {
                                scaleX = fabProgress
                                scaleY = fabProgress
                                translationY = (1f - fabProgress) * 20.dp.toPx()
                                shape = CircleShape
                                clip = true
                                shadowElevation = 6.dp.toPx() * fabProgress
                            }
                            .background(Primary)
                            .clickable(
                                enabled = uiState.isControlsCollapsed,
                                onClick = onToggleControlsCollapsed
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Mở lại bảng điều khiển",
                            tint = OnPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                ReaderMode.LISTEN -> {
                    DrivingModeCanvas(
                        playbackState = uiState.playbackState,
                        sentences = uiState.sentences,
                        sleepTimerMinutes = uiState.sleepTimerMinutes,
                        onTogglePlayPause = onTogglePlayPause,
                        onPrevSentenceClick = onPrevSentenceClick,
                        onNextSentenceClick = onNextSentenceClick,
                        onRewind15Click = onRewind15Click,
                        onForward15Click = onForward15Click,
                        onSeekToSeconds = onSeekToSeconds,
                        onCycleSpeedClick = onCycleSpeedClick,
                        onOpenSleepTimerSheet = onOpenSleepTimerSheet,
                        onExitDrivingMode = { onModeChange(ReaderMode.READ_ALONG) }
                    )
                }
            }
        }

        if (uiState.isAppearanceSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissAppearanceSheet,
                sheetState = sheetState,
                containerColor = SurfaceContainerLowest,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                ReaderAppearanceSheetContent(
                    currentTheme = uiState.readingThemeMode,
                    currentFontSizePx = uiState.fontSizePx,
                    onUpdateTheme = onUpdateTheme,
                    onUpdateFontSize = onUpdateFontSize,
                    onClose = onDismissAppearanceSheet
                )
            }
        }

        if (uiState.isTocSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissTocSheet,
                sheetState = sheetState,
                containerColor = SurfaceContainerLowest,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                ReaderTocSheetContent(
                    chapters = uiState.chapters,
                    currentChapterNumber = uiState.currentChapterNumber,
                    onSelectChapter = onSelectChapter,
                    onClose = onDismissTocSheet
                )
            }
        }

        if (uiState.isSleepTimerSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissSleepTimerSheet,
                sheetState = sheetState,
                containerColor = SurfaceContainerLowest,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                SleepTimerSheetContent(
                    title = "Hẹn giờ tắt audio",
                    subtitle = "Tự động dừng phát theo thời gian hoặc khi xong chương",
                    currentMinutes = uiState.sleepTimerMinutes,
                    remainingSeconds = uiState.sleepTimerRemainingSeconds,
                    showEndOfChapterOption = true,
                    stopAtEndOfChapter = uiState.stopAtEndOfChapter,
                    onToggleStopAtEndOfChapter = onToggleStopAtEndOfChapter,
                    onSelectMinutes = onSelectSleepTimerMinutes,
                    onClose = onDismissSleepTimerSheet
                )
            }
        }
    }
}

@Composable
private fun ReadAlongCanvas(
    chapterLabel: String,
    chapterTitle: String,
    sentences: List<TranscriptSentence>,
    activeSentenceIndex: Int,
    highlightLevel: HighlightLevel,
    activeWordStart: Int,
    activeWordEnd: Int,
    readingThemeMode: ReadingThemeMode,
    fontSizePx: Int,
    currentChapterNumber: Int,
    totalChaptersCount: Int,
    onSentenceClick: (Int) -> Unit,
    onPrevChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    onTocClick: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val canvasBgColor = Color(readingThemeMode.previewBgHex)
    val inkColor = Color(readingThemeMode.previewTextHex)
    val isDarkTheme = readingThemeMode == ReadingThemeMode.NIGHT

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(canvasBgColor),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 248.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chapter Metadata Card (#readalong-view header)
        item(key = "chapter_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTocClick)
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = "$chapterLabel / $totalChaptersCount CHƯƠNG",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = if (isDarkTheme) Color(0xFFFFB68B) else Primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = inkColor.copy(alpha = 0.15f)
                )
            }
        }

        // Sync Reading Paragraphs (.sentence-item)
        itemsIndexed(
            items = sentences,
            key = { _, sentence -> sentence.id }
        ) { index, sentence ->
            val isActive = index == activeSentenceIndex
            val itemClick = remember(index, onSentenceClick) { { onSentenceClick(index) } }
            SyncSentenceParagraphItem(
                sentence = sentence,
                isActive = isActive,
                highlightLevel = highlightLevel,
                activeWordStart = if (isActive) activeWordStart else -1,
                activeWordEnd = if (isActive) activeWordEnd else -1,
                fontSizePx = fontSizePx,
                inkColor = inkColor,
                isDarkTheme = isDarkTheme,
                onClick = itemClick
            )
        }

        // Chapter Navigation Footer ("← Chương trước" | "Mục lục" | "Chương tiếp →")
        item(key = "chapter_footer") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasPrev = currentChapterNumber > 1
                val hasNext = currentChapterNumber < totalChaptersCount

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = hasPrev, onClick = onPrevChapterClick),
                    shape = RoundedCornerShape(12.dp),
                    color = inkColor.copy(alpha = if (hasPrev) 0.08f else 0.03f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "← Chương trước",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = inkColor.copy(alpha = if (hasPrev) 0.85f else 0.35f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = hasNext, onClick = onNextChapterClick),
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasNext) Primary else inkColor.copy(alpha = 0.03f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Chương tiếp →",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (hasNext) OnPrimary else inkColor.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual synchronized paragraph/sentence item matching `.sentence-item` in `stitch/02-dang-nghe-doc-dong-bo.html`.
 */
@Composable
private fun SyncSentenceParagraphItem(
    sentence: TranscriptSentence,
    isActive: Boolean,
    highlightLevel: HighlightLevel,
    activeWordStart: Int,
    activeWordEnd: Int,
    fontSizePx: Int,
    inkColor: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBgColor = if (isDarkTheme) {
        Color(0xFF422C1B).copy(alpha = 0.7f)
    } else {
        PrimaryFixed.copy(alpha = 0.35f)
    }
    val activeBorderColor = if (isDarkTheme) Color(0xFFFFB68B) else Primary

    // Vẫn highlight đoạn như đối với đoạn cho cả 3 chế độ (Đoạn / Câu / Từ)
    val containerBg = if (isActive) activeBgColor else Color.Transparent

    val highlightBgColor = HighlightBg
    val highlightWordBgColor = if (isDarkTheme) {
        Color(0xFF6B4518)
    } else {
        HighlightBorder.copy(alpha = 0.25f)
    }
    val primaryColor = Primary

    val sentenceSpans = remember(sentence.text, isActive) {
        if (isActive) VietnameseTextHighlighter.getSentenceSpans(sentence.text) else null
    }

    val styledText = remember(
        sentence.text,
        sentenceSpans,
        isActive,
        highlightLevel,
        activeWordStart,
        activeWordEnd,
        inkColor,
        highlightBgColor,
        highlightWordBgColor,
        primaryColor
    ) {
        VietnameseTextHighlighter.buildHighlightedParagraph(
            text = sentence.text,
            isActive = isActive,
            highlightLevel = highlightLevel,
            activeWordStart = activeWordStart,
            activeWordEnd = activeWordEnd,
            inkColor = inkColor,
            highlightBgColor = highlightBgColor,
            highlightWordBgColor = highlightWordBgColor,
            primaryColor = primaryColor,
            sentenceSpans = sentenceSpans
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .drawBehind {
                if (isActive) {
                    val borderWidthPx = 3.dp.toPx()
                    drawRoundRect(
                        color = activeBorderColor,
                        topLeft = Offset.Zero,
                        size = Size(borderWidthPx, size.height),
                        cornerRadius = CornerRadius(borderWidthPx, borderWidthPx)
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = styledText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = HeadlineSerifFamily,
                fontSize = fontSizePx.sp,
                lineHeight = (fontSizePx * 1.75f).sp
            )
        )
    }
}

/**
 * Driving Mode Canvas (`ReaderMode.LISTEN`):
 * Designed for in-car use, workouts, or chores where safety, high contrast, and oversized touch targets are critical.
 * Keeps screen awake (`FLAG_KEEP_SCREEN_ON`) and displays the active spoken sentence with large typography.
 */
@Composable
private fun DrivingModeCanvas(
    playbackState: PlaybackState,
    sentences: List<TranscriptSentence>,
    sleepTimerMinutes: Int,
    onTogglePlayPause: () -> Unit,
    onPrevSentenceClick: () -> Unit,
    onNextSentenceClick: () -> Unit,
    onRewind15Click: () -> Unit,
    onForward15Click: () -> Unit,
    onSeekToSeconds: (Int) -> Unit,
    onCycleSpeedClick: () -> Unit,
    onOpenSleepTimerSheet: () -> Unit,
    onExitDrivingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSentence = sentences.getOrNull(playbackState.activeSentenceIndex)?.text
        ?: playbackState.title

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Safety Mode Badge & Exit Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PrimaryContainer.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CHẾ ĐỘ LÁI XE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Giữ sáng",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Primary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onExitDrivingMode),
                shape = CircleShape,
                color = SurfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = OnSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Về đọc sách",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = OnSurface
                    )
                }
            }
        }

        // 2. Center Focus Card: Large Active Sentence Display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceContainerLowest,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Chapter & Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playbackState.chapterShortLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = Primary
                        )
                        Text(
                            text = playbackState.chapterTitle.substringAfter(":", playbackState.chapterTitle).trim(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (sentences.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceContainer
                        ) {
                            Text(
                                text = "${playbackState.activeSentenceIndex + 1} / ${sentences.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Giant Active Sentence Text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeSentence,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = HeadlineSerifFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            lineHeight = 32.sp
                        ),
                        color = OnSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Voice Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playbackState.voiceName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // 3. Large Timeline Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val totalSec = playbackState.totalSeconds.coerceAtLeast(1)
            val currentSec = playbackState.currentSeconds.coerceIn(0, totalSec)
            val progress = currentSec.toFloat() / totalSec.toFloat()

            Slider(
                value = progress,
                onValueChange = { frac ->
                    val targetSec = (frac * totalSec.toFloat()).toInt()
                    onSeekToSeconds(targetSec)
                },
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = SurfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playbackState.currentTime,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurfaceVariant
                )
                Text(
                    text = playbackState.remainingLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Oversized Driving Controller Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Câu trước (54dp)
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPrevSentenceClick),
                shape = CircleShape,
                color = SurfaceContainerHigh,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = "Câu trước",
                        tint = OnSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Lùi 15s (56dp)
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRewind15Click),
                shape = CircleShape,
                color = SurfaceContainerHigh,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "Lùi 15 giây",
                        tint = OnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "15s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            // Nút Play / Pause KHỔNG LỒ (84dp)
            Surface(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePlayPause),
                shape = CircleShape,
                color = Primary,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Tạm dừng" else "Phát",
                        tint = OnPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Tua 15s (56dp)
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onForward15Click),
                shape = CircleShape,
                color = SurfaceContainerHigh,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "Tua 15 giây",
                        tint = OnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "15s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            // Câu tiếp (54dp)
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNextSentenceClick),
                shape = CircleShape,
                color = SurfaceContainerHigh,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = "Câu tiếp theo",
                        tint = OnSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Quick Utility Pills (Tốc độ, Hẹn giờ)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Nút Tốc độ đọc (height 44dp)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCycleSpeedClick),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerHigh,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${"%.2f".format(playbackState.playbackSpeed).trimEnd('0').trimEnd('.')}x",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = OnSurface
                    )
                }
            }

            // Nút Hẹn giờ tắt (height 44dp)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenSleepTimerSheet),
                shape = RoundedCornerShape(12.dp),
                color = if (sleepTimerMinutes > 0) PrimaryContainer.copy(alpha = 0.2f) else SurfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (sleepTimerMinutes > 0) Primary.copy(alpha = 0.4f) else OutlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = if (sleepTimerMinutes > 0) Primary else OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}p" else "Hẹn giờ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (sleepTimerMinutes > 0) Primary else OnSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderAppearanceSheetContent(
    currentTheme: ReadingThemeMode,
    currentFontSizePx: Int,
    onUpdateTheme: (ReadingThemeMode) -> Unit,
    onUpdateFontSize: (Int) -> Unit,
    onClose: () -> Unit
) {
    val fontSizes = listOf(14, 16, 17, 19, 21, 24)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Giao diện & Cỡ chữ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
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

        // 1. Theme Swatches
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Màu nền trang sách",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReadingThemeMode.entries.forEach { mode ->
                    val isSelected = mode == currentTheme
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateTheme(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(mode.previewBgHex),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Primary else OutlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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

        // 2. Font Size Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cỡ chữ đọc",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurfaceVariant
                )
                Text(
                    text = "${currentFontSizePx}px",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fontSizes.forEach { sizePx ->
                    val isSelected = sizePx == currentFontSizePx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdateFontSize(sizePx) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryContainer else SurfaceContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${sizePx}px",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) OnPrimaryContainer else OnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderTocSheetContent(
    chapters: List<com.vietreader.tts.domain.model.BookChapter>,
    currentChapterNumber: Int,
    onSelectChapter: (Int) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isBlank()) {
            chapters
        } else {
            val q = searchQuery.trim()
            val numQuery = q.toIntOrNull()
            chapters.filter { ch ->
                (numQuery != null && ch.number == numQuery) ||
                    ch.title.contains(q, ignoreCase = true) ||
                    ch.fullDisplayTitle.contains(q, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f)
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mục lục (${chapters.size} chương)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
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

        if (chapters.size > 8) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm theo số chương hoặc tiêu đề...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = filteredChapters,
                key = { it.number }
            ) { chapter ->
                val isCurrent = chapter.number == currentChapterNumber
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectChapter(chapter.number) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCurrent) PrimaryContainer.copy(alpha = 0.45f) else SurfaceContainerLow,
                    border = if (isCurrent) BorderStroke(1.dp, Primary.copy(alpha = 0.4f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "%02d".format(chapter.number),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (isCurrent) Primary else OnSurfaceVariant
                        )
                        Text(
                            text = chapter.fullDisplayTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            ),
                            color = if (isCurrent) Primary else OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Chương đang đọc",
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderScreenPreview() {
    VietReaderTheme {
        ReaderScreen(
            uiState = ReaderUiState(),
            onBackClick = {},
            onModeChange = {},
            onSentenceClick = {},
            onTogglePlayPause = {},
            onPrevSentenceClick = {},
            onNextSentenceClick = {},
            onRewind15Click = {},
            onForward15Click = {},
            onSeekToSeconds = {},
            onCycleSpeedClick = {},
            onSelectVoice = {},
            onTocClick = {},
            onBookmarkClick = {},
            onToggleControlsCollapsed = {},
            onToggleOverflowMenu = {},
            onDismissOverflowMenu = {},
            onCycleSleepTimer = {},
            onToggleAutoScroll = {},
            onHighlightLevelChange = {}
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
