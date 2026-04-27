package ru.dapadz.paging.ui.adapter.state_adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.dapadz.paging.domain.state.PagingUiState

/**
 * Base adapter used to render paging-specific UI state inside a [RecyclerView].
 *
 * Typical implementations show loading, error, or retry rows inside a [ConcatAdapter].
 *
 * @param VH the [RecyclerView.ViewHolder] type used by the adapter
 */
abstract class PagingStateAdapter<VH : ViewHolder> : RecyclerView.Adapter<VH>() {

    /**
     * Called when the paging UI state changes.
     *
     * @param pagingUiState updated paging state
     */
    abstract fun onStateUpdate(pagingUiState: PagingUiState<*>)
}
