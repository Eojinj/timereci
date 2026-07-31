package com.timereci.focus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedFocusDao {

    @Query("SELECT * FROM planned_focus ORDER BY createdAtEpoch ASC")
    fun observeAll(): Flow<List<PlannedFocusEntity>>

    /**
     * The queue in "what to do next" order. One-offs come first so they actually drain — a
     * repeating task is never finished, so letting one hold the head would starve the rest.
     * (Today's own list keeps [observeAll]'s creation order; this ordering is only for
     * picking what to offer next.)
     */
    @Query("SELECT * FROM planned_focus ORDER BY isRepeating ASC, createdAtEpoch ASC")
    suspend fun getQueue(): List<PlannedFocusEntity>

    @Insert
    suspend fun insert(item: PlannedFocusEntity): Long

    @Update
    suspend fun update(item: PlannedFocusEntity)

    @Query("DELETE FROM planned_focus WHERE id = :id")
    suspend fun deleteById(id: Long)
}
