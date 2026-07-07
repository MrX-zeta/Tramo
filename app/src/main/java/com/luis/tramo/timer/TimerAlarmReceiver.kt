package com.luis.tramo.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager at the exact cycle end — the authoritative completion trigger. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        PomodoroTimerService.sendAction(context, PomodoroTimerService.ACTION_COMPLETE)
    }
}
