package com.timereci.focus.di

import android.content.Context
import androidx.room.Room
import com.timereci.focus.data.ActiveSessionDao
import com.timereci.focus.data.FocusDatabase
import com.timereci.focus.data.PlannedFocusDao
import com.timereci.focus.data.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusDatabase =
        Room.databaseBuilder(context, FocusDatabase::class.java, FocusDatabase.NAME)
            .addMigrations(FocusDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideReceiptDao(db: FocusDatabase): ReceiptDao = db.receiptDao()

    @Provides
    fun provideActiveSessionDao(db: FocusDatabase): ActiveSessionDao = db.activeSessionDao()

    @Provides
    fun providePlannedFocusDao(db: FocusDatabase): PlannedFocusDao = db.plannedFocusDao()
}
