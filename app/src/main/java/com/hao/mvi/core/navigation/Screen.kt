package com.hao.mvi.core.navigation

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Detail : Screen("detail/{count}") {
        fun createRoute(count: Int) = "detail/$count"
    }
}
