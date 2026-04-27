package ru.dapadz.paging.domain.source.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.dapadz.paging.domain.events.PagingUiEvent
import ru.dapadz.paging.domain.pagination.Pagination
import ru.dapadz.paging.domain.state.PagingUiState

@OptIn(ExperimentalCoroutinesApi::class)
class PagingSourceRefreshTest {

    @Test
    fun refreshRepeatsLastLoadedPage() = runTest {
        val source = TestPagingSource(this)

        source.requestPageForTest(3)
        source.sendUiEvent(PagingUiEvent.Refresh(clear = false))
        advanceUntilIdle()

        assertEquals(listOf(3, 3), source.requestedPages)
        assertEquals(3, source.currentPage)
    }

    @Test
    fun refreshUsesDefaultPageWhenNothingWasLoadedYet() = runTest {
        val source = TestPagingSource(this)

        source.sendUiEvent(PagingUiEvent.Refresh(clear = false))
        advanceUntilIdle()

        assertEquals(listOf(PagingSource.DEFAULT_PAGE), source.requestedPages)
        assertEquals(PagingSource.DEFAULT_PAGE, source.currentPage)
    }

    @Test
    fun repeatLastRetriesTheLastRequestedPageAfterFailure() = runTest {
        val source = TestPagingSource(this, failPages = setOf(3))

        source.requestPageForTest(2)
        source.sendUiEvent(PagingUiEvent.LoadMore)
        advanceUntilIdle()

        source.failPages = emptySet()
        source.sendUiEvent(PagingUiEvent.RepeatLast)
        advanceUntilIdle()

        assertEquals(listOf(2, 3, 3), source.requestedPages)
        assertEquals(3, source.currentPage)
    }

    private class TestPagingSource(
        scope: CoroutineScope,
        var failPages: Set<Int> = emptySet(),
    ) : PagingSource<Int>(scope) {

        val requestedPages = mutableListOf<Int>()
        val states = mutableListOf<PagingUiState<Int>>()

        suspend fun requestPageForTest(page: Int) {
            pagingRequest(page)
        }

        override suspend fun getPagination(page: Int): Pagination<Int> {
            requestedPages += page
            if (page in failPages) {
                throw IllegalStateException("Failed page $page")
            }
            return Pagination(page = page, totalPage = 10, items = listOf(page))
        }

        override fun updateUiState(uiState: PagingUiState<Int>) {
            states += uiState
        }
    }
}
