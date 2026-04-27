package ru.dapadz.paging.ui.adapter.paging_adapter

import androidx.recyclerview.widget.RecyclerView
import ru.dapadz.paging.domain.state.PagingUiState

/**
 * Base [RecyclerView.Adapter] for paginated lists.
 *
 * The adapter stores the latest [PagingUiState] and updates its item list when
 * [PagingUiState.Data] is received.
 *
 * @param T the item type rendered by the adapter
 * @param VH the [RecyclerView.ViewHolder] type used by the adapter
 */
abstract class PagingAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    internal var pagingStateUpdateListener: OnPagingStateUpdate<T>? = null

    internal var scrollListener: RecyclerView.OnScrollListener? = null

    internal var pagingState: PagingUiState<T> = PagingUiState.Loading
        private set

    /**
     * Sets the current paging UI state and updates the adapter accordingly.
     *
     * @param pagingUiState new paging state
     */
    open fun setPagingState(pagingUiState: PagingUiState<T>, block: () -> Unit = {}) {
        pagingState = pagingUiState
        onPagingStateUpdate(pagingUiState, block)
    }

    /**
     * Handles a paging state update.
     *
     * If [pagingUiState] is [PagingUiState.Data], the new items are submitted to the adapter.
     *
     * @param pagingUiState new paging state
     */
    protected open fun onPagingStateUpdate(
        pagingUiState: PagingUiState<T>,
        block: () -> Unit
    ) {
        if (pagingUiState is PagingUiState.Data<T>) {
            submitList(pagingUiState.items, block)
        }
        pagingStateUpdateListener?.onUpdate(pagingUiState)
    }

    /**
     * Sets the listener that will be notified after every paging state update.
     *
     * @param listener listener to register
     */
    internal open fun setOnPagingUiStateUpdateListener(listener: OnPagingStateUpdate<T>) {
        pagingStateUpdateListener = listener
    }

    /**
     * Submits a new list of items to the adapter.
     *
     * @param data new list of items
     */
     abstract fun submitList(data: List<T>)

    /**
     * Submits a new list of items to the adapter.
     *
     * @param data new list of items
     */
     abstract fun submitList(data: List<T>, block: () -> Unit)

    /**
     * Returns the item at the specified position.
     *
     * @param position item position
     * @return item at [position], or `null` if it does not exist
     */
    abstract fun getItem(position: Int): T?

    /**
     * Finds the first item that matches the given predicate.
     *
     * @param predicate predicate used to match items
     * @return the first matching item, or `null` if no match is found
     */
    abstract fun findItem(predicate: (T) -> Boolean): T?

    /**
     * Returns the position of the specified item.
     *
     * @param item item to look up
     * @return position of [item], or `RecyclerView.NO_POSITION` if it is not present
     */
    abstract fun getItemPosition(item: T): Int

    /**
     * Attaches the adapter to a [RecyclerView] and registers the scroll listener, if any.
     *
     * @param recyclerView target [RecyclerView]
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        scrollListener?.let { recyclerView.addOnScrollListener(it) }
        super.onAttachedToRecyclerView(recyclerView)
    }

    /**
     * Listener notified after the paging state changes.
     *
     * @param T the item type rendered by the adapter
     */
    internal fun interface OnPagingStateUpdate<T> {

        /**
         * Called after the paging state is updated.
         *
         * @param pagingState new paging state
         */
        fun onUpdate(pagingState: PagingUiState<T>)
    }
}
