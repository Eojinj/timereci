package com.timereci.focus.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Formatting helpers shared across screens. All wall-clock values use the device zone. */
object Formatters {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val korean = Locale.KOREAN

    /** Countdown / clock display, e.g. 1500000ms -> "25:00". Minutes are not wrapped to hours. */
    fun clock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Focused-duration badge, same MM:SS shape as the prototype ("90:00"). */
    fun focus(ms: Long): String = clock(ms)

    fun localDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** Feed day heading, e.g. "07.19". */
    fun dayLabel(date: LocalDate): String = "%02d.%02d".format(date.monthValue, date.dayOfMonth)

    /** Full title with year, e.g. "2026.07.19". */
    fun dayTitle(date: LocalDate): String =
        "%d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)

    /** Korean weekday, e.g. "토요일". */
    fun weekday(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, korean)

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

    /** Day summary line, e.g. "2 세션 · 1h 15m". */
    fun daySummary(sessionCount: Int, totalFocusMs: Long): String {
        val totalMin = (totalFocusMs / 60000).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        val duration = if (h > 0) "${h}h ${m}m" else "${m}m"
        return "$sessionCount 세션 · $duration"
    }
}
