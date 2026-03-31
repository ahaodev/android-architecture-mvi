package com.hao.mvi.feature.counter.data

/**
 * Fake implementation of [CounterRepository] for unit testing.
 * Uses immediate in-memory computation with no delays.
 */
class FakeCounterRepository : CounterRepository {

    var shouldThrow: Boolean = false

    override suspend fun increment(current: Int): Int {
        if (shouldThrow) throw RuntimeException("Fake increment error")
        return current + 1
    }

    override suspend fun decrement(current: Int): Int {
        if (shouldThrow) throw RuntimeException("Fake decrement error")
        return current - 1
    }

    override suspend fun reset(): Int {
        if (shouldThrow) throw RuntimeException("Fake reset error")
        return 0
    }
}
