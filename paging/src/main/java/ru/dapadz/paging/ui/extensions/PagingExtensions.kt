package ru.dapadz.paging.ui.extensions

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.dapadz.paging.domain.state.PagingUiState
import ru.dapadz.paging.ui.adapter.concat_adapter.PagingConcatAdapter
import ru.dapadz.paging.ui.adapter.paging_adapter.PagingAdapter
import ru.dapadz.paging.ui.scroll_listener.PagingScrollCallback
import ru.dapadz.paging.ui.scroll_listener.PagingScrollListener

/**
 * Attaches the built-in paging scroll listener to the content adapter of a [PagingConcatAdapter].
 *
 * @param onLoadMore callback invoked when the next page should be loaded
 */
fun <T, VH : ViewHolder> PagingConcatAdapter<T, VH>.attachPagingScrollListener(
    onLoadMore: () -> Unit
) {
    contentAdapter.attachPagingScrollListener(onLoadMore)
}

/**
 * Attaches the built-in paging scroll listener to a [PagingAdapter].
 *
 * @param onLoadMore callback invoked when the next page should be loaded
 */
fun <T, VH : ViewHolder> PagingAdapter<T, VH>.attachPagingScrollListener(
    onLoadMore: () -> Unit
) {
    scrollListener = PagingScrollCallback(
        object : PagingScrollListener {
            override fun loadMore() = onLoadMore()
            override fun isError(): Boolean = pagingState is PagingUiState.Error
            override fun isLoading(): Boolean = pagingState is PagingUiState.Loading
        }
    )
}
