package com.hao.mvi.feature.counter.domain

import com.hao.mvi.feature.counter.data.CounterRepository

/**
 * Use case for counter operations - domain layer
 */
class IncrementCounterUseCase(private val repository: CounterRepository) {
    suspend operator fun invoke(current: Int): Int = repository.increment(current)
}

class DecrementCounterUseCase(private val repository: CounterRepository) {
    suspend operator fun invoke(current: Int): Int = repository.decrement(current)
}

class ResetCounterUseCase(private val repository: CounterRepository) {
    suspend operator fun invoke(): Int = repository.reset()
}
