package ru.dapadz.paging.ui.adapter.concat_adapter

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.dapadz.paging.ui.adapter.paging_adapter.PagingAdapter

/**
 * Contract for wrappers that expose a content [PagingAdapter] inside a [ConcatAdapter].
 *
 * @param T the item type rendered by the content adapter
 * @param VH the [RecyclerView.ViewHolder] type used by the content adapter
 */
interface PagingConcatAdapter<T, VH : ViewHolder> {
	/**
	 * The main content adapter.
	 */
	val contentAdapter: PagingAdapter<T, VH>

	/**
	 * The [ConcatAdapter] that hosts [contentAdapter] and any additional adapters.
	 */
	val adapter: ConcatAdapter

	/**
	 * Adds an adapter to the underlying [ConcatAdapter].
	 */
	fun addAdapter(position: Int = 0, adapter: RecyclerView.Adapter<*>)

	/**
	 * Removes the specified adapter from the underlying [ConcatAdapter].
	 */
	fun removeAdapter(adapter: RecyclerView.Adapter<*>)
}
