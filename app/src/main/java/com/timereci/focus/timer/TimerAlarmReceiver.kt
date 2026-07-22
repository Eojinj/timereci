package com.timereci.focus.timer

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timereci.focus.MainActivity
import com.timereci.focus.R

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

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, FocusTimerService.DONE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("집중 완주")
            .setContentText("영수증이 준비됐어요. 열어서 담아보세요.")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(FocusTimerService.DONE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_COMPLETE = "com.timereci.focus.TIMER_COMPLETE"
    }
}
