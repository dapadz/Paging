# Paging

[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://www.apache.org/licenses/LICENSE-2.0)

English | [Русский](./README.ru.md)

Paging is a lightweight Android library for simple page-based loading in `RecyclerView` UIs.
It gives you a small set of primitives around `Flow`, `RecyclerView`, and explicit pagination events without the heavier architecture of `androidx.paging`.

## Why this library

- Simple contract for APIs that already return `page`, `totalPage`, and `items`.
- Explicit `load more`, `refresh`, `retry`, and `clear` actions.
- Built-in accumulation of already loaded items inside the paging source.
- RecyclerView adapters backed by either `DiffUtil` or `AsyncListDiffer`.
- Optional concat/state adapters for loading and error rows.
- Optional duplicate filtering via `comparator = { it.id }`.

## When to use Paging

Use this library when:

- your backend already exposes classic numbered pagination,
- `RecyclerView` is your target UI,
- you want to control pagination from your own `ViewModel`,
- you want a smaller surface area than `androidx.paging`.

## Installation

```kotlin
dependencies {
    implementation("ru.dapadz.paging:paging:1.0.0")
}
```

## Core model

Your data source should return `Pagination<T>`:

```kotlin
data class Pagination<T>(
    val page: Int,
    val totalPage: Int,
    val items: List<T>
)
```

This is the only contract the paging source needs in order to:

- cache already loaded items,
- detect the last page,
- merge new pages into a single list,
- publish a UI state.

## Quick Start

### 1. Create a repository method

```kotlin
class ArticleRepository(
    private val api: ArticleApi
) {
    fun getArticles(page: Int): Flow<Pagination<Article>> = flow {
        emit(api.getArticles(page))
    }
}
```

### 2. Create a `FlowPagingSource`

`FlowPagingSource` requests the first page immediately after creation.

```kotlin
class ArticlesViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _pagingState =
        MutableStateFlow<PagingUiState<Article>>(PagingUiState.Loading)
    val pagingState: StateFlow<PagingUiState<Article>> = _pagingState

    val pagingSource = FlowPagingSource(
        scope = viewModelScope,
        comparator = { it.id },
        onRequest = repository::getArticles,
        onUiStateUpdate = { state ->
            _pagingState.value = state
        }
    )

    fun loadMore() {
        pagingSource.sendUiEvent(PagingUiEvent.LoadMore)
    }

    fun refresh() {
        pagingSource.sendUiEvent(PagingUiEvent.Refresh())
    }

    fun retry() {
        pagingSource.sendUiEvent(PagingUiEvent.RepeatLast)
    }

    fun clear() {
        pagingSource.sendUiEvent(PagingUiEvent.Clear)
    }
}
```

### 3. Create an adapter

Choose one of the built-in adapter bases:

- `DefaultPagingAdapter` for synchronous `DiffUtil.calculateDiff(...)`
- `AsyncPagingAdapter` for background diffing via `AsyncListDiffer`

```kotlin
class ArticleAdapter : DefaultPagingAdapter<Article, ArticleViewHolder>(
    object : PagingDiffUtilCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        return ArticleViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_article, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }
}
```

### 4. Connect it to `RecyclerView`

```kotlin
class ArticlesFragment : Fragment(R.layout.fragment_articles) {

    private val viewModel: ArticlesViewModel by viewModels()
    private val adapter = ArticleAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val progress = view.findViewById<View>(R.id.progress)
        val errorView = view.findViewById<View>(R.id.errorView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        adapter.attachPagingScrollListener {
            viewModel.loadMore()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagingState.collect { state ->
                    adapter.setPagingState(state)

                    progress.isVisible =
                        state is PagingUiState.Loading && adapter.itemCount == 0
                    errorView.isVisible = state is PagingUiState.Error
                }
            }
        }
    }
}
```

`adapter.setPagingState(...)` is what pushes loaded items into the adapter when the state becomes `PagingUiState.Data`.

## `FlowPagingSource` vs `LazyFlowPagingSource`

Use `FlowPagingSource` when you want the first page to be requested immediately.

Use `LazyFlowPagingSource` when creation and first request should happen separately:

```kotlin
val pagingSource = LazyFlowPagingSource(
    scope = viewModelScope,
    comparator = { it.id },
    onRequest = repository::getArticles,
    onUiStateUpdate = { _pagingState.value = it }
)

fun startPaging() {
    pagingSource.initialize()
}
```

This is useful when the first request depends on a selected tab, query, filter, or another UI event.

## UI events

`PagingSource` accepts four explicit actions:

- `PagingUiEvent.LoadMore`: requests the next page if the current page is not the last one.
- `PagingUiEvent.Refresh(clear = true)`: clears cached items and repeats the last request.
- `PagingUiEvent.RepeatLast`: retries the last requested page.
- `PagingUiEvent.Clear`: clears cached items and publishes an empty data state.

## UI states

The paging source reports one of three states:

- `PagingUiState.Loading`
- `PagingUiState.Error`
- `PagingUiState.Data(items)`

That makes it easy to drive both the list contents and surrounding UI such as progress, retry, or empty states.

## Working with `ConcatAdapter`

If you want to combine content, loading rows, headers, or footers, use `SimplePagingConcatAdapter`:

```kotlin
val contentAdapter = ArticleAdapter()

val concatAdapter = SimplePagingConcatAdapter(contentAdapter).apply {
    setupStateAdapter {
        ArticlePagingStateAdapter(
            onRetryClick = viewModel::retry
        )
    }
}

recyclerView.adapter = concatAdapter.adapter

concatAdapter.attachPagingScrollListener {
    viewModel.loadMore()
}
```

`PagingStateAdapter` receives the latest `PagingUiState<*>` in `onStateUpdate(...)`, so you can render loading or error rows inside the adapter tree.

## Updating already loaded items

When you need to update cached items locally, use `updatePagingDataItems(...)`:

```kotlin
fun updateBookmark(articleId: Long, isBookmarked: Boolean) {
    pagingSource.updatePagingDataItems { items ->
        items.map { article ->
            if (article.id == articleId) {
                article.copy(isBookmarked = isBookmarked)
            } else {
                article
            }
        }
    }
}
```

This is useful for optimistic UI updates such as toggles, likes, bookmarks, or read markers.

## API overview

- `FlowPagingSource`: flow-backed source with eager first-page loading.
- `LazyFlowPagingSource`: flow-backed source with manual initialization.
- `PagingSource`: base abstraction that owns cache, page state, and events.
- `PagingUiEvent`: explicit commands for pagination control.
- `PagingUiState`: loading, error, and data output for UI.
- `DefaultPagingAdapter`: synchronous diff-based adapter.
- `AsyncPagingAdapter`: async diff-based adapter.
- `PagingDiffUtilCallback`: reusable diff callback abstraction.
- `SimplePagingConcatAdapter`: helper around `ConcatAdapter` for content/state/header/footer composition.

## License

Paging is distributed under the Apache License 2.0.
See [LICENSE](./LICENSE).
