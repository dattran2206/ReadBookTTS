package com.vietreader.tts.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.LibraryFilter
import com.vietreader.tts.domain.model.LibrarySortOption
import com.vietreader.tts.domain.model.LibraryViewMode
import com.vietreader.tts.domain.repository.BookRepository
import com.vietreader.tts.domain.usecase.GetLibraryBooksUseCase
import com.vietreader.tts.ui.components.AppBottomTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: BookRepository = BookRepositoryImpl.instance,
    private val getLibraryBooksUseCase: GetLibraryBooksUseCase = GetLibraryBooksUseCase()
) : ViewModel() {

    private data class FilterControlState(
        val selectedFilter: LibraryFilter = LibraryFilter.ALL,
        val sortOption: LibrarySortOption = LibrarySortOption.RECENTLY_ADDED,
        val viewMode: LibraryViewMode = LibraryViewMode.GRID,
        val isSearchActive: Boolean = false,
        val searchQuery: String = "",
        val isFilterSheetVisible: Boolean = false,
        val selectedTab: AppBottomTab = AppBottomTab.LIBRARY
    )

    private val controlState = MutableStateFlow(FilterControlState())

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.books,
        repository.playbackState,
        controlState
    ) { books, playbackState, controls ->
        val filteredBooks = getLibraryBooksUseCase(
            books = books,
            filter = controls.selectedFilter,
            sortOption = controls.sortOption,
            searchQuery = controls.searchQuery
        )

        LibraryUiState(
            allBooksCount = books.size,
            displayedBooks = filteredBooks,
            selectedFilter = controls.selectedFilter,
            sortOption = controls.sortOption,
            viewMode = controls.viewMode,
            isSearchActive = controls.isSearchActive,
            searchQuery = controls.searchQuery,
            isFilterSheetVisible = controls.isFilterSheetVisible,
            selectedTab = controls.selectedTab,
            playbackState = playbackState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(
            allBooksCount = repository.books.value.size,
            displayedBooks = repository.books.value,
            playbackState = repository.playbackState.value
        )
    )

    fun onFilterSelected(filter: LibraryFilter) {
        controlState.update { it.copy(selectedFilter = filter) }
    }

    fun onSortOptionSelected(sortOption: LibrarySortOption) {
        controlState.update {
            it.copy(
                sortOption = sortOption,
                isFilterSheetVisible = false
            )
        }
    }

    fun onViewModeSelected(viewMode: LibraryViewMode) {
        controlState.update { it.copy(viewMode = viewMode) }
    }

    fun onToggleSearch() {
        controlState.update { current ->
            val nextActive = !current.isSearchActive
            current.copy(
                isSearchActive = nextActive,
                searchQuery = if (nextActive) current.searchQuery else ""
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        controlState.update { it.copy(searchQuery = query) }
    }

    fun onOpenFilterSheet() {
        controlState.update { it.copy(isFilterSheetVisible = true) }
    }

    fun onDismissFilterSheet() {
        controlState.update { it.copy(isFilterSheetVisible = false) }
    }

    fun onBookCardClick(bookId: String) {
        repository.selectBookForDetail(bookId)
    }

    fun onDeleteBook(bookId: String) {
        repository.deleteBook(bookId)
    }

    fun onTogglePlayPause() {
        repository.togglePlayPause()
    }

    fun onMiniPlayerClick() {
        // Handled by navigation callback to ReaderScreen
    }

    fun onTabSelected(tab: AppBottomTab) {
        if (tab == AppBottomTab.LIBRARY) {
            controlState.update { it.copy(selectedTab = tab) }
        }
    }

    fun onImportBookFromUri(
        context: Context,
        uri: Uri,
        onImported: (Book) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.importBookFromUri(context, uri)
            result.onSuccess { book ->
                onImported(book)
            }.onFailure { err ->
                onError(err.message ?: "Không thể nhập tệp sách này")
            }
        }
    }
}
