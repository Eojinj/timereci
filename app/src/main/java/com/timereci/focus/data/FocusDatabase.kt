package com.timereci.focus.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReceiptEntity::class, ActiveSessionEntity::class, PlannedFocusEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun plannedFocusDao(): PlannedFocusDao

    companion object {
        const val NAME = "focus.db"

        /** v2 adds the planned_focus table; existing receipts are preserved. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `planned_focus` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`plannedMs` INTEGER NOT NULL, " +
                        "`createdAtEpoch` INTEGER NOT NULL)",
                )
            }
        }

        /** v3 adds the repeating flag on planned focus; existing tasks stay one-offs (0). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `planned_focus` ADD COLUMN `isRepeating` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
