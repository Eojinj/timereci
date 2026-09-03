# 하루치 2 — 캘린더 축 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 시스템 캘린더를 읽고 쓰는 계층을 만들고, Today를 "일정은 땅금, 할일은 목록" 한 장으로 재구성한다.

**Architecture:** 캘린더는 `CalendarContract.Instances`만 읽는다 — 반복 규칙이 이미 회차별로 펼쳐져 나오므로 RRULE·타임존·DST를 우리가 해석할 일이 없다. 캐시하지 않고 `ContentObserver`로 갱신해서, 진실의 원본이 시스템 캘린더 하나로 유지된다. 할일 쪽은 `dueDateEpochDay`와 `doneAtEpoch` 두 필드만 추가하고, 이월은 DB를 고치지 않고 읽는 시점에 계산한다.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose, Hilt, Room, `android.provider.CalendarContract`, JUnit4 + Robolectric

**디자인:** `docs/design/2026-09-02-haruchi-three-screens.dc.html` (Today · ＋ 시트 · 설정)과 토큰 정리 `docs/design/2026-09-02-tokens-and-mapping.md`. 화면을 만드는 작업(Task 5·7)은 그 문서를 따른다.

**선행 조건:** 계획 1(`2026-09-02-haruchi-1-strip-and-rename.md`)이 끝나 있어야 한다. 아래 경로는 전부 개명 후 기준(`com.haruchi.today`)이다.

---

### Task 1: 할일에 날짜와 완료를 붙인다

이월을 DB에 쓰지 않고 읽는 시점에 계산한다. 자정 알람도 백그라운드 작업도 없고, "3일째" 배지가 공짜로 나온다.

**Files:**
- Modify: `app/src/main/java/com/haruchi/today/data/PlannedFocusEntity.kt`
- Modify: `app/src/main/java/com/haruchi/today/data/FocusDatabase.kt`
- Modify: `app/src/main/java/com/haruchi/today/data/FocusRepository.kt`
- Create: `app/src/main/java/com/haruchi/today/ui/today/TodayTasks.kt`
- Test: `app/src/test/java/com/haruchi/today/ui/today/TodayTasksTest.kt`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/haruchi/today/ui/today/TodayTasksTest.kt`:

```kotlin
package com.haruchi.today.ui.today

