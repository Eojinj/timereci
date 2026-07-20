package com.timereci.focus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A planned focus intention ("오늘 해야 할 집중"). Tapping one starts the timer prefilled
 * with its label and duration. Purely a to-do list — it isn't tied to a published receipt.
 */
@Entity(tableName = "planned_focus")
data class PlannedFocusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val plannedMs: Long,
    val createdAtEpoch: Long,
)
