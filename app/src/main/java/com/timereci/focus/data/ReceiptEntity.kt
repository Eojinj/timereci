package com.timereci.focus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A published receipt — the archive unit. One row is created when a session is completed
 * and the user commits it to the feed. Abandoned sessions never reach this table
 * ("포기 세션은 흔적을 남기지 않는다").
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Wall-clock publish time (for display + grouping into feed days). */
    val issuedAtEpoch: Long,

    /** Actual focused duration — drives receipt-length rendering. */
    val focusedMs: Long,

    /** Duration the user originally set. */
    val plannedMs: Long,

    /** The task label entered before the session (may be blank). */
    val taskLabel: String,

    /** Single free-form note, edited during and/or after the session. */
    val comment: String? = null,

    /** Ordered photos; empty means the card shows a gradient placeholder. */
    val photos: List<PhotoRef> = emptyList(),

    /** The session's start time, used only to tell two sessions apart. Auto-saving happens
     * when the timer reaches COMPLETED, and a crash between saving and resetting would
     * otherwise let the same session be recorded twice on the next launch. 0 for rows
     * written before this column existed. */
    val startedAtEpoch: Long = 0L,

    /** Unused: a leftover column from the original concept. Kept only so the stored schema
     * stays as-is; dropping it would need another Room migration for no user-visible gain. */
    val themeId: String = "mono",
)
