# Paging

[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Русский | [English](./README.md)

Paging - лёгкая Android-библиотека для простой постраничной загрузки в `RecyclerView`.
Она даёт небольшой набор примитивов вокруг `Flow`, `RecyclerView` и явных событий пагинации без более тяжёлой архитектуры `androidx.paging`.

## Зачем эта библиотека

- Простой контракт для API, которые уже возвращают `page`, `totalPage` и `items`.
- Явные действия `load more`, `refresh`, `retry` и `clear`.
- Встроенное накопление уже загруженных элементов внутри paging source.
- RecyclerView-адаптеры на базе `DiffUtil` или `AsyncListDiffer`.
- Опциональные concat/state-адаптеры для loading/error строк.
- Опциональная фильтрация дублей через `comparator = { it.id }`.

## Когда использовать Paging

Эта библиотека подходит, если:

- ваш backend использует классическую номерную пагинацию,
- целевой UI построен на `RecyclerView`,
- вы хотите сами управлять пагинацией из своего `ViewModel`,
- вам нужен более компактный API, чем `androidx.paging`.

## Подключение

```kotlin
dependencies {
    implementation("ru.dapadz.paging:paging:1.0.0")
}
```

## Базовая модель данных

Источник данных должен возвращать `Pagination<T>`:

```kotlin
data class Pagination<T>(
    val page: Int,
    val totalPage: Int,
    val items: List<T>
)
```

Этого контракта достаточно, чтобы paging source мог:

- кэшировать уже загруженные элементы,
- определять последнюю страницу,
- объединять новые страницы в единый список,
- публиковать UI state.

## Быстрый старт

### 1. Создайте метод в репозитории

```kotlin
class ArticleRepository(
    private val api: ArticleApi
) {
    fun getArticles(page: Int): Flow<Pagination<Article>> = flow {
        emit(api.getArticles(page))
    }
}
```

### 2. Создайте `FlowPagingSource`

`FlowPagingSource` сам запрашивает первую страницу сразу после создания.

```kotlin
class ArticlesViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _pagingState = MutableStateFlow<PagingUiState<Article>>(PagingUiState.Loading)
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

### 3. Создайте адаптер

На выбор есть две базовые реализации:

- `DefaultPagingAdapter` для синхронного `DiffUtil.calculateDiff(...)`
- `AsyncPagingAdapter` для фонового diffing через `AsyncListDiffer`

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

### 4. Подключите всё к `RecyclerView`

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

Именно `adapter.setPagingState(...)` передаёт в адаптер новые элементы, когда состояние становится `PagingUiState.Data`.

## `FlowPagingSource` и `LazyFlowPagingSource`

Используйте `FlowPagingSource`, если первая страница должна загрузиться сразу.

Используйте `LazyFlowPagingSource`, если создание источника и первый запрос должны быть разделены:

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

Это удобно, когда первый запрос зависит от выбранного таба, поисковой строки, фильтра или другого UI-события.

## UI events

`PagingSource` принимает четыре явных действия:

- `PagingUiEvent.LoadMore`: запрашивает следующую страницу, если текущая ещё не последняя.
- `PagingUiEvent.Refresh(clear = true)`: очищает кэш и повторяет последний запрос.
- `PagingUiEvent.RepeatLast`: повторяет последнюю запрошенную страницу.
- `PagingUiEvent.Clear`: очищает кэш и публикует пустое состояние данных.

## UI states

Paging source сообщает одно из трёх состояний:

- `PagingUiState.Loading`
- `PagingUiState.Error`
- `PagingUiState.Data(items)`

Это позволяет одним и тем же состоянием управлять и содержимым списка, и внешним UI: progress, retry, empty state.

## Работа с `ConcatAdapter`

Если нужно объединить контент, loading row, header или footer, используйте `SimplePagingConcatAdapter`:

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

`PagingStateAdapter` получает актуальный `PagingUiState<*>` в `onStateUpdate(...)`, поэтому loading/error строки можно рендерить прямо внутри дерева адаптеров.

## Обновление уже загруженных элементов

Если нужно локально изменить закэшированные элементы, используйте `updatePagingDataItems(...)`:

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

Это удобно для optimistic UI обновлений: лайков, bookmark-ов, переключателей и маркеров прочитанного.

## Обзор API

- `FlowPagingSource`: источник на базе Flow с автоматической загрузкой первой страницы.
- `LazyFlowPagingSource`: источник на базе Flow с ручной инициализацией.
- `PagingSource`: базовая абстракция, которая владеет кэшем, состоянием страниц и событиями.
- `PagingUiEvent`: явные команды управления пагинацией.
- `PagingUiState`: выходное состояние для UI.
- `DefaultPagingAdapter`: синхронный адаптер с diff-обновлением.
- `AsyncPagingAdapter`: асинхронный адаптер с diff-обновлением.
- `PagingDiffUtilCallback`: переиспользуемая абстракция для DiffUtil.
- `SimplePagingConcatAdapter`: helper вокруг `ConcatAdapter` для композиции content/state/header/footer.

## Лицензия

Paging распространяется по лицензии Apache License 2.0.
См. [LICENSE](./LICENSE).
