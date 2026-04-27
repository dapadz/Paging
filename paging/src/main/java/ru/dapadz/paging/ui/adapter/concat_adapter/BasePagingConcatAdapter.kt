package ru.dapadz.paging.ui.adapter.concat_adapter

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.dapadz.paging.ui.adapter.paging_adapter.PagingAdapter

/**
 * Base [PagingConcatAdapter] implementation backed by a [ConcatAdapter].
 *
 * @param T the item type rendered by the content adapter
 * @param VH the [RecyclerView.ViewHolder] type used by the content adapter
 * @property contentAdapter the primary paging adapter
 */
abstract class BasePagingConcatAdapter<T, VH : RecyclerView.ViewHolder>(
    contentAdapter: PagingAdapter<T, VH>,
) : PagingConcatAdapter<T, VH> {

    override val adapter: ConcatAdapter = ConcatAdapter(contentAdapter)

    /**
     * Adds an adapter to the underlying [ConcatAdapter].
     *
     * @param position insertion index
     * @param adapter adapter to insert
     */
    override fun addAdapter(position: Int, adapter: RecyclerView.Adapter<*>) {
        this.adapter.addAdapter(position, adapter)
    }

    /**
     * Removes the specified adapter from the underlying [ConcatAdapter].
     *
     * @param adapter adapter to remove
     */
    override fun removeAdapter(adapter: RecyclerView.Adapter<*>) {
        this.adapter.removeAdapter(adapter)
    }

}
