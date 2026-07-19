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
    val photos: List<PhotoRef>,
)

/** One tile in a feed day's photo grid. */
data class FeedTile(
    val photo: PhotoRef,
    /** "+3" overlay when this is the last visible tile and more exist; else null. */
    val moreLabel: String?,
)

/** A day's worth of sessions in the feed. */
data class FeedDay(
    val epochDay: Long,
    val dayLabel: String,   // "07.19"
    val weekday: String,    // "토요일"
    val summary: String,    // "2 세션 · 1h 15m"
    val tiles: List<FeedTile>,
    val sessions: List<SessionCard>,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

object FeedBuilder {
    private const val MAX_TILES = 6

    /** Groups receipts (already sorted newest-first) into feed days. */
    fun build(receipts: List<ReceiptEntity>): List<FeedDay> {
        return receipts
            .groupBy { Formatters.localDate(it.issuedAtEpoch) }
            .toSortedMap(compareByDescending { it })
            .map { (date, dayReceipts) ->
                val sorted = dayReceipts.sortedByDescending { it.issuedAtEpoch }
                val sessions = sorted.map { r ->
                    SessionCard(
                        id = r.id,
                        stamp = Formatters.stamp(r.issuedAtEpoch),
                        task = r.taskLabel,
                        focus = Formatters.focus(r.focusedMs),
                        photos = r.photos,
                    )
                }
                val allPhotos = sorted.flatMap { it.photos.ifEmpty { listOf(PhotoRef()) } }
                val shown = allPhotos.take(MAX_TILES)
                val tiles = shown.mapIndexed { i, p ->
                    val isLast = i == MAX_TILES - 1 && allPhotos.size > MAX_TILES
                    FeedTile(p, if (isLast) "+${allPhotos.size - MAX_TILES}" else null)
                }
                val totalFocus = sorted.sumOf { it.focusedMs }
                FeedDay(
                    epochDay = date.toEpochDay(),
                    dayLabel = Formatters.dayLabel(date),
                    weekday = Formatters.weekday(date),
                    summary = Formatters.daySummary(sorted.size, totalFocus),
                    tiles = tiles,
                    sessions = sessions,
                )
            }
    }
}
