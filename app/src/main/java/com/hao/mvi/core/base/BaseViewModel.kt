package com.hao.mvi.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI architecture
 * @param State UI state
 * @param Event User actions/intents
 * @param Effect One-time side effects
 */
abstract class BaseViewModel<State : IViewState, Event : IViewEvent, Effect : IViewEffect>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val initialState: State by lazy { createInitialState() }

    abstract fun createInitialState(): State

    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    val currentState: State get() = _viewState.value

    private val _event = MutableSharedFlow<Event>()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch(mainDispatcher) {
            _event.collect { event ->
                handleEvent(event)
            }
        }
    }

    abstract fun handleEvent(event: Event)

    fun sendEvent(event: Event) {
        viewModelScope.launch(mainDispatcher) {
            _event.emit(event)
        }
    }

    protected fun setState(reduce: State.() -> State) {
        _viewState.update { it.reduce() }
    }

    protected fun setEffect(effect: Effect) {
        viewModelScope.launch(mainDispatcher) {
            _effect.send(effect)
        }
    }
}
