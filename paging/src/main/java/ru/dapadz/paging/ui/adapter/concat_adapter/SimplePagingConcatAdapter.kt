package ru.dapadz.paging.ui.adapter.concat_adapter

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.dapadz.paging.ui.adapter.paging_adapter.PagingAdapter
import ru.dapadz.paging.ui.adapter.state_adapter.PagingStateAdapter

/**
 * [PagingConcatAdapter] implementation that can manage optional header, footer, and state adapters.
 *
 * @param T the item type rendered by the content adapter
 * @param VH the [RecyclerView.ViewHolder] type used by the content adapter
 * @property contentAdapter the primary paging adapter
 */
class SimplePagingConcatAdapter<T, VH : ViewHolder>(
    override val contentAdapter: PagingAdapter<T, VH>
) : BasePagingConcatAdapter<T, VH>(contentAdapter) {

    private var isHeaderSetup = false
    private var isStateSetup = false
    private var isFooterSetup = false

    private val stateAdapter: PagingStateAdapter<*>?
        get() = adapter.adapters.find { it is PagingStateAdapter<*> } as? PagingStateAdapter

    /**
     * Forwards paging state updates to the optional state adapter on the main thread.
     */
    init {
        contentAdapter.setOnPagingUiStateUpdateListener {
            Handler(Looper.getMainLooper()).post {
                stateAdapter?.onStateUpdate(it)
            }
        }
    }

    /**
     * Adds a header adapter to the beginning of the concat chain.
     *
     * @param block factory that creates the header adapter
     */
    fun setupHeader(block: () -> RecyclerView.Adapter<*>) {
        val adapter = block.invoke()
        addAdapter(0, adapter)
        isHeaderSetup = true
    }

    /**
     * Removes the header adapter if one is currently attached.
     */
    fun removeHeaderAdapter() {
        if (isHeaderSetup) {
            removeAdapter(adapter.adapters.first())
            isHeaderSetup = false
        }
    }

    /**
     * Adds a state adapter that reacts to paging state changes.
     *
     * @param block factory that creates the state adapter
     */
    fun setupStateAdapter(block: () -> PagingStateAdapter<*>) {
        val adapter = block.invoke()
        val position = this.adapter.adapters.size
        addAdapter(position, adapter)
        isStateSetup = true
    }

    /**
     * Removes the state adapter if one is currently attached.
     */
    fun removeStateAdapter() {
        if (isStateSetup) {
            val adapter = adapter.adapters.find { it is PagingStateAdapter }
            removeAdapter(adapter as RecyclerView.Adapter<*>)
            isStateSetup = false
        }
    }

    /**
     * Adds a footer adapter to the concat chain.
     *
     * If a state adapter is present, the footer is inserted right before it.
     *
     * @param block factory that creates the footer adapter
     */
    fun setupFooterAdapter(block: () -> RecyclerView.Adapter<*>) {
        val adapter = block.invoke()
        if (isStateSetup) {
            addAdapter(this.adapter.adapters.size - 1, adapter)
        } else {
            addAdapter(this.adapter.adapters.size, adapter)
        }
        isFooterSetup = true
    }

    /**
     * Removes the footer adapter if one is currently attached.
     */
    fun removeFooterAdapter() {
        if (isFooterSetup) {
            if (isStateSetup) {
                val adapter = adapter.adapters.getOrNull(adapter.adapters.size - 2)
                removeAdapter(adapter as RecyclerView.Adapter)
            } else {
                val adapter = adapter.adapters.last()
                removeAdapter(adapter)
            }
            isFooterSetup = false
        }
    }

}
