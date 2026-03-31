package com.hao.mvi.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI architecture.
 *
 * All one-time events (navigation, messages) are modeled as nullable fields in [State]
 * and cleared after the UI consumes them, following the official Android architecture guide:
 * "ViewModel events should always result in a UI state update."
 *
 * @param State UI state — immutable data class implementing [IUiState]
 * @param Event User actions/intents — sealed class implementing [IUiEvent]
 */
abstract class BaseViewModel<State : IUiState, Event : IUiEvent>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val initialState: State by lazy { createInitialState() }

    abstract fun createInitialState(): State

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    val currentState: State get() = _uiState.value

    private val _event = MutableSharedFlow<Event>()

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
        _uiState.update { it.reduce() }
    }
}
