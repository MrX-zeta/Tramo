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
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.luis.tramo.MainActivity
import com.luis.tramo.R
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.widget.PomodoroWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Foreground service that owns the countdown. It counts against an absolute
 * [SystemClock.elapsedRealtime] deadline so the remaining time stays accurate even while the
 * process is backgrounded or dozing, and pushes every tick to the notification and the widget.
 */
@AndroidEntryPoint
class PomodoroTimerService : Service() {

    @Inject lateinit var stateHolder: TimerStateHolder
    @Inject lateinit var preferences: UserPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob()) + Dispatchers.Default
    private var tickJob: Job? = null
    private var deadlineElapsed = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startOrResume()
            ACTION_PAUSE -> pause()
            ACTION_SKIP -> skip()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun startOrResume() {
        val state = stateHolder.state.value
        val remaining = if (state.remainingSeconds > 0) state.remainingSeconds else state.totalSeconds
        deadlineElapsed = SystemClock.elapsedRealtime() + remaining * 1000L
        stateHolder.update { it.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining) }
        startForeground(NOTIFICATION_ID, buildNotification(stateHolder.state.value))
        startTicking()
    }

    private fun pause() {
        tickJob?.cancel()
        stateHolder.update { it.copy(status = TimerStatus.PAUSED, remainingSeconds = currentRemaining()) }
        refreshOutputs()
    }

    private fun skip() {
        tickJob?.cancel()
        scope.launch {
            val next = nextSessionType(stateHolder.state.value.sessionType, preferences.sessionsTodayValue())
            stateHolder.set(TimerState.forSession(next))
            startOrResume()
        }
    }

    private fun stop() {
        tickJob?.cancel()
        stateHolder.set(TimerState.forSession(SessionType.FOCUS))
        scope.launch { PomodoroWidget().updateAll(applicationContext) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = currentRemaining()
                if (remaining != stateHolder.state.value.remainingSeconds) {
                    stateHolder.update { it.copy(remainingSeconds = remaining) }
                    refreshOutputs()
                }
                if (remaining <= 0) {
                    onSessionComplete()
                    break
                }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private suspend fun onSessionComplete() {
        val finished = stateHolder.state.value.sessionType
        val focusCount = if (finished == SessionType.FOCUS) {
            preferences.incrementFocusSessions()
        } else {
            preferences.sessionsTodayValue()
        }
        val next = nextSessionType(finished, focusCount)
        // Auto-advance: move to the next session and start it immediately.
        stateHolder.set(TimerState.forSession(next))
        startOrResume()
    }

    /** Ceil of the remaining millis, so the display shows the full starting minute. */
    private fun currentRemaining(): Int {
        val millis = deadlineElapsed - SystemClock.elapsedRealtime()
        return ((millis + 999) / 1000).toInt().coerceAtLeast(0)
    }

    private fun refreshOutputs() {
        notificationManager().notify(NOTIFICATION_ID, buildNotification(stateHolder.state.value))
        scope.launch { PomodoroWidget().updateAll(applicationContext) }
    }

    private fun buildNotification(state: TimerState): Notification {
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
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

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PomodoroTimerService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.luis.tramo.timer.START"
        const val ACTION_PAUSE = "com.luis.tramo.timer.PAUSE"
        const val ACTION_SKIP = "com.luis.tramo.timer.SKIP"
        const val ACTION_STOP = "com.luis.tramo.timer.STOP"

        private const val CHANNEL_ID = "pomodoro_timer"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_INTERVAL_MS = 200L

        /** After a break, back to focus; after focus, long break every [SESSIONS_PER_LONG_BREAK]. */
        fun nextSessionType(current: SessionType, focusCount: Int): SessionType = when {
            current != SessionType.FOCUS -> SessionType.FOCUS
            focusCount % SESSIONS_PER_LONG_BREAK == 0 -> SessionType.LONG_BREAK
            else -> SessionType.SHORT_BREAK
        }

        /** Launches the service with [action]; uses a foreground start only when beginning to run. */
        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, PomodoroTimerService::class.java).setAction(action)
            if (action == ACTION_START && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
