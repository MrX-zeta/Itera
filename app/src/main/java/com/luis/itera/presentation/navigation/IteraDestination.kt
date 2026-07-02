package com.luis.itera.presentation.navigation

sealed class IteraDestination(val route: String) {
    data object ActiveWorkout : IteraDestination("active_workout")
    data object History : IteraDestination("history")
    data object Hydration : IteraDestination("hydration")
}