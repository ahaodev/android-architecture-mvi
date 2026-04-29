package com.hao.mvi.feature.counter.presentation

import com.hao.mvi.core.base.IUiEvent
import com.hao.mvi.core.base.IUiState
import java.util.UUID

data class NavigationEvent(
    val count: Int,
    val id: String = UUID.randomUUID().toString()
)

data class CounterUiState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val navigateToDetail: NavigationEvent? = null
) : IUiState

sealed class CounterEvent : IUiEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
    data object Reset : CounterEvent()
    data object NavigateToDetail : CounterEvent()
    data object UserMessageShown : CounterEvent()
    data object NavigationHandled : CounterEvent()
}
