package com.hao.mvi.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hao.mvi.feature.counter.presentation.CounterScreen
import com.hao.mvi.feature.detail.presentation.DetailScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Main.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Main.route) {
            CounterScreen(
                onNavigateToDetail = { count ->
                    navController.navigate(Screen.Detail.createRoute(count))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("count") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val count = backStackEntry.arguments?.getInt("count") ?: 0
            DetailScreen(
                count = count,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
