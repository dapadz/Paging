package ru.dapadz.paging.ui.scroll_listener

/**
 * Callback used by [PagingScrollCallback] to decide when the next page should be requested.
 */
interface PagingScrollListener {

    /**
     * Called when the next page should be requested.
     */
    fun loadMore()

    /**
     * Returns `true` when the list is currently showing an error state.
     */
    fun isError(): Boolean

    /**
     * Returns `true` when a page request is already in progress.
     */
    fun isLoading(): Boolean

}
