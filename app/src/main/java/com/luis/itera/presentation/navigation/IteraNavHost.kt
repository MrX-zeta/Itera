package com.luis.itera.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luis.itera.R
import androidx.compose.ui.unit.dp
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.active_workout.ActiveWorkoutScreen
import com.luis.itera.presentation.hydration.HydrationScreen
import com.luis.itera.presentation.history.HistoryScreen

private data class NavItem(
    val destination: IteraDestination,
    val iconRes: Int
)

private val navItems = listOf(
    NavItem(IteraDestination.ActiveWorkout, R.drawable.ic_barbell),
    NavItem(IteraDestination.History, R.drawable.ic_calendar),
    NavItem(IteraDestination.Hydration, R.drawable.ic_droplet)
)

@Composable
fun IteraNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = IteraColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = IteraColors.Background,
                tonalElevation = 0.dp
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == item.destination.route } == true
                    NavigationBarItem(
                        selected = selected,
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
                                contentDescription = item.destination.route
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
            composable(IteraDestination.ActiveWorkout.route) { }
            composable(IteraDestination.History.route) { }
            composable(IteraDestination.Hydration.route) { }
            composable(IteraDestination.ActiveWorkout.route) { ActiveWorkoutScreen() }
            composable(IteraDestination.Hydration.route) { HydrationScreen() }
            composable(IteraDestination.History.route) { HistoryScreen() }
        }
    }
}