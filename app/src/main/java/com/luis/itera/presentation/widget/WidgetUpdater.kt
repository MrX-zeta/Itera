package com.luis.itera.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Punto único de actualización del widget. Los ViewModels llaman a [refresh]
 * (fire-and-forget) tras cada cambio; aquí se agrupan las ráfagas con un
 * [debounce] y se emite un solo [updateAll] con el estado final.
 *
 * El debounce importa porque MIUI/AppWidgetHost limita cuántas veces por segundo
 * se aplica una actualización de widget: varios updateAll seguidos hacían que se
 * descartaran casi todos y a veces ganara un estado intermedio.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    init {
        scope.launch { collectRequests() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun collectRequests() {
        requests.debounce(250).collect {
            // Nunca dejamos escapar la excepción: un updateAll fallido no debe
            // matar el colector y congelar futuras actualizaciones.
            try {
                IteraWidget().updateAll(context)
            } catch (e: Exception) {
                Log.w("WidgetUpdater", "updateAll falló", e)
            }
        }
    }

    /** Solicita un refresco. No suspende; agrupa llamadas seguidas en una sola. */
    fun refresh() {
        requests.tryEmit(Unit)
    }
}
