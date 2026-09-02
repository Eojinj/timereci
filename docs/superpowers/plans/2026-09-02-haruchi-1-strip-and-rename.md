# 하루치 1 — 정리와 개명 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 완주 흐름을 자동 저장으로 바꾸고 영수증 잔재를 걷어낸 뒤, 앱을 `하루치`(`com.haruchi.today`)로 개명한다.

**Architecture:** 지금은 세션 저장이 `PublishScreen`이라는 UI 화면 안에 있어서, 그 화면을 지나야만 기록이 남는다. 저장을 앱 스코프의 `SessionRecorder`로 옮겨 타이머가 완주하는 순간 저장되게 하고, 화면 셋(`Publish`·`NextUp`)은 Today 위에 뜨는 시트 하나로 합친다. 그 뒤 파일·패키지 이름을 일괄 정리한다.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Hilt 2.52, Room 2.6.1, JUnit4 + Robolectric + kotlinx-coroutines-test

**선행 조건 없음.** 이 계획을 마치면 캘린더 없는 "하루치"가 온전히 동작한다. 캘린더 축은 계획 2에서 얹는다.

**참고:** 이 저장소에는 테스트가 하나도 없다. Task 1에서 테스트 소스 세트를 만든다. 삭제·개명 작업(Task 3~5)에는 의미 있는 단위 테스트가 없으므로, 검증은 빌드 통과와 명시된 수동 확인으로 한다. 없는 동작에 테스트를 지어내지 않는다.

---

### Task 1: 완주한 세션을 자동으로 저장한다

지금은 `PublishViewModel.store()`를 눌러야 저장된다. 타이머가 `COMPLETED`가 되는 순간 저장하도록 옮긴다. 앱이 죽은 사이 완주한 경우 재실행 때 `restore()`가 다시 `COMPLETED`를 내놓으므로, 두 번 저장되지 않도록 `startedAtEpoch`로 막는다.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/timereci/focus/data/ReceiptEntity.kt`
- Modify: `app/src/main/java/com/timereci/focus/data/ReceiptDao.kt`
- Modify: `app/src/main/java/com/timereci/focus/data/FocusDatabase.kt`
- Modify: `app/src/main/java/com/timereci/focus/data/FocusRepository.kt`
- Create: `app/src/main/java/com/timereci/focus/timer/SessionRecorder.kt`
- Modify: `app/src/main/java/com/timereci/focus/FocusReceiptApp.kt`
- Test: `app/src/test/java/com/timereci/focus/timer/SessionRecorderTest.kt`

- [ ] **Step 1: 테스트 의존성을 추가한다**

`gradle/libs.versions.toml`의 `[versions]`에 추가:

```toml
robolectric = "4.14.1"
coroutinesTest = "1.9.0"
```

`[libraries]`에 추가:

```toml
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
```

`app/build.gradle.kts`의 `android { }` 블록 안, `packaging { }` 바로 위에 추가:

```kotlin
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
```

`dependencies { }`의 `testImplementation(libs.junit)` 바로 아래에 추가:

```kotlin
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 2: `ReceiptEntity`에 세션 식별자를 추가한다**

`ReceiptEntity.kt`의 `themeId` 필드 바로 위에 추가:

```kotlin
    /** The session's start time, used only to tell two sessions apart. Auto-saving happens
     * when the timer reaches COMPLETED, and a crash between saving and resetting would
     * otherwise let the same session be recorded twice on the next launch. 0 for rows
     * written before this column existed. */
    val startedAtEpoch: Long = 0L,
```

- [ ] **Step 3: 같은 세션이 이미 기록됐는지 묻는 쿼리를 추가한다**

`ReceiptDao.kt`의 `getById` 아래에 추가:

```kotlin
    @Query("SELECT COUNT(*) FROM receipts WHERE startedAtEpoch = :startedAtEpoch")
    suspend fun countForSession(startedAtEpoch: Long): Int
```

`FocusRepository.kt`의 `publish` 위에 추가:

