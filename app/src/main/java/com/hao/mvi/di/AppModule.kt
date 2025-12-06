package com.hao.mvi.di

import com.hao.mvi.feature.counter.data.CounterRepository
import com.hao.mvi.feature.counter.data.CounterRepositoryImpl
import com.hao.mvi.feature.counter.domain.DecrementCounterUseCase
import com.hao.mvi.feature.counter.domain.IncrementCounterUseCase
import com.hao.mvi.feature.counter.domain.ResetCounterUseCase
import com.hao.mvi.feature.counter.presentation.CounterViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single<CounterRepository> { CounterRepositoryImpl() }
}

val domainModule = module {
    factory { IncrementCounterUseCase(get()) }
    factory { DecrementCounterUseCase(get()) }
    factory { ResetCounterUseCase(get()) }
}

val viewModelModule = module {
    viewModel { CounterViewModel(get(), get(), get()) }
}

val appModules = listOf(dataModule, domainModule, viewModelModule)
