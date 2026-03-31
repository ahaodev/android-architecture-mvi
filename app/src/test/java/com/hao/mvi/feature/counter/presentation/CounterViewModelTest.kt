package com.hao.mvi.feature.counter.presentation

import com.hao.mvi.feature.counter.data.FakeCounterRepository
import com.hao.mvi.feature.counter.domain.DecrementCounterUseCase
import com.hao.mvi.feature.counter.domain.IncrementCounterUseCase
import com.hao.mvi.feature.counter.domain.ResetCounterUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeCounterRepository()

    private lateinit var viewModel: CounterViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CounterViewModel(
            incrementUseCase = IncrementCounterUseCase(fakeRepository),
            decrementUseCase = DecrementCounterUseCase(fakeRepository),
            resetUseCase = ResetCounterUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has zero count`() {
        val state = viewModel.uiState.value
        assertEquals(0, state.count)
        assertEquals(false, state.isLoading)
        assertNull(state.userMessage)
        assertNull(state.navigateToDetail)
    }

    @Test
    fun `increment event increases count by one`() = runTest {
        viewModel.sendEvent(CounterEvent.Increment)

        assertEquals(1, viewModel.uiState.value.count)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `decrement event decreases count by one`() = runTest {
        viewModel.sendEvent(CounterEvent.Increment)
        viewModel.sendEvent(CounterEvent.Decrement)

        assertEquals(0, viewModel.uiState.value.count)
    }

    @Test
    fun `reset event sets count to zero`() = runTest {
        viewModel.sendEvent(CounterEvent.Increment)
        viewModel.sendEvent(CounterEvent.Increment)
        viewModel.sendEvent(CounterEvent.Reset)

        assertEquals(0, viewModel.uiState.value.count)
    }

    @Test
    fun `reset event shows user message`() = runTest {
        viewModel.sendEvent(CounterEvent.Reset)

        assertEquals("Counter reset!", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `increment to milestone shows user message`() = runTest {
        repeat(5) { viewModel.sendEvent(CounterEvent.Increment) }

        assertEquals("Count reached 5!", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `increment to non-milestone does not show user message`() = runTest {
        repeat(3) { viewModel.sendEvent(CounterEvent.Increment) }

        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `UserMessageShown event clears user message`() = runTest {
        viewModel.sendEvent(CounterEvent.Reset)
        assertNotNull(viewModel.uiState.value.userMessage)

        viewModel.sendEvent(CounterEvent.UserMessageShown)
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `NavigateToDetail event sets navigateToDetail in state`() = runTest {
        viewModel.sendEvent(CounterEvent.Increment)
        viewModel.sendEvent(CounterEvent.Increment)
        viewModel.sendEvent(CounterEvent.NavigateToDetail)

        assertEquals(2, viewModel.uiState.value.navigateToDetail)
    }

    @Test
    fun `NavigationHandled event clears navigateToDetail`() = runTest {
        viewModel.sendEvent(CounterEvent.NavigateToDetail)
        assertNotNull(viewModel.uiState.value.navigateToDetail)

        viewModel.sendEvent(CounterEvent.NavigationHandled)
        assertNull(viewModel.uiState.value.navigateToDetail)
    }

    @Test
    fun `multiple increments accumulate correctly`() = runTest {
        repeat(10) { viewModel.sendEvent(CounterEvent.Increment) }

        assertEquals(10, viewModel.uiState.value.count)
    }

    @Test
    fun `decrement below zero works`() = runTest {
        viewModel.sendEvent(CounterEvent.Decrement)

        assertEquals(-1, viewModel.uiState.value.count)
    }
}
