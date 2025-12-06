# Android MVI Architecture

[中文文档](README_CN.md)

Modern Android MVI architecture example with Jetpack Compose, Koin, and Navigation.

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Kotlin | 1.9.20 | Language |
| Compose BOM | 2023.10.01 | UI Framework |
| Material3 | BOM | Design System |
| Navigation Compose | 2.7.6 | Navigation |
| Koin | 3.5.0 | Dependency Injection |
| StateFlow | - | State Management |
| Gradle KTS | 8.5 | Build System |

## Project Structure

```
com.hao.mvi/
│
├── MainActivity.kt                 # App entry, Compose + Navigation
├── MviApplication.kt               # Koin initialization
│
├── core/                           # 🔧 Core layer (shared across features)
│   ├── base/                       # MVI infrastructure
│   │   ├── BaseViewModel.kt        # Generic ViewModel<State, Event, Effect>
│   │   ├── MviContract.kt          # IViewState / IViewEvent / IViewEffect
│   │   ├── ObserveAsEvents.kt      # Lifecycle-safe Effect collector
│   │   └── UiState.kt              # Loading/Success/Error wrapper
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
User Action → Event → ViewModel.handleEvent() → setState() → State → UI
                                              ↘ setEffect() → Effect → Toast/Navigation
```

### Layer Responsibilities

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **Core** | `core.base` | MVI abstractions |
| **Navigation** | `core.navigation` | Routes + NavGraph |
| **DI** | `di` | Koin module definitions |
| **Data** | `feature.*.data` | Repository interface + impl |
| **Domain** | `feature.*.domain` | UseCase business logic |
| **Presentation** | `feature.*.presentation` | Screen + ViewModel + Contract |

## Key Components

### BaseViewModel

```kotlin
abstract class BaseViewModel<State : IViewState, Event : IViewEvent, Effect : IViewEffect>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    abstract fun createInitialState(): State
    abstract fun handleEvent(event: Event)

    protected fun setState(reduce: State.() -> State)
    protected fun setEffect(effect: Effect)
}
```

### Contract Pattern

```kotlin
// State - UI state, survives configuration changes
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : IViewState

// Event - User intents
sealed class CounterEvent : IViewEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
}

// Effect - One-time side effects
sealed class CounterEffect : IViewEffect {
    data class ShowToast(val message: String) : CounterEffect()
    data class NavigateToDetail(val count: Int) : CounterEffect()
}
```

### Lifecycle-safe Effect Collection

```kotlin
@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(onEvent)
        }
    }
}
```

## Adding New Feature

1. Create feature package:
```
feature/newfeature/
├── data/
│   └── NewFeatureRepository.kt
├── domain/
│   └── NewFeatureUseCases.kt
└── presentation/
    ├── NewFeatureContract.kt
    ├── NewFeatureScreen.kt
    └── NewFeatureViewModel.kt
```

2. Register in Koin (`di/AppModule.kt`)
3. Add route in `core/navigation/Screen.kt`
4. Add composable in `core/navigation/AppNavGraph.kt`

## License

MIT
