package com.hao.mvi

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.hao.mvi.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel<MainState, MainEvent, MainEffect>() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    override fun createInitialState(): MainState = MainState()

    override fun handleEvent(event: MainEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            is MainEvent.Increment -> increment()
            is MainEvent.Decrement -> decrement()
            is MainEvent.Reset -> reset()
        }
    }

    private fun increment() {
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true) }
            delay(100) // Simulate async work
            setState { copy(count = count + 1, isLoading = false) }
            if (currentState.count % 5 == 0) {
                setEffect(MainEffect.ShowToast("Count reached ${currentState.count}!"))
            }
        }
    }

    private fun decrement() {
        setState { copy(count = count - 1) }
    }

    private fun reset() {
        setState { copy(count = 0) }
        setEffect(MainEffect.ShowToast("Counter reset!"))
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: ${currentState.count}")
    }
}
