package com.timereci.focus.ui

/** Navigation routes. Kept as plain strings for a small, single-module app. */
object Routes {
    /** The task list — the app's start destination. Tapping a task or finishing Quick Start
     * both start the session directly (via FocusTimerController) before landing here. */
    const val TODAY = "today"

    /** Type "Laundry 20" and go straight into a running session. */
    const val QUICK_START = "quickstart"

    /** Always shows whatever FocusTimerController reports — no nav args. A session is started
     * (by Today, Quick Start or the completion sheet) *before* navigating here. */
    const val TIMER = "timer"

    /** A short break. It ends by returning to Today — it no longer carries a task to start
     * afterwards, because the completion sheet is where "what's next" gets decided. */
    const val BREAK = "break?breakMinutes={breakMinutes}"
    const val ARG_BREAK_MINUTES = "breakMinutes"
    fun breakScreen(breakMinutes: Int) = "break?breakMinutes=$breakMinutes"

    const val HISTORY = "history"

    const val SESSION_DETAIL = "sessionDetail/{receiptId}"
    fun sessionDetail(receiptId: Long) = "sessionDetail/$receiptId"
    const val ARG_RECEIPT_ID = "receiptId"

    /** Per-task totals, reached from History. */
    const val STATS = "stats"

    // One task's own trend. The arg is the normalized label, which can be blank (an unnamed
    // session) — hence a query parameter rather than a path segment, which can't be empty.
    const val TASK_STATS = "taskStats?key={key}"
    const val ARG_TASK_KEY = "key"
    fun taskStats(key: String) = "taskStats?key=${android.net.Uri.encode(key)}"

    const val SETTINGS = "settings"
}
