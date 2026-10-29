package com.vietreader.tts.ui.library

import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.LibraryFilter
import com.vietreader.tts.domain.model.LibrarySortOption
import com.vietreader.tts.domain.model.LibraryViewMode
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.ui.components.AppBottomTab
import com.vietreader.tts.ui.components.FilterChipUiModel

data class LibraryUiState(
    val allBooksCount: Int = 0,
    val displayedBooks: List<Book> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val sortOption: LibrarySortOption = LibrarySortOption.RECENTLY_ADDED,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isFilterSheetVisible: Boolean = false,
    val selectedTab: AppBottomTab = AppBottomTab.LIBRARY,
    val playbackState: PlaybackState? = null
) {
    val filterChips: List<FilterChipUiModel>
        get() = listOf(
            FilterChipUiModel(LibraryFilter.ALL, "Tất cả ($allBooksCount)"),
            FilterChipUiModel(LibraryFilter.PLAYING, "Đang đọc"),
            FilterChipUiModel(LibraryFilter.COMPLETED, "Đã đọc xong"),
            FilterChipUiModel(LibraryFilter.IMPORTED, "Đã nhập tệp")
        )
}
