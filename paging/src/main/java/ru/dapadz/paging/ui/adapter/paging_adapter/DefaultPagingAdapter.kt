package ru.dapadz.paging.ui.adapter.paging_adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.dapadz.paging.utils.diffutil.PagingDiffUtilCallback

/**
 * [PagingAdapter] implementation that updates items with synchronous [DiffUtil] calculations.
 *
 * @param T the item type rendered by the adapter
 * @param VH the [RecyclerView.ViewHolder] type used by the adapter
 * @property diffUtilCallback callback used to compare old and new items
 */
abstract class DefaultPagingAdapter<T, VH : ViewHolder>(
    private val diffUtilCallback: PagingDiffUtilCallback<T>,
) : PagingAdapter<T, VH>() {

    protected var currentList = listOf<T>()
        private set

    /**
     * Submits a new list and dispatches the calculated diff to the adapter.
     *
     * @param data new list of items
     */
    override fun submitList(data: List<T>) {
        val callback = diffUtilCallback.setItems(currentList, data)
        DiffUtil.calculateDiff(callback).apply {
            currentList = data
            dispatchUpdatesTo(this@DefaultPagingAdapter)
        }
    }

    override fun submitList(data: List<T>, block: () -> Unit) {
        submitList(data)
        block.invoke()
    }

    override fun getItem(position: Int): T? = currentList.getOrNull(position)

    override fun findItem(predicate: (T) -> Boolean): T? = currentList.find { predicate(it) }

    override fun getItemPosition(item: T): Int = currentList.indexOf(item)

    override fun getItemCount(): Int = currentList.size
}
