package com.timereci.focus.timer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.timereci.focus.MainActivity
import com.timereci.focus.R

/**
 * Central builder for every timer-related notification, so the ongoing timer and the
 * completion alert share one look and one set of action buttons. Actions are delivered to
 * [TimerActionReceiver], which forwards them to the singleton [FocusTimerController].
 */
object TimerNotifications {

    /** Ongoing, silent notification shown while a session runs — carries 그만두기 / 완주 buttons. */
    fun ongoing(context: Context, state: TimerState): Notification {
        val remaining = formatRemaining(state.remainingMs)
        val task = state.taskLabel.ifBlank { "집중 세션" }
        val text = if (state.phase == TimerPhase.PAUSED) "$remaining · 일시정지" else remaining

        return NotificationCompat.Builder(context, FocusTimerService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                0,
                context.getString(R.string.timer_action_quit),
                actionIntent(context, TimerActionReceiver.ACTION_QUIT, REQUEST_QUIT),
            )
            .addAction(
                0,
                context.getString(R.string.timer_action_finish),
                actionIntent(context, TimerActionReceiver.ACTION_FINISH, REQUEST_FINISH),
            )
            .build()
    }

    /**
     * Completion alert on the audible channel — carries an 알림 끄기 button so the ringing alarm
     * can be silenced without opening the app. Tapping the body opens the receipt.
     */
    fun completion(context: Context): Notification =
        NotificationCompat.Builder(context, FocusTimerService.DONE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("집중 완주")
            .setContentText("영수증이 준비됐어요. 열어서 담아보세요.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .addAction(
                0,
                context.getString(R.string.timer_action_stop_alarm),
                actionIntent(context, TimerActionReceiver.ACTION_DISMISS_ALARM, REQUEST_DISMISS_ALARM),
            )
            .build()

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatRemaining(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private const val REQUEST_QUIT = 4101
    private const val REQUEST_FINISH = 4102
    private const val REQUEST_DISMISS_ALARM = 4103
}
