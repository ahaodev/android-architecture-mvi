# Android MVI Architecture

[中文文档](README_CN.md)

Modern Android MVI architecture example with Jetpack Compose, Koin, and Navigation, following the [official Android architecture guide](https://developer.android.com/topic/architecture).

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Kotlin | 2.3.20 | Language |
| Compose BOM | 2026.03.01 | UI Framework |
| Material3 | BOM | Design System |
| Navigation Compose | 2.9.7 | Navigation |
| Koin | 4.2.0 | Dependency Injection |
| StateFlow | - | State Management |
| Gradle | 8.14.3 | Build System |
| AGP | 8.13.2 | Android Gradle Plugin |
| compileSdk / minSdk | 36 / 24 | Android SDK |

## Project Structure

```
com.hao.mvi/
│
├── MainActivity.kt                 # App entry, Compose + Navigation
├── App.kt                          # Koin initialization
│
├── core/                           # 🔧 Core layer (shared across features)
│   ├── base/                       # MVI infrastructure
│   │   ├── BaseViewModel.kt        # Generic ViewModel<State, Event>
│   │   ├── MviContract.kt          # IUiState / IUiEvent
│   │   └── UiState.kt              # Idle/Loading/Success/Error wrapper
│   ├── navigation/
│   │   ├── Screen.kt               # Route definitions
│   │   └── AppNavGraph.kt          # NavHost configuration
│   └── ui/theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── di/                             # 💉 Dependency Injection
│   └── AppModule.kt                # Koin modules
│
└── feature/                        # 📦 Feature modules
    ├── counter/
    │   ├── data/
    │   │   └── CounterRepository.kt
    │   ├── domain/
    │   │   └── CounterUseCases.kt
    │   └── presentation/
    │       ├── CounterContract.kt
    │       ├── CounterScreen.kt
    │       └── CounterViewModel.kt
    └── detail/
        └── presentation/
            └── DetailScreen.kt
```

## Architecture

### MVI Flow

```
User Action → Event → ViewModel.handleEvent() → setState() → StateFlow → UI recomposition
```

One-time events (toast messages, navigation) are modeled as nullable fields in UI State (e.g., `userMessage: String?`, `navigateToDetail: Int?`). The UI consumes them via `LaunchedEffect` and notifies the ViewModel to clear the field. This follows the [official recommendation](https://developer.android.com/topic/architecture/ui-layer/events) that "ViewModel events should always result in a UI state update."

### Layer Responsibilities

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **Core** | `core.base` | MVI abstractions (`IUiState`, `IUiEvent`, `UiState<T>`) |
| **Navigation** | `core.navigation` | Routes + NavGraph |
| **DI** | `di` | Koin module definitions |
| **Data** | `feature.*.data` | Repository interface + implementation |
| **Domain** | `feature.*.domain` | UseCase business logic |
| **Presentation** | `feature.*.presentation` | Screen + ViewModel + Contract |

## Key Components

### BaseViewModel

```kotlin
abstract class BaseViewModel<State : IUiState, Event : IUiEvent>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    abstract fun createInitialState(): State
    abstract fun handleEvent(event: Event)

    val uiState: StateFlow<State>
    val currentState: State

    fun sendEvent(event: Event)
    protected fun setState(reduce: State.() -> State)
}
```

### Contract Pattern

Each feature defines a contract file grouping UI State and Events:

```kotlin
// State — UI state, survives configuration changes
// Nullable fields for one-time events (consumed then cleared)
data class CounterUiState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val navigateToDetail: Int? = null
) : IUiState

// Event — user intents + consumption events
sealed class CounterEvent : IUiEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
    data object Reset : CounterEvent()
    data object NavigateToDetail : CounterEvent()
    data object UserMessageShown : CounterEvent()
    data object NavigationHandled : CounterEvent()
}
```

### Compose Screen Pattern

Each screen has two composables:

1. **Stateful wrapper** (e.g., `CounterScreen`) — connects to ViewModel via Koin, collects state with `collectAsStateWithLifecycle()`, handles transient state consumption via `LaunchedEffect`
2. **Stateless content** (e.g., `CounterContent`) — receives state and event callbacks as parameters, includes `@Preview`

### UiState Wrapper

Generic sealed class for async operations:

```kotlin
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}
```

## Adding New Feature

1. Create feature package:
```
feature/newfeature/
├── data/
│   └── NewFeatureRepository.kt     # Interface + implementation
├── domain/
│   └── NewFeatureUseCases.kt       # Use cases with operator fun invoke()
└── presentation/
    ├── NewFeatureContract.kt        # UiState + Event definitions
    ├── NewFeatureScreen.kt          # Stateful + Stateless composables
    └── NewFeatureViewModel.kt       # Extends BaseViewModel
```

2. Register in Koin (`di/AppModule.kt`) — `single` for repositories, `factory` for use cases, `viewModel` for ViewModels
3. Add route in `core/navigation/Screen.kt`
4. Add composable in `core/navigation/AppNavGraph.kt`

## Testing

- **Fake** implementations (not mocks) for repositories — see `FakeCounterRepository` in test sources
- ViewModel tests use `UnconfinedTestDispatcher` + `Dispatchers.setMain()` for coroutine control
- Test state by asserting on `viewModel.uiState.value` after sending events

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.hao.mvi.feature.counter.presentation.CounterViewModelTest"
```

## License

MIT
