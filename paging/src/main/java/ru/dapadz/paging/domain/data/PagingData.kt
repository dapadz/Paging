package ru.dapadz.paging.domain.data

/**
 * Cached snapshot of the current paging session.
 *
 * @param lastSavedPage index of the last successfully loaded page
 * @param totalPage total number of pages reported by the data source
 * @param items accumulated items from all loaded pages
 */
data class PagingData<T>(
    val lastSavedPage: Int,
    val totalPage: Int,
    val items: List<T>
) {

    /**
     * Returns `true` when the last loaded page is also the final page.
     */
    val isLastPage get() = lastSavedPage == totalPage

    companion object {
        /**
         * Creates an empty paging snapshot with no cached items.
         */
        fun <T> empty(): PagingData<T> = PagingData(0, 1, emptyList())
    }
}
