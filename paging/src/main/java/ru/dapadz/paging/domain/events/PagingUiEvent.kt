package ru.dapadz.paging.domain.events

/**
 * Commands sent from the UI layer to a [PagingSource].
 */
sealed class PagingUiEvent {

    /**
     * Clears cached items and emits an empty data state.
     */
    data object Clear : PagingUiEvent()

    /**
     * Requests the next page when one is available.
     */
    data object LoadMore : PagingUiEvent()

    /**
     * Repeats the most recent page request.
     */
    data object RepeatLast : PagingUiEvent()

    /**
     * Refreshes the list by optionally clearing cached items and then repeating the last request.
     *
     * @param clear whether the current cached list should be cleared before the request is repeated
     */
    data class Refresh(val clear: Boolean = true) : PagingUiEvent()
}
