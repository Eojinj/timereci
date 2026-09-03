package com.haruchi.today.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Query("SELECT * FROM receipts ORDER BY issuedAtEpoch DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): ReceiptEntity?

    @Query("SELECT COUNT(*) FROM receipts WHERE startedAtEpoch = :startedAtEpoch")
    suspend fun countForSession(startedAtEpoch: Long): Int

    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Update
    suspend fun update(receipt: ReceiptEntity)

    @Delete
    suspend fun delete(receipt: ReceiptEntity)
}
