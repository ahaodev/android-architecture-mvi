# Android MVI 架构

[English](README.md)

现代 Android MVI 架构示例，基于 Jetpack Compose、Koin 和 Navigation，遵循 [Android 官方架构指南](https://developer.android.com/topic/architecture)。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.3.20 | 编程语言 |
| Compose BOM | 2026.03.01 | UI 框架 |
| Material3 | BOM | 设计系统 |
| Navigation Compose | 2.9.7 | 页面导航 |
| Koin | 4.2.0 | 依赖注入 |
| StateFlow | - | 状态管理 |
| Gradle | 8.14.3 | 构建系统 |
| AGP | 8.13.2 | Android Gradle 插件 |
| compileSdk / minSdk | 36 / 24 | Android SDK |

## 项目结构

```
com.hao.mvi/
│
├── MainActivity.kt                 # App 入口，Compose + Navigation
├── App.kt                          # Koin 初始化
│
├── core/                           # 🔧 核心层（跨 feature 共享）
│   ├── base/                       # MVI 基础设施
│   │   ├── BaseViewModel.kt        # 泛型 ViewModel<State, Event>
│   │   ├── MviContract.kt          # IUiState / IUiEvent
│   │   └── UiState.kt              # Idle/Loading/Success/Error 封装
│   ├── navigation/
│   │   ├── Screen.kt               # 路由定义
│   │   └── AppNavGraph.kt          # NavHost 配置
│   └── ui/theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── di/                             # 💉 依赖注入
│   └── AppModule.kt                # Koin 模块
│
└── feature/                        # 📦 功能模块
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

## 架构

### MVI 数据流

```
用户操作 → Event → ViewModel.handleEvent() → setState() → StateFlow → UI 重组
```

一次性事件（Toast 消息、页面导航）通过 UI State 中的可空字段建模（例如 `userMessage: String?`、`navigateToDetail: Int?`）。UI 层通过 `LaunchedEffect` 消费事件后通知 ViewModel 清除该字段。这遵循了 [官方建议](https://developer.android.com/topic/architecture/ui-layer/events)："ViewModel 事件应始终产生 UI 状态更新。"

### 各层职责

| 层级 | 包路径 | 职责 |
|------|--------|------|
| **核心层** | `core.base` | MVI 抽象（`IUiState`、`IUiEvent`、`UiState<T>`） |
| **导航层** | `core.navigation` | 路由 + NavGraph |
| **依赖注入** | `di` | Koin 模块定义 |
| **数据层** | `feature.*.data` | Repository 接口 + 实现 |
| **领域层** | `feature.*.domain` | UseCase 业务逻辑 |
| **表现层** | `feature.*.presentation` | Screen + ViewModel + Contract |

## 核心组件

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

### Contract 模式

每个 feature 定义一个 Contract 文件，包含 UI State 和 Event：

```kotlin
// State — UI 状态，配置变更后保留
// 可空字段用于一次性事件（消费后清除）
data class CounterUiState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val navigateToDetail: Int? = null
) : IUiState

// Event — 用户意图 + 消费事件
sealed class CounterEvent : IUiEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
    data object Reset : CounterEvent()
    data object NavigateToDetail : CounterEvent()
    data object UserMessageShown : CounterEvent()
    data object NavigationHandled : CounterEvent()
}
```

### Compose Screen 模式

每个页面包含两个 Composable：

1. **有状态包装器**（如 `CounterScreen`）— 通过 Koin 连接 ViewModel，使用 `collectAsStateWithLifecycle()` 收集状态，通过 `LaunchedEffect` 消费一次性事件
2. **无状态内容**（如 `CounterContent`）— 接收状态和事件回调作为参数，包含 `@Preview`

### UiState 封装

用于异步操作的泛型密封类：

```kotlin
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}
```

## 添加新功能模块

1. 创建功能包：
```
feature/newfeature/
├── data/
│   └── NewFeatureRepository.kt     # 接口 + 实现
├── domain/
│   └── NewFeatureUseCases.kt       # 用例，使用 operator fun invoke()
└── presentation/
    ├── NewFeatureContract.kt        # UiState + Event 定义
    ├── NewFeatureScreen.kt          # 有状态 + 无状态 Composable
    └── NewFeatureViewModel.kt       # 继承 BaseViewModel
```

2. 在 Koin 中注册（`di/AppModule.kt`）— `single` 用于 Repository，`factory` 用于 UseCase，`viewModel` 用于 ViewModel
3. 在 `core/navigation/Screen.kt` 添加路由
4. 在 `core/navigation/AppNavGraph.kt` 添加 composable

## 测试

- 使用 **Fake** 实现（而非 Mock）— 参见测试源码中的 `FakeCounterRepository`
- ViewModel 测试使用 `UnconfinedTestDispatcher` + `Dispatchers.setMain()` 控制协程
- 通过发送事件后断言 `viewModel.uiState.value` 来测试状态

```bash
# 运行所有单元测试
./gradlew testDebugUnitTest

# 运行单个测试类
./gradlew testDebugUnitTest --tests "com.hao.mvi.feature.counter.presentation.CounterViewModelTest"
```

## 许可证

MIT
