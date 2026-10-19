package com.prenatal.app.reminders

import android.annotation.SuppressLint
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
import com.prenatal.app.MainActivity
import com.prenatal.app.R
import com.prenatal.app.data.Event
import com.prenatal.app.data.KeystoreCipher
import com.prenatal.app.data.Store
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Appointment reminders: the day before at 9:00 and two hours before.
 * Rescheduled from scratch whenever the schedule changes and after the phone restarts.
 */
object Reminders {
    private const val CHANNEL = "appointments"
    private const val PREFS = "reminders"
    private const val KEY_CODES = "codes"
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")
    private val DAY = DateTimeFormatter.ofPattern("dd/MM")

    /** (when, request code, title, text) for each reminder still in the future. */
    fun plan(events: List<Event>, now: LocalDateTime = LocalDateTime.now()): List<Planned> =
        events.filter { it.remind && !it.done && it.at.isAfter(now) }.flatMap { e ->
            val dayBefore = e.at.toLocalDate().minusDays(1).atTime(9, 0)
            val twoHours = e.at.minusHours(2)
            val where = if (e.place.isNotBlank()) " · ${e.place}" else ""
            listOfNotNull(
                dayBefore.takeIf { it.isAfter(now) }?.let {
                    Planned(it, (e.id + "d").hashCode(), "Tomorrow: ${e.title}", "${e.at.format(DAY)} at ${e.at.format(TIME)}$where")
                },
                twoHours.takeIf { it.isAfter(now) && it.isAfter(dayBefore) }?.let {
                    Planned(it, (e.id + "h").hashCode(), "Today at ${e.at.format(TIME)}: ${e.title}", "In about 2 hours$where")
                },
            )
        }

    data class Planned(val at: LocalDateTime, val code: Int, val title: String, val text: String)

    fun reschedule(context: Context, events: List<Event>) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().forEach { code ->
            alarms.cancel(pending(context, code.toInt(), "", ""))
        }
        val planned = plan(events)
        planned.forEach { p ->
            val millis = p.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(context, p.code, p.title, p.text))
        }
        prefs.edit().putStringSet(KEY_CODES, planned.map { it.code.toString() }.toSet()).apply()
    }

    private fun pending(context: Context, code: Int, title: String, text: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, ReminderReceiver::class.java).putExtra("title", title).putExtra("text", text).putExtra("code", code),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Appointment reminders", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }

    @SuppressLint("MissingPermission") // Guarded: notify() is a no-op without the permission.
    fun show(context: Context, code: Int, title: String, text: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(code, n) } // no permission → silently skipped
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.show(context, intent.getIntExtra("code", 0), intent.getStringExtra("title").orEmpty(), intent.getStringExtra("text").orEmpty())
    }
}

/** Alarms don't survive a restart, so read the (encrypted) schedule and set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val data = Store(File(context.filesDir, "store"), KeystoreCipher()).load()
                if (data != null) Reminders.reschedule(context, data.events)
            } catch (_: Exception) {
                // Nothing to reschedule.
            } finally {
                pending.finish()
            }
        }.start()
    }
}
