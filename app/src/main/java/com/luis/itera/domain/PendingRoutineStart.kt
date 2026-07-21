package com.luis.itera.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordina el arranque de una rutina desde la pestaña Rutinas hacia la sesión activa, que vive
 * en la pestaña Entrenamiento. Evita duplicar la ruta de inicio (lo que provocaba parpadeos):
 * la pestaña Rutinas hace un cambio de pestaña normal y deja aquí el id + la intención de volver.
 *
 * - [startEvents]: id de la rutina a arrancar. Lo colecta el ViewModel de Entrenamiento (vivo).
 * - [returnToRoutines]: si el "atrás" en Entrenamiento debe volver a Rutinas (no salir a Home).
 */
@Singleton
class PendingRoutineStart @Inject constructor() {

    private val _startEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val startEvents: SharedFlow<Long> = _startEvents.asSharedFlow()

    private val _returnToRoutines = MutableStateFlow(false)
    val returnToRoutines: StateFlow<Boolean> = _returnToRoutines.asStateFlow()

    /** La pestaña Rutinas pide arrancar la rutina y marca que el atrás debe volver a Rutinas. */
    fun requestStart(id: Long) {
        _startEvents.tryEmit(id)
        _returnToRoutines.value = true
    }

    fun disarmReturn() { _returnToRoutines.value = false }
}
