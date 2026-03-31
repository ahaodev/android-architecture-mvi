# Copilot Instructions

## Build & Test

```bash
# Build
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.hao.mvi.feature.counter.presentation.CounterViewModelTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Lint check
./gradlew lintDebug
```

## Architecture

This project implements **MVI (Model-View-Intent)** with **Clean Architecture** layers using Jetpack Compose, following the [official Android architecture guide](https://developer.android.com/topic/architecture).

### Data Flow

```
User Action → Event → ViewModel.handleEvent() → setState() → StateFlow → UI recomposition
```

One-time events (toast messages, navigation) are modeled as nullable fields in UI State (e.g., `userMessage: String?`, `navigateToDetail: Int?`). The UI consumes them via `LaunchedEffect` and notifies the ViewModel to clear the field. There is no separate Effect/Channel mechanism — this follows the official recommendation that "ViewModel events should always result in a UI state update."

### Layer Structure per Feature

Each feature under `feature/` follows this package structure:

- `data/` — Repository interface + implementation (named descriptively, e.g., `InMemoryCounterRepository`)
- `domain/` — Use case classes with `operator fun invoke()`
- `presentation/` — Contract (UiState/Event), ViewModel, and Compose Screen

### BaseViewModel

All ViewModels extend `BaseViewModel<State, Event>` from `core/base/`. Subclasses must implement:

- `createInitialState(): State`
- `handleEvent(event: Event)`

State updates use a reducer pattern: `setState { copy(count = count + 1) }`.

### MVI Contract Pattern

Each feature defines a contract file grouping:
- A `data class ${Feature}UiState(...) : IUiState` — includes transient fields for one-time events
- A `sealed class ${Feature}Event : IUiEvent` — includes consumption events like `UserMessageShown`, `NavigationHandled`

### Compose Screen Pattern

Each screen has two composables:

1. **Stateful wrapper** (e.g., `CounterScreen`) — connects to ViewModel via Koin, collects state with `collectAsStateWithLifecycle()`, handles transient state consumption via `LaunchedEffect`
2. **Stateless content** (e.g., `CounterContent`) — receives state and event callbacks as parameters, includes `@Preview`

### Navigation

Routes are defined as sealed class entries in `core/navigation/Screen.kt` with type-safe argument passing. The nav graph is configured in `AppNavGraph.kt`.

### Dependency Injection

Koin modules are defined in `di/AppModule.kt` and initialized in `App.kt`:

- `single` scope for repositories
- `factory` scope for use cases
- `viewModel` scope for ViewModels

All dependencies use constructor injection.

## Adding a New Feature

1. Create `feature/<name>/` with `data/`, `domain/`, `presentation/` sub-packages
2. Define the Contract file with UiState (including transient event fields) and Event types
3. Implement Repository, UseCases, and ViewModel
4. Create the Compose screen with stateful/stateless split
5. Register all components in `di/AppModule.kt`
6. Add route to `core/navigation/Screen.kt` and composable to `AppNavGraph.kt`

## Testing

- Use **Fake** implementations (not mocks) for repositories — see `FakeCounterRepository` in test sources
- ViewModel tests use `UnconfinedTestDispatcher` + `Dispatchers.setMain()` for coroutine control
- Test state by asserting on `viewModel.uiState.value` after sending events

## Key Conventions

- **No android.util.Log in ViewModels** — keeps ViewModels pure-JVM testable
- **State collection**: Use `collectAsStateWithLifecycle()`, not `collectAsState()`
- **State naming**: `${Feature}UiState`, exposed as `uiState` property
- **Repository naming**: Descriptive names (e.g., `InMemoryCounterRepository`), Fakes prefixed with `Fake`
- **Package naming**: `com.hao.mvi` root, features are `feature.<name>.<layer>`
- **Kotlin**: Target JVM 17, min SDK 24, compile SDK 36
