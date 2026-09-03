package com.haruchi.today.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {

    @Query("SELECT * FROM active_session WHERE id = :id")
    fun observe(id: Int = ActiveSessionEntity.SINGLETON_ID): Flow<ActiveSessionEntity?>

    @Query("SELECT * FROM active_session WHERE id = :id")
    suspend fun get(id: Int = ActiveSessionEntity.SINGLETON_ID): ActiveSessionEntity?

    @Upsert
    suspend fun upsert(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun clear()
}
