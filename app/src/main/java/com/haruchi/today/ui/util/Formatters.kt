package com.haruchi.today.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Formatting helpers shared across screens. All wall-clock values use the device zone. */
object Formatters {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Countdown / clock display, e.g. 1500000ms -> "25:00". Minutes are not wrapped to hours. */
    fun clock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Focused-duration badge, e.g. "25 min" (rounded, min 1). */
    fun focus(ms: Long): String {
        val minutes = ((ms + 30_000) / 60_000).toInt().coerceAtLeast(1)
        return "$minutes min"
    }

    fun localDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** Feed day heading, e.g. "07.19". */
    fun dayLabel(date: LocalDate): String = "%02d.%02d".format(date.monthValue, date.dayOfMonth)

    /** Card stamp, e.g. "07.19 · 14:20". */
    fun stamp(epochMs: Long): String {
        val t = Instant.ofEpochMilli(epochMs).atZone(zone)
        return "%02d.%02d · %02d:%02d".format(t.monthValue, t.dayOfMonth, t.hour, t.minute)
    }

    /** Time-only, e.g. "14:20". */
    fun timeOfDay(epochMs: Long): String {
        val t = Instant.ofEpochMilli(epochMs).atZone(zone)
        return "%02d:%02d".format(t.hour, t.minute)
    }

    /** Total focused duration, e.g. "1h 15m" or "25m". */
    fun focusDuration(totalFocusMs: Long): String {
        val totalMin = (totalFocusMs / 60000).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
