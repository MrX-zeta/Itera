package com.luis.itera.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import com.luis.itera.presentation.components.rememberReduceMotion
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

// 80dp del NavigationBar (default de Material3) + 1dp del HorizontalDivider. Hardcodeado
// porque NavigationBarDefaults no expone ese alto; solo se usa para animar la altura
// reservada de la barra sin tener que medirla (ver comentario en IteraNavHost).
private val BottomBarHeight = 81.dp

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

    // Duración ÚNICA para la barra y el fade del NavHost, en ambos sentidos. Con
    // entrada/salida asimétricas (220/160 antes), al volver de Ajustes ésta ya
    // había desaparecido (160ms) mientras Main+barra seguían llegando a opacidad
    // completa (220ms) — ese margen de 60ms "todavía terminando" es lo que se
    // sentía como falta de fluidez. Con una sola duración, contenido y barra
    // arrancan y terminan exactamente juntos en los dos sentidos.
    val reduceMotion = rememberReduceMotion()
    val enterMs = if (reduceMotion) 0 else 200
    val exitMs = enterMs

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
            // La barra queda SIEMPRE compuesta (nunca se dispone/reconstruye al entrar o
            // salir de Ajustes/detalle de sesión) — solo se anima su alto, opacidad y
            // desplazamiento. Antes, con AnimatedVisibility, ocultarla la retiraba de la
            // composición del todo; al volver, Compose debía reconstruir NavigationBar y
            // sus 5 iconos desde cero justo cuando debía empezar a animarse, y ese trabajo
            // se notaba como un pequeño retraso antes de aparecer/desaparecer (el mismo tipo
            // de "recompose en frío" que ya resolvimos en el pager con beyondViewportPageCount).
            val progress by animateFloatAsState(
                targetValue = if (showBottomBar) 1f else 0f,
                animationSpec = tween(if (showBottomBar) enterMs else exitMs, easing = FastOutSlowInEasing),
                label = "bottom_bar_visibility"
            )
            // El alto NO sigue a `progress` (eso forzaba un remeasure del Scaffold en CADA
            // frame de la animación — trabajo de layout compitiendo con el fundido, y es lo
            // que se sentía como un tirón/lentitud al salir de Ajustes). El espacio se
            // reserva de golpe mientras dura la animación y colapsa a 0 solo al terminar
            // (ambos saltos ocurren con la barra ya invisible, así que no se ven). El
            // movimiento real —lo único que anima frame a frame— vive solo en graphicsLayer
            // (alpha + traslación), que es trabajo de dibujo, no de layout.
            val barHeightPx = with(LocalDensity.current) { BottomBarHeight.toPx() }
            val reserveSpace = showBottomBar || progress > 0f
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(if (reserveSpace) BottomBarHeight else 0.dp)
                    .clipToBounds()
                    .graphicsLayer {
                        alpha = progress
                        translationY = (1f - progress) * barHeightPx
                    }
            ) {
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) IteraDestination.Main.route else IteraDestination.Onboarding.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            enterTransition = { fadeIn(tween(enterMs, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(exitMs, easing = FastOutSlowInEasing)) }
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
            TAB_HYDRATION -> HydrationScreen(onSettingsClick = onSettings)
        }
    }

    // Atrás desde una pestaña que no es Entrenamiento → vuelve a Entrenamiento (patrón habitual).
    // En Entrenamiento (page 0, en reposo) se deshabilita para que actúen el handler de la sesión
    // o el back del sistema. DOS sutilezas deliberadas:
    // 1) Va DESPUÉS del HorizontalPager: se registra al final y por eso GANA prioridad sobre los
    //    BackHandlers de las páginas (siempre compuestas por beyondViewportPageCount) — sin esto,
    //    un back en Historial con sesión viva la descartaría en vez de volver a Entrenamiento.
    // 2) CARRERA: durante un slide programático (goToTab, tween 350ms) currentPage vale 0 en parte
    //    del trayecto aunque en pantalla se vea otra pestaña; un gesto en esa ventana no encontraba
    //    ningún callback habilitado y CERRABA la app. targetPage + isScrollInProgress cubren toda
    //    la animación: el back en pleno slide re-apunta a Entrenamiento, inofensivo.
    BackHandler(
        enabled = pagerState.currentPage != TAB_ACTIVE_WORKOUT ||
            pagerState.targetPage != TAB_ACTIVE_WORKOUT ||
            pagerState.isScrollInProgress
    ) {
        goToTab(TAB_ACTIVE_WORKOUT)
    }
}
