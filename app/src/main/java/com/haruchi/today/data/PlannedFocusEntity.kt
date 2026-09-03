package com.haruchi.today.data

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

    /**
     * A recurring task: starting it leaves it in the list so it can be run again tomorrow,
     * instead of being consumed like a one-off to-do. This is what makes a task's stats
     * accumulate over time rather than being a single entry in history.
     */
    val isRepeating: Boolean = false,
)