```kotlin
    suspend fun receiptExistsForSession(startedAtEpoch: Long): Boolean =
        startedAtEpoch != 0L && receiptDao.countForSession(startedAtEpoch) > 0
```

`FocusDatabase.kt`에서 `version = 3`을 `version = 4`로 바꾼다. DI가 이미 `fallbackToDestructiveMigration()`이라 마이그레이션 코드는 필요 없다.

- [ ] **Step 4: 실패하는 테스트를 쓴다**

`app/src/test/java/com/timereci/focus/timer/SessionRecorderTest.kt`:

```kotlin
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
```

- [ ] **Step 5: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*SessionRecorderTest*"`

Expected: 컴파일 실패 — `Unresolved reference: SessionRecorder`

- [ ] **Step 6: `SessionRecorder`를 만든다**

`app/src/main/java/com/timereci/focus/timer/SessionRecorder.kt`:

```kotlin
package com.timereci.focus.timer

import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.ReceiptEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves a session the moment the timer completes, so no screen has to be visited for the
 * record to exist. The completion sheet then edits the row this produced, rather than
 * deciding whether to create one.
 */
@Singleton
class SessionRecorder @Inject constructor(
    private val controller: FocusTimerController,
    private val repository: FocusRepository,
) {

    private val _lastSavedId = MutableStateFlow<Long?>(null)

    /** Row id of the session saved most recently, which the completion sheet edits.
     * Null once the sheet has been dismissed. */
    val lastSavedId: StateFlow<Long?> = _lastSavedId.asStateFlow()

    /** Called once from the Application. Watches for completions for the process's lifetime. */
    fun attach(scope: CoroutineScope) {
        scope.launch {
            controller.state.collect { state ->
                if (state.phase != TimerPhase.COMPLETED) return@collect
                record(repository, state)?.let { _lastSavedId.value = it }
                controller.reset()
            }
        }
    }

    fun dismissSheet() {
        _lastSavedId.value = null
    }

    companion object {
        /**
         * Writes the row, or returns null if this session is already recorded. Kept in the
         * companion so it can be tested without a coroutine scope or a live controller.
         */
        suspend fun record(repository: FocusRepository, state: TimerState): Long? {
            if (repository.receiptExistsForSession(state.startedAtEpoch)) return null
            return repository.publish(
                ReceiptEntity(
                    issuedAtEpoch = System.currentTimeMillis(),
                    focusedMs = state.focusedMs,
                    plannedMs = state.plannedMs,
                    taskLabel = state.taskLabel,
                    comment = state.draftComment.ifBlank { null },
                    photos = emptyList(),
                    startedAtEpoch = state.startedAtEpoch,
                ),
            )
        }
    }
}
```

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "*SessionRecorderTest*"`

Expected: 3 tests, all PASS

- [ ] **Step 8: 앱 시작 시 기록자를 붙인다**

`FocusReceiptApp.kt`에 다음을 추가한다. 이미 `onCreate`가 있으면 본문 끝에 `sessionRecorder.attach(appScope)` 한 줄만 더한다.

```kotlin
    @Inject lateinit var sessionRecorder: SessionRecorder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        sessionRecorder.attach(appScope)
    }
```

임포트: `com.timereci.focus.timer.SessionRecorder`, `kotlinx.coroutines.CoroutineScope`, `kotlinx.coroutines.Dispatchers`, `kotlinx.coroutines.SupervisorJob`, `javax.inject.Inject`.

- [ ] **Step 9: 커밋한다**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main app/src/test
```

```bash
git commit -m "Save a session as soon as the timer completes"
```

---

### Task 2: 완주 시트로 화면 세 개를 합친다

`SESSION_COMPLETE`(사진/메모)와 `UP_NEXT`(다음 할일)를 없애고, Today 위에 뜨는 바텀시트 하나로 만든다. 세션은 시트가 뜨기 전에 이미 저장돼 있으므로, 시트를 그냥 내려도 기록은 남는다.

