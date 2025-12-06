package com.hao.mvi.feature.counter.presentation

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.hao.mvi.core.base.BaseViewModel
import com.hao.mvi.feature.counter.domain.DecrementCounterUseCase
import com.hao.mvi.feature.counter.domain.IncrementCounterUseCase
import com.hao.mvi.feature.counter.domain.ResetCounterUseCase
import kotlinx.coroutines.launch

class CounterViewModel(
    private val incrementUseCase: IncrementCounterUseCase,
    private val decrementUseCase: DecrementCounterUseCase,
    private val resetUseCase: ResetCounterUseCase
) : BaseViewModel<CounterState, CounterEvent, CounterEffect>() {

    companion object {
        private const val TAG = "CounterViewModel"
    }

    override fun createInitialState(): CounterState = CounterState()

    override fun handleEvent(event: CounterEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            is CounterEvent.Increment -> increment()
            is CounterEvent.Decrement -> decrement()
            is CounterEvent.Reset -> reset()
            is CounterEvent.NavigateToDetail -> navigateToDetail()
        }
    }

    private fun increment() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val newCount = incrementUseCase(currentState.count)
            setState { copy(count = newCount, isLoading = false) }
            if (newCount % 5 == 0) {
                setEffect(CounterEffect.ShowToast("Count reached $newCount!"))
            }
        }
    }

    private fun decrement() {
        viewModelScope.launch {
            val newCount = decrementUseCase(currentState.count)
            setState { copy(count = newCount) }
        }
    }

    private fun reset() {
        viewModelScope.launch {
            val newCount = resetUseCase()
            setState { copy(count = newCount) }
            setEffect(CounterEffect.ShowToast("Counter reset!"))
        }
    }

    private fun navigateToDetail() {
        setEffect(CounterEffect.NavigateToDetail(currentState.count))
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: ${currentState.count}")
    }
}
