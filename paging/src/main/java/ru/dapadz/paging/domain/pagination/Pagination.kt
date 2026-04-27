package ru.dapadz.paging.domain.pagination

/**
 * Represents a single page returned by a paginated data source.
 *
 * @param T the item type
 * @property page 1-based index of the current page
 * @property totalPage total number of pages available
 * @property items items returned for the current page
 */
data class Pagination<T>(
	val page: Int,
	val totalPage: Int,
	val items: List<T>
) {
	companion object {
		/**
		 * Creates an empty pagination result.
		 */
		fun <T> empty() = Pagination<T>(0, 1, emptyList())
	}
}
