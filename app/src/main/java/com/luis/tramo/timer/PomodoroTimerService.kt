package com.luis.tramo.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import com.luis.tramo.MainActivity
import com.luis.tramo.R
import com.luis.tramo.data.ActiveSession
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.SessionRepository
import com.luis.tramo.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Foreground service that owns the countdown. Time is driven by an absolute wall-clock deadline
 * ([endEpochMillis]) persisted to DataStore, so it never drifts in the background and the running
 * session survives process death. An exact [TimerAlarmScheduler] alarm is the authoritative
 * completion fire; the tick loop only updates the live display.
 */
@AndroidEntryPoint
class PomodoroTimerService : Service() {

    @Inject lateinit var stateHolder: TimerStateHolder
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var sessions: SessionRepository
    @Inject lateinit var widgetUpdater: WidgetUpdater

    private val scope = CoroutineScope(SupervisorJob()) + Dispatchers.Main.immediate
    private val alarms by lazy { TimerAlarmScheduler(applicationContext) }
    private var tickJob: Job? = null
    private var endEpochMillis = 0L

    @Volatile private var completedForEnd = 0L

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> beginRunning()
            ACTION_PAUSE -> pause()
            ACTION_SKIP -> skip()
            ACTION_STOP -> stop()
            ACTION_COMPLETE -> handleComplete()
            else -> restore() // null intent (START_STICKY restart) or ACTION_RESTORE
        }
        return START_STICKY
    }

    /** Starts (or resumes) the current session in the holder, using its remaining seconds. */
    private fun beginRunning() {
        val state = stateHolder.state.value
        val remaining = if (state.remainingSeconds > 0) state.remainingSeconds else state.totalSeconds
        endEpochMillis = System.currentTimeMillis() + remaining * 1000L
        completedForEnd = 0L
        stateHolder.update { it.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining) }
        startForeground(NOTIFICATION_ID, buildOngoingNotification(stateHolder.state.value))
        alarms.schedule(endEpochMillis)
        persistRunning(state.sessionType, state.totalSeconds, endEpochMillis)
        startTicking()
    }

    private fun pause() {
        tickJob?.cancel()
        alarms.cancel()
        val remaining = currentRemaining()
        stateHolder.update { it.copy(status = TimerStatus.PAUSED, remainingSeconds = remaining) }
        val state = stateHolder.state.value
        scope.launch {
            preferences.saveActiveSession(
                ActiveSession(state.sessionType.name, state.totalSeconds, running = false, endEpochMillis = 0L, pausedRemaining = remaining)
            )
        }
        notificationManager().notify(NOTIFICATION_ID, buildOngoingNotification(state))
    }

    private fun skip() {
        tickJob?.cancel()
        alarms.cancel()
        scope.launch {
            val before = preferences.sessionsBeforeLongBreak.first()
            val next = nextSessionType(stateHolder.state.value.sessionType, countFocusToday(), before)
            stateHolder.set(freshSession(next))
            beginRunning()
        }
    }

    /** Stops running and leaves the next session ready in IDLE (used when auto-start is off). */
    private fun goIdle() {
        tickJob?.cancel()
        alarms.cancel()
        scope.launch {
            preferences.clearActiveSession()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stop() {
        tickJob?.cancel()
        alarms.cancel()
        scope.launch {
            preferences.clearActiveSession()
            stateHolder.set(freshSession(SessionType.FOCUS))
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Idempotent completion, invoked by the tick loop or the alarm (whichever fires first). */
    private fun handleComplete() {
        val end = endEpochMillis
        synchronized(this) {
            if (end == 0L || end == completedForEnd) return
            completedForEnd = end
        }
        tickJob?.cancel()
        alarms.cancel()
        scope.launch {
            val finished = stateHolder.state.value
            val finishedType = finished.sessionType
            sessions.record(finishedType, finished.totalSeconds, System.currentTimeMillis())
            // A completed session changes today's count/streak — refresh the widget (not per-second).
            // Routed through WidgetUpdater so every repaint goes through the same debounce that
            // keeps MIUI/HyperOS from discarding closely-spaced updates.
            widgetUpdater.refresh()
            postCompletionNotification(finishedType, preferences.soundVibrationOnFinish.first())

            val before = preferences.sessionsBeforeLongBreak.first()
            val next = nextSessionType(finishedType, countFocusToday(), before)
            stateHolder.set(freshSession(next))

            // Honor the auto-start preferences for this transition.
            val autoStart = if (finishedType == SessionType.FOCUS) {
                preferences.autoStartBreaks.first()
            } else {
                preferences.autoStartNextFocus.first()
            }
            if (autoStart) beginRunning() else goIdle()
        }
    }

    /** Restores state after process death from the persisted snapshot. */
    private fun restore() {
        scope.launch {
            val active = preferences.getActiveSession()
            if (active == null) {
                stopSelf()
                return@launch
            }
            val type = runCatching { SessionType.valueOf(active.sessionType) }.getOrNull() ?: SessionType.FOCUS
            if (active.running) {
                endEpochMillis = active.endEpochMillis
                completedForEnd = 0L
                if (active.endEpochMillis > System.currentTimeMillis()) {
                    val remaining = currentRemaining()
                    stateHolder.set(TimerState(type, TimerStatus.RUNNING, remaining, active.totalSeconds))
                    startForeground(NOTIFICATION_ID, buildOngoingNotification(stateHolder.state.value))
                    alarms.schedule(endEpochMillis)
                    startTicking()
                } else {
                    // Completed while the process was dead.
                    stateHolder.set(TimerState(type, TimerStatus.RUNNING, 0, active.totalSeconds))
                    handleComplete()
                }
            } else {
                stateHolder.set(TimerState(type, TimerStatus.PAUSED, active.pausedRemaining, active.totalSeconds))
                stopSelf()
            }
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = currentRemaining()
                if (remaining != stateHolder.state.value.remainingSeconds) {
                    stateHolder.update { it.copy(remainingSeconds = remaining) }
                    notificationManager().notify(NOTIFICATION_ID, buildOngoingNotification(stateHolder.state.value))
                }
                if (remaining <= 0) {
                    handleComplete()
                    break
                }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    /** Ceil of the remaining wall-clock millis, so the display shows the full starting minute. */
    private fun currentRemaining(): Int {
        val millis = endEpochMillis - System.currentTimeMillis()
        return ((millis + 999) / 1000).toInt().coerceAtLeast(0)
    }

    private suspend fun countFocusToday(): Int {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return sessions.countFocusSince(todayStart)
    }

    private fun persistRunning(type: SessionType, totalSeconds: Int, end: Long) {
        scope.launch {
            preferences.saveActiveSession(
                ActiveSession(type.name, totalSeconds, running = true, endEpochMillis = end, pausedRemaining = 0)
            )
        }
    }

    /** Focus honors the current preset; the short break honors its preset; long break is fixed. */
    private suspend fun freshSession(type: SessionType): TimerState {
        val seconds = when (type) {
            SessionType.FOCUS -> preferences.focusPresetMinutes.first() * 60
            SessionType.SHORT_BREAK -> preferences.breakPresetMinutes.first() * 60
            SessionType.LONG_BREAK -> preferences.longBreakMinutes.first() * 60
        }
        return TimerState(type, TimerStatus.IDLE, seconds, seconds)
    }

    private fun buildOngoingNotification(state: TimerState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (toggleLabel, toggleAction) = if (state.isRunning) {
            getString(R.string.timer_pause) to ACTION_PAUSE
        } else {
            getString(R.string.timer_play) to ACTION_START
        }
        return NotificationCompat.Builder(this, ONGOING_CHANNEL)
            .setContentTitle(getString(state.sessionType.labelRes))
            .setContentText(formatTime(state.remainingSeconds))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(state.isRunning)
            .setOnlyAlertOnce(true)
            .addAction(0, toggleLabel, servicePendingIntent(toggleAction, 1))
            .addAction(0, getString(R.string.timer_skip), servicePendingIntent(ACTION_SKIP, 2))
            .addAction(0, getString(R.string.timer_stop), servicePendingIntent(ACTION_STOP, 3))
            .build()
    }

    /** The end-of-cycle alert — the core onboarding promise. [sound] gates sound/vibration. */
    private fun postCompletionNotification(finished: SessionType, sound: Boolean) {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // A HIGH channel alerts (sound+vibration); the LOW channel shows the notice silently.
        val channel = if (sound) ALERT_CHANNEL else ONGOING_CHANNEL
        val notification = NotificationCompat.Builder(this, channel)
            .setContentTitle(getString(R.string.timer_completed_title))
            .setContentText(getString(finished.labelRes))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setSilent(!sound)
            .setPriority(if (sound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager().notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PomodoroTimerService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannels() {
        val manager = notificationManager()
        manager.createNotificationChannel(
            NotificationChannel(ONGOING_CHANNEL, getString(R.string.timer_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
        manager.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, getString(R.string.timer_alert_channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        // Note: the alarm is intentionally NOT cancelled here — it must still fire if we were killed.
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.luis.tramo.timer.START"
        const val ACTION_PAUSE = "com.luis.tramo.timer.PAUSE"
        const val ACTION_SKIP = "com.luis.tramo.timer.SKIP"
        const val ACTION_STOP = "com.luis.tramo.timer.STOP"
        const val ACTION_COMPLETE = "com.luis.tramo.timer.COMPLETE"
        const val ACTION_RESTORE = "com.luis.tramo.timer.RESTORE"

        private const val ONGOING_CHANNEL = "pomodoro_timer"
        private const val ALERT_CHANNEL = "pomodoro_timer_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        private const val TICK_INTERVAL_MS = 250L

        /** After a break, back to focus; after focus, a long break every [sessionsBeforeLong] sessions. */
        fun nextSessionType(current: SessionType, focusCount: Int, sessionsBeforeLong: Int): SessionType = when {
            current != SessionType.FOCUS -> SessionType.FOCUS
            sessionsBeforeLong > 0 && focusCount % sessionsBeforeLong == 0 -> SessionType.LONG_BREAK
            else -> SessionType.SHORT_BREAK
        }

        /** START/COMPLETE promote to foreground; other actions are plain (sent while app-visible). */
        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, PomodoroTimerService::class.java).setAction(action)
            val foreground = action == ACTION_START || action == ACTION_COMPLETE
            if (foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Ask the service to restore any persisted session (called from the UI on cold start). */
        fun requestRestore(context: Context) {
            context.startService(Intent(context, PomodoroTimerService::class.java).setAction(ACTION_RESTORE))
        }
    }
}
