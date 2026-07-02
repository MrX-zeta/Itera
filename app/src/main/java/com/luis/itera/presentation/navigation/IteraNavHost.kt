package com.luis.itera.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luis.itera.R
import com.luis.itera.presentation.active_workout.ActiveWorkoutScreen
import com.luis.itera.presentation.history.HistoryScreen
import com.luis.itera.presentation.hydration.HydrationScreen
import com.luis.itera.presentation.session_detail.SessionDetailScreen
import com.luis.itera.presentation.statistics.StatisticsScreen
import com.luis.itera.presentation.theme.IteraColors

private data class NavItem(
    val destination: IteraDestination,
    val iconRes: Int
)

private val navItems = listOf(
    NavItem(IteraDestination.ActiveWorkout, R.drawable.ic_barbell),
    NavItem(IteraDestination.History, R.drawable.ic_calendar),
    NavItem(IteraDestination.Statistics, R.drawable.ic_stats),
    NavItem(IteraDestination.Hydration, R.drawable.ic_droplet)
)

private fun IteraDestination.ownsRoute(route: String?): Boolean = when (this) {
    IteraDestination.History ->
        route == IteraDestination.History.route || route == IteraDestination.SessionDetail.route
    else -> route == this.route
}

@Composable
fun IteraNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = IteraColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = IteraColors.Background,
                tonalElevation = 0.dp
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = item.destination.ownsRoute(currentRoute),
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(item.iconRes),
                                contentDescription = item.destination.route,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IteraColors.Accent,
                            unselectedIconColor = IteraColors.TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = IteraDestination.ActiveWorkout.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(IteraDestination.ActiveWorkout.route) {
                ActiveWorkoutScreen(
                    onSessionFinished = { sessionId ->
                        navController.navigate(IteraDestination.SessionDetail.buildRoute(sessionId))
                    },
                    onLastSessionClick = { sessionId ->
                        navController.navigate(IteraDestination.SessionDetail.buildRoute(sessionId))
                    },
                    onHydrationClick = {
                        navController.navigate(IteraDestination.Hydration.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(IteraDestination.History.route) {
                HistoryScreen(
                    onSessionClick = { sessionId ->
                        navController.navigate(IteraDestination.SessionDetail.buildRoute(sessionId))
                    }
                )
            }
            composable(IteraDestination.Hydration.route) { HydrationScreen() }
            composable(
                route = IteraDestination.SessionDetail.route,
                arguments = listOf(
                    navArgument(IteraDestination.SessionDetail.ARG_SESSION_ID) { type = NavType.LongType }
                )
            ) {
                SessionDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(IteraDestination.Statistics.route) { StatisticsScreen() }
        }
    }
}