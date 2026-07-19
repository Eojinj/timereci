package com.timereci.focus.ui

/** Navigation routes. Kept as plain strings for a small, single-module app. */
object Routes {
    const val FEED = "feed"
    const val TIMER = "timer"
    const val PUBLISH = "publish"

    const val DAY = "day/{epochDay}"
    fun day(epochDay: Long) = "day/$epochDay"
    const val ARG_EPOCH_DAY = "epochDay"

    const val DETAIL = "detail/{receiptId}"
    fun detail(receiptId: Long) = "detail/$receiptId"
    const val ARG_RECEIPT_ID = "receiptId"

    const val SETTINGS = "settings"
}
