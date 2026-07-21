package com.luis.itera.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.luis.itera.presentation.onboarding.OnboardingScreen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.luis.itera.presentation.routines.RoutineEditorScreen
import com.luis.itera.presentation.routines.RoutinesScreen
import com.luis.itera.presentation.session_detail.SessionDetailScreen
import com.luis.itera.presentation.settings.SettingsScreen
import com.luis.itera.presentation.statistics.StatisticsScreen
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent

private data class NavItem(
    val destination: IteraDestination,
    val iconRes: Int
)

private val navItems = listOf(
    NavItem(IteraDestination.ActiveWorkout, R.drawable.ic_barbell),
    NavItem(IteraDestination.Routines, R.drawable.ic_bookmark),
    NavItem(IteraDestination.History, R.drawable.ic_calendar),
    NavItem(IteraDestination.Statistics, R.drawable.ic_stats),
    NavItem(IteraDestination.Hydration, R.drawable.ic_droplet)
)

private fun IteraDestination.ownsRoute(route: String?): Boolean = when (this) {
    IteraDestination.ActiveWorkout ->
        route == IteraDestination.ActiveWorkout.route || route == IteraDestination.SessionDetail.route
    IteraDestination.History ->
        route == IteraDestination.History.route
    IteraDestination.Routines ->
        route == IteraDestination.Routines.route || route?.startsWith("routine_editor") == true
    else -> route == this.route
}

@Composable
fun IteraNavHost(
    onboardingCompleted: Boolean,
    onOnboardingDone: () -> Unit,
    deepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute != IteraDestination.Onboarding.route &&
        currentRoute != IteraDestination.Settings.route &&
        currentRoute?.startsWith("routine_editor") != true

    // Navega al destino pedido por el widget (p. ej. hidratación) una vez que la
    // app ya pasó el onboarding.
    LaunchedEffect(deepLinkRoute, onboardingCompleted) {
        if (deepLinkRoute != null && onboardingCompleted) {
            navController.navigate(deepLinkRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onDeepLinkHandled()
        }
    }

    Scaffold(
        containerColor = IteraColors.Background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = IteraColors.BorderStrong)
                    NavigationBar(
                        containerColor = IteraColors.Background,
                        tonalElevation = 0.dp
                    ) {
                    navItems.forEach { item ->
                        val selected = item.destination.ownsRoute(currentRoute)
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "tab_scale"
                        )
                        val iconAlpha by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.6f,
                            label = "tab_alpha"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute == IteraDestination.SessionDetail.route) {
                                    navController.popBackStack(
                                        route = item.destination.route,
                                        inclusive = false
                                    )
                                }
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
                                    modifier = Modifier
                                        .size(30.dp)
                                        .scale(iconScale)
                                        .graphicsLayer { alpha = iconAlpha }
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalAccent.current.color,
                                unselectedIconColor = IteraColors.TextSecondary,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) IteraDestination.ActiveWorkout.route else IteraDestination.Onboarding.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(IteraDestination.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        onOnboardingDone()
                        navController.navigate(IteraDestination.ActiveWorkout.route) {
                            popUpTo(IteraDestination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
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
                    },
                    onSettingsClick = {
                        navController.navigate(IteraDestination.Settings.route)
                    },
                    onSeeAllRoutinesClick = {
                        navController.navigate(IteraDestination.Routines.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    // Volver a Rutinas (cambio de pestaña) cuando la sesión se arrancó desde ahí.
                    onBackToRoutines = {
                        navController.navigate(IteraDestination.Routines.route) {
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
            composable(IteraDestination.Statistics.route) { StatisticsScreen() }
            composable(IteraDestination.Hydration.route) { HydrationScreen() }
            composable(IteraDestination.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(IteraDestination.Routines.route) {
                RoutinesScreen(
                    onCreate = { navController.navigate(IteraDestination.RoutineEditor.buildRoute()) },
                    onEdit = { id -> navController.navigate(IteraDestination.RoutineEditor.buildRoute(id)) },
                    // Cambia a la pestaña Entrenamiento (sin duplicar la ruta de inicio → sin flash).
                    // El ViewModel de Entrenamiento arranca la rutina; el atrás vuelve aquí.
                    onStart = {
                        navController.navigate(IteraDestination.ActiveWorkout.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = IteraDestination.RoutineEditor.route,
                arguments = listOf(
                    navArgument(IteraDestination.RoutineEditor.ARG_ROUTINE_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                RoutineEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = IteraDestination.SessionDetail.route,
                arguments = listOf(
                    navArgument(IteraDestination.SessionDetail.ARG_SESSION_ID) { type = NavType.LongType }
                )
            ) {
                SessionDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}