**Files:**
- Create: `app/src/main/java/com/timereci/focus/ui/completion/CompletionViewModel.kt`
- Create: `app/src/main/java/com/timereci/focus/ui/completion/CompletionSheet.kt`
- Delete: `app/src/main/java/com/timereci/focus/ui/publish/` (폴더 전체)
- Delete: `app/src/main/java/com/timereci/focus/ui/nextup/NextUpScreen.kt`, `NextUpViewModel.kt`
- Modify: `app/src/main/java/com/timereci/focus/ui/Routes.kt`
- Modify: `app/src/main/java/com/timereci/focus/ui/FocusApp.kt`
- Modify: `app/src/main/java/com/timereci/focus/ui/RootViewModel.kt`
- Modify: `app/src/main/java/com/timereci/focus/ui/nextup/BreakViewModel.kt`

- [ ] **Step 1: `CompletionViewModel`을 만든다**

`ui/completion/CompletionViewModel.kt`:

```kotlin
package com.timereci.focus.ui.completion

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.ReceiptEntity
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.timer.SessionRecorder
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompletionUiState(
    val visible: Boolean = false,
    val task: String = "",
    val focus: String = "",
    val comment: String = "",
    val photo: PhotoRef? = null,
    val next: PlannedFocusEntity? = null,
)

/**
 * Drives the sheet shown over Today after a session completes. Everything here edits a row
 * that already exists — there is no "save" and no "discard".
 */
@HiltViewModel
class CompletionViewModel @Inject constructor(
    private val recorder: SessionRecorder,
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
) : ViewModel() {

    private val _ui = MutableStateFlow(CompletionUiState())
    val ui: StateFlow<CompletionUiState> = _ui.asStateFlow()

    val alarmActive: StateFlow<Boolean> = controller.alarmActive

    private var receiptId: Long? = null

    init {
        viewModelScope.launch {
            recorder.lastSavedId.collect { id ->
                if (id == null) {
                    receiptId = null
                    _ui.value = CompletionUiState()
                    return@collect
                }
                val receipt = repository.getReceipt(id) ?: return@collect
                receiptId = id
                _ui.value = CompletionUiState(
                    visible = true,
                    task = receipt.taskLabel,
                    focus = Formatters.focus(receipt.focusedMs),
                    comment = receipt.comment.orEmpty(),
                    photo = receipt.photos.firstOrNull(),
                    next = repository.nextPlannedFocus(excludingLabel = receipt.taskLabel),
                )
            }
        }
    }

    fun stopAlarm() = controller.stopAlarm()

    fun setComment(text: String) {
        _ui.value = _ui.value.copy(comment = text)
        persist { it.copy(comment = text.ifBlank { null }) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val added = repository.photoStorageRef.import(uri) ?: return@launch
            _ui.value = _ui.value.copy(photo = added)
            persist { it.copy(photos = listOf(added)) }
        }
    }

    fun setExistingPhoto(ref: PhotoRef) {
        viewModelScope.launch {
            val added = repository.reusePhoto(ref, 0) ?: return@launch
            _ui.value = _ui.value.copy(photo = added)
            persist { it.copy(photos = listOf(added)) }
        }
    }

    fun dismiss() = recorder.dismissSheet()

    private fun persist(edit: (ReceiptEntity) -> ReceiptEntity) {
        val id = receiptId ?: return
        viewModelScope.launch {
            repository.getReceipt(id)?.let { repository.updateReceipt(edit(it)) }
        }
    }
}
```

- [ ] **Step 2: `CompletionSheet`을 만든다**

