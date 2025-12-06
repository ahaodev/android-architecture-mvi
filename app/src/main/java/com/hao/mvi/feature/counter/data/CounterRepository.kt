package com.hao.mvi.feature.counter.data

import kotlinx.coroutines.delay

/**
 * Counter repository - data layer
 */
interface CounterRepository {
    suspend fun increment(current: Int): Int
    suspend fun decrement(current: Int): Int
    suspend fun reset(): Int
}

class CounterRepositoryImpl : CounterRepository {
    
    override suspend fun increment(current: Int): Int {
        delay(100) // Simulate network/db delay
        return current + 1
    }

    override suspend fun decrement(current: Int): Int {
        return current - 1
    }

    override suspend fun reset(): Int {
        return 0
    }
}
