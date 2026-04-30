---
name: mvi-feature-guide
description: Complete guide for building features in this MVI architecture template. Scaffolds new features (Contract, ViewModel, Screen, Repository, UseCase, DI, tests) and explains UiState modeling and transient event handling. Use this skill whenever the user wants to add a new feature/screen/page, asks about UiState design, handling navigation or toast/snackbar from ViewModel, modeling loading/error states, or any MVI question in this project. Trigger on phrases like "add a login screen", "create settings feature", "how to show a toast", "how to navigate from ViewModel", "should state be sealed class or data class".
---

# MVI Feature Guide

Build features following the official Android architecture guideline: **"ViewModel events should always result in a UI state update."**

Data flow: **UI → Event → ViewModel → State → UI** (unidirectional, no Effect channel).

---

## Examples in this codebase

| File | Demonstrates |
|------|-------------|
| `feature/counter/presentation/CounterContract.kt` | NavigationEvent wrapper, transient message field |
| `feature/counter/presentation/CounterViewModel.kt` | Mutex for rapid-fire events, setState pattern |
| `feature/counter/presentation/CounterScreen.kt` | LaunchedEffect keyed on wrapper id, stateful/stateless split |
| `feature/counter/domain/CounterUseCases.kt` | Multiple UseCases grouped in one file |
| `feature/detail/presentation/DetailScreen.kt` | Pure UI destination — no ViewModel needed |

---

## 1. Feature Structure

**Stateful feature** (has its own business logic and state):

```
feature/{name}/
├── data/{Name}Repository.kt         # Interface + descriptive implementation
├── domain/{Name}UseCases.kt         # One or more UseCases grouped per feature
└── presentation/
    ├── {Name}Contract.kt            # UiState + Event
    ├── {Name}ViewModel.kt           # Extends BaseViewModel
    └── {Name}Screen.kt              # Composable UI
```

**Simple destination screen** (receives data as params, no business logic):

```
feature/{name}/presentation/{Name}Screen.kt   # Pure composable only
```

Also create matching test files under `test/` and register dependencies in `di/AppModule.kt`.

---

## 2. Contract — `{Name}Contract.kt`

```kotlin
// Navigation wrapper — UUID ensures LaunchedEffect re-fires even if the
// destination/payload is identical to the previous navigation.
data class {Name}NavigationEvent(
    val {arg}: {ArgType},
    val id: String = UUID.randomUUID().toString()
)

data class {Name}UiState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    // Transient fields — nullable, default null, cleared after UI consumes
    val userMessage: String? = null,
    val navigateTo{Target}: {Name}NavigationEvent? = null
) : IUiState

sealed class {Name}Event : IUiEvent {
    data object Load : {Name}Event()
    // One cleanup event per transient field
    data object UserMessageShown : {Name}Event()
    data object NavigationHandled : {Name}Event()
}
```

- UiState must be a **data class** (not sealed class) — `setState { copy() }` depends on it.
- Every field needs a default so `createInitialState()` works with no args.
- Navigation uses a **wrapper with `id: String = UUID.randomUUID().toString()`** so `LaunchedEffect` fires even when the same payload is emitted twice in a row.
- One-time events (toast, navigation) are nullable state fields, not a Channel — see §5.

---

## 3. ViewModel — `{Name}ViewModel.kt`

```kotlin
class {Name}ViewModel(
    private val someUseCase: Some{Name}UseCase  // inject UseCases, not Repos
) : BaseViewModel<{Name}UiState, {Name}Event>() {

    // Only needed when events read currentState, do async work, then write
    // derived state — e.g., rapid Increment/Decrement buttons that race.
    private val mutex = Mutex()

    override fun createInitialState(): {Name}UiState = {Name}UiState()

    override fun handleEvent(event: {Name}Event) {
        when (event) {
            is {Name}Event.Load -> loadData()
            is {Name}Event.UserMessageShown -> setState { copy(userMessage = null) }
            is {Name}Event.NavigationHandled -> setState { copy(navigateTo{Target} = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val result = someUseCase()
            setState { copy(isLoading = false, items = result) }
        }
    }
}
```

- **No `android.*` imports** — keeps ViewModel pure-JVM testable.
- `setState { copy(...) }` for all mutations, `currentState` for reads.
- Add a `Mutex` only when an event does `currentState → async → setState` and can race under rapid repeated input. Don't add it by default.

---

## 4. Screen — `{Name}Screen.kt`

Split into stateful wrapper (connects ViewModel) + stateless content (previewable):

