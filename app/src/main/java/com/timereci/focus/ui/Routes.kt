package com.timereci.focus.ui

import android.net.Uri

/** Navigation routes. Kept as plain strings for a small, single-module app. */
object Routes {
    const val FEED = "feed"
    const val PUBLISH = "publish"

    // Timer optionally accepts a prefilled task + duration (from a planned focus).
    const val TIMER = "timer?task={task}&minutes={minutes}"
    const val ARG_TASK = "task"
    const val ARG_MINUTES = "minutes"
    fun timer(task: String = "", minutes: Int = 0) =
        "timer?task=${Uri.encode(task)}&minutes=$minutes"

    const val DAY = "day/{epochDay}"
    fun day(epochDay: Long) = "day/$epochDay"
    const val ARG_EPOCH_DAY = "epochDay"

    const val DETAIL = "detail/{receiptId}"
    fun detail(receiptId: Long) = "detail/$receiptId"
    const val ARG_RECEIPT_ID = "receiptId"

    const val SETTINGS = "settings"
}
