package com.haruchi.today.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single in-flight session (0 or 1 row). Persisted so the timer survives process
 * death: we store an *end time* against [android.os.SystemClock.elapsedRealtime], not a
 * tick count, so elapsed time keeps advancing while the app is dead.
 */
@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,

    /** Duration the user set for this session. */
    val plannedMs: Long,

    /** Wall-clock start (used to compute focusedMs / issuedAt on completion). */
    val startedAtEpoch: Long,

    /**
     * elapsedRealtime target at which the session completes, while running.
     * Null when paused — see [pausedRemainingMs].
     */
    val endsAtElapsed: Long?,

    /** Remaining ms captured at the moment of pause; null while running. */
    val pausedRemainingMs: Long?,

    val taskLabel: String,

    /** Photo picked before start to act as the timer backdrop. */
    val backdropFileName: String? = null,

    /** Comment drafted while focusing (kept even if the app is killed). */
    val draftComment: String? = null,
) {
    val isRunning: Boolean get() = endsAtElapsed != null

    companion object {
        const val SINGLETON_ID = 0
    }
}
