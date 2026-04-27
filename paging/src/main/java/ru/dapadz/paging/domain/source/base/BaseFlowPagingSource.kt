package ru.dapadz.paging.domain.source.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dapadz.paging.domain.comparator.SelectorListComparator
import ru.dapadz.paging.domain.pagination.Pagination
import ru.dapadz.paging.domain.state.PagingUiState

/**
 * Base [PagingSource] implementation for APIs that expose page results as [Flow] values.
 *
 * Subclasses decide when the first page should be requested.
 */
abstract class BaseFlowPagingSource<T>(
    private val scope: CoroutineScope,
    open val comparator: ((T) -> Any)? = null,
    open val onRequest: suspend (Int) -> Flow<Pagination<T>>,
    open val onUiStateUpdate: (PagingUiState<T>) -> Unit,
) : PagingSource<T>(scope) {

    /**
     * Starts loading from the default page.
     */
    fun initialize() {
        scope.launch { pagingRequest(DEFAULT_PAGE) }
    }

    protected fun checkComparatorAndSetup() {
        if (comparator != null) {
            setupComparator(SelectorListComparator(comparator!!))
        }
    }

    override suspend fun getPagination(page: Int): Pagination<T> {
        return onRequest.invoke(page).first()
    }

    override fun updateUiState(uiState: PagingUiState<T>) {
        onUiStateUpdate.invoke(uiState)
    }

}
