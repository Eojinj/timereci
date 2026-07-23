package com.timereci.focus.timer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Fired by [TimerAlarmScheduler] as the dead-process backstop: posts a completion
 * notification so the user learns a session finished even if the app was killed.
 * The actual state is reconciled by [FocusTimerController.restore] on next launch.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context)
            .notify(FocusTimerService.DONE_NOTIFICATION_ID, TimerNotifications.completion(context))
    }

    companion object {
        const val ACTION_COMPLETE = "com.timereci.focus.TIMER_COMPLETE"
    }
}
