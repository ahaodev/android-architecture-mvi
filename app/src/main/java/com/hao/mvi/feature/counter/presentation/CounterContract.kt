package com.hao.mvi.feature.counter.presentation

import com.hao.mvi.core.base.IViewEffect
import com.hao.mvi.core.base.IViewEvent
import com.hao.mvi.core.base.IViewState

data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : IViewState

sealed class CounterEvent : IViewEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
    data object Reset : CounterEvent()
    data object NavigateToDetail : CounterEvent()
}

sealed class CounterEffect : IViewEffect {
    data class ShowToast(val message: String) : CounterEffect()
    data class NavigateToDetail(val count: Int) : CounterEffect()
}
