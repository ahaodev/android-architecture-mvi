package com.hao.mvi.core.base

/**
 * Generic UI state wrapper for async operations
 */
sealed class UiState<out T> {
    
    data object Idle : UiState<Nothing>()
    
    data object Loading : UiState<Nothing>()
    
    data class Success<T>(val data: T) : UiState<T>()
    
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): UiState<R> = when (this) {
        is Idle -> Idle
        is Loading -> Loading
        is Success -> Success(transform(data))
        is Error -> Error(message, throwable)
    }
}

/**
 * Execute a suspending block and wrap result in UiState
 */
suspend fun <T> runCatchingToUiState(block: suspend () -> T): UiState<T> {
    return try {
        UiState.Success(block())
    } catch (e: Exception) {
        UiState.Error(e.message ?: "Unknown error", e)
    }
}
