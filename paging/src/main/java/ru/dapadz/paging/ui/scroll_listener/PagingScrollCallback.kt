package ru.dapadz.paging.ui.scroll_listener

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Default [RecyclerView.OnScrollListener] used to trigger "load more" events near the end of the list.
 *
 * The callback currently supports [LinearLayoutManager].
 *
 * @param listener listener that receives pagination callbacks and state checks
 */
class PagingScrollCallback(
    private val listener: PagingScrollListener
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(
        recyclerView: RecyclerView,
        dx: Int,
        dy: Int
    ) {
        super.onScrolled(recyclerView, dx, dy)
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val visibleItemCount: Int = lm.childCount
        val totalItemCount: Int = lm.itemCount
        val firstVisibleItemPosition: Int = lm.findFirstVisibleItemPosition()

        val isLoadingAllow = !listener.isLoading() && !listener.isError()

        if (isLoadingAllow) {
            val totalItemVisiblePosition = visibleItemCount + firstVisibleItemPosition
            val pagingTriggerCount = totalItemCount - (totalItemCount * 0.3f).toInt()
            if (totalItemVisiblePosition >= pagingTriggerCount && firstVisibleItemPosition >= 0) {
                listener.loadMore()
            }
        }
    }
}
