package ru.dapadz.paging.domain.source.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.dapadz.paging.domain.comparator.api.PagingListComparator
import ru.dapadz.paging.domain.data.PagingData
import ru.dapadz.paging.domain.events.PagingUiEvent
import ru.dapadz.paging.domain.pagination.Pagination
import ru.dapadz.paging.domain.state.PagingUiState

/**
 * Base abstraction that coordinates page loading, cached items, and UI state updates.
 *
 * Subclasses are responsible for loading a page and forwarding state changes to the UI layer.
 *
 * @param T the item type
 * @property scope coroutine scope used for page requests
 */
abstract class PagingSource<T>(
    private val scope: CoroutineScope
) {

    companion object {
        const val DEFAULT_PAGE = 1
    }

    private var lastRequestedPage: Int = DEFAULT_PAGE

    /**
     * Index of the next page to be loaded.
     */
    val nextPage get() = pagingData.lastSavedPage + 1

    /**
     * Index of the most recently loaded page.
     */
    val currentPage get() = pagingData.lastSavedPage

    /**
     * Cached paging snapshot.
     */
    var pagingData: PagingData<T> = PagingData.empty()
        private set

    /**
     * Comparator used to post-process accumulated items before they are cached.
     */
    var pagingComparator: PagingListComparator<T>? = null
        private set

    /**
     * Loads data for the requested page.
     *
     * @param page page index to request
     * @return page result returned by the data source
     */
    protected abstract suspend fun getPagination(page: Int): Pagination<T>

    /**
     * Publishes a new [PagingUiState] to the UI layer.
     *
     * @param uiState new state to expose
     */
    protected abstract fun updateUiState(uiState: PagingUiState<T>)

    /**
     * Handles a [PagingUiEvent] sent from the UI layer.
     *
     * @param event action to execute
     */
    open fun sendUiEvent(event: PagingUiEvent) {
        scope.launch {
            when (event) {
                PagingUiEvent.Clear -> clearPagingData()
                PagingUiEvent.RepeatLast -> pagingRequest(lastRequestedPage)
                PagingUiEvent.LoadMore -> {
                    if (!pagingData.isLastPage) {
                        pagingRequest(nextPage)
                    }
                }
                is PagingUiEvent.Refresh -> {
                    val pageToRefresh = lastRequestedPage
                    clearPagingData()
                    if (event.clear) {
                        delay(50)
                    }
                    pagingRequest(pageToRefresh)
                }
            }
        }
    }

    /**
     * Sets the comparator used to process accumulated items before they are cached.
     *
     * @param comparator comparator implementation
     */
    open fun setupComparator(comparator: PagingListComparator<T>) {
        pagingComparator = comparator
    }

    /**
     * Applies a local update to cached items and emits the updated list.
     *
     * @param block transformation applied to the cached items
     */
    open fun updatePagingDataItems(block: (List<T>) -> List<T>) {
        if (pagingData.items.isEmpty()) return
        val newList = block.invoke(pagingData.items)
        pagingData = pagingData.copy(
            items = newList
        )
        updateUiState(PagingUiState.Data(pagingData.items))
    }

    /**
     * Executes a request for the given page and maps its result to a UI state.
     *
     * @param page page index to request
     */
    protected open suspend fun pagingRequest(page: Int) {
        lastRequestedPage = page.coerceAtLeast(DEFAULT_PAGE)
        runCatching {
            updateUiState(PagingUiState.Loading)
            getPagination(page)
        }
            .onSuccess(::onPaginationSuccess)
            .onFailure(::onPaginationError)
    }

    /**
     * Persists a successful page result and emits [PagingUiState.Data].
     *
     * @param pagination page result returned by the data source
     */
    protected open fun onPaginationSuccess(pagination: Pagination<T>) {
        savePaginationToData(pagination)
        updateUiState(PagingUiState.Data(pagingData.items))
    }

    /**
     * Emits an error state for a failed page request.
     *
     * @param throwable failure thrown during the request
     */
    protected open fun onPaginationError(throwable: Throwable) {
        updateUiState(PagingUiState.Error(throwable))
    }

    /**
     * Merges the received page into the cached paging snapshot.
     *
     * @param pagination page result to cache
     */
    protected open fun savePaginationToData(pagination: Pagination<T>) {
        pagingData = pagination.toPagingData(pagingData)
    }

    /**
     * Clears cached items and emits an empty data state.
     */
    protected open suspend fun clearPagingData() {
        lastRequestedPage = DEFAULT_PAGE
        pagingData = PagingData.empty()
        onPaginationSuccess(Pagination.empty())
    }

    /**
     * Applies the configured comparator to the accumulated items, if one exists.
     *
     * @return processed items, or the original list when no comparator is configured
     */
    private fun List<T>.compareOrGetCurrent(): List<T> {
        return pagingComparator?.compare(this) ?: this
    }

    /**
     * Converts a single [Pagination] result into an updated [PagingData] snapshot.
     *
     * @param pagingData current cached snapshot
     * @return updated snapshot containing merged items
     */
    private fun Pagination<T>.toPagingData(pagingData: PagingData<T>): PagingData<T> {
        return PagingData(
            lastSavedPage = page,
            totalPage = totalPage,
            items = (pagingData.items + items).compareOrGetCurrent()
        )
    }
}