`ui/completion/CompletionSheet.kt`. 시트 안의 구성은 아래 순서 그대로다. 사진 행과 코멘트 칸은 삭제 예정인 `ui/publish/PublishScreen.kt`에 이미 구현돼 있으므로, 지우기 전에 그 코드(포토 피커 런처, `ChoosePhotoSheet` 호출, Coil `AsyncImage` + `PhotoStorage.fileIn`)를 그대로 옮겨온다.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionSheet(
    onStartNext: (PlannedFocusEntity) -> Unit,
    onTakeBreak: (minutes: Int) -> Unit,
    viewModel: CompletionViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val alarmActive by viewModel.alarmActive.collectAsStateWithLifecycle()
    if (!ui.visible) return

    ModalBottomSheet(onDismissRequest = viewModel::dismiss) {
        // 1. 제목 한 줄: "${ui.task} · ${ui.focus} 완료"
        // 2. alarmActive 이면 폭 전체 "알람 끄기" 버튼 → viewModel.stopAlarm()
        // 3. 메모: BasicTextField(value = ui.comment, onValueChange = viewModel::setComment)
        // 4. 사진 한 줄: ui.photo 가 있으면 썸네일, 없으면 "사진 추가".
        //    탭하면 ChoosePhotoSheet → viewModel.setPhoto(uri) / viewModel.setExistingPhoto(ref)
        // 5. "5분 쉬기" → onTakeBreak(5)
        // 6. ui.next 가 null 이 아니면 "다음: ${ui.next!!.label}" → onStartNext(ui.next!!)
    }
}
```

저장 버튼도 버리기 버튼도 넣지 않는다 — 그게 이 화면의 요점이다.

- [ ] **Step 3: 라우트를 정리한다**

`Routes.kt`에서 `SESSION_COMPLETE`와 `UP_NEXT` 상수를 지운다. `BREAK`에서 인자를 없앤다:

```kotlin
    /** A short break. It ends by returning to Today — it no longer carries a task to start
     * afterwards, because the completion sheet is where "what's next" gets decided. */
    const val BREAK = "break?breakMinutes={breakMinutes}"
    const val ARG_BREAK_MINUTES = "breakMinutes"
    fun breakScreen(breakMinutes: Int) = "break?breakMinutes=$breakMinutes"
