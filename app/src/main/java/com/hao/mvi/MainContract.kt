package com.hao.mvi

import com.hao.mvi.base.IViewEffect
import com.hao.mvi.base.IViewEvent
import com.hao.mvi.base.IViewState

data class MainState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : IViewState

sealed class MainEvent : IViewEvent {
    data object Increment : MainEvent()
    data object Decrement : MainEvent()
    data object Reset : MainEvent()
}

sealed class MainEffect : IViewEffect {
    data class ShowToast(val message: String) : MainEffect()
}
