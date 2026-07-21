package com.luis.itera.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class WidgetPinResult { ALREADY_PINNED, REQUESTED, UNSUPPORTED }

@Singleton
class WidgetPinner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Anclado con idempotencia real (getAppWidgetIds): comprueba si el widget YA está
     * anclado antes de pedir el diálogo. Única vía de anclaje (botón de Ajustes); ya
     * no hay disparo automático tras el onboarding.
     */
    fun requestPinWithStatus(): WidgetPinResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return WidgetPinResult.UNSUPPORTED
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, IteraWidgetReceiver::class.java)
        if (manager.getAppWidgetIds(provider).isNotEmpty()) return WidgetPinResult.ALREADY_PINNED
        if (!manager.isRequestPinAppWidgetSupported) return WidgetPinResult.UNSUPPORTED
        return if (manager.requestPinAppWidget(provider, null, null)) {
            WidgetPinResult.REQUESTED
        } else {
            WidgetPinResult.UNSUPPORTED
        }
    }
}