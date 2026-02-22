package com.matedroid.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver entry point for the car home screen widget.
 * Registered in AndroidManifest.xml with the APPWIDGET_UPDATE action.
 */
class CarWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CarWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Schedule background updates when the first widget instance is added
        CarWidgetUpdateWorker.schedulePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel background updates when the last widget instance is removed
        CarWidgetUpdateWorker.cancelPeriodicWork(context)
    }
}
