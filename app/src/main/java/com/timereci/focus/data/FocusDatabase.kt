package com.timereci.focus.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ReceiptEntity::class, ActiveSessionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun activeSessionDao(): ActiveSessionDao

    companion object {
        const val NAME = "focus.db"
    }
}
