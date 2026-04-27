package ru.dapadz.paging.domain.comparator

import ru.dapadz.paging.domain.comparator.api.PagingListComparator

/**
 * [PagingListComparator] implementation that removes duplicates by key.
 *
 * @param T the item type stored in the list
 * @param K the key type returned by [selector]
 * @property selector function used to extract a stable key from each item
 */
class SelectorListComparator<T, K>(
    private val selector: (T) -> K
) : PagingListComparator<T> {

    /**
     * Returns a list that keeps only the first item for each selected key.
     *
     * For example:
     * ```kotlin
     * val comparator = SelectorListComparator<User, Int> { user -> user.id }
     * val uniqueUsers = comparator.compare(userList)
     * ```
     *
     * @param list the source list
     * @return a de-duplicated list
     */
    override fun compare(list: List<T>): List<T> {
        return list.distinctBy(selector)
    }
}
