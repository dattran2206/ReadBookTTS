package com.vietreader.tts.domain.usecase

import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.LibraryFilter
import com.vietreader.tts.domain.model.LibrarySortOption

class GetLibraryBooksUseCase {

    operator fun invoke(
        books: List<Book>,
        filter: LibraryFilter,
        sortOption: LibrarySortOption,
        searchQuery: String
    ): List<Book> {
        val normalizedQuery = searchQuery.trim().lowercase()

        val filtered = books.filter { book ->
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.PLAYING -> book.progressPercent in 1..99 && !book.isCompleted
                LibraryFilter.COMPLETED -> book.isCompleted || book.progressPercent >= 100
                LibraryFilter.IMPORTED -> book.isImported
            }
            val matchesSearch = if (normalizedQuery.isEmpty()) {
                true
            } else {
                book.title.lowercase().contains(normalizedQuery) ||
                    book.author.lowercase().contains(normalizedQuery) ||
                    book.voiceName.lowercase().contains(normalizedQuery)
            }
            matchesFilter && matchesSearch
        }

        return when (sortOption) {
            LibrarySortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAtEpochMillis }
            LibrarySortOption.RECENTLY_READ -> filtered.sortedByDescending { it.lastReadEpochMillis }
            LibrarySortOption.TITLE -> filtered.sortedBy { it.title }
            LibrarySortOption.AUTHOR -> filtered.sortedBy { it.author }
            LibrarySortOption.PROGRESS -> filtered.sortedByDescending { it.progressPercent }
        }
    }
}
