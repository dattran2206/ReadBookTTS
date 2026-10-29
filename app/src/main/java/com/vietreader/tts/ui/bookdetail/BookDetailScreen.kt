package com.vietreader.tts.ui.bookdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookBookmark
import com.vietreader.tts.domain.model.BookChapter
import com.vietreader.tts.domain.model.BookDetailTab
import com.vietreader.tts.domain.model.ChapterStatus
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.domain.model.VoiceOption
import com.vietreader.tts.ui.components.BookCoverBackdrop
import com.vietreader.tts.ui.components.BookDetailTopBar
import com.vietreader.tts.ui.theme.Background
import com.vietreader.tts.ui.theme.HeadlineSerifFamily
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnPrimaryFixed
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.Outline
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryFixed
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerHighest
import com.vietreader.tts.ui.theme.SurfaceContainerLow
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary
import com.vietreader.tts.ui.theme.TertiaryContainer
import com.vietreader.tts.ui.theme.VietReaderTheme

/**
 * Stateful Route for Screen 3 (`stitch/03-chi-tiet-sach.html`).
 */
@Composable
fun BookDetailRoute(
    bookId: String? = null,
    viewModel: BookDetailViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToReader: (ReaderMode) -> Unit = {},
    onNavigateToVoiceManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    LaunchedEffect(bookId) {
        if (!bookId.isNullOrBlank()) {
            viewModel.onSelectBook(bookId)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    BookDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onDeleteBook = {
            viewModel.onDeleteBook()
            onBackClick()
        },
        onDeleteBookmark = viewModel::onDeleteBookmark,
        onPlayContinueClick = {
            viewModel.onPlayContinueClick()
            onNavigateToReader(ReaderMode.LISTEN)
        },
        onReadBookClick = {
            viewModel.onReadBookClick()
            onNavigateToReader(ReaderMode.READ_ALONG)
        },
        onSelectTab = viewModel::onSelectTab,
        onChapterClick = { chapterNumber ->
            viewModel.onChapterClick(chapterNumber)
            onNavigateToReader(ReaderMode.READ_ALONG)
        },
        onBookmarkItemClick = { bookmark ->
            viewModel.onBookmarkClick(bookmark)
            onNavigateToReader(ReaderMode.READ_ALONG)
        },
        onOpenVoiceSheet = viewModel::onOpenVoiceSheet,
        onDismissVoiceSheet = viewModel::onDismissVoiceSheet,
        onSelectVoiceOption = viewModel::onSelectVoiceOption,
        onDownloadAllClick = viewModel::onDownloadAllClick,
        onNavigateToVoiceManagement = onNavigateToVoiceManagement,
        onNavigateToSettings = onNavigateToSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    uiState: BookDetailUiState,
    onBackClick: () -> Unit,
    onPlayContinueClick: () -> Unit,
    onReadBookClick: () -> Unit,
    onSelectTab: (BookDetailTab) -> Unit,
    onChapterClick: (Int) -> Unit,
    onOpenVoiceSheet: () -> Unit,
    onDismissVoiceSheet: () -> Unit,
    onSelectVoiceOption: (VoiceOption) -> Unit,
    onDownloadAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteBook: () -> Unit = {},
    onDeleteBookmark: (String) -> Unit = {},
    onBookmarkItemClick: (BookBookmark) -> Unit = {},
    onNavigateToVoiceManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val book = uiState.book
    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Xóa sách khỏi thư viện?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa cuốn \"${book.title}\" và toàn bộ dữ liệu đọc khỏi thiết bị?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteBook()
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            BookDetailTopBar(
                onBackClick = onBackClick,
                onDeleteBookClick = { showDeleteDialog = true },
                onNavigateToVoiceManagement = onNavigateToVoiceManagement,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. Book Hero Section
            item {
                BookDetailHeroSection(
                    book = book,
                    onOpenVoiceSheet = onOpenVoiceSheet,
                    onPlayContinueClick = onPlayContinueClick,
                    onReadBookClick = onReadBookClick
                )
            }

            // 2. Navigation Tabs (Mục lục chương | Đánh dấu (4) | Thông tin sách)
            item {
                BookDetailTabsRow(
                    selectedTab = uiState.selectedTab,
                    bookmarkCount = book.bookmarks.size,
                    onSelectTab = onSelectTab
                )
            }

            // 3. Tab Content
            when (uiState.selectedTab) {
                BookDetailTab.CHAPTERS -> {
                    item {
                        ChapterListHeader(
                            totalChaptersCount = book.totalChaptersCount,
                            voiceName = book.voiceName,
                            isAllDownloaded = uiState.isAllChaptersDownloaded,
                            onDownloadAllClick = onDownloadAllClick
                        )
                    }

                    items(
                        items = book.chapters,
                        key = { it.number }
                    ) { chapter ->
                        ChapterItemCard(
                            chapter = chapter,
                            onClick = { onChapterClick(chapter.number) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }

                BookDetailTab.BOOKMARKS -> {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    if (book.bookmarks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có dấu trang nào được lưu.\nKhi đọc sách, nhấn biểu tượng Dấu trang để lưu lại câu trích dẫn yêu thích.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(
                            items = book.bookmarks,
                            key = { it.id }
                        ) { bookmark ->
                            BookmarkItemCard(
                                bookmark = bookmark,
                                onClick = { onBookmarkItemClick(bookmark) },
                                onDeleteClick = { onDeleteBookmark(bookmark.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                BookDetailTab.INFO -> {
                    item {
                        BookInfoSection(
                            book = book,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Voice Selection Bottom Sheet (`Chọn giọng đọc`)
    if (uiState.isVoiceSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissVoiceSheet,
            sheetState = sheetState,
            containerColor = SurfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            VoiceSelectionSheetContent(
                voiceOptions = uiState.voiceOptions,
                selectedVoiceName = book.voiceName,
                onSelectVoice = onSelectVoiceOption,
                onManageAllVoicesClick = {
                    onDismissVoiceSheet()
                    onNavigateToVoiceManagement()
                },
                onClose = onDismissVoiceSheet
            )
        }
    }
}

@Composable
private fun BookDetailHeroSection(
    book: Book,
    onOpenVoiceSheet: () -> Unit,
    onPlayContinueClick: () -> Unit,
    onReadBookClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover Container (`w-40 aspect-[2/3]` = 160dp x 240dp)
        Box(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .width(160.dp)
                .height(240.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color(0xFF451E03).copy(alpha = 0.35f),
                    spotColor = Color(0xFF451E03).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(book.coverGradient.startColorHex),
                            Color(book.coverGradient.centerColorHex),
                            Color(book.coverGradient.endColorHex)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            // Optional real cover image from extracted EPUB
            BookCoverBackdrop(
                coverImagePath = book.coverImagePath,
                modifier = Modifier.fillMaxSize()
            )

            // Left Book Spine Shadow Strip (`w-2.5 bg-gradient-to-r from-black/35 via-white/10 to-transparent`)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.38f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Badge ("TÁC PHẨM KINH ĐIỂN")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = book.categoryBadge.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.6.sp
                        ),
                        color = Color(0xFFFFDCC2).copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(Color(0xFFFFDCC2).copy(alpha = 0.3f))
                    )
                }

                // Center Title & Author
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = HeadlineSerifFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFFFFDCC2).copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Ornament (`auto_stories` + "Truyện dài")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = Color(0xFFFFDCC2).copy(alpha = 0.65f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = book.genreLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp
                        ),
                        color = Color(0xFFFFDCC2).copy(alpha = 0.65f)
                    )
                }
            }
        }

        // Book Title (`text-[22px] font-headline font-semibold`)
        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = HeadlineSerifFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 28.sp
            ),
            color = OnSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Author (`Tác giả Tô Hoài`)
        Text(
            text = "Tác giả ${book.author}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Unified Metadata Line (`3g 15p • 10 chương • 48%`)
        Surface(
            shape = CircleShape,
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = book.totalDurationFormatted,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(OutlineVariant)
                )
                Text(
                    text = "${book.totalChaptersCount} chương",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(OutlineVariant)
                )
                Text(
                    text = "${book.progressPercent}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compact Voice Selector Card
        Surface(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenVoiceSheet),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = OnPrimaryFixed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Giọng đọc",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "${book.voiceName} (${book.voiceRegion})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Đổi giọng",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Primary
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split Action Buttons (`Nghe tiếp (Chương 3)` + `Đọc`)
        Row(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primary Play Button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPlayContinueClick),
                shape = RoundedCornerShape(12.dp),
                color = Primary,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Nghe tiếp (${book.currentChapterShortLabel})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = OnPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${book.remainingTimeLabel} trong chương",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = OnPrimary.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${book.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnPrimary
                        )
                    }
                }
            }

            // Secondary Read Button (`w-14 h-14`)
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onReadBookClick),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "Đọc sách",
                        tint = OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Đọc",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BookDetailTabsRow(
    selectedTab: BookDetailTab,
    bookmarkCount: Int,
    onSelectTab: (BookDetailTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            BookDetailTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tabTitle = when (tab) {
                    BookDetailTab.CHAPTERS -> "Mục lục chương"
                    BookDetailTab.BOOKMARKS -> "Đánh dấu ($bookmarkCount)"
                    BookDetailTab.INFO -> "Thông tin sách"
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabTitle,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) Primary else OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) Primary else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = OutlineVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun ChapterListHeader(
    totalChaptersCount: Int,
    voiceName: String,
    isAllDownloaded: Boolean,
    onDownloadAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$totalChaptersCount chương • $voiceName",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = OnSurfaceVariant
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDownloadAllClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isAllDownloaded) Icons.Outlined.DownloadDone else Icons.Outlined.DownloadForOffline,
                contentDescription = null,
                tint = if (isAllDownloaded) Tertiary else Primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isAllDownloaded) "Đã tải ngoại tuyến" else "Tải toàn bộ",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isAllDownloaded) Tertiary else Primary
            )
        }
    }
}

