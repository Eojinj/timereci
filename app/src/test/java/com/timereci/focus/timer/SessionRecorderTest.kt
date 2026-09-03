package com.timereci.focus.timer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.timereci.focus.data.FocusDatabase
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoStorage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRecorderTest {

    private lateinit var db: FocusDatabase
    private lateinit var repository: FocusRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FocusRepository(
            db.receiptDao(),
            db.activeSessionDao(),
            db.plannedFocusDao(),
            PhotoStorage(context),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun completed(startedAt: Long) = TimerState(
        phase = TimerPhase.COMPLETED,
        plannedMs = 25 * 60_000L,
        remainingMs = 0L,
        focusedMs = 25 * 60_000L,
        taskLabel = "논문 읽기",
        startedAtEpoch = startedAt,
    )

    @Test
    fun `records a completed session`() = runTest {
        val id = SessionRecorder.record(repository, completed(1_000L))

        assertEquals(1, db.receiptDao().countForSession(1_000L))
        val saved = repository.getReceipt(id!!)
        assertEquals("논문 읽기", saved!!.taskLabel)
        assertEquals(25 * 60_000L, saved.focusedMs)
    }

    @Test
    fun `does not record the same session twice`() = runTest {
        SessionRecorder.record(repository, completed(1_000L))
        val second = SessionRecorder.record(repository, completed(1_000L))

        assertNull(second)
        assertEquals(1, db.receiptDao().countForSession(1_000L))
    }

    @Test
    fun `records two different sessions`() = runTest {
        SessionRecorder.record(repository, completed(1_000L))
        SessionRecorder.record(repository, completed(2_000L))

        assertEquals(1, db.receiptDao().countForSession(1_000L))
        assertEquals(1, db.receiptDao().countForSession(2_000L))
    }
}
