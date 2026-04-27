package ru.dapadz.paging.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import ru.dapadz.paging.domain.events.PagingUiEvent
import ru.dapadz.paging.domain.pagination.Pagination
import ru.dapadz.paging.domain.source.FlowPagingSource
import ru.dapadz.paging.domain.source.LazyFlowPagingSource
import ru.dapadz.paging.domain.source.base.PagingSource
import ru.dapadz.paging.domain.state.PagingUiState

class PagingDemoViewModel : ViewModel() {

    private val _screenState = MutableStateFlow(PagingDemoScreenState.initial())
    val screenState: StateFlow<PagingDemoScreenState> = _screenState.asStateFlow()

    private var currentScenario: DemoScenario = DemoScenario.entries.first()
    private var currentSource: PagingSource<DemoArticle>? = null
    private var lazyStarted = false

    init {
        selectScenario(DemoScenario.entries.first())
    }

    fun selectScenario(scenario: DemoScenario) {
        currentScenario = scenario
        lazyStarted = !scenario.isLazy

        publishState(
            pagingState = PagingUiState.Data(emptyList()),
            currentPage = 0,
            totalPages = scenario.totalPages,
            itemCount = 0,
            hasStarted = lazyStarted,
        )

        val session = DemoPagingSession(scenario)
        val comparator: ((DemoArticle) -> Any)? = if (scenario.usesComparator) {
            { article -> article.id }
        } else {
            null
        }

        if (scenario.isLazy) {
            currentSource = LazyFlowPagingSource(
                scope = viewModelScope,
                comparator = comparator,
                onRequest = session::loadPage,
                onUiStateUpdate = ::publishState,
            )
        } else {
            currentSource = FlowPagingSource(
                scope = viewModelScope,
                comparator = comparator,
                onRequest = session::loadPage,
                onUiStateUpdate = ::publishState,
            )
        }
    }

    fun startScenario() {
        val source = currentSource as? LazyFlowPagingSource<DemoArticle> ?: return
        if (lazyStarted) return

        lazyStarted = true
        publishState(PagingUiState.Loading)
        source.initialize()
    }

    fun loadMore() {
        if (currentScenario.isLazy && !lazyStarted) return
        currentSource?.sendUiEvent(PagingUiEvent.LoadMore)
    }

    fun refresh() {
        if (currentScenario.isLazy && !lazyStarted) {
            startScenario()
            return
        }
        currentSource?.sendUiEvent(PagingUiEvent.Refresh())
    }

    fun retry() {
        if (currentScenario.isLazy && !lazyStarted) {
            startScenario()
            return
        }
        currentSource?.sendUiEvent(PagingUiEvent.RepeatLast)
    }

    fun clear() {
        currentSource?.sendUiEvent(PagingUiEvent.Clear)
    }

    fun toggleBookmark(articleId: Long) {
        currentSource?.updatePagingDataItems { items ->
            items.map { article ->
                if (article.id == articleId) {
                    article.copy(isBookmarked = !article.isBookmarked)
                } else {
                    article
                }
            }
        }
    }

    fun toggleFirstBookmark() {
        val firstId = currentSource?.pagingData?.items?.firstOrNull()?.id ?: return
        toggleBookmark(firstId)
    }

    private fun publishState(
        pagingState: PagingUiState<DemoArticle>,
        currentPage: Int = currentSource?.currentPage ?: 0,
        totalPages: Int = currentSource?.pagingData?.totalPage ?: currentScenario.totalPages,
        itemCount: Int = when (pagingState) {
            is PagingUiState.Data -> pagingState.items.size
            else -> currentSource?.pagingData?.items?.size ?: 0
        },
        hasStarted: Boolean = lazyStarted,
    ) {
        _screenState.value = PagingDemoScreenState(
            scenario = currentScenario,
            pagingState = pagingState,
            currentPage = currentPage,
            totalPages = totalPages,
            itemCount = itemCount,
            hasStarted = hasStarted,
        )
    }
}

data class PagingDemoScreenState(
    val scenario: DemoScenario,
    val pagingState: PagingUiState<DemoArticle>,
    val currentPage: Int,
    val totalPages: Int,
    val itemCount: Int,
    val hasStarted: Boolean,
) {
    companion object {
        fun initial(): PagingDemoScreenState {
            val scenario = DemoScenario.entries.first()
            return PagingDemoScreenState(
                scenario = scenario,
                pagingState = PagingUiState.Data(emptyList()),
                currentPage = 0,
                totalPages = scenario.totalPages,
                itemCount = 0,
                hasStarted = false,
            )
        }
    }
}

data class DemoArticle(
    val id: Long,
    val title: String,
    val summary: String,
    val page: Int,
    val isBookmarked: Boolean,
)

enum class DemoScenario(
    val tabTitle: String,
    val title: String,
    val description: String,
    val totalPages: Int = 5,
    val isLazy: Boolean = false,
    val usesComparator: Boolean = false,
    val failPageOnce: Int? = null,
) {
    AUTO(
        tabTitle = "Auto",
        title = "FlowPagingSource with eager first load",
        description = "Demonstrates the default setup. The first page is requested as soon as the source is created.\n\nTry scrolling, refreshing, clearing the list, and tapping items to update them locally.",
    ),
    LAZY(
        tabTitle = "Lazy",
        title = "LazyFlowPagingSource with manual start",
        description = "Demonstrates deferred initialization. The source is created immediately, but the first page is requested only after you tap Start.",
        isLazy = true,
    ),
    DEDUPE(
        tabTitle = "Dedupe",
        title = "Duplicate filtering with comparator",
        description = "Each page intentionally overlaps with the previous one. The scenario enables comparator = { it.id } so only unique items remain in the cached list.",
        usesComparator = true,
    ),
    RETRY(
        tabTitle = "Retry",
        title = "Error handling and retry flow",
        description = "Page 2 fails on the first attempt. The bottom state row exposes a Retry action so you can repeat the failed request and continue paging.",
        failPageOnce = 2,
    ),
}

private class DemoPagingSession(
    private val scenario: DemoScenario,
) {

    private val failedPages = mutableSetOf<Int>()

    fun loadPage(page: Int): Flow<Pagination<DemoArticle>> = flow {
        delay(650)

        val failPage = scenario.failPageOnce
        if (failPage != null && page == failPage && failedPages.add(page)) {
            throw IllegalStateException(
                "Page $page failed on purpose. Tap Retry to request it again."
            )
        }

        emit(
            Pagination(
                page = page,
                totalPage = scenario.totalPages,
                items = buildItems(page),
            )
        )
    }

    private fun buildItems(page: Int): List<DemoArticle> {
        val startId = when (scenario) {
            DemoScenario.DEDUPE -> ((page - 1) * 6) + 1
            else -> ((page - 1) * 8) + 1
        }

        return List(8) { index ->
            val id = (startId + index).toLong()
            DemoArticle(
                id = id,
                title = "Article #$id",
                summary = buildSummary(page, id),
                page = page,
                isBookmarked = id % 4L == 0L,
            )
        }
    }

    private fun buildSummary(page: Int, id: Long): String {
        return when (scenario) {
            DemoScenario.AUTO -> "Auto-start item $id from page $page. Scroll to trigger the next request."
            DemoScenario.LAZY -> "Lazy-start item $id from page $page. The first request is manual in this scenario."
            DemoScenario.DEDUPE -> "Overlapping item $id from page $page. Duplicate IDs are filtered by the comparator."
            DemoScenario.RETRY -> "Retry item $id from page $page. Page 2 fails once before succeeding."
        }
    }
}