```kotlin
@Composable
fun {Name}Screen(
    viewModel: {Name}ViewModel = koinViewModel(),
    onNavigateTo{Target}: ({ArgType}) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Consume transient message (see §5 for pattern explanation)
    state.userMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.sendEvent({Name}Event.UserMessageShown)
        }
    }

    // Consume transient navigation
    state.navigateTo{Target}?.let { arg ->
        LaunchedEffect(arg) {
            onNavigateTo{Target}(arg)
            viewModel.sendEvent({Name}Event.NavigationHandled)
        }
    }

    {Name}Content(state = state, onLoad = { viewModel.sendEvent({Name}Event.Load) })
}

@Composable
fun {Name}Content(state: {Name}UiState, onLoad: () -> Unit) {
    // Pure UI — no ViewModel reference
}
```

- Content composable takes only state + lambdas → previewable & testable.
- Navigation via **lambda params**, not hardcoded `NavController`.

---

## 5. Transient Event Pattern

One-time events (toast, snackbar, navigation) follow a **set → consume → clear** cycle:

```
ViewModel sets field        →  setState { copy(userMessage = "Saved!") }
UI consumes via LaunchedEffect  →  LaunchedEffect(message) { showToast(); sendEvent(Shown) }
ViewModel clears field      →  setState { copy(userMessage = null) }
```

`LaunchedEffect(key)` fires once per unique key. When field resets to `null`, `?.let` doesn't execute, preventing re-triggers.

**Quick reference:**

| Intent | State field |
|--------|-------------|
| Toast / Snackbar | `userMessage: String? = null` |
| Navigate with arg | `navigateTo{Screen}: {ArgType}? = null` |
| Navigate back | `navigateBack: Boolean? = null` |
| Loading spinner | `isLoading: Boolean = false` |
| Error display | `errorMessage: String? = null` |
| Form input | `fieldName: String = ""` |
| Validation error | `fieldError: String? = null` |

---

## 6. Data & Domain Layer

**Repository** — always interface + descriptive implementation name (never `...Impl`):

```kotlin
interface {Name}Repository {
    suspend fun getData(): List<Item>
}

class InMemory{Name}Repository : {Name}Repository {
    override suspend fun getData(): List<Item> { /* ... */ }
}
```

**UseCase** — pure Kotlin, one action, `operator fun invoke()`:

```kotlin
class Get{Name}DataUseCase(private val repository: {Name}Repository) {
    suspend operator fun invoke(): List<Item> = repository.getData()
}
```

**DI** — add to `di/AppModule.kt`:

```kotlin
single<{Name}Repository> { InMemory{Name}Repository() }  // dataModule
factory { Get{Name}DataUseCase(get()) }                   // domainModule
viewModel { {Name}ViewModel(get()) }                      // viewModelModule
```

---

## 7. Testing

Use `UnconfinedTestDispatcher` + Fake implementations (preferred over mocks):

```kotlin
class {Name}ViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: {Name}ViewModel

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = {Name}ViewModel(Get{Name}DataUseCase(Fake{Name}Repository()))
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initial state is correct`() =
        assertEquals({Name}UiState(), viewModel.currentState)

    @Test fun `transient message set then cleared`() {
        viewModel.sendEvent({Name}Event.TriggerMessage)
        assertNotNull(viewModel.currentState.userMessage)
        viewModel.sendEvent({Name}Event.UserMessageShown)
        assertNull(viewModel.currentState.userMessage)
    }
}
```

---

## 8. Anti-Patterns

| ❌ Don't | Why | ✅ Do instead |
|----------|-----|--------------|
| `Channel<Effect>` / `SharedFlow` for VM→UI events | Lost on config change | Nullable state field + cleanup event |
| `sealed class` for UiState | Can't `copy()` | `data class` with defaults |
| Navigation without cleanup event | Re-triggers on recomposition | `sendEvent(NavigationHandled)` |
| Android types (`Bitmap`, `Context`) in UiState | Untestable, may leak Activity | Primitive/serializable types only |

---

## 9. Checklist

- [ ] UiState is `data class` with defaults; Event is `sealed class`
- [ ] ViewModel: no `android.*` imports, uses `setState { copy() }`
- [ ] Screen: `collectAsStateWithLifecycle()`, transient state via `LaunchedEffect`
- [ ] Repository interface + descriptive implementation name
- [ ] UseCases: pure Kotlin, `operator fun invoke()`
- [ ] DI registered in `AppModule.kt`
- [ ] Tests with `UnconfinedTestDispatcher` and Fake implementations
- [ ] `./gradlew assembleDebug && ./gradlew test` passes
