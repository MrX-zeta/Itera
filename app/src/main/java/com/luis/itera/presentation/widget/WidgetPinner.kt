package com.luis.itera.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetPinner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun requestPin(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, IteraWidgetReceiver::class.java)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(provider, null, null)
    }
}