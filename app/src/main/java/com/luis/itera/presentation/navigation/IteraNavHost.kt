package com.luis.itera.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

// Orden de las pestañas = orden de las páginas del pager. El índice ES la página.
private data class TabItem(val iconRes: Int, val label: String)

private val TAB_ACTIVE_WORKOUT = 0
private val TAB_ROUTINES = 1
private val TAB_HISTORY = 2
private val TAB_STATISTICS = 3
private val TAB_HYDRATION = 4

private val tabs = listOf(
    TabItem(R.drawable.ic_barbell, "Entrenamiento"),
    TabItem(R.drawable.ic_clipboard_list, "Rutinas"),
    TabItem(R.drawable.ic_calendar, "Historial"),
    TabItem(R.drawable.ic_stats, "Estadísticas"),
    TabItem(R.drawable.ic_droplet, "Hidratación")
)

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
    val pagerState = rememberPagerState(initialPage = TAB_ACTIVE_WORKOUT, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val onMain = currentRoute == IteraDestination.Main.route
    // La barra inferior se ve en las pestañas (Main) y en el detalle de sesión.
    val showBottomBar = onMain || currentRoute == IteraDestination.SessionDetail.route

    // Deep link del widget (p. ej. hidratación): abre la pestaña correspondiente en el pager.
    LaunchedEffect(deepLinkRoute, onboardingCompleted) {
        if (deepLinkRoute != null && onboardingCompleted) {
            val idx = when (deepLinkRoute) {
                IteraDestination.Hydration.route -> TAB_HYDRATION
                IteraDestination.History.route -> TAB_HISTORY
                IteraDestination.Statistics.route -> TAB_STATISTICS
                IteraDestination.Routines.route -> TAB_ROUTINES
                else -> TAB_ACTIVE_WORKOUT
            }
            pagerState.scrollToPage(idx)
            onDeepLinkHandled()
        }
    }

    Scaffold(
        containerColor = IteraColors.Background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = IteraColors.BorderStrong)
                    NavigationBar(containerColor = IteraColors.Background, tonalElevation = 0.dp) {
                        tabs.forEachIndexed { index, item ->
                            // El pager conserva su página aunque el detalle de sesión esté encima.
                            val selected = pagerState.currentPage == index
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
                                    // Si hay un detalle encima, vuelve a las pestañas y desliza.
                                    if (!onMain) {
                                        navController.popBackStack(IteraDestination.Main.route, inclusive = false)
                                    }
                                    scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(350)) }
                                },
                                icon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(item.iconRes),
                                        contentDescription = item.label,
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
            startDestination = if (onboardingCompleted) IteraDestination.Main.route else IteraDestination.Onboarding.route,
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
                        navController.navigate(IteraDestination.Main.route) {
                            popUpTo(IteraDestination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(IteraDestination.Main.route) {
                MainTabsPager(
                    pagerState = pagerState,
                    // Duración explícita: el spring por defecto puede completar demasiado
                    // rápido en distancias cortas (1 página), leyéndose casi como un corte.
                    goToTab = { idx -> scope.launch { pagerState.animateScrollToPage(idx, animationSpec = tween(350)) } },
                    onSessionDetail = { id -> navController.navigate(IteraDestination.SessionDetail.buildRoute(id)) },
                    onSettings = { navController.navigate(IteraDestination.Settings.route) },
                    onCreateRoutine = { navController.navigate(IteraDestination.RoutineEditor.buildRoute()) },
                    onEditRoutine = { id -> navController.navigate(IteraDestination.RoutineEditor.buildRoute(id)) }
                )
            }
            composable(IteraDestination.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
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

/**
 * Las 5 pestañas en un [HorizontalPager] deslizable. El cambio de pestaña del flujo play/atrás
 * (que tanto costó estabilizar) se hace con [goToTab] (scroll del pager), conservando la lógica
 * intacta: la sesión arranca vía el coordinador y el atrás/"←" vuelve a Rutinas.
 */
@Composable
private fun MainTabsPager(
    pagerState: PagerState,
    goToTab: (Int) -> Unit,
    onSessionDetail: (Long) -> Unit,
    onSettings: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (Long) -> Unit
) {
    // Atrás desde una pestaña que no es Entrenamiento → vuelve a Entrenamiento (patrón habitual).
    // En Entrenamiento (page 0) se deshabilita para que actúe el back propio de la sesión / sistema.
    BackHandler(enabled = pagerState.currentPage != TAB_ACTIVE_WORKOUT) {
        goToTab(TAB_ACTIVE_WORKOUT)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // Mantiene TODAS las pestañas compuestas aunque no sean la visible (por defecto el
        // pager solo compone la actual). Sin esto, volver a una pestaña "fría" la recompone
        // desde cero al vuelo — un instante de fondo vacío/negro antes de que llegue el
        // contenido real, justo lo que se reportó al volver a Rutinas y a Entrenamiento.
        beyondViewportPageCount = tabs.size
    ) { page ->
        when (page) {
            TAB_ACTIVE_WORKOUT -> ActiveWorkoutScreen(
                onSessionFinished = onSessionDetail,
                onLastSessionClick = onSessionDetail,
                onHydrationClick = { goToTab(TAB_HYDRATION) },
                onSettingsClick = onSettings,
                onSeeAllRoutinesClick = { goToTab(TAB_ROUTINES) },
                // El atrás de la sesión de rutina desliza de vuelta a Rutinas (misma lógica de antes).
                onBackToRoutines = { goToTab(TAB_ROUTINES) }
            )
            TAB_ROUTINES -> RoutinesScreen(
                onCreate = onCreateRoutine,
                onEdit = onEditRoutine,
                // Play → desliza a Entrenamiento; el ViewModel arranca la rutina (guard anti-flash intacto).
                onStart = { goToTab(TAB_ACTIVE_WORKOUT) }
            )
            TAB_HISTORY -> HistoryScreen(onSessionClick = onSessionDetail)
            TAB_STATISTICS -> StatisticsScreen()
            TAB_HYDRATION -> HydrationScreen()
        }
    }
}
