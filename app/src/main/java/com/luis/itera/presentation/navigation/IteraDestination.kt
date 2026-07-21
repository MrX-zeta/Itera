package com.luis.itera.presentation.navigation

sealed class IteraDestination(val route: String) {
    data object ActiveWorkout : IteraDestination("active_workout")
    data object History : IteraDestination("history")
    data object Hydration : IteraDestination("hydration")
    data object Onboarding : IteraDestination("onboarding")
    data object SessionDetail : IteraDestination("session_detail/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"
        fun buildRoute(sessionId: Long) = "session_detail/$sessionId"
    }
    data object Statistics : IteraDestination("statistics")
    data object Settings : IteraDestination("settings")

    /** Pestaña de gestión de rutinas (CRUD). */
    data object Routines : IteraDestination("routines")

    /** Editor de rutina: crea si no hay id, edita si lo hay. */
    data object RoutineEditor : IteraDestination("routine_editor?routineId={routineId}") {
        const val ARG_ROUTINE_ID = "routineId"
        fun buildRoute(routineId: Long? = null): String =
            if (routineId == null) "routine_editor" else "routine_editor?routineId=$routineId"
    }
}