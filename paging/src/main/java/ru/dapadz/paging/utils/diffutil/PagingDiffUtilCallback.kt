package ru.dapadz.paging.utils.diffutil

import androidx.recyclerview.widget.DiffUtil

/**
 * Base [DiffUtil.Callback] implementation for paging adapters.
 *
 * It stores the old and new lists internally so subclasses can focus on item comparison logic.
 *
 * @param T the item type being compared
 */
abstract class PagingDiffUtilCallback<T> : DiffUtil.Callback() {

    private val oldItems = mutableListOf<T>()
    private val newItems = mutableListOf<T>()

    /**
     * Stores the old and new lists that should be compared.
     *
     * @param oldItems previous list
     * @param newItems new list
     * @return this callback instance
     */
    fun setItems(oldItems: List<T>, newItems: List<T>): DiffUtil.Callback {
        this.oldItems.clear()
        this.oldItems.addAll(oldItems)

        this.newItems.clear()
        this.newItems.addAll(newItems)
        return this
    }

    /**
     * Creates a [DiffUtil.ItemCallback] backed by the same comparison logic.
     */
    fun toItemCallback(): DiffUtil.ItemCallback<T> {
        return object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
                return this@PagingDiffUtilCallback.areItemsTheSame(oldItem, newItem)
            }
            override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
                return this@PagingDiffUtilCallback.areContentsTheSame(oldItem, newItem)
            }
            override fun getChangePayload(oldItem: T & Any, newItem: T & Any): Any? {
                return this@PagingDiffUtilCallback.getChangePayload(oldItem, newItem)
            }
        }
    }

    /**
     * Checks whether two items represent the same entity.
     *
     * @param oldItem item from the old list
     * @param newItem item from the new list
     * @return `true` if both items represent the same entity
     */
    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

    /**
     * Checks whether two matching items also have the same content.
     *
     * @param oldItem item from the old list
     * @param newItem item from the new list
     * @return `true` if both items have identical content
     */
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean

    /**
     * Returns an optional payload describing the change between two items.
     *
     * This method is called when [areItemsTheSame] returns `true`, but
     * [areContentsTheSame] returns `false`.
     *
     * @param oldItem item from the old list
     * @param newItem item from the new list
     */
    open fun getChangePayload(oldItem: T, newItem: T): Any? = null

    override fun getOldListSize() = oldItems.size

    override fun getNewListSize() = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return areItemsTheSame(oldItem = oldItems[oldItemPosition], newItem = newItems[newItemPosition])
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return areContentsTheSame(oldItem = oldItems[oldItemPosition], newItem = newItems[newItemPosition])
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return getChangePayload(oldItem = oldItems[oldItemPosition], newItem = newItems[newItemPosition])
    }
}