```

`ARG_TASK`, `ARG_MINUTES`, 그리고 3-인자 `breakScreen`을 지운다. `BreakViewModel`에서 `task`/`minutes`를 `SavedStateHandle`로 읽는 부분과 그 값을 쓰는 코드를 지운다. `BreakScreen`의 `onDone`은 Today로 `popBackStack` 한다.

- [ ] **Step 4: `RootViewModel`에서 발행 복귀를 없앤다**

`enum class ResumeTarget { TIMER, PUBLISH }`를 `enum class ResumeTarget { TIMER }`로 바꾸고, `init` 블록의 `when`에서 `TimerPhase.COMPLETED -> ResumeTarget.PUBLISH`를 `TimerPhase.COMPLETED -> null`로 바꾼다. 완주 복귀는 이제 `SessionRecorder.lastSavedId`가 시트를 띄우는 것으로 대체된다.

- [ ] **Step 5: `FocusApp`에 시트를 매단다**

`FocusApp.kt`에서 `composable(Routes.SESSION_COMPLETE) { ... }`와 `composable(Routes.UP_NEXT) { ... }` 블록을 통째로 지운다. 그리고 `NavHost` **바깥**, `Scaffold` 콘텐츠의 맨 뒤에 시트를 놓는다 — 어느 화면 위에서든 떠야 한다:

```kotlin
CompletionSheet(
    onStartNext = { task ->
        startTaskAndGoToTimer(task.label, task.plannedMs)
    },
    onTakeBreak = { minutes -> navController.navigate(Routes.breakScreen(minutes)) },
)
```

`startTaskAndGoToTimer`는 지어낸 이름이다. `composable(Routes.TODAY)` 블록에서 `TodoScreen`이 할일을 눌렀을 때 실제로 호출하는 경로(세션을 시작하고 `Routes.TIMER`로 이동하는 코드)를 찾아 그것을 그대로 쓴다.

- [ ] **Step 6: 옛 화면을 지운다**

```bash
git rm -r app/src/main/java/com/timereci/focus/ui/publish
```

```bash
git rm app/src/main/java/com/timereci/focus/ui/nextup/NextUpScreen.kt app/src/main/java/com/timereci/focus/ui/nextup/NextUpViewModel.kt
```

- [ ] **Step 7: 빌드하고 손으로 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL

기기 또는 에뮬레이터에서:
1. 할일을 탭해 1분 타이머를 시작하고 완주시킨다 → Today로 돌아오고 시트가 뜬다
2. 아무것도 안 누르고 시트를 아래로 내린다 → History에 그 세션이 남아 있다
3. 다시 1분 세션을 완주시키고 메모를 적은 뒤 시트를 내린다 → History의 그 항목에 메모가 있다
4. 세션 중 앱을 강제 종료하고, 완주 시각이 지난 뒤 다시 연다 → 시트가 뜨고 History에 항목이 **하나만** 있다

- [ ] **Step 8: 커밋한다**

```bash
git add -A
```

```bash
git commit -m "Replace the publish and up-next screens with one completion sheet"
```

---

### Task 3: 영수증 렌더링 잔재를 지운다

**Files:**
- Delete: `ui/detail/Exporter.kt`, `ui/components/PhotoCard.kt`, `ui/theme/PhotoTones.kt`, `ui/theme/PatternPlaceholder.kt`
- Modify: `data/SettingsRepository.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`
- Modify: `ui/feed/FeedScreen.kt`, `ui/detail/DetailScreen.kt`

- [ ] **Step 1: 설정에서 사진 비율을 걷어낸다**

`SettingsRepository.kt`에서 `enum class PhotoAspect`, `FocusSettings.photoAspect`, `KEY_PHOTO_ASPECT`, 그리고 그 값을 읽고 쓰는 함수를 지운다. `SettingsScreen`과 `SettingsViewModel`에서 해당 UI 항목과 호출을 지운다.

- [ ] **Step 2: 영수증 카드 컴포넌트를 지운다**

```bash
git rm app/src/main/java/com/timereci/focus/ui/components/PhotoCard.kt app/src/main/java/com/timereci/focus/ui/theme/PhotoTones.kt app/src/main/java/com/timereci/focus/ui/theme/PatternPlaceholder.kt app/src/main/java/com/timereci/focus/ui/detail/Exporter.kt
```

- [ ] **Step 3: 깨진 참조를 고친다**

Run: `./gradlew :app:assembleDebug`

깨지는 곳은 `FeedScreen`(카드 그리드)과 `DetailScreen`(확대 + 내보내기)뿐이다. 각각 이렇게 바꾼다:

- `FeedScreen`: `PhotoCard` 대신 한 줄짜리 목록 행 — 날짜 헤더 아래에 `시각 · 라벨 · 집중 시간`, 사진이 있으면 왼쪽에 48dp 썸네일(`AsyncImage` + `PhotoStorage.fileIn`)
- `DetailScreen`: 사진 한 장(있으면) + 라벨 + 집중 시간 + 메모. "갤러리에 저장" 버튼과 그 호출을 지운다

- [ ] **Step 4: 빌드하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add -A
```

```bash
git commit -m "Drop receipt rendering: photo cards, tones, and gallery export"
```

---

### Task 4: 화면 폴더 이름을 뜻에 맞게 정리한다

**Files:** `ui/feed/` → `ui/history/`, `ui/todo/` → `ui/today/`

- [ ] **Step 1: 폴더와 파일을 옮긴다**

```bash
U=app/src/main/java/com/timereci/focus/ui && git mv $U/feed $U/history && git mv $U/history/FeedScreen.kt $U/history/HistoryScreen.kt && git mv $U/history/FeedViewModel.kt $U/history/HistoryViewModel.kt && git mv $U/todo $U/today && git mv $U/today/TodoScreen.kt $U/today/TodayScreen.kt && git mv $U/today/TodoViewModel.kt $U/today/TodayViewModel.kt
```

- [ ] **Step 2: 패키지 선언과 심볼 이름을 바꾼다**

저장소 루트에서:

