package com.hao.mvi.feature.counter.presentation

import androidx.lifecycle.viewModelScope
import com.hao.mvi.core.base.BaseViewModel
import com.hao.mvi.feature.counter.domain.DecrementCounterUseCase
import com.hao.mvi.feature.counter.domain.IncrementCounterUseCase
import com.hao.mvi.feature.counter.domain.ResetCounterUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CounterViewModel(
    private val incrementUseCase: IncrementCounterUseCase,
    private val decrementUseCase: DecrementCounterUseCase,
    private val resetUseCase: ResetCounterUseCase
) : BaseViewModel<CounterUiState, CounterEvent>() {

    private val mutex = Mutex()

    override fun createInitialState(): CounterUiState = CounterUiState()

    override fun handleEvent(event: CounterEvent) {
        when (event) {
            is CounterEvent.Increment -> increment()
            is CounterEvent.Decrement -> decrement()
            is CounterEvent.Reset -> reset()
            is CounterEvent.NavigateToDetail -> navigateToDetail()
            is CounterEvent.UserMessageShown -> setState { copy(userMessage = null) }
            is CounterEvent.NavigationHandled -> setState { copy(navigateToDetail = null) }
        }
    }

    private fun increment() {
        viewModelScope.launch {
            mutex.withLock {
                setState { copy(isLoading = true) }
                val newCount = incrementUseCase(currentState.count)
                setState {
                    copy(
                        count = newCount,
                        isLoading = false,
                        userMessage = if (newCount % 5 == 0) "Count reached $newCount!" else null
                    )
                }
            }
        }
    }

    private fun decrement() {
        viewModelScope.launch {
            mutex.withLock {
                val newCount = decrementUseCase(currentState.count)
                setState { copy(count = newCount) }
            }
        }
    }

    private fun reset() {
        viewModelScope.launch {
            mutex.withLock {
                val newCount = resetUseCase()
                setState { copy(count = newCount, userMessage = "Counter reset!") }
            }
        }
    }

    private fun navigateToDetail() {
        setState { copy(navigateToDetail = NavigationEvent(count = currentState.count)) }
    }
}
