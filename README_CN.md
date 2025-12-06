# Android MVI 架构

现代 Android MVI 架构示例，基于 Jetpack Compose、Koin 和 Navigation。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9.20 | 编程语言 |
| Compose BOM | 2023.10.01 | UI 框架 |
| Material3 | BOM | 设计系统 |
| Navigation Compose | 2.7.6 | 页面导航 |
| Koin | 3.5.0 | 依赖注入 |
| StateFlow | - | 状态管理 |
| Gradle KTS | 8.5 | 构建系统 |

## 项目结构

```
com.hao.mvi/
│
├── MainActivity.kt                 # App 入口，Compose + Navigation
├── MviApplication.kt               # Koin 初始化
│
├── core/                           # 🔧 核心层（跨 feature 共享）
│   ├── base/                       # MVI 基础设施
│   │   ├── BaseViewModel.kt        # 泛型 ViewModel<State, Event, Effect>
│   │   ├── MviContract.kt          # IViewState / IViewEvent / IViewEffect
│   │   ├── ObserveAsEvents.kt      # 生命周期安全的 Effect 收集器
│   │   └── UiState.kt              # Loading/Success/Error 封装
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
用户操作 → Event → ViewModel.handleEvent() → setState() → State → UI 更新
                                           ↘ setEffect() → Effect → Toast/导航
```

### 各层职责

| 层级 | 包路径 | 职责 |
|------|--------|------|
| **核心层** | `core.base` | MVI 抽象 |
| **导航层** | `core.navigation` | 路由 + NavGraph |
| **依赖注入** | `di` | Koin 模块定义 |
| **数据层** | `feature.*.data` | Repository 接口 + 实现 |
| **领域层** | `feature.*.domain` | UseCase 业务逻辑 |
| **表现层** | `feature.*.presentation` | Screen + ViewModel + Contract |

## 核心组件

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

### Contract 模式

```kotlin
// State - UI 状态，配置变更后保留
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : IViewState

// Event - 用户意图
sealed class CounterEvent : IViewEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
}

// Effect - 一次性副作用
sealed class CounterEffect : IViewEffect {
    data class ShowToast(val message: String) : CounterEffect()
    data class NavigateToDetail(val count: Int) : CounterEffect()
}
```

### 生命周期安全的 Effect 收集

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

## 添加新功能模块

1. 创建功能包：
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

2. 在 Koin 中注册（`di/AppModule.kt`）
3. 在 `core/navigation/Screen.kt` 添加路由
4. 在 `core/navigation/AppNavGraph.kt` 添加 composable

## 许可证

MIT
