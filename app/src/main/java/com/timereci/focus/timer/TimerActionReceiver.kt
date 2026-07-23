package com.timereci.focus.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives taps on the timer notification's action buttons and forwards them to the singleton
 * [FocusTimerController]. Kept separate from [TimerAlarmReceiver] (which is the dead-process
 * alarm backstop) so user actions and the scheduled backstop don't share intents.
 */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: FocusTimerController

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_QUIT -> controller.abandon()
            ACTION_FINISH -> controller.completeNow()
            ACTION_DISMISS_ALARM -> controller.stopAlarm()
        }
    }

    companion object {
        const val ACTION_QUIT = "com.timereci.focus.TIMER_QUIT"
        const val ACTION_FINISH = "com.timereci.focus.TIMER_FINISH"
        const val ACTION_DISMISS_ALARM = "com.timereci.focus.TIMER_DISMISS_ALARM"
    }
}
