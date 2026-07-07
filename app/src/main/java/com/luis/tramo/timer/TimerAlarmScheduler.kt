package com.luis.tramo.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the authoritative completion fire. Uses [AlarmManager.setExactAndAllowWhileIdle] so the
 * cycle-end alert happens on time even in Doze / screen-off; falls back to an inexact
 * allow-while-idle alarm when the app lacks the Android 12+ exact-alarm permission.
 */
class TimerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(triggerAtEpochMillis: Long) {
        val pending = pendingIntent()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pending)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE = 7001
    }
}