@Composable
private fun ChapterItemCard(
    chapter: BookChapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = chapter.status == ChapterStatus.PLAYING
    val isCompleted = chapter.status == ChapterStatus.COMPLETED

    val containerColor = if (isPlaying) {
        PrimaryFixed.copy(alpha = 0.28f)
    } else {
        SurfaceContainerLow
    }

    val borderStroke = if (isPlaying) {
        BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    } else {
        BorderStroke(1.dp, Color.Transparent)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Status Indicator (`w-8 h-8` = 32.dp)
            when (chapter.status) {
                ChapterStatus.COMPLETED -> {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TertiaryContainer.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Đã nghe xong",
                            tint = Tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                ChapterStatus.PLAYING -> {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val sweepAngle = (chapter.progressPercent.coerceIn(0, 100) / 100f) * 360f
                        val trackColor = SurfaceContainerHighest
                        val progressColor = Primary
                        Canvas(modifier = Modifier.size(32.dp)) {
                            val strokeWidthPx = 2.8.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidthPx)
                            )
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = "Đang nghe",
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                ChapterStatus.UNPLAYED -> {
                    val numFontSize = when {
                        chapter.number >= 1000 -> 8.5.sp
                        chapter.number >= 100 -> 10.sp
                        else -> 12.sp
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(chapter.number),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = numFontSize,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Center Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.fullDisplayTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (isPlaying) Primary else OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                when {
                    isCompleted -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Đã nghe xong",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Tertiary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Outline
                            )
                            Text(
                                text = chapter.durationLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    isPlaying -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Đang nghe ${chapter.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Primary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = chapter.remainingLabel.ifBlank { chapter.durationLabel },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Primary.copy(alpha = 0.85f)
                            )
                        }
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Chưa nghe",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Outline
                            )
                            Text(
                                text = chapter.durationLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Right Action Icon (`w-8 h-8`)
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Phát chương đang nghe",
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Phát chương",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: BookBookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = bookmark.chapterTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryFixed.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = bookmark.timestampLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Primary
                        )
                    }

                    if (onDeleteClick != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDeleteClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Xóa dấu trang",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "“${bookmark.quoteText}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = HeadlineSerifFamily,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 21.sp
                ),
                color = OnSurface
            )
        }
    }
}

