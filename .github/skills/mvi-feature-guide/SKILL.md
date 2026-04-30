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

    // Key on navEvent.id (not the payload) so LaunchedEffect re-fires when
    // the same destination is navigated to twice in a row.
    state.navigateTo{Target}?.let { navEvent ->
        LaunchedEffect(navEvent.id) {
            onNavigateTo{Target}(navEvent.{arg})
            viewModel.sendEvent({Name}Event.NavigationHandled)
        }
    }

    // Key on message content — a new toast text = new LaunchedEffect execution.
    state.userMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.sendEvent({Name}Event.UserMessageShown)
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
- For simple destination screens with no business logic, skip the ViewModel entirely — just accept data as `@Composable fun {Name}Screen(data: Type, onBack: () -> Unit)` (see `DetailScreen`).

---

## 5. Transient Event Pattern

One-time events (toast, snackbar, navigation) follow a **set → consume → clear** cycle:

```
ViewModel sets field         →  setState { copy(userMessage = "Saved!") }
UI consumes via LaunchedEffect  →  LaunchedEffect(message) { showToast(); sendEvent(Shown) }
ViewModel clears field       →  setState { copy(userMessage = null) }
```

`LaunchedEffect(key)` fires once per unique key. When the field resets to `null`, `?.let` doesn't execute, preventing re-triggers.

**Choosing the LaunchedEffect key:**

- `userMessage: String?` → key on the message string. Different messages = different toasts; same message repeated = fires again (acceptable).
- `navigateTo{Screen}: {Name}NavigationEvent?` → key on `navEvent.id`. This ensures navigation fires even when the same destination is requested twice (same payload, different UUID).

**Quick reference:**

| Intent | State field | LaunchedEffect key |
|--------|-------------|-------------------|
| Toast / Snackbar | `userMessage: String? = null` | `message` |
| Navigate with arg | `navigateTo{Screen}: {Name}NavigationEvent? = null` | `navEvent.id` |
| Navigate back | `navigateBack: {Name}NavigationEvent? = null` | `navEvent.id` |
| Loading spinner | `isLoading: Boolean = false` | *(not transient)* |
| Error display | `errorMessage: String? = null` | `errorMessage` |
| Form input | `fieldName: String = ""` | *(not transient)* |
| Validation error | `fieldError: String? = null` | `fieldError` |

> **Why a wrapper for navigation?** If you store the raw destination value and the user navigates to the same screen twice with identical arguments, `LaunchedEffect` won't re-fire because the key hasn't changed. Wrapping in a data class with `id = UUID.randomUUID().toString()` guarantees each navigation emission is a distinct key.

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

**UseCases** — pure Kotlin, one action per class, `operator fun invoke()`. Group related UseCases for the same feature in a single file:

```kotlin
// {Name}UseCases.kt
class Get{Name}DataUseCase(private val repository: {Name}Repository) {
    suspend operator fun invoke(): List<Item> = repository.getData()
}

class Save{Name}DataUseCase(private val repository: {Name}Repository) {
    suspend operator fun invoke(item: Item) = repository.save(item)
}
```

**DI** — add to `di/AppModule.kt`:

```kotlin
single<{Name}Repository> { InMemory{Name}Repository() }  // dataModule
factory { Get{Name}DataUseCase(get()) }                   // domainModule
factory { Save{Name}DataUseCase(get()) }
viewModel { {Name}ViewModel(get(), get()) }               // viewModelModule
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

    // Verify the navigation wrapper carries a unique id on repeated triggers
    @Test fun `navigation emits distinct ids on repeated requests`() {
        viewModel.sendEvent({Name}Event.NavigateToDetail)
        val first = viewModel.currentState.navigateTo{Target}?.id
        viewModel.sendEvent({Name}Event.NavigationHandled)
        viewModel.sendEvent({Name}Event.NavigateToDetail)
        val second = viewModel.currentState.navigateTo{Target}?.id
        assertNotEquals(first, second)
    }
}
```

---

## 8. Anti-Patterns

| ❌ Don't | Why | ✅ Do instead |
|----------|-----|--------------|
| `Channel<Effect>` / `SharedFlow` for VM→UI events | Lost on config change | Nullable state field + cleanup event |
| `sealed class` for UiState | Can't `copy()` | `data class` with defaults |
| `LaunchedEffect(navArg)` for navigation | Won't re-fire if same arg emitted twice | Wrap in `NavigationEvent(arg, id = UUID)`, key on `id` |
| Navigation without cleanup event | Re-triggers on recomposition | `sendEvent(NavigationHandled)` |
| Android types (`Bitmap`, `Context`) in UiState | Untestable, may leak Activity | Primitive/serializable types only |
| `Mutex` in every ViewModel | Unnecessary serialization of unrelated work | Add Mutex only when rapid repeated events race on `currentState` |

---

## 9. Checklist

- [ ] UiState is `data class` with defaults; Event is `sealed class`
- [ ] Navigation uses `NavigationEvent(arg, id = UUID.randomUUID().toString())`; LaunchedEffect keyed on `.id`
- [ ] ViewModel: no `android.*` imports, uses `setState { copy() }`
- [ ] Mutex added only if events race on `currentState` under rapid input
- [ ] Screen: `collectAsStateWithLifecycle()`, transient state via `LaunchedEffect`
- [ ] Repository interface + descriptive implementation name
- [ ] UseCases: pure Kotlin, `operator fun invoke()`, grouped in one file per feature
- [ ] DI registered in `AppModule.kt`
- [ ] Tests with `UnconfinedTestDispatcher` and Fake implementations
- [ ] Repeated navigation test confirms distinct ids
- [ ] `./gradlew assembleDebug && ./gradlew testDebugUnitTest` passes
