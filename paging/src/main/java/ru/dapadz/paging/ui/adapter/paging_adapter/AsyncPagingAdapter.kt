package ru.dapadz.paging.ui.adapter.paging_adapter

import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.dapadz.paging.utils.diffutil.PagingDiffUtilCallback

/**
 * [PagingAdapter] implementation that calculates list diffs on a background thread
 * using [AsyncListDiffer].
 *
 * @param T the item type rendered by the adapter
 * @param VH the [RecyclerView.ViewHolder] type used by the adapter
 * @property diffUtilCallback callback used to compare old and new items
 */
abstract class AsyncPagingAdapter<T, VH : ViewHolder>(
    private val diffUtilCallback: PagingDiffUtilCallback<T>
) : PagingAdapter<T, VH>() {

    private val diffUtilItemCallback = object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
            return diffUtilCallback.areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
            return diffUtilCallback.areContentsTheSame(oldItem, newItem)
        }

        override fun getChangePayload(oldItem: T & Any, newItem: T & Any): Any? {
            return diffUtilCallback.getChangePayload(oldItem, newItem)
        }
    }

    @Suppress("LeakingThis")
    protected val asyncDiffer = AsyncListDiffer(this, diffUtilItemCallback)

    override fun submitList(data: List<T>) {
        asyncDiffer.submitList(data)
    }

    override fun submitList(data: List<T>, block: () -> Unit) {
        asyncDiffer.submitList(data, block)
    }

    override fun findItem(predicate: (T) -> Boolean): T? = asyncDiffer.currentList.find { predicate.invoke(it) }

    override fun getItem(position: Int): T? = asyncDiffer.currentList.getOrNull(position)

    override fun getItemCount(): Int = asyncDiffer.currentList.size

    override fun getItemPosition(item: T): Int = asyncDiffer.currentList.indexOf(item)

}