import com.haruchi.today.data.PlannedFocusEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodayTasksTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val today: LocalDate = LocalDate.of(2026, 9, 2)

    private fun task(
        id: Long,
        label: String,
        due: LocalDate,
        repeating: Boolean = false,
        doneOn: LocalDate? = null,
    ) = PlannedFocusEntity(
        id = id,
        label = label,
        plannedMs = 25 * 60_000L,
        createdAtEpoch = id,
        isRepeating = repeating,
        dueDateEpochDay = due.toEpochDay(),
        doneAtEpoch = doneOn?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
    )

    @Test
    fun `shows tasks due on the day being viewed`() {
        val tasks = listOf(task(1, "논문 읽기", today))

        val result = TodayTasks.visibleOn(tasks, day = today, today = today, zone = zone)

        assertEquals(listOf("논문 읽기"), result.map { it.task.label })
        assertEquals(0, result.single().carriedDays)
    }

    @Test
    fun `carries unfinished tasks forward to today`() {
        val tasks = listOf(task(1, "운동", today.minusDays(3)))

        val result = TodayTasks.visibleOn(tasks, day = today, today = today, zone = zone)

        assertEquals(3, result.single().carriedDays)
    }

    @Test
    fun `does not carry a task that was finished`() {
        val tasks = listOf(task(1, "장보기", today.minusDays(1), doneOn = today.minusDays(1)))

        val result = TodayTasks.visibleOn(tasks, day = today, today = today, zone = zone)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `does not carry anything when viewing a past day`() {
        val tasks = listOf(task(1, "운동", today.minusDays(3)))

        val result = TodayTasks.visibleOn(
            tasks, day = today.minusDays(1), today = today, zone = zone,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `repeating tasks appear on every day`() {
        val tasks = listOf(task(1, "스트레칭", today.minusDays(30), repeating = true))

        val result = TodayTasks.visibleOn(
            tasks, day = today.plusDays(5), today = today, zone = zone,
        )

        assertEquals(listOf("스트레칭"), result.map { it.task.label })
        assertEquals(0, result.single().carriedDays)
    }

    @Test
    fun `a repeating task done today reads as done today only`() {
        val tasks = listOf(
            task(1, "스트레칭", today.minusDays(30), repeating = true, doneOn = today),
        )

        val doneToday = TodayTasks.visibleOn(tasks, day = today, today = today, zone = zone)
        val notDoneTomorrow = TodayTasks.visibleOn(
            tasks, day = today.plusDays(1), today = today, zone = zone,
        )

        assertTrue(doneToday.single().isDone)
        assertTrue(!notDoneTomorrow.single().isDone)
    }

    @Test
    fun `finished tasks sort below unfinished ones`() {
        val tasks = listOf(
            task(1, "먼저 만든 것", today, doneOn = today),
            task(2, "나중 만든 것", today),
        )

        val result = TodayTasks.visibleOn(tasks, day = today, today = today, zone = zone)

        assertEquals(listOf("나중 만든 것", "먼저 만든 것"), result.map { it.task.label })
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*TodayTasksTest*"`

Expected: 컴파일 실패 — `Unresolved reference: TodayTasks`, `dueDateEpochDay`, `doneAtEpoch`

- [ ] **Step 3: 엔티티에 필드를 추가한다**

`PlannedFocusEntity.kt`의 `isRepeating` 아래에 추가:

```kotlin
    /**
     * Which day this belongs to. Every task belongs to some day — there is no "someday"
     * drawer. Repeating tasks ignore this and appear on every day.
     */
    val dueDateEpochDay: Long,

    /** When it was ticked off, if it was. Repeating tasks read as done only on the day
     * they were last ticked, so they reset at midnight without any scheduled work. */
    val doneAtEpoch: Long? = null,
```

`dueDateEpochDay`에 기본값을 주지 않는다 — 기존 호출부를 컴파일러가 전부 짚어주게 한다.

`FocusDatabase.kt`에서 `version = 4`를 `version = 5`로 바꾼다.

- [ ] **Step 4: `TodayTasks`를 만든다**

`app/src/main/java/com/haruchi/today/ui/today/TodayTasks.kt`:

```kotlin
package com.haruchi.today.ui.today

import com.haruchi.today.data.PlannedFocusEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One row of the to-do list as Today shows it. */
data class TodayTask(
    val task: PlannedFocusEntity,
    /** How many days late this is. 0 unless it was carried forward. */
    val carriedDays: Int,
    val isDone: Boolean,
)

/**
 * Which to-dos belong on a given day.
 *
 * Carry-forward is computed here rather than written to the database: nothing has to run at
 * midnight, viewing a past day shows what that day actually held, and "3일째" falls out of
 * the arithmetic instead of needing its own column.
 */
object TodayTasks {

    fun visibleOn(
        tasks: List<PlannedFocusEntity>,
        day: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): List<TodayTask> = tasks
        .mapNotNull { task ->
            val doneOn = task.doneAtEpoch?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            }
            val isDone = doneOn == day

            if (task.isRepeating) {
                return@mapNotNull TodayTask(task, carriedDays = 0, isDone = isDone)
            }

            val due = LocalDate.ofEpochDay(task.dueDateEpochDay)
            when {
                due == day -> TodayTask(task, carriedDays = 0, isDone = isDone)
                // Only the day you are actually living in collects what fell behind.
                due < day && day == today && doneOn == null ->
                    TodayTask(task, ChronoUnit.DAYS.between(due, today).toInt(), false)
                else -> null
            }
        }
        .sortedWith(compareBy({ it.isDone }, { it.task.createdAtEpoch }))
}
```

- [ ] **Step 5: 기존 호출부를 고친다**

Run: `./gradlew :app:assembleDebug`

`dueDateEpochDay`가 없다고 깨지는 곳은 `FocusRepository.addPlannedFocus`와 그 호출부다. 저장소 함수에 날짜를 받게 한다:

```kotlin
    suspend fun addPlannedFocus(
        label: String,
        plannedMs: Long,
        isRepeating: Boolean = false,
        dueDateEpochDay: Long = LocalDate.now().toEpochDay(),
    ) {
```

그리고 만드는 `PlannedFocusEntity(...)`에 `dueDateEpochDay = dueDateEpochDay`를 넘긴다. 완료 토글도 추가한다:

```kotlin
    suspend fun setPlannedFocusDone(id: Long, doneAtEpoch: Long?) {
        val item = plannedFocusDao.getById(id) ?: return
        plannedFocusDao.update(item.copy(doneAtEpoch = doneAtEpoch))
    }
```

`PlannedFocusDao`에 `getById`가 없으면 추가한다:

```kotlin
    @Query("SELECT * FROM planned_focus WHERE id = :id")
    suspend fun getById(id: Long): PlannedFocusEntity?
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*TodayTasksTest*"`

Expected: 7 tests, all PASS

- [ ] **Step 7: 커밋한다**

```bash
git add -A
```

```bash
git commit -m "Give to-dos a day and a done mark, with carry-forward computed on read"
```

---

### Task 2: 캘린더 행을 우리 모델로 옮긴다

`Instances` 커서 한 줄을 `CalendarEvent`로 바꾸는 순수한 매핑. 종일 일정의 시각이 UTC 자정으로 저장된다는 함정이 여기 있으므로, 저장소보다 먼저 이것부터 못 박는다.

**Files:**
- Create: `app/src/main/java/com/haruchi/today/data/calendar/CalendarEvent.kt`
- Test: `app/src/test/java/com/haruchi/today/data/calendar/CalendarEventTest.kt`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/haruchi/today/data/calendar/CalendarEventTest.kt`:

```kotlin
package com.haruchi.today.data.calendar

import android.database.MatrixCursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class CalendarEventTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    private fun cursorOf(vararg rows: Array<Any?>) =
        MatrixCursor(CalendarEvent.PROJECTION).apply { rows.forEach { addRow(it) } }

    @Test
    fun `maps a timed event`() {
        val start = LocalDate.of(2026, 9, 2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val end = start + 60 * 60_000L
        val cursor = cursorOf(arrayOf<Any?>(42L, "팀 회의", start, end, 0, -0xbf7f80))

        cursor.moveToFirst()
        val event = CalendarEvent.fromCursor(cursor)

        assertEquals(42L, event.eventId)
        assertEquals("팀 회의", event.title)
        assertEquals(60 * 60_000L, event.durationMs)
        assertTrue(!event.isAllDay)
    }

    @Test
    fun `an untitled event still has something to show`() {
        val cursor = cursorOf(arrayOf<Any?>(1L, null, 0L, 0L, 0, 0))

        cursor.moveToFirst()
        assertEquals("", CalendarEvent.fromCursor(cursor).title)
    }

    @Test
    fun `all-day events are stored at UTC midnight, not local midnight`() {
        // Instances stores an all-day event's BEGIN as midnight UTC of that date. Reading it
        // in Asia/Seoul (UTC+9) as a local time would land on 09:00, and a date-only reading
        // in a negative-offset zone would land on the day before.
        val utcMidnight = LocalDate.of(2026, 9, 2)
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val cursor = cursorOf(arrayOf<Any?>(7L, "추석 연휴", utcMidnight, utcMidnight, 1, 0))

        cursor.moveToFirst()
        val event = CalendarEvent.fromCursor(cursor)

        assertTrue(event.isAllDay)
        assertEquals(LocalDate.of(2026, 9, 2), event.allDayDate())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*CalendarEventTest*"`

Expected: 컴파일 실패 — `Unresolved reference: CalendarEvent`

- [ ] **Step 3: `CalendarEvent`를 만든다**

`app/src/main/java/com/haruchi/today/data/calendar/CalendarEvent.kt`:

```kotlin
package com.haruchi.today.data.calendar

import android.database.Cursor
import android.provider.CalendarContract.Instances
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One occurrence of a calendar event, as Today needs it. Attendees, reminders, descriptions
 * and locations are deliberately not read — this app only ever shows a line and starts a timer.
 */
data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val isAllDay: Boolean,
    val color: Int,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    /**
     * The date an all-day event falls on. All-day rows are stored at midnight UTC regardless
     * of the device's zone, so they must be read back in UTC — reading them locally shifts
     * them by the offset and, west of Greenwich, onto the wrong day.
     */
    fun allDayDate(): LocalDate =
        Instant.ofEpochMilli(startMs).atZone(ZoneId.of("UTC")).toLocalDate()

    companion object {
        val PROJECTION = arrayOf(
            Instances.EVENT_ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.DISPLAY_COLOR,
        )

        fun fromCursor(cursor: Cursor) = CalendarEvent(
            eventId = cursor.getLong(0),
            title = cursor.getString(1).orEmpty(),
            startMs = cursor.getLong(2),
            endMs = cursor.getLong(3),
            isAllDay = cursor.getInt(4) == 1,
            color = cursor.getInt(5),
        )
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*CalendarEventTest*"`

Expected: 3 tests, all PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
```

```bash
git commit -m "Add the calendar event model and its cursor mapping"
```

---

### Task 3: 캘린더 저장소

**Files:**
- Create: `app/src/main/java/com/haruchi/today/data/calendar/CalendarRepository.kt`
- Create: `app/src/main/java/com/haruchi/today/data/calendar/ContentResolverCalendarRepository.kt`
- Modify: `app/src/main/java/com/haruchi/today/di/DataModule.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/haruchi/today/data/calendar/ContentResolverCalendarRepositoryTest.kt`

- [ ] **Step 1: 인터페이스를 정의한다**

`CalendarRepository.kt`:

```kotlin
package com.haruchi.today.data.calendar

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/** A calendar we are allowed to write into. */
data class CalendarAccount(val id: Long, val displayName: String, val accountName: String)

/**
 * Reads and writes the device's calendars. Kept behind an interface so Today can be assembled
 * against a fake, and so the ContentResolver details stay in one file.
 */
interface CalendarRepository {

    /** Occurrences on [date], sorted by start. Emits again whenever the provider changes. */
    fun eventsOn(date: LocalDate): Flow<List<CalendarEvent>>

    /** Which days in [month] have at least one event — the dots in the mini calendar. */
    fun daysWithEvents(month: YearMonth): Flow<Set<LocalDate>>

    suspend fun writableCalendars(): List<CalendarAccount>

    /** Returns the new event's id, or null if it could not be written. */
    suspend fun addEvent(title: String, startMs: Long, endMs: Long, calendarId: Long): Long?

    fun openInSystemCalendar(eventId: Long)
}
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`ContentResolverCalendarRepositoryTest.kt`. Robolectric에 가짜 프로바이더를 등록해 `eventsOn`이 정렬된 결과를 내는지 본다.

```kotlin
package com.haruchi.today.data.calendar

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

class FakeCalendarProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor = MatrixCursor(CalendarEvent.PROJECTION).apply {
        val zone = ZoneId.of("Asia/Seoul")
        val threePm = LocalDate.of(2026, 9, 2).atTime(15, 0).atZone(zone).toInstant().toEpochMilli()
        val nineAm = LocalDate.of(2026, 9, 2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        addRow(arrayOf<Any?>(2L, "치과", threePm, threePm + 3_600_000L, 0, 0))
        addRow(arrayOf<Any?>(1L, "팀 회의", nineAm, nineAm + 3_600_000L, 0, 0))
    }
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}

@RunWith(RobolectricTestRunner::class)
class ContentResolverCalendarRepositoryTest {

    private lateinit var repository: ContentResolverCalendarRepository

    @Before
    fun setUp() {
        Robolectric.buildContentProvider(FakeCalendarProvider::class.java)
            .create(CalendarContract.AUTHORITY)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository = ContentResolverCalendarRepository(context, ZoneId.of("Asia/Seoul"))
    }

    @Test
    fun `returns the day's events sorted by start time`() = runTest {
        val events = repository.eventsOn(LocalDate.of(2026, 9, 2)).first()

        assertEquals(listOf("팀 회의", "치과"), events.map { it.title })
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*ContentResolverCalendarRepositoryTest*"`

Expected: 컴파일 실패 — `Unresolved reference: ContentResolverCalendarRepository`

- [ ] **Step 4: 구현을 만든다**

`ContentResolverCalendarRepository.kt`:

```kotlin
package com.haruchi.today.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads `Instances`, never `Events`, for anything shown on screen: Instances hands back
 * recurring events already expanded into individual occurrences, so RRULE, per-occurrence
 * exceptions, time zones and DST are the provider's problem rather than ours.
 *
 * Nothing is cached. The provider is the single source of truth, so there is no second copy
 * that can drift out of step with the Samsung calendar.
 */
/*
 * Constructed by CalendarModule, not by @Inject: Dagger ignores Kotlin default arguments, so
 * an injected constructor would demand a ZoneId binding. Taking the zone as a plain parameter
 * also lets tests pin it.
 */
class ContentResolverCalendarRepository(
    private val context: Context,
    private val zone: ZoneId,
) : CalendarRepository {

    override fun eventsOn(date: LocalDate): Flow<List<CalendarEvent>> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return providerChanges().map { queryInstances(start, end) }.flowOn(Dispatchers.IO)
    }

    override fun daysWithEvents(month: YearMonth): Flow<Set<LocalDate>> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return providerChanges().map {
            queryInstances(start, end).map { event ->
                if (event.isAllDay) event.allDayDate()
                else java.time.Instant.ofEpochMilli(event.startMs).atZone(zone).toLocalDate()
            }.toSet()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun writableCalendars(): List<CalendarAccount> {
        val projection = arrayOf(
            Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME,
        )
        // ACCESS_LEVEL >= CONTRIBUTOR is the provider's own definition of "may add events".
        val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}"
        return runCatching {
            context.contentResolver.query(
                Calendars.CONTENT_URI, projection, selection, null, null,
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CalendarAccount(
                                id = cursor.getLong(0),
                                displayName = cursor.getString(1).orEmpty(),
                                accountName = cursor.getString(2).orEmpty(),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    override suspend fun addEvent(
        title: String,
        startMs: Long,
        endMs: Long,
        calendarId: Long,
    ): Long? = runCatching {
        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, calendarId)
            put(Events.TITLE, title)
            put(Events.DTSTART, startMs)
            put(Events.DTEND, endMs)
            put(Events.EVENT_TIMEZONE, zone.id)
        }
        context.contentResolver.insert(Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
    }.getOrNull()

    override fun openInSystemCalendar(eventId: Long) {
        val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun queryInstances(startMs: Long, endMs: Long): List<CalendarEvent> {
        val uri: Uri = Instances.CONTENT_URI.buildUpon()
            .appendPath(startMs.toString())
            .appendPath(endMs.toString())
            .build()
        return runCatching {
            context.contentResolver.query(uri, CalendarEvent.PROJECTION, null, null, null)
                ?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(CalendarEvent.fromCursor(cursor))
                    }
                }.orEmpty()
        }.getOrDefault(emptyList()).sortedBy { it.startMs }
    }

    /** Emits once immediately, then again every time the calendar provider changes. */
    private fun providerChanges(): Flow<Unit> = callbackFlow {
        trySend(Unit)
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                CalendarContract.CONTENT_URI, true, observer,
            )
        }
        awaitClose { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
    }
}
```

모든 프로바이더 접근이 `runCatching`으로 감싸여 있다. 권한이 나중에 회수되면 `SecurityException`이 나는데, 그때 앱이 죽는 대신 "일정 없음"으로 떨어지고 Task 4의 권한 줄이 다시 나타난다.

- [ ] **Step 5: DI와 매니페스트를 고친다**

`DataModule.kt`에 바인딩을 추가한다. `object DataModule` 옆에 인터페이스 바인딩용 모듈을 하나 더 둔다:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {
    @Provides
    @Singleton
    fun provideCalendarRepository(@ApplicationContext context: Context): CalendarRepository =
        ContentResolverCalendarRepository(context, ZoneId.systemDefault())
}
```

`@Binds`가 아니라 `@Provides`인 이유는 위와 같다 — 구현이 `ZoneId`를 생성자로 받는데 Dagger는 Kotlin 기본 인자를 모른다.

임포트: `com.haruchi.today.data.calendar.CalendarRepository`, `com.haruchi.today.data.calendar.ContentResolverCalendarRepository`, `java.time.ZoneId`.

`AndroidManifest.xml`의 기존 `uses-permission` 목록 아래에 추가:

```xml
    <!-- Read and add calendar events. Requested in context from Today, never at first launch. -->
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*ContentResolverCalendarRepositoryTest*"`

Expected: 1 test, PASS

- [ ] **Step 7: 커밋한다**

```bash
git add -A
```

```bash
git commit -m "Read and write the device calendar through Instances"
```

---

### Task 4: 캘린더 권한을 맥락 안에서 묻는다

첫 실행 때 묻지 않는다. Today 맨 위의 "캘린더 연결하기" 줄을 탭했을 때 묻는다.

**Files:**
- Create: `app/src/main/java/com/haruchi/today/ui/today/CalendarPermission.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/i18n/Strings.kt`

- [ ] **Step 1: 권한 상태를 담는 컴포저블을 만든다**

`CalendarPermission.kt`:

```kotlin
package com.haruchi.today.ui.today

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR,
)

fun hasCalendarPermission(context: Context): Boolean = CALENDAR_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

/** Granted state plus the one call that asks for it. */
class CalendarAccess(
    val granted: Boolean,
    val permanentlyDenied: Boolean,
    val request: () -> Unit,
)

/**
 * Asks only when [CalendarAccess.request] is called — never on composition. A permission
 * dialog that appears before the user has asked for anything gets reflexively refused, and
 * a refusal is much harder to walk back than a delay.
 */
@Composable
fun rememberCalendarAccess(): CalendarAccess {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCalendarPermission(context)) }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        asked = true
        granted = result.values.all { it }
    }

    return CalendarAccess(
        granted = granted,
        // Asked once, still refused: the system will not show the dialog again.
        permanentlyDenied = asked && !granted,
        request = {
            if (granted) return@CalendarAccess
            if (asked) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } else {
                launcher.launch(CALENDAR_PERMISSIONS)
            }
        },
    )
}
```

- [ ] **Step 2: Today 맨 위에 줄을 놓는다**

`TodayScreen.kt`에서 `rememberCalendarAccess()`를 부르고, `access.granted`가 false일 때만 목록 맨 위에 한 줄짜리 행을 그린다. 문구는 `access.permanentlyDenied`에 따라 두 가지다:

- `false` → "캘린더 연결하기" / 탭하면 `access.request()`
- `true` → "설정에서 캘린더 권한을 켜주세요" / 탭하면 같은 `access.request()`가 시스템 설정을 연다

`Strings.kt`의 영어·한국어 양쪽에 `connectCalendar`, `calendarPermissionBlocked` 두 문자열을 추가한다.

- [ ] **Step 3: 손으로 확인하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

기기에서:
1. 앱을 새로 설치하고 연다 → 권한 팝업이 **뜨지 않는다**. 맨 위에 "캘린더 연결하기" 줄이 있다
2. 그 줄을 탭한다 → 권한 팝업이 뜬다. 허용하면 줄이 사라진다
3. 앱을 지우고 다시 설치해 이번엔 거부한다 → 줄이 "설정에서..."로 바뀌고, 탭하면 시스템 설정이 열린다
4. 거부한 상태로 앱을 계속 쓴다 → 할일과 타이머가 정상 동작한다

```bash
git add -A
```

```bash
git commit -m "Ask for calendar permission from Today, in context"
```

---

### Task 5: Today에 일정 땅금을 얹는다

디자인은 `docs/design/2026-09-02-haruchi-three-screens.dc.html`의 화면 1이고, 색·치수는 `docs/design/2026-09-02-tokens-and-mapping.md`에 정리돼 있다.

정렬과 흐림 판정을 순수 함수로 빼고 먼저 테스트한다. 화면에서 직접 하면 겹치는 일정이나 자정을 걸친 일정을 손으로 앱을 열어봐야만 확인할 수 있다.

**Files:**
- Create: `app/src/main/java/com/haruchi/today/ui/today/DayAgenda.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/today/TodayViewModel.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/haruchi/today/data/SettingsRepository.kt`
- Test: `app/src/test/java/com/haruchi/today/ui/today/DayAgendaTest.kt`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/haruchi/today/ui/today/DayAgendaTest.kt`:

```kotlin
package com.haruchi.today.ui.today

import com.haruchi.today.data.calendar.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayAgendaTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val day: LocalDate = LocalDate.of(2026, 9, 2)

    /** Hours past this day's midnight, so 26 means 02:00 tomorrow. */
    private fun at(hour: Int, minute: Int = 0): Long = day.atStartOfDay(zone)
        .plusHours(hour.toLong()).plusMinutes(minute.toLong())
        .toInstant().toEpochMilli()

    private fun timed(id: Long, title: String, from: Long, to: Long) =
        CalendarEvent(id, title, from, to, isAllDay = false, color = 0)

    private fun allDay(id: Long, title: String) = CalendarEvent(
        id, title,
        startMs = day.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        endMs = day.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        isAllDay = true, color = 0,
    )

    @Test
    fun `an empty day produces no rows`() {
        assertTrue(DayAgenda.rowsFor(emptyList(), day, now = at(11), zone = zone).isEmpty())
    }

    @Test
    fun `all-day events come first, then timed ones in order`() {
        val events = listOf(
            timed(2, "치과", at(15), at(16)),
            allDay(9, "분기 마감 주간"),
            timed(1, "스탠드업", at(9, 30), at(9, 45)),
        )

        val rows = DayAgenda.rowsFor(events, day, now = at(8), zone = zone)

        assertEquals(listOf("분기 마감 주간", "스탠드업", "치과"), rows.map { it.event.title })
    }

    @Test
    fun `overlapping events both appear, ordered by start`() {
        val events = listOf(
            timed(1, "회의 A", at(9), at(11)),
            timed(2, "회의 B", at(10), at(10, 30)),
        )

        val rows = DayAgenda.rowsFor(events, day, now = at(8), zone = zone)

        assertEquals(listOf("회의 A", "회의 B"), rows.map { it.event.title })
    }

    @Test
    fun `an event that has ended reads as past`() {
        val events = listOf(
            timed(1, "스탠드업", at(9, 30), at(9, 45)),
            timed(2, "치과", at(15), at(16)),
        )

        val rows = DayAgenda.rowsFor(events, day, now = at(12, 40), zone = zone)

        assertTrue(rows.first { it.event.title == "스탠드업" }.isPast)
        assertFalse(rows.first { it.event.title == "치과" }.isPast)
    }

    @Test
    fun `nothing is past on a future day`() {
        val events = listOf(timed(1, "온보딩 워크숍", at(10), at(12)))

        val rows = DayAgenda.rowsFor(events, day, now = at(-10), zone = zone)

        assertFalse(rows.single().isPast)
    }

    @Test
    fun `an all-day event is not past on the day it falls on`() {
        val rows = DayAgenda.rowsFor(listOf(allDay(9, "재택")), day, now = at(23), zone = zone)

        assertFalse(rows.single().isPast)
    }

    @Test
    fun `an event running past midnight is clipped to the day being viewed`() {
        val rows = DayAgenda.rowsFor(
            listOf(timed(1, "야근", at(22), at(26))), day, now = at(8), zone = zone,
        )

        assertEquals(at(24) - 1, rows.single().endsAtOnThisDay)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*DayAgendaTest*"`

Expected: 컴파일 실패 — `Unresolved reference: DayAgenda`

- [ ] **Step 3: `DayAgenda`를 만든다**

`app/src/main/java/com/haruchi/today/ui/today/DayAgenda.kt`:

```kotlin
package com.haruchi.today.ui.today

import com.haruchi.today.data.calendar.CalendarEvent
import java.time.LocalDate
import java.time.ZoneId

/** One line in the day's timeline. */
data class AgendaRow(
    val event: CalendarEvent,
    /** The event's end as it should read on this day — something running past midnight
     * is shown ending at this day's last instant, not on tomorrow's clock. */
    val endsAtOnThisDay: Long,
    /** Already over, so the row is drawn faded. The design has no "now" line; dimming what
     * has passed is how the screen says where you are in the day. */
    val isPast: Boolean,
)

/**
 * Lays out one day's events. Kept apart from the screen so the awkward cases — an event
 * crossing midnight, two events overlapping, an all-day event's odd stored time — are
 * settled by tests rather than by opening the app and squinting.
 */
object DayAgenda {

    fun rowsFor(
        events: List<CalendarEvent>,
        day: LocalDate,
        now: Long,
        zone: ZoneId,
    ): List<AgendaRow> {
        val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        fun row(event: CalendarEvent) = AgendaRow(
            event = event,
            endsAtOnThisDay = minOf(event.endMs, dayEnd - 1),
            isPast = event.endMs <= now,
        )

        // All-day first. The design's mock happens to list one mid-day, but that is the
        // order of its demo array; a timed list with "종일" wedged into it reads as broken.
        return events.filter { it.isAllDay }.map(::row) +
            events.filterNot { it.isAllDay }.sortedBy { it.startMs }.map(::row)
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*DayAgendaTest*"`

Expected: 7 tests, all PASS

- [ ] **Step 5: 설정에 「지난 일정 흐리게」를 추가한다**

디자인의 `dimPastEvents` 노브다. `SettingsRepository.kt`의 `FocusSettings`에 추가:

```kotlin
    /** Fade events that are already over. The screen has no "now" line, so this is what
     * shows where you are in the day. */
    val dimPastEvents: Boolean = true,
```

`KEY_DIM_PAST = booleanPreferencesKey("dim_past_events")`와 setter를 기존 것들과 같은 모양으로 추가한다.

- [ ] **Step 6: 뷰모델이 일정과 할일을 함께 내놓게 한다**

`TodayViewModel`에 `CalendarRepository`와 `@ApplicationContext Context`를 주입하고, 보고 있는 날짜를 상태로 둔다:

```kotlin
    private val _day = MutableStateFlow(LocalDate.now())
    val day: StateFlow<LocalDate> = _day.asStateFlow()

    fun showDay(date: LocalDate) { _day.value = date }
    fun goToToday() { _day.value = LocalDate.now() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val agenda: StateFlow<List<AgendaRow>> = _day
        .flatMapLatest { date ->
            if (!hasCalendarPermission(context)) flowOf(emptyList())
            else calendarRepository.eventsOn(date).map { events ->
                DayAgenda.rowsFor(
                    events, date, System.currentTimeMillis(), ZoneId.systemDefault(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TodayTask>> = combine(
        repository.observePlannedFocus(), _day,
    ) { all, date ->
        TodayTasks.visibleOn(all, day = date, today = LocalDate.now(), zone = ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openInSystemCalendar(eventId: Long) = calendarRepository.openInSystemCalendar(eventId)

    fun toggleDone(task: TodayTask) {
        viewModelScope.launch {
            repository.setPlannedFocusDone(
                task.task.id,
                if (task.isDone) null else System.currentTimeMillis(),
            )
        }
    }
```

- [ ] **Step 7: 화면을 디자인대로 그린다**

`TodayScreen`을 디자인 화면 1에 맞춘다. 위에서 아래로:

1. **날짜 헤더** — `‹`(40dp 원형) · 가운데 버튼 · `›`. 가운데는 두 줄: 굵은 `9월 2일 수요일`(700/22, `Ink`)과 `오늘 · 일정 4개`(400/12, `InkFaint`). 오늘이 아니면 아래 줄은 `어제` / `내일` / `2026년`. 탭하면 미니 달력(Task 8)
2. **`일정` 섹션 라벨** — 500/11 `InkFaint` `letter-spacing .08em`, 오른쪽으로 1dp `Line`, 그 끝에 `좌우로 스와이프`(400/11, `InkFainter`)
3. **일정 행** (`agenda`) — 높이 패딩 11dp, 모서리 16dp, 눌림 `Hover`
   - 시각: 폭 54dp 고정, Roboto 500/13, `InkMuted`. 종일이면 `종일`
   - 색 막대: 폭 3dp, 세로로 꽉, 모서리 2dp. 종일은 `AllDayBar`, 그 외는 `Color(event.color)` — 색이 0이면 `Green`
   - 제목 500/15 `Ink`, 메타 400/12 `InkFaint`. 메타는 종일이면 `종일 · 기본 25분으로 시작`, 아니면 `60분`
   - 오른쪽 36dp 아이콘 버튼 — 바깥으로 나가는 화살표. 탭하면 `openInSystemCalendar` + 토스트 `삼성 캘린더에서 「치과」을 엽니다`
   - `settings.dimPastEvents && row.isPast`면 행 전체 `alpha 0.45`
4. **`할 일` 섹션 라벨** — 같은 모양, 오른쪽 힌트 없음
5. **할일 행** (`tasks`) — 22dp 원형 체크(테두리 1.5dp `InkFainter`, 완료 시 `Green` 채움 + 흰 체크) · 제목(완료 시 취소선) · 메타. 메타는 `50분`, 이월된 것은 `50분 · 3일째`. 완료 행은 `alpha 0.5`. 체크는 행 시작과 따로 동작해야 한다
6. **세션 바** — 세션이 도는 동안 하단 고정. 모서리 24dp, `Green` 바탕, 그림자. `집중 중`(400/11 `GreenSofter`) · 이름(500/15 흰색, 한 줄 말줄임) · 시계(Roboto 500/30, tabular figures) · 44dp 원형 `정지`. 기존 `SessionOverlayCard`를 이 모양으로 고친다
7. **FAB `＋`** — 오른쪽 아래 16dp/20dp 띄우고 64×64, 모서리 20dp, `GreenSoft` 바탕에 `GreenDark` 글자. Task 7의 시트를 연다
8. **토스트** — 하단 좌우 16dp, 모서리 12dp, `Toast` 바탕/글자. 2.2초 뒤 사라진다

권한이 없으면 2~3번 자리에 Task 4의 「캘린더 연결하기」 줄이 대신 들어간다. 권한은 있는데 일정이 없으면 `일정이 없습니다` 한 줄.

- [ ] **Step 8: 손으로 확인하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

기기에서: 삼성 캘린더에 오늘 일정 세 개(하나는 종일, 하나는 이미 지난 것)를 만들고 하루치를 연다 → 종일이 맨 위, 나머지는 시간순, 지난 것은 흐리게 보인다. 할일 체크를 눌러본다 → 취소선이 그어지고 목록 아래로 내려간다. 삼성 캘린더에서 제목을 고치고 돌아온다 → 새로고침 없이 바뀌어 있다.

```bash
git add -A
```

```bash
git commit -m "Show the day's calendar events above the to-do list"
```

---

### Task 6: 일정을 탭하면 타이머가 돈다

**Files:**
- Modify: `app/src/main/java/com/haruchi/today/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/FocusApp.kt`

- [ ] **Step 1: 일정 행에 시작을 붙인다**

일정 행을 탭하면 할일을 탭했을 때와 **같은 경로**로 세션을 시작한다 (계획 1 Task 2 Step 5에서 찾은 그 함수). 인자는 이렇게 만든다:

```kotlin
val label = event.title
val minutesMs = if (event.isAllDay || event.durationMs <= 0L) defaultDurationMs
                else event.durationMs
```

`defaultDurationMs`는 `SettingsRepository`의 `defaultDurationMs`다. 종일 일정은 길이가 없으므로 기본 시간을 쓴다.

지난 일정도 탭할 수 있다 — 흐리게 보일 뿐 잠기지 않는다. 규칙이 하나여야 익히기 쉽고, 늦게 시작하는 일이 실제로 흔하다.

- [ ] **Step 2: 행 오른쪽에 "캘린더에서 열기"를 붙인다**

일정 행 오른쪽 끝에 작은 아이콘 버튼(`Icons.Outlined.OpenInNew`)을 놓고, 탭하면 `viewModel.openInSystemCalendar(event.eventId)`를 부른다. 뷰모델에 한 줄 위임을 추가한다:

```kotlin
    fun openInSystemCalendar(eventId: Long) = calendarRepository.openInSystemCalendar(eventId)
```

- [ ] **Step 3: 손으로 확인하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

기기에서:
1. 1시간짜리 일정을 탭한다 → 타이머가 그 제목, 60분으로 시작한다
2. 완주시킨다 → 완주 시트에 그 제목이 뜨고, History와 통계에 그 라벨로 쌓인다
3. 종일 일정을 탭한다 → 설정의 기본 시간(25분)으로 시작한다
4. 오른쪽 아이콘을 탭한다 → 삼성 캘린더가 그 일정으로 열린다

```bash
git add -A
```

```bash
git commit -m "Start a timer from a calendar event"
```

---

### Task 7: ＋ 시트와 설정 화면

디자인 화면 2(`＋ 시트`)와 3(`설정`)이다. 색·치수는 `docs/design/2026-09-02-tokens-and-mapping.md`.

**Files:**
- Modify: `app/src/main/java/com/haruchi/today/data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`
- Create: `app/src/main/java/com/haruchi/today/ui/today/AddSheet.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/today/TodayScreen.kt`, `TodayViewModel.kt`

- [ ] **Step 1: 설정에 기본 캘린더를 넣는다**

`SettingsRepository.kt`의 `FocusSettings`에 추가:

```kotlin
    /** Which calendar new events go into. 0 means "not chosen yet", which is what makes
     * the add-event form refuse to save. */
    val defaultCalendarId: Long = 0L,
```

`KEY_DEFAULT_CALENDAR = longPreferencesKey("default_calendar_id")`와 `suspend fun setDefaultCalendarId(id: Long)`를 기존 setter들과 같은 모양으로 추가한다.

- [ ] **Step 2: 설정 화면을 디자인대로 만든다**

`SettingsScreen`을 디자인 화면 3에 맞춘다:

1. 제목 `설정` — 700/26 `Ink`, 위아래 여백 8/18dp
2. `타이머` 구역 라벨 — 500/11 `Green` `letter-spacing .08em`
3. **기본 집중 시간** 행 — 제목 500/15 `Ink`, 설명 `종일 일정과 길이 없는 할 일에 쓰인다` 400/12 `InkFaint`, 오른쪽에 `25분`(Roboto 500/15, `Green`). 탭하면 기존 시간 선택 UI
4. 1dp `Line` 구분선, 좌우 20dp 여백
5. `캘린더` 구역 라벨, 그 아래 **일정을 추가할 캘린더** 제목과 `＋에서 만든 일정이 여기로 들어간다` 설명
6. **캘린더 목록** — `calendarRepository.writableCalendars()` 결과. 행마다 20dp 라디오(선택 시 `Green` 테두리 + 10dp 안쪽 원) · 10dp 둥근 사각 색 견본(`CalendarAccount`에 색이 없으므로 이 작업에서 `Calendars.CALENDAR_COLOR`를 프로젝션에 추가해 `CalendarAccount.color`로 들고 온다) · 이름 500/15와 계정명 400/12. 탭하면 `setDefaultCalendarId`
7. **빈 상태** — 목록이 비면 대신 `Hover` 바탕 18dp 카드: `쓸 수 있는 캘린더가 없습니다` / `캘린더 권한을 허용하거나, 삼성 캘린더에서 계정을 추가해 주세요.` / `권한 허용` 버튼(`Green`). 버튼은 Task 4의 `rememberCalendarAccess().request()`를 부른다
8. 기존 설정 항목들(알림·진동·언어·기본 길이 프리셋)은 아래에 그대로 둔다

`CalendarAccount`에 `val color: Int`를 더하고, `writableCalendars()`의 프로젝션에 `Calendars.CALENDAR_COLOR`를 넣는다. Task 9의 가짜 구현(`RecordingCalendar`)도 새 인자를 넘기도록 고친다.

- [ ] **Step 3: `＋` 시트를 만든다**

`ui/today/AddSheet.kt` — `ModalBottomSheet`. 디자인 화면 2 그대로:

1. 32×4dp 손잡이
2. **탭 두 개** — `할 일 추가` / `일정 추가`, 각각 flex 1, 모서리 16dp. 고른 쪽은 `GreenSoft` 바탕 + `GreenDark` 글자, 나머지는 투명 + `LineStrong` 테두리 + `InkMuted` 글자
3. **제목** 칸 — 라벨 400/11 `InkFaint`, 입력칸 `Hover` 바탕 모서리 14dp, 글자 500/16
4. **두 번째 칸** — 일정이면 `시작 시각`(시각 선택), 할 일이면 `반복`(끄기/매일 — 기존 `isRepeating`)
5. **길이** — 칩 세 개 `25분` `50분` `1시간`. 고른 칩은 `GreenSoft`, 나머지는 테두리만
6. **힌트 한 줄** — 400/12. 할 일이면 `보고 있는 날짜(9월 2일)의 할 일로 저장됩니다.`, 일정이고 기본 캘린더가 있으면 `「내 캘린더」에 저장되고 삼성 캘린더에도 나타납니다.`, 없으면 `Sunday` 색으로 `기본 캘린더가 없습니다 — 탭하면 설정으로 갑니다.`
7. **버튼 줄** — `취소`(테두리) + 저장. 저장 라벨은 `할 일 추가` / `일정 추가`이고, 일정인데 기본 캘린더가 없으면 `설정에서 캘린더 고르기`가 되어 `Line` 바탕 `InkFaint` 글자로 바뀌며, 누르면 설정 화면으로 보낸다

저장 동작:

```kotlin
// 할 일
repository.addPlannedFocus(title, lengthMs, isRepeating, dueDateEpochDay = day.toEpochDay())

// 일정
val start = day.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
calendarRepository.addEvent(title, start, start + lengthMs, settings.defaultCalendarId)
```

둘 다 저장 뒤 시트를 닫고 `저장했습니다` 토스트를 띄운다. 반복 규칙 편집 같은 건 여기서 하지 않는다 — 삼성 캘린더의 몫이다.

- [ ] **Step 4: 손으로 확인하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

기기에서:
1. 설정을 연다 → 캘린더 목록이 색 견본과 계정명까지 보인다. 하나 고른다
2. Today에서 `＋` → `일정 추가` → `치과 15:00 1시간` → 저장 → 땅금에 나타나고 토스트가 뜬다
3. 삼성 캘린더를 연다 → 같은 일정이 거기에도 있다
4. 내일로 스와이프해 `＋` → `할 일 추가` → 저장 → 내일 목록에만 있고 오늘엔 없다
5. 설정에서 캘린더 선택을 지운 상태를 만든다(계정이 없는 기기로 확인하거나 잠시 `defaultCalendarId`를 0으로 두고) → `일정 추가` 저장 버튼이 `설정에서 캘린더 고르기`로 바뀐다

```bash
git add -A
```

```bash
git commit -m "Add the plus sheet and the calendar settings screen"
```

---

### Task 8: 날짜 이동과 미니 달력

**Files:**
- Create: `app/src/main/java/com/haruchi/today/ui/today/MiniCalendar.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/today/TodayScreen.kt`, `TodayViewModel.kt`

- [ ] **Step 1: 미니 달력을 만든다**

`MiniCalendar.kt` — 한 달치 격자를 그리는 다이얼로그다.

```kotlin
@Composable
fun MiniCalendarDialog(
    month: YearMonth,
    selected: LocalDate,
    daysWithEvents: Set<LocalDate>,
    onMonthChange: (YearMonth) -> Unit,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
)
```

- 헤더: `← 2026년 9월 →`, 화살표는 `onMonthChange`
- 요일 머리글 일곱 칸, 그 아래 날짜 격자. 첫 칸 오프셋은 `month.atDay(1).dayOfWeek`
- 일정이 있는 날(`daysWithEvents`)은 숫자 아래에 점 하나
- 오늘은 테두리, 선택된 날은 채운 원
- 날짜를 탭하면 `onPick`, 그리고 닫는다

뷰모델에 붙인다:

```kotlin
    private val _month = MutableStateFlow(YearMonth.from(LocalDate.now()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()
    fun showMonth(m: YearMonth) { _month.value = m }

    val daysWithEvents: StateFlow<Set<LocalDate>> = _month
        .flatMapLatest { m ->
            if (!hasCalendarPermission(context)) flowOf(emptySet())
            else calendarRepository.daysWithEvents(m)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
```

- [ ] **Step 2: 스와이프로 날짜를 넘긴다**

`TodayScreen`의 콘텐츠에 수평 드래그를 붙인다. 왼쪽으로 밀면 다음 날, 오른쪽으로 밀면 전날:

```kotlin
val threshold = with(LocalDensity.current) { 64.dp.toPx() }
var dragged by remember { mutableFloatStateOf(0f) }

Modifier.pointerInput(day) {
    detectHorizontalDragGestures(
        onDragEnd = {
            when {
                dragged <= -threshold -> viewModel.showDay(day.plusDays(1))
                dragged >= threshold -> viewModel.showDay(day.minusDays(1))
            }
            dragged = 0f
        },
        onDragCancel = { dragged = 0f },
    ) { change, amount ->
        change.consume()
        dragged += amount
    }
}
```

`pointerInput(day)`의 키가 `day`인 것이 중요하다. 날짜가 바뀔 때 제스처 감지기가 다시 만들어져야 `onDragEnd` 안의 `day`가 오래된 값을 붙들지 않는다.

할일 행의 기존 스와이프 동작(`SwipeableRow`)과 부딪히지 않도록, 이 `Modifier`는 일정 땅금 구역과 날짜 헤더에만 건다 — 할일 목록에는 걸지 않는다.

- [ ] **Step 3: 손으로 확인하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

기기에서:
1. 좌우로 스와이프한다 → 날짜가 하루씩 움직이고 일정·할일이 그 날 것으로 바뀐다
2. 날짜 헤더를 탭한다 → 미니 달력이 뜨고, 일정 있는 날에 점이 있다
3. 다음 달의 어떤 날을 고른다 → 그 날로 이동한다
4. 과거 날짜로 간다 → 이월된 할일이 **보이지 않는다** (그 날 실제로 있던 것만 보인다)
5. 할일 행을 스와이프한다 → 날짜가 넘어가지 않고 기존 동작이 난다

```bash
git add -A
```

```bash
git commit -m "Move between days by swipe and a mini calendar"
```

---

### Task 9: 집중 기록을 캘린더에 남긴다 (설정에서 켤 때만)

**Files:**
- Modify: `app/src/main/java/com/haruchi/today/data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/haruchi/today/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`
- Modify: `app/src/main/java/com/haruchi/today/timer/SessionRecorder.kt`
- Test: `app/src/test/java/com/haruchi/today/timer/SessionRecorderTest.kt`

- [ ] **Step 1: 설정 스위치를 추가한다**

`FocusSettings`에 `val writeSessionsToCalendar: Boolean = false`와 `KEY_WRITE_SESSIONS = booleanPreferencesKey("write_sessions_to_calendar")`, setter를 추가한다. 기본값은 **꺼짐**이다 — 켜져 있으면 하루 여러 번 집중할 때 남의 캘린더가 금방 빽빽해진다. `SettingsScreen`에 스위치 한 줄을 넣는다.

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`SessionRecorderTest.kt`에 추가한다. 가짜 캘린더 저장소로 호출 여부만 본다:

```kotlin
    private class RecordingCalendar : CalendarRepository {
        val added = mutableListOf<Triple<String, Long, Long>>()
        override fun eventsOn(date: java.time.LocalDate) = kotlinx.coroutines.flow.flowOf(emptyList<CalendarEvent>())
        override fun daysWithEvents(month: java.time.YearMonth) = kotlinx.coroutines.flow.flowOf(emptySet<java.time.LocalDate>())
        override suspend fun writableCalendars() = listOf(CalendarAccount(1L, "내 캘린더", "me"))
        override suspend fun addEvent(title: String, startMs: Long, endMs: Long, calendarId: Long): Long {
            added += Triple(title, startMs, endMs)
            return added.size.toLong()
        }
        override fun openInSystemCalendar(eventId: Long) = Unit
    }

    @Test
    fun `writes a calendar block when the setting is on`() = runTest {
        val calendar = RecordingCalendar()

        SessionRecorder.record(repository, completed(1_000L), calendar, calendarId = 1L)

        assertEquals(1, calendar.added.size)
        assertEquals("논문 읽기", calendar.added.single().first)
    }

    @Test
    fun `writes nothing when no calendar is chosen`() = runTest {
        val calendar = RecordingCalendar()

        SessionRecorder.record(repository, completed(2_000L), calendar, calendarId = 0L)

        assertTrue(calendar.added.isEmpty())
    }
```

기존 세 테스트의 `SessionRecorder.record(repository, completed(...))` 호출에는 `calendar = null, calendarId = 0L`을 넘기도록 고친다.

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*SessionRecorderTest*"`

Expected: 컴파일 실패 — `record`가 인자 네 개를 받지 않는다

- [ ] **Step 4: `SessionRecorder`를 고친다**

`record`의 시그니처와 본문을 바꾼다:

```kotlin
        /**
         * Writes the row, or returns null if this session is already recorded. When
         * [calendar] is given and [calendarId] is a real calendar, the finished session is
         * also left as a block on that calendar.
         */
        suspend fun record(
            repository: FocusRepository,
            state: TimerState,
            calendar: CalendarRepository? = null,
            calendarId: Long = 0L,
        ): Long? {
            if (repository.receiptExistsForSession(state.startedAtEpoch)) return null
            val issuedAt = System.currentTimeMillis()
            val id = repository.publish(
                ReceiptEntity(
                    issuedAtEpoch = issuedAt,
                    focusedMs = state.focusedMs,
                    plannedMs = state.plannedMs,
                    taskLabel = state.taskLabel,
                    comment = state.draftComment.ifBlank { null },
                    photos = emptyList(),
                    startedAtEpoch = state.startedAtEpoch,
                ),
            )
            if (calendar != null && calendarId != 0L) {
                calendar.addEvent(
                    title = state.taskLabel,
                    startMs = issuedAt - state.focusedMs,
                    endMs = issuedAt,
                    calendarId = calendarId,
                )
            }
            return id
        }
```

`attach`에서는 설정을 읽어 넘긴다. `SessionRecorder`의 생성자에 `SettingsRepository`와 `CalendarRepository`를 주입하고:

```kotlin
                val settings = settingsRepository.settings.first()
                val calendar = if (settings.writeSessionsToCalendar) calendarRepository else null
                record(repository, state, calendar, settings.defaultCalendarId)
                    ?.let { _lastSavedId.value = it }
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest`

Expected: 전체 통과 (SessionRecorder 5, TodayTasks 7, CalendarEvent 3, CalendarRepository 1)

- [ ] **Step 6: 손으로 확인하고 커밋한다**

기기에서: 설정에서 스위치를 켜고 1분 세션을 완주시킨다 → 삼성 캘린더의 그 시간대에 블록이 하나 생긴다. 스위치를 끄고 다시 완주시킨다 → 새 블록이 생기지 않는다.

```bash
git add -A
```

```bash
git commit -m "Optionally leave a finished session on the calendar"
```

---

## 이 계획을 마치면

하루치가 스펙대로 동작한다: 오늘 한 장에 일정과 할일이 같이 있고, 무엇이든 탭하면 타이머가 돌고, 완주하면 알아서 기록된다. 캘린더는 시스템과 한 몸이고, 못 끝낸 할일은 스스로 따라온다.
