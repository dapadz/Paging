package ru.dapadz.paging.domain.comparator.api

/**
 * Strategy interface for normalizing a list before it is exposed by a paging source.
 *
 * Typical use cases include de-duplication or custom merge rules.
 *
 * @param T the item type stored in the list
 */
interface PagingListComparator<T> {

    /**
     * Returns the normalized list that should be exposed to the UI.
     *
     * @param list the source list
     * @return the processed list
     */
    fun compare(list: List<T>): List<T>
}
