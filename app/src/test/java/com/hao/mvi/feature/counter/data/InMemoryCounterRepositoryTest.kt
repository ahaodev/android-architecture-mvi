package com.hao.mvi.feature.counter.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryCounterRepositoryTest {

    private val repository = InMemoryCounterRepository()

    @Test
    fun `increment returns current plus one`() = runTest {
        assertEquals(1, repository.increment(0))
        assertEquals(11, repository.increment(10))
        assertEquals(0, repository.increment(-1))
    }

    @Test
    fun `decrement returns current minus one`() = runTest {
        assertEquals(-1, repository.decrement(0))
        assertEquals(9, repository.decrement(10))
    }

    @Test
    fun `reset returns zero`() = runTest {
        assertEquals(0, repository.reset())
    }
}
