package ru.dapadz.paging.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import ru.dapadz.paging.domain.state.PagingUiState
import ru.dapadz.paging.ui.adapter.paging_adapter.DefaultPagingAdapter
import ru.dapadz.paging.ui.adapter.state_adapter.PagingStateAdapter
import ru.dapadz.paging.utils.diffutil.PagingDiffUtilCallback

class DemoArticleAdapter(
    private val onArticleClick: (DemoArticle) -> Unit,
) : DefaultPagingAdapter<DemoArticle, DemoArticleAdapter.ArticleViewHolder>(
    object : PagingDiffUtilCallback<DemoArticle>() {
        override fun areItemsTheSame(oldItem: DemoArticle, newItem: DemoArticle): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DemoArticle, newItem: DemoArticle): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_demo_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        getItem(position)?.let { article ->
            holder.bind(article, onArticleClick)
        }
    }

    class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val titleView = view.findViewById<TextView>(R.id.demoArticleTitle)
        private val summaryView = view.findViewById<TextView>(R.id.demoArticleSummary)
        private val metaView = view.findViewById<TextView>(R.id.demoArticleMeta)
        private val bookmarkView = view.findViewById<TextView>(R.id.demoArticleBookmark)

        fun bind(article: DemoArticle, onArticleClick: (DemoArticle) -> Unit) {
            titleView.text = if (article.isBookmarked) {
                "\u2605 ${article.title}"
            } else {
                article.title
            }
            summaryView.text = article.summary
            metaView.text = "ID ${article.id} • loaded from page ${article.page}"
            bookmarkView.text = if (article.isBookmarked) {
                "Tap to remove the local bookmark"
            } else {
                "Tap to bookmark this item locally"
            }
            itemView.setOnClickListener { onArticleClick(article) }
        }
    }
}

class DemoPagingStateAdapter(
    private val hasContentProvider: () -> Boolean,
    private val onRetryClick: () -> Unit,
) : PagingStateAdapter<DemoPagingStateAdapter.StateViewHolder>() {

    private var currentState: PagingUiState<*> = PagingUiState.Data(emptyList<Any>())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_demo_paging_state, parent, false)
        return StateViewHolder(view, onRetryClick)
    }

    override fun onBindViewHolder(holder: StateViewHolder, position: Int) {
        holder.bind(currentState)
    }

    override fun getItemCount(): Int {
        if (!hasContentProvider()) return 0
        return when (currentState) {
            is PagingUiState.Loading,
            is PagingUiState.Error -> 1
            is PagingUiState.Data -> 0
        }
    }

    override fun onStateUpdate(pagingUiState: PagingUiState<*>) {
        val hadRow = itemCount > 0
        currentState = pagingUiState
        val hasRow = itemCount > 0

        when {
            !hadRow && hasRow -> notifyItemInserted(0)
            hadRow && !hasRow -> notifyItemRemoved(0)
            hasRow -> notifyItemChanged(0)
        }
    }

    class StateViewHolder(
        view: View,
        onRetryClick: () -> Unit,
    ) : RecyclerView.ViewHolder(view) {

        private val progressView = view.findViewById<CircularProgressIndicator>(R.id.demoStateProgress)
        private val messageView = view.findViewById<TextView>(R.id.demoStateMessage)
        private val retryButton = view.findViewById<MaterialButton>(R.id.demoStateRetryButton)

        init {
            retryButton.setOnClickListener { onRetryClick() }
        }

        fun bind(state: PagingUiState<*>) {
            when (state) {
                is PagingUiState.Loading -> {
                    progressView.isVisible = true
                    retryButton.isVisible = false
                    messageView.text = "Loading the next page..."
                }

                is PagingUiState.Error -> {
                    progressView.isVisible = false
                    retryButton.isVisible = true
                    messageView.text = state.error.message ?: "Unable to load the next page."
                }

                is PagingUiState.Data -> Unit
            }
        }
    }
}
