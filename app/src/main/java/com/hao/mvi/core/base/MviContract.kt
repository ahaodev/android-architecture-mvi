package com.hao.mvi.core.base

/**
 * Base interface for UI State - represents the current state of the screen.
 * Implementations should be immutable data classes named ${Feature}UiState.
 */
interface IUiState

/**
 * Base interface for UI Event - user actions/intents.
 * Implementations should be sealed classes named ${Feature}Event.
 */
interface IUiEvent
