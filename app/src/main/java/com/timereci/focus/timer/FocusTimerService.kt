package com.timereci.focus.timer

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.timereci.focus.MainActivity
import com.timereci.focus.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the process alive during a focus session and mirrors the
 * remaining time into an ongoing notification. It owns no timer logic — it observes
 * [FocusTimerController] and stops itself once the session is no longer active.
 */
@AndroidEntryPoint
class FocusTimerService : LifecycleService() {

    @Inject lateinit var controller: FocusTimerController

    override fun onCreate() {
        super.onCreate()
        startForeground(buildNotification(controller.state.value))
        observeTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun observeTimer() {
        lifecycleScope.launch {
            controller.state
                // Re-post at most once per visible second, not on every 250ms tick.
                .map { it.phase to it.remainingMs / 1000 }
                .distinctUntilChanged()
                .collect {
                    val state = controller.state.value
                    if (state.isActive) {
                        startForeground(buildNotification(state))
                    } else {
                        ServiceCompat.stopForeground(this@FocusTimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        }
    }

    private fun startForeground(notification: android.app.Notification) {
        // The SPECIAL_USE type only exists (and is required) on API 34+.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, ONGOING_NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(state: TimerState): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val remaining = formatRemaining(state.remainingMs)
        val task = state.taskLabel.ifBlank { "Focus session" }
        val text = if (state.phase == TimerPhase.PAUSED) "$remaining · Paused" else remaining

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun formatRemaining(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    companion object {
        const val CHANNEL_ID = "focus_timer"
        /** Separate, audible channel — the ticking channel above is deliberately silent. */
        const val DONE_CHANNEL_ID = "focus_timer_done"
        const val ONGOING_NOTIFICATION_ID = 42
        const val DONE_NOTIFICATION_ID = 43
    }
}
