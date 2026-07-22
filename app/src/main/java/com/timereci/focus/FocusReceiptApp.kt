package com.timereci.focus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.timereci.focus.timer.FocusTimerService
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated with [HiltAndroidApp] so Hilt can generate the
 * dependency graph and provide singletons (database, repository, timer controller).
 */
@HiltAndroidApp
class FocusReceiptApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createTimerNotificationChannels()
    }

    private fun createTimerNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ticking = NotificationChannel(
            FocusTimerService.CHANNEL_ID,
            getString(R.string.timer_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.timer_channel_desc)
            setShowBadge(false)
        }
        // Separate, higher-importance channel so the completion alert can actually ring/pop up —
        // the ticking channel above is deliberately silent (IMPORTANCE_LOW).
        val done = NotificationChannel(
            FocusTimerService.DONE_CHANNEL_ID,
            getString(R.string.timer_done_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.timer_done_channel_desc)
        }
        manager.createNotificationChannel(ticking)
        manager.createNotificationChannel(done)
    }
}
