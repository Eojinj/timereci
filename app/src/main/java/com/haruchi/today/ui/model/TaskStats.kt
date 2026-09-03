package com.haruchi.today.ui.model

import com.haruchi.today.data.ReceiptEntity
import com.haruchi.today.data.TaskKey
import com.haruchi.today.ui.util.Formatters
import java.time.LocalDate

/** Everything history knows about one task, rolled up. */
data class TaskStat(
    /** Normalized label — the identity two sessions are considered "the same task" by. */
    val key: String,
    /** Display spelling, taken from the most recent session so it follows how you last typed it. */
    val label: String,
    val sessions: Int,
    val totalFocusMs: Long,
    val averageFocusMs: Long,
    /** Distinct calendar days this task was worked on — repeats within a day count once. */
    val daysActive: Int,
    val lastDoneEpoch: Long,
    /** The length to prefill when starting this task again, from its most recent run. */
    val typicalMinutes: Int,
)

/** One day's total for a task, including empty days so gaps stay visible in the chart. */
data class TrendBar(val date: LocalDate, val focusMs: Long)

object TaskStatsBuilder {

    /**
     * Tasks are matched by their label — there is no task ID in the schema, and adding one
     * would leave every already-published session unattributed. Going by label means the whole
     * existing history rolls up on its own, and it matches how the app is actually used: you
     * retype (or tap a Recent chip for) the same name each time.
     */
    fun keyOf(label: String): String = TaskKey.of(label)

    fun displayOf(label: String): String = label.trim().ifBlank { "Focus" }

    /** Every task that appears in history, heaviest total first. */
    fun build(receipts: List<ReceiptEntity>): List<TaskStat> =
        receipts
            .groupBy { keyOf(it.taskLabel) }
            .map { (key, group) ->
                val newestFirst = group.sortedByDescending { it.issuedAtEpoch }
                val newest = newestFirst.first()
                val total = group.sumOf { it.focusedMs }
                TaskStat(
                    key = key,
                    label = displayOf(newest.taskLabel),
                    sessions = group.size,
                    totalFocusMs = total,
                    averageFocusMs = total / group.size,
                    daysActive = group.map { Formatters.localDate(it.issuedAtEpoch) }.distinct().size,
                    lastDoneEpoch = newest.issuedAtEpoch,
                    typicalMinutes = (newest.plannedMs / 60_000L).toInt().coerceAtLeast(1),
                )
            }
            .sortedByDescending { it.totalFocusMs }

    fun sessionsOf(receipts: List<ReceiptEntity>, key: String): List<ReceiptEntity> =
        receipts.filter { keyOf(it.taskLabel) == key }.sortedByDescending { it.issuedAtEpoch }

    /** The last [days] days ending today, oldest first — zero-filled so idle days show as gaps. */
    fun trend(receipts: List<ReceiptEntity>, key: String, days: Int = 14): List<TrendBar> {
        val byDate = sessionsOf(receipts, key)
            .groupBy { Formatters.localDate(it.issuedAtEpoch) }
            .mapValues { (_, dayReceipts) -> dayReceipts.sumOf { it.focusedMs } }
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            TrendBar(date = date, focusMs = byDate[date] ?: 0L)
        }
    }
}
