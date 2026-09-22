package com.serranoie.app.minus.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver

private const val TAG = "GlanceWidgetIds"

/**
 * Returns the [GlanceId]s of the widgets currently bound to [receiver], resolved straight from
 * [AppWidgetManager] by the receiver's [ComponentName].
 *
 * [GlanceAppWidgetManager.getGlanceIds] resolves receivers through a persisted map keyed by the
 * *class name* of the [GlanceAppWidget]. In release builds R8 can horizontally merge our widget
 * classes (they are all shape-identical), so several widgets end up sharing one class name and
 * that lookup returns ids that belong to other receivers — calling `update()` with one of those
 * ids renders a widget inside another widget's slot. Binding through the receiver component is
 * immune to that.
 *
 */
internal suspend fun boundGlanceIds(
    context: Context,
    receiver: Class<out GlanceAppWidgetReceiver>,
    widget: Class<out GlanceAppWidget>,
): List<GlanceId> {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val glanceManager = GlanceAppWidgetManager(context)
    val boundIds = appWidgetManager.getAppWidgetIds(ComponentName(context, receiver)).toList()

    val glanceLookupIds = glanceManager.getGlanceIds(widget).map { glanceManager.getAppWidgetId(it) }
    if (glanceLookupIds.toSet() != boundIds.toSet()) {
        Log.w(
            TAG,
            "Glance id lookup for ${widget.name} disagrees with ${receiver.simpleName} binding: " +
                "glance=$glanceLookupIds bound=$boundIds. Using the receiver binding."
        )
    }

    return boundIds.mapNotNull { appWidgetId ->
        runCatching { glanceManager.getGlanceIdBy(appWidgetId) }.getOrNull()
    }
}
