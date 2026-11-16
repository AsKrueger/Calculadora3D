package com.tdcostmanager.app.core

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.tdcostmanager.app.MainActivity
import com.tdcostmanager.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Widget de pantalla de inicio: la impresión en marcha con su cuenta atrás (la mueve el sistema,
 * sin gastar batería) y accesos a nuevo cálculo e importar archivo.
 */
class PrintsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context)) }
    }

    companion object {
        /** Repinta todos los widgets; se llama cuando cambian las impresiones en curso. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrintsWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun build(context: Context): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_prints)
            // Textos desde la app, en el idioma elegido en Ajustes (el lanzador usaría el del sistema)
            v.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            v.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_empty))
            v.setTextViewText(R.id.widget_new, context.getString(R.string.shortcut_new_calc))
            v.setTextViewText(R.id.widget_import, context.getString(R.string.shortcut_import))
            val now = System.currentTimeMillis()
            val jobs = PrintTimer.current(context).filter { it.endAt > now }.sortedBy { it.endAt }
            val first = jobs.firstOrNull()
            if (first == null) {
                v.setViewVisibility(R.id.widget_job, View.GONE)
                v.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            } else {
                v.setViewVisibility(R.id.widget_job, View.VISIBLE)
                v.setViewVisibility(R.id.widget_empty, View.GONE)
                v.setTextViewText(R.id.widget_job_name, first.name)
                val end = DateTimeFormatter.ofPattern("HH:mm")
                    .format(Instant.ofEpochMilli(first.endAt).atZone(ZoneId.systemDefault()))
                val more = if (jobs.size > 1) " · " + context.getString(R.string.widget_more, jobs.size - 1) else ""
                v.setTextViewText(R.id.widget_job_end, context.getString(R.string.widget_ends, end) + more)
                v.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + (first.endAt - now), null, true)
                v.setChronometerCountDown(R.id.widget_countdown, true)
            }
            v.setOnClickPendingIntent(R.id.widget_root, open(context, null, 0))
            v.setOnClickPendingIntent(R.id.widget_new, open(context, MainActivity.SHORTCUT_NEW_CALC, 1))
            v.setOnClickPendingIntent(R.id.widget_import, open(context, MainActivity.SHORTCUT_IMPORT, 2))
            return v
        }

        private fun open(context: Context, shortcut: String?, code: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                if (shortcut != null) action = MainActivity.ACTION_PREFIX + shortcut
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
