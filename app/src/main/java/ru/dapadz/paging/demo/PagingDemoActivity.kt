package ru.dapadz.paging.demo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import ru.dapadz.paging.domain.state.PagingUiState
import ru.dapadz.paging.ui.adapter.concat_adapter.SimplePagingConcatAdapter
import ru.dapadz.paging.ui.extensions.attachPagingScrollListener

class PagingDemoActivity : AppCompatActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[PagingDemoViewModel::class.java]
    }

    private val articleAdapter = DemoArticleAdapter { article ->
        viewModel.toggleBookmark(article.id)
    }

    private val stateAdapter = DemoPagingStateAdapter(
        hasContentProvider = { articleAdapter.itemCount > 0 },
        onRetryClick = { viewModel.retry() }
    )

    private val concatAdapter = SimplePagingConcatAdapter(articleAdapter)

    private lateinit var scenarioTabs: TabLayout
    private lateinit var scenarioTitle: TextView
    private lateinit var scenarioDescription: TextView
    private lateinit var scenarioStatus: TextView
    private lateinit var startButton: MaterialButton
    private lateinit var refreshButton: MaterialButton
    private lateinit var retryButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var toggleFirstButton: MaterialButton
    private lateinit var loadingView: View
    private lateinit var emptyStateView: View
    private lateinit var emptyStateTitle: TextView
    private lateinit var emptyStateMessage: TextView
    private lateinit var emptyStateAction: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paging_demo)
        title = getString(R.string.app_name)

        bindViews()
        setupRecycler()
        setupTabs()
        setupButtons()
        observeState()
    }

    private fun bindViews() {
        scenarioTabs = findViewById(R.id.demoScenarioTabs)
        scenarioTitle = findViewById(R.id.demoScenarioTitle)
        scenarioDescription = findViewById(R.id.demoScenarioDescription)
        scenarioStatus = findViewById(R.id.demoScenarioStatus)
        startButton = findViewById(R.id.demoStartButton)
        refreshButton = findViewById(R.id.demoRefreshButton)
        retryButton = findViewById(R.id.demoRetryButton)
        clearButton = findViewById(R.id.demoClearButton)
        toggleFirstButton = findViewById(R.id.demoToggleFirstButton)
        loadingView = findViewById(R.id.demoLoadingView)
        emptyStateView = findViewById(R.id.demoEmptyStateView)
        emptyStateTitle = findViewById(R.id.demoEmptyStateTitle)
        emptyStateMessage = findViewById(R.id.demoEmptyStateMessage)
        emptyStateAction = findViewById(R.id.demoEmptyStateAction)
    }

    private fun setupRecycler() {
        val recyclerView = findViewById<RecyclerView>(R.id.demoRecyclerView)

        concatAdapter.setupStateAdapter { stateAdapter }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = concatAdapter.adapter

        concatAdapter.attachPagingScrollListener {
            viewModel.loadMore()
        }
    }

    private fun setupTabs() {
        DemoScenario.entries.forEach { scenario ->
            scenarioTabs.addTab(
                scenarioTabs.newTab().setText(scenario.tabTitle)
            )
        }

        scenarioTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                DemoScenario.entries.getOrNull(tab.position)?.let(viewModel::selectScenario)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupButtons() {
        startButton.setOnClickListener { viewModel.startScenario() }
        refreshButton.setOnClickListener { viewModel.refresh() }
        retryButton.setOnClickListener { viewModel.retry() }
        clearButton.setOnClickListener { viewModel.clear() }
        toggleFirstButton.setOnClickListener { viewModel.toggleFirstBookmark() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenState.collect(::render)
            }
        }
    }

    private fun render(state: PagingDemoScreenState) {
        scenarioTitle.text = state.scenario.title
        scenarioDescription.text = state.scenario.description
        scenarioStatus.text = buildStatus(state)

        articleAdapter.setPagingState(state.pagingState)

        startButton.isVisible = state.scenario.isLazy
        startButton.isEnabled = !state.hasStarted
        refreshButton.isEnabled = !state.scenario.isLazy || state.hasStarted
        retryButton.isEnabled = state.itemCount > 0 || state.pagingState is PagingUiState.Error
        clearButton.isEnabled = state.itemCount > 0
        toggleFirstButton.isEnabled = state.itemCount > 0

        val isInitialLoading = state.pagingState is PagingUiState.Loading && state.itemCount == 0
        loadingView.isVisible = isInitialLoading

        when {
            isInitialLoading -> hideEmptyState()

            state.scenario.isLazy && !state.hasStarted && state.itemCount == 0 -> {
                showEmptyState(
                    title = "Lazy source is ready",
                    message = "This scenario uses LazyFlowPagingSource. Tap Start to request the first page manually.",
                    actionLabel = getString(R.string.demo_action_start),
                    onAction = viewModel::startScenario,
                )
            }

            state.pagingState is PagingUiState.Error && state.itemCount == 0 -> {
                val message = state.pagingState.error.message ?: "Unable to load the demo page."
                showEmptyState(
                    title = "Request failed",
                    message = message,
                    actionLabel = getString(R.string.demo_action_refresh),
                    onAction = viewModel::refresh,
                )
            }

            state.pagingState is PagingUiState.Data && state.itemCount == 0 -> {
                showEmptyState(
                    title = "List is empty",
                    message = "Use Refresh to request data again or switch to another demo scenario.",
                    actionLabel = getString(R.string.demo_action_refresh),
                    onAction = viewModel::refresh,
                )
            }

            else -> hideEmptyState()
        }
    }

    private fun buildStatus(state: PagingDemoScreenState): String {
        val sourceName = if (state.scenario.isLazy) {
            "LazyFlowPagingSource"
        } else {
            "FlowPagingSource"
        }

        val comparatorStatus = if (state.scenario.usesComparator) {
            "Comparator enabled"
        } else {
            "Comparator disabled"
        }

        val pageSummary = if (state.currentPage == 0) {
            "No pages loaded yet"
        } else {
            "Page ${state.currentPage} of ${state.totalPages}"
        }

        val stateSummary = when (val pagingState = state.pagingState) {
            is PagingUiState.Loading -> {
                if (state.itemCount == 0) "Loading first page"
                else "Loading more items"
            }

            is PagingUiState.Error -> pagingState.error.message ?: "Last request failed"

            is PagingUiState.Data -> {
                if (state.itemCount == 0 && state.scenario.isLazy && !state.hasStarted) {
                    "Waiting for manual start"
                } else if (state.itemCount == 0) {
                    "No cached items"
                } else {
                    "${state.itemCount} cached items"
                }
            }
        }

        return "$sourceName • $comparatorStatus\n$pageSummary • $stateSummary"
    }

    private fun showEmptyState(
        title: String,
        message: String,
        actionLabel: String,
        onAction: () -> Unit,
    ) {
        emptyStateView.isVisible = true
        emptyStateTitle.text = title
        emptyStateMessage.text = message
        emptyStateAction.text = actionLabel
        emptyStateAction.setOnClickListener { onAction() }
    }

    private fun hideEmptyState() {
        emptyStateView.isVisible = false
        emptyStateAction.setOnClickListener(null)
    }
}
