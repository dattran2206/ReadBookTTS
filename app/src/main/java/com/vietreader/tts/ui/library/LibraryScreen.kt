package com.vietreader.tts.ui.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vietreader.tts.domain.model.LibraryFilter
import com.vietreader.tts.domain.model.LibrarySortOption
import com.vietreader.tts.domain.model.LibraryViewMode
import com.vietreader.tts.ui.components.AppBottomTab
import com.vietreader.tts.ui.components.AudioControllerBar
import com.vietreader.tts.ui.components.BottomNavBar
import com.vietreader.tts.ui.components.BookCard
import com.vietreader.tts.ui.components.BookListItemCard
import com.vietreader.tts.ui.components.FilterChipRow
import com.vietreader.tts.ui.components.LibraryFilterBottomSheet
import com.vietreader.tts.ui.components.LibraryTopBar
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.VietReaderTheme

/**
 * Stateful Library Screen entry point connected to [LibraryViewModel].
 */
@Composable
fun LibraryRoute(
    viewModel: LibraryViewModel = viewModel(),
    onNavigateToNowPlaying: (String) -> Unit = {},
    onNavigateToBookDetail: (String) -> Unit = {},
    onNavigateToAddBook: () -> Unit = {},
    onNavigateToTab: (AppBottomTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val epubPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImportBookFromUri(
                context = context,
                uri = uri,
                onImported = { book ->
                    Toast.makeText(
                        context,
                        "Đã nhập: ${book.title} (${book.totalChaptersCount} chương)",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onError = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    LibraryScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onViewModeSelected = viewModel::onViewModeSelected,
        onToggleSearch = viewModel::onToggleSearch,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenFilterSheet = viewModel::onOpenFilterSheet,
        onDismissFilterSheet = viewModel::onDismissFilterSheet,
        onImportClick = {
            onNavigateToAddBook()
            epubPickerLauncher.launch(
                arrayOf("application/epub+zip", "text/plain", "application/octet-stream")
            )
        },
        onBookClick = { bookId ->
            viewModel.onBookCardClick(bookId)
            onNavigateToBookDetail(bookId)
        },
        onDeleteBook = viewModel::onDeleteBook,
        onTogglePlayPause = viewModel::onTogglePlayPause,
        onMiniPlayerClick = {
            viewModel.onMiniPlayerClick()
            uiState.playbackState?.bookId?.let(onNavigateToNowPlaying)
        },
        onTabSelected = { tab ->
            viewModel.onTabSelected(tab)
            onNavigateToTab(tab)
        }
    )
}

/**
 * Stateless Library Screen (`Thư viện - Văn Âm Mobile`) faithfully matching `stitch/01-thu-vien.html`.
 */
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onSortOptionSelected: (LibrarySortOption) -> Unit,
    onViewModeSelected: (LibraryViewMode) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onImportClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteBook: (String) -> Unit = {}
) {
    var bookPendingDelete by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.vietreader.tts.domain.model.Book?>(null)
    }

    if (bookPendingDelete != null) {
        val targetBook = bookPendingDelete!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { bookPendingDelete = null },
            title = {
                Text(
                    text = "Xóa sách khỏi thư viện?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa cuốn \"${targetBook.title}\" và toàn bộ dữ liệu đọc khỏi thiết bị?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val idToDelete = targetBook.id
                        bookPendingDelete = null
                        onDeleteBook(idToDelete)
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { bookPendingDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LibraryTopBar(
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onOpenFilterSheet = onOpenFilterSheet,
                onImportClick = onImportClick,
                filterChipsContent = {
                    FilterChipRow(
                        chips = uiState.filterChips,
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = onFilterSelected
                    )
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.displayedBooks.isEmpty()) {
                EmptyLibraryPlaceholder(
                    isLibraryCompletelyEmpty = uiState.allBooksCount == 0,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (uiState.viewMode) {
                    LibraryViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = uiState.displayedBooks,
                                key = { it.id }
                            ) { book ->
                                val isPlayingThisBook =
                                    uiState.playbackState?.bookId == book.id &&
                                        uiState.playbackState.isPlaying

                                BookCard(
                                    book = book,
                                    isCurrentlyPlaying = isPlayingThisBook,
                                    onClick = { onBookClick(book.id) },
                                    onLongClick = { bookPendingDelete = book }
                                )
                            }
                        }
                    }

                    LibraryViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.displayedBooks,
                                key = { it.id }
                            ) { book ->
                                val isPlayingThisBook =
                                    uiState.playbackState?.bookId == book.id &&
                                        uiState.playbackState.isPlaying

                                BookListItemCard(
                                    book = book,
                                    isCurrentlyPlaying = isPlayingThisBook,
                                    onClick = { onBookClick(book.id) },
                                    onLongClick = { bookPendingDelete = book }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Mini Player fixed above the bottom navigation bar (fixed bottom-16 px-4 pb-2)
            uiState.playbackState?.let { playback ->
                AudioControllerBar(
                    playbackState = playback,
                    onTogglePlayPause = onTogglePlayPause,
                    onBarClick = onMiniPlayerClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
            }
        }

        if (uiState.isFilterSheetVisible) {
            LibraryFilterBottomSheet(
                viewMode = uiState.viewMode,
                sortOption = uiState.sortOption,
                onViewModeSelected = onViewModeSelected,
                onSortOptionSelected = onSortOptionSelected,
                onDismiss = onDismissFilterSheet
            )
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder(
    isLibraryCompletelyEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isLibraryCompletelyEmpty) "Thư viện đang trống" else "Không tìm thấy sách phù hợp",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isLibraryCompletelyEmpty) {
                "Nhấn nút \"+ Thêm\" ở góc trên để nhập sách .epub hoặc .txt vào thư viện."
            } else {
                "Thử đổi bộ lọc hoặc nhấn \"+ Thêm\" để nhập tệp EPUB mới vào thư viện."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryScreenPreview() {
    VietReaderTheme {
        LibraryScreen(
            uiState = LibraryUiState(
                allBooksCount = 0,
                displayedBooks = emptyList(),
                playbackState = null
            ),
            onFilterSelected = {},
            onSortOptionSelected = {},
            onViewModeSelected = {},
            onToggleSearch = {},
            onSearchQueryChange = {},
            onOpenFilterSheet = {},
            onDismissFilterSheet = {},
            onImportClick = {},
            onBookClick = {},
            onTogglePlayPause = {},
            onMiniPlayerClick = {},
            onTabSelected = {}
        )
    }
}