```bash
grep -rl "ui\.feed\|ui\.todo\|FeedScreen\|FeedViewModel\|TodoScreen\|TodoViewModel" app/src/main | xargs sed -i "s/ui\.feed/ui.history/g; s/ui\.todo/ui.today/g; s/FeedScreen/HistoryScreen/g; s/FeedViewModel/HistoryViewModel/g; s/TodoScreen/TodayScreen/g; s/TodoViewModel/TodayViewModel/g"
```

`ui/model/FeedBuilder`와 `FeedModels.kt`는 그대로 둔다 — "날짜별로 묶는다"는 뜻이 여전히 맞다.

- [ ] **Step 3: 빌드하고 커밋한다**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add -A
```

```bash
git commit -m "Rename the feed and todo screens to history and today"
```

---

### Task 5: 앱을 하루치로 개명한다

**Files:** `app/build.gradle.kts`, `AndroidManifest.xml`, `res/values/strings.xml`, `res/values/themes.xml`, 소스 트리 전체

- [ ] **Step 1: 소스 디렉터리를 옮긴다**

```bash
git mv app/src/main/java/com/timereci app/src/main/java/com/haruchi && git mv app/src/main/java/com/haruchi/focus app/src/main/java/com/haruchi/today
```

```bash
mkdir -p app/src/test/java/com/haruchi && git mv app/src/test/java/com/timereci/focus app/src/test/java/com/haruchi/today && rmdir app/src/test/java/com/timereci
```

- [ ] **Step 2: 패키지 선언을 전부 바꾼다**

```bash
grep -rl "com\.timereci\.focus" app/src | xargs sed -i "s/com\.timereci\.focus/com.haruchi.today/g"
```

- [ ] **Step 3: 빌드 설정과 매니페스트를 바꾼다**

`app/build.gradle.kts`에서 `namespace`와 `applicationId`를 둘 다 `"com.haruchi.today"`로 바꾼다.

`AndroidManifest.xml`의 `android:name=".FocusReceiptApp"`은 그대로 둔다 (상대 경로라 새 패키지를 따라간다). 테마 이름 `Theme.Timereci`를 `Theme.Haruchi`로 바꾸고, `res/values/themes.xml`(그리고 `values-night/themes.xml`이 있으면 거기서도) 같은 이름을 바꾼다.

- [ ] **Step 4: 표시 이름을 바꾼다**

`app/src/main/res/values/strings.xml`의 `app_name`을 `하루치`로 바꾼다. `values-ko`가 따로 있으면 거기도 바꾼다. 그리고 남은 옛 이름을 찾는다:

```bash
grep -rn "Merci\|Timereci\|timereci" app/src
```

`ui/i18n/Strings.kt`에 있는 것까지 남는 게 없을 때까지 고친다.

- [ ] **Step 5: Application 클래스 이름을 정리한다**

```bash
grep -rl "FocusReceiptApp" app/src | xargs sed -i "s/FocusReceiptApp/HaruchiApp/g"
```

```bash
git mv app/src/main/java/com/haruchi/today/FocusReceiptApp.kt app/src/main/java/com/haruchi/today/HaruchiApp.kt
```

`FocusDatabase`, `FocusRepository`, `FocusTimerController` 등 나머지 `Focus*` 이름은 **그대로 둔다** — "집중"은 이 앱에서 여전히 맞는 말이고, 바꿔서 얻는 게 없다.

- [ ] **Step 6: 빌드하고 테스트하고 커밋한다**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, 3 tests PASS

기기에서 이전 설치본(`com.timereci.focus`)을 지우고 새로 설치해, 런처에 `하루치`로 뜨는지 확인한다. 패키지가 바뀌었으므로 옛 앱은 별개로 남아 있다.

```bash
git add -A
```

```bash
git commit -m "Rename the app to 하루치 (com.haruchi.today)"
```

---

## 이 계획을 마치면

캘린더 없는 하루치가 온전히 돈다: 할일 목록 → 탭 → 타이머 → 완주 시 자동 저장 → 시트에서 원하면 메모·사진·휴식·다음 할일. 기록과 통계는 그대로다. 계획 2가 여기에 캘린더 축을 얹는다.
