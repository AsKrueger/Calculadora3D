package com.tdcostmanager.app.core

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tdcostmanager.app.MainActivity
import com.tdcostmanager.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PrintJob(val id: Int, val name: String, val startAt: Long, val endAt: Long)

/**
 * Temporizador de impresión: una notificación con cuenta atrás mientras imprime y un aviso al terminar,
 * aunque la app esté cerrada. Sin permisos de alarma exacta: un aviso con un par de minutos de margen basta.
 */
object PrintTimer {

    private const val PREFS = "print_timer"
    private const val KEY = "jobs"
    private const val CHANNEL_RUNNING = "print_running"
    private const val CHANNEL_DONE = "print_done"
    const val EXTRA_ID = "job_id"

    private val json = Json { ignoreUnknownKeys = true }
    private val _jobs = MutableStateFlow<List<PrintJob>>(emptyList())
    val jobs: StateFlow<List<PrintJob>> = _jobs

    fun init(context: Context) {
        createChannels(context)
        _jobs.value = read(context).filter { it.endAt > System.currentTimeMillis() - 60_000 }
    }

    fun start(context: Context, name: String, durationMs: Long): PrintJob {
        val now = System.currentTimeMillis()
        val job = PrintJob((now % Int.MAX_VALUE).toInt(), name, now, now + durationMs)
        save(context, read(context) + job)
        schedule(context, job)
        showRunning(context, job)
        return job
    }

    fun cancel(context: Context, id: Int) {
        val job = read(context).firstOrNull { it.id == id } ?: return
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, job.id))
        NotificationManagerCompat.from(context).cancel(job.id)
        save(context, read(context).filterNot { it.id == id })
    }

    /** Tras reiniciar el teléfono las alarmas se pierden: se vuelven a programar. */
    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        read(context).forEach { job ->
            if (job.endAt <= now) finish(context, job.id) else {
                schedule(context, job)
                showRunning(context, job)
            }
        }
    }

    internal fun finish(context: Context, id: Int) {
        val job = read(context).firstOrNull { it.id == id } ?: return
        save(context, read(context).filterNot { it.id == id })
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_print)
            .setContentTitle(context.getString(R.string.timer_done_title))
            .setContentText(context.getString(R.string.timer_done_body, job.name))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notify(context, job.id, n)
    }

    private fun showRunning(context: Context, job: PrintJob) {
        val n = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_print)
            .setContentTitle(job.name)
            .setContentText(context.getString(R.string.timer_running_notif))
            .setWhen(job.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp(context))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        notify(context, job.id, n)
    }

    private fun notify(context: Context, id: Int, n: android.app.Notification) {
        // Sin permiso de notificaciones (Android 13+) el temporizador sigue visible dentro de la app
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    private fun schedule(context: Context, job: PrintJob) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, job.endAt, alarmIntent(context, job.id))
    }

    private fun alarmIntent(context: Context, id: Int) = PendingIntent.getBroadcast(
        context, id,
        Intent(context, PrintTimerReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNNING, context.getString(R.string.timer_channel_running), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.timer_channel_done), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun read(context: Context): List<PrintJob> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { runCatching { json.decodeFromString<List<PrintJob>>(it) }.getOrNull() } ?: emptyList()

    private fun save(context: Context, jobs: List<PrintJob>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.encodeToString(jobs)).apply()
        _jobs.value = jobs
    }
}

class PrintTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> PrintTimer.rescheduleAll(context)
            else -> PrintTimer.finish(context, intent.getIntExtra(PrintTimer.EXTRA_ID, -1))
        }
    }
}
