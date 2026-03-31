package com.hao.mvi.core.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {

    @Test
    fun `Idle state properties`() {
        val state: UiState<String> = UiState.Idle
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertNull(state.getOrNull())
    }

    @Test
    fun `Loading state properties`() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertNull(state.getOrNull())
    }

    @Test
    fun `Success state properties`() {
        val state: UiState<String> = UiState.Success("hello")
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isError)
        assertEquals("hello", state.getOrNull())
    }

    @Test
    fun `Error state properties`() {
        val error = RuntimeException("fail")
        val state: UiState<String> = UiState.Error("fail", error)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isError)
        assertNull(state.getOrNull())
    }

    @Test
    fun `map transforms Success data`() {
        val state = UiState.Success(42)
        val mapped = state.map { it.toString() }
        assertEquals(UiState.Success("42"), mapped)
    }

    @Test
    fun `map preserves Idle`() {
        val state: UiState<Int> = UiState.Idle
        val mapped = state.map { it.toString() }
        assertEquals(UiState.Idle, mapped)
    }

    @Test
    fun `map preserves Loading`() {
        val state: UiState<Int> = UiState.Loading
        val mapped = state.map { it.toString() }
        assertEquals(UiState.Loading, mapped)
    }

    @Test
    fun `map preserves Error`() {
        val state: UiState<Int> = UiState.Error("oops")
        val mapped = state.map { it.toString() }
        assertTrue(mapped.isError)
        assertEquals("oops", (mapped as UiState.Error).message)
    }
}
