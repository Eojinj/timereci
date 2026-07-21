package com.timereci.focus.ui.model

import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.ReceiptEntity
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.util.Formatters
import java.time.LocalDate

/** A single published receipt, flattened for display. */
data class SessionCard(
    val id: Long,
    val stamp: String,
    val task: String,
    val focus: String,
    val photos: List<PhotoRef>,
    val comment: String? = null,
)

/** Shared mapping from a stored receipt to its display card. */
fun ReceiptEntity.toSessionCard() = SessionCard(
    id = id,
    stamp = Formatters.stamp(issuedAtEpoch),
    task = taskLabel,
    focus = Formatters.focus(focusedMs),
    photos = photos.ifEmpty { listOf(PhotoRef(toneIndex = PhotoTones.indexFor(id))) },
    comment = comment,
)

/** One tile in a feed day's photo grid. */
data class FeedTile(
    val photo: PhotoRef,
    val sessionId: Long,
    val task: String,
    val comment: String?,
    /** "+3" overlay when this is the last visible tile and more exist; else null. */
    val moreLabel: String?,
)

/** A day's worth of sessions in the feed. */
data class FeedDay(
    val epochDay: Long,
    val dayLabel: String,   // "07.19"
    val weekday: String,    // "토요일"
    val focusText: String,  // total focused time, "1h 15m"
    val sessionText: String, // "3 세션"
    val tiles: List<FeedTile>,
    val sessions: List<SessionCard>,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

private data class FlatTile(val sessionId: Long, val task: String, val comment: String?, val photo: PhotoRef)

object FeedBuilder {
    private const val MAX_TILES = 6

    /** Groups receipts (already sorted newest-first) into feed days. */
    fun build(receipts: List<ReceiptEntity>): List<FeedDay> {
        return receipts
            .groupBy { Formatters.localDate(it.issuedAtEpoch) }
            .toSortedMap(compareByDescending { it })
            .map { (date, dayReceipts) ->
                val sorted = dayReceipts.sortedByDescending { it.issuedAtEpoch }
                val sessions = sorted.map { it.toSessionCard() }
                // Flatten to (session, photo) so each tile knows which session it belongs to.
                val flat = sorted.flatMap { r ->
                    r.photos.ifEmpty { listOf(PhotoRef(toneIndex = PhotoTones.indexFor(r.id))) }
                        .map { p -> FlatTile(r.id, r.taskLabel, r.comment, p) }
                }
                val shown = flat.take(MAX_TILES)
                val tiles = shown.mapIndexed { i, flatTile ->
                    val isLast = i == MAX_TILES - 1 && flat.size > MAX_TILES
                    FeedTile(
                        photo = flatTile.photo,
                        sessionId = flatTile.sessionId,
                        task = flatTile.task,
                        comment = flatTile.comment,
                        moreLabel = if (isLast) "+${flat.size - MAX_TILES}" else null,
                    )
                }
                val totalFocus = sorted.sumOf { it.focusedMs }
                FeedDay(
                    epochDay = date.toEpochDay(),
                    dayLabel = Formatters.dayLabel(date),
                    weekday = Formatters.weekday(date),
                    focusText = Formatters.focusDuration(totalFocus),
                    sessionText = "${sorted.size} 세션",
                    tiles = tiles,
                    sessions = sessions,
                )
            }
    }
}
