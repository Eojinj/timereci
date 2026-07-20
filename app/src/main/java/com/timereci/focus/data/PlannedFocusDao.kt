package com.timereci.focus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedFocusDao {

    @Query("SELECT * FROM planned_focus ORDER BY createdAtEpoch ASC")
    fun observeAll(): Flow<List<PlannedFocusEntity>>

    @Insert
    suspend fun insert(item: PlannedFocusEntity): Long

    @Query("DELETE FROM planned_focus WHERE id = :id")
    suspend fun deleteById(id: Long)
}