@Composable
private fun BookInfoSection(
    book: Book,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Giới thiệu tác phẩm",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = HeadlineSerifFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
                Text(
                    text = book.synopsis.ifBlank {
                        "Tác phẩm văn học kinh điển Việt Nam được tối ưu hóa cho trải nghiệm nghe TTS và đọc đồng bộ từng câu."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BookInfoRow(label = "Tác giả", value = book.author)
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))
                BookInfoRow(label = "Thể loại", value = "${book.genreLabel} • ${book.categoryBadge}")
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))
                BookInfoRow(label = "Định dạng gốc", value = book.format.label)
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))
                BookInfoRow(label = "Tổng thời lượng", value = "${book.totalDurationFormatted} (${book.totalChaptersCount} chương)")
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))
                BookInfoRow(label = "Giọng đọc hiện tại", value = "${book.voiceName} (${book.voiceRegion})")
            }
        }
    }
}

@Composable
private fun BookInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = OnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = OnSurface
        )
    }
}

@Composable
private fun VoiceSelectionSheetContent(
    voiceOptions: List<VoiceOption>,
    selectedVoiceName: String,
    onSelectVoice: (VoiceOption) -> Unit,
    onManageAllVoicesClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chọn giọng đọc",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = HeadlineSerifFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
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

        voiceOptions.forEach { option ->
            val isSelected = option.name == selectedVoiceName
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectVoice(option) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) PrimaryFixed.copy(alpha = 0.3f) else SurfaceContainerLow,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Primary.copy(alpha = 0.4f) else OutlineVariant.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (option.isAiVoice) {
                                    Icons.Outlined.AutoAwesome
                                } else {
                                    Icons.Outlined.RecordVoiceOver
                                },
                                contentDescription = null,
                                tint = if (isSelected) OnPrimary else OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isSelected) Primary else OnSurface
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Đang chọn",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onManageAllVoicesClick),
            shape = CircleShape,
            color = SurfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quản lý & tải thêm giọng đọc",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BookDetailScreenPreview() {
    VietReaderTheme {
        BookDetailScreen(
            uiState = BookDetailUiState(),
            onBackClick = {},
            onPlayContinueClick = {},
            onReadBookClick = {},
            onSelectTab = {},
            onChapterClick = {},
            onOpenVoiceSheet = {},
            onDismissVoiceSheet = {},
            onSelectVoiceOption = {},
            onDownloadAllClick = {}
        )
    }
}
