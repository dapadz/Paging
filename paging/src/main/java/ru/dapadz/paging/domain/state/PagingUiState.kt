package ru.dapadz.paging.domain.state

/**
 * Represents the UI state of a paginated list.
 *
 * @param T the item type exposed to the UI
 */
sealed class PagingUiState<out T> {

    /**
     * Indicates that a page request is currently in progress.
     */
    data object Loading : PagingUiState<Nothing>()

    /**
     * Indicates that the last page request failed.
     *
     * @param error the failure that occurred
     */
    data class Error(val error: Throwable) : PagingUiState<Nothing>()

    /**
     * Indicates that page data was loaded successfully.
     *
     * @param items accumulated items that should be rendered by the UI
     */
    data class Data<T>(val items: List<T>) : PagingUiState<T>() {
        val isEmpty get() = items.isEmpty()
    }
}
