package com.timereci.focus.ui

/** Navigation routes. Kept as plain strings for a small, single-module app. */
object Routes {
    /** The task list — the app's start destination. Tapping a task or finishing Quick Start
     * both start the session directly (via FocusTimerController) before landing here. */
    const val TODAY = "today"

    /** Type "Laundry 20" and go straight into a running session. */
    const val QUICK_START = "quickstart"

    /** Always shows whatever FocusTimerController reports — no nav args. A session is started
     * (by Today, Quick Start, Up Next or Break) *before* navigating here. */
    const val TIMER = "timer"

    // Shown right after a session completes, to add a photo/note and save or discard it.
    const val SESSION_COMPLETE = "sessionComplete"

    // Shown after saving when the todo queue still has something in it.
    const val UP_NEXT = "upNext"

    // A short break before continuing into the next queued task. Carries the task it will
    // start when the break ends, since the timer screen itself no longer accepts prefill args.
    const val BREAK = "break?breakMinutes={breakMinutes}&task={task}&minutes={minutes}"
    const val ARG_BREAK_MINUTES = "breakMinutes"
    const val ARG_TASK = "task"
    const val ARG_MINUTES = "minutes"
    fun breakScreen(breakMinutes: Int, task: String, minutes: Int) =
        "break?breakMinutes=$breakMinutes&task=${android.net.Uri.encode(task)}&minutes=$minutes"

    const val HISTORY = "history"

    const val SESSION_DETAIL = "sessionDetail/{receiptId}"
    fun sessionDetail(receiptId: Long) = "sessionDetail/$receiptId"
    const val ARG_RECEIPT_ID = "receiptId"

    const val SETTINGS = "settings"
}
