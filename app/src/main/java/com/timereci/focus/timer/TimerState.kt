package com.timereci.focus.timer

/** Lifecycle of the one in-flight focus session. */
enum class TimerPhase { IDLE, RUNNING, PAUSED, COMPLETED }

/**
 * Snapshot of the timer, exposed as a StateFlow by [FocusTimerController].
 * Time math is end-time based: [remainingMs] is derived from an elapsedRealtime target,
 * never accumulated tick-by-tick, so it stays correct across process death.
 */
data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val plannedMs: Long = 25 * 60_000L,
    val remainingMs: Long = 25 * 60_000L,
    /** Focused time actually earned; meaningful once [phase] is COMPLETED. */
    val focusedMs: Long = 0L,
    val taskLabel: String = "",
    val backdropFileName: String? = null,
    val draftComment: String = "",
    val startedAtEpoch: Long = 0L,
) {
    val isActive: Boolean get() = phase == TimerPhase.RUNNING || phase == TimerPhase.PAUSED
    val progress: Float
        get() = if (plannedMs <= 0) 0f else (1f - remainingMs.toFloat() / plannedMs).coerceIn(0f, 1f)
}
