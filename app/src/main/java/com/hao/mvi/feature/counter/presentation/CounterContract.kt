package com.hao.mvi.feature.counter.presentation

import com.hao.mvi.core.base.IUiEvent
import com.hao.mvi.core.base.IUiState

data class CounterUiState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val navigateToDetail: Int? = null
) : IUiState

sealed class CounterEvent : IUiEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
    data object Reset : CounterEvent()
    data object NavigateToDetail : CounterEvent()
    data object UserMessageShown : CounterEvent()
    data object NavigationHandled : CounterEvent()
}
