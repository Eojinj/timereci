package com.timereci.focus.ui.model

import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.ReceiptEntity
import com.timereci.focus.ui.util.Formatters
import java.time.LocalDate

/** A single published receipt, flattened for display. */
data class SessionCard(
    val id: Long,
    val stamp: String,
    val task: String,
    val focus: String,
    val focusMs: Long,
    val photos: List<PhotoRef>,
    val comment: String? = null,
)

/** Shared mapping from a stored receipt to its display card. */
fun ReceiptEntity.toSessionCard() = SessionCard(
    id = id,
    stamp = Formatters.stamp(issuedAtEpoch),
    task = taskLabel,
    focus = Formatters.focus(focusedMs),
    focusMs = focusedMs,
    photos = photos.ifEmpty { listOf(PhotoRef()) },
    comment = comment,
)

/** A day's worth of sessions in History. */
data class FeedDay(
    val epochDay: Long,
    val dayLabel: String,   // "07.19"
    val focusMs: Long,       // total focused time, raw
    val focusText: String,  // total focused time, "1h 15m"
    val sessions: List<SessionCard>,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

object FeedBuilder {
    /** Groups receipts (already sorted newest-first) into feed days. */
    fun build(receipts: List<ReceiptEntity>): List<FeedDay> {
        return receipts
            .groupBy { Formatters.localDate(it.issuedAtEpoch) }
            .toSortedMap(compareByDescending { it })
            .map { (date, dayReceipts) ->
                val sorted = dayReceipts.sortedByDescending { it.issuedAtEpoch }
                val sessions = sorted.map { it.toSessionCard() }
                val totalFocus = sorted.sumOf { it.focusedMs }
                FeedDay(
                    epochDay = date.toEpochDay(),
                    dayLabel = Formatters.dayLabel(date),
                    focusMs = totalFocus,
                    focusText = Formatters.focusDuration(totalFocus),
                    sessions = sessions,
                )
            }
    }
}
