package ru.dapadz.paging.domain.source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ru.dapadz.paging.domain.pagination.Pagination
import ru.dapadz.paging.domain.source.base.BaseFlowPagingSource
import ru.dapadz.paging.domain.source.base.PagingSource
import ru.dapadz.paging.domain.state.PagingUiState

/**
 * Flow-backed [PagingSource] with manual initialization.
 *
 * Call [initialize] when you are ready to request the first page.
 *
 * For example:
 *
 * ```kotlin
 * private val pagingSource = LazyFlowPagingSource(
 *      scope = viewModelScope,
 *      onRequest = { page ->
 *          locationRepository.getLimitedCities(
 *              page = page,
 *              perPage = 30,
 *              query = cityQuery
 *          )
 *      },
 *      comparator = { it.id },
 *      onUiStateUpdate = {
 *          _uiState.update { state ->
 *              state.copy(
 *                  pagingUiState = it
 *              )
 *          }
 *      }
 *  ).also {
 *      // Trigger the first request when appropriate for your screen
 *      it.initialize()
 *  }
 * ```
 *
 * @param T the item type
 * @param scope coroutine scope used for page requests
 * @property comparator optional selector used to remove duplicates from accumulated items
 * @property onRequest suspend function that returns page data as a [Flow]
 * @property onUiStateUpdate callback invoked whenever the paging UI state changes
 */
class LazyFlowPagingSource<T>(
    scope: CoroutineScope,
    override val comparator: ((T) -> Any)? = null,
    override val onRequest: suspend (Int) -> Flow<Pagination<T>>,
    override val onUiStateUpdate: (PagingUiState<T>) -> Unit,
) : BaseFlowPagingSource<T>(scope, comparator, onRequest, onUiStateUpdate) {

    init {
        checkComparatorAndSetup()
    }

}
