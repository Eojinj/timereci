package com.timereci.focus.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalDate
import java.util.Locale

enum class AppLanguage { SYSTEM, ENGLISH, KOREAN }

/**
 * Every user-visible string, in one table.
 *
 * The app's copy lives in Kotlin rather than strings.xml, so localization is a value swap
 * instead of a resource lookup: [English] is the defaults, and [Korean] overrides only what it
 * translates. A missed translation therefore falls back to English rather than breaking the
 * build — and because it is plain data, the language can change without recreating the
 * activity, which a per-app locale would require.
 *
 * Anything that interpolates or pluralizes is a function, since neither survives concatenation
 * across languages.
 */
class Strings(
    // ---- Shared ----
    val cancel: String = "Cancel",
    val save: String = "Save",
    val delete: String = "Delete",
    val edit: String = "Edit",
    val remove: String = "Remove",
    val back: String = "Back",
    val focusFallback: String = "Focus",
    val minLabel: String = "min",
    val selected: String = "Selected",
    val none: String = "None",
    val optional: String = "Optional",

    /** "25 min" — a single session's length. */
    val minutes: (Int) -> String = { "$it min" },
    /** "25m" — the same length where space is tight, like a photo tile badge. */
    val minutesCompact: (Int) -> String = { "${it}m" },
    /** "1h 15m" / "25m" — an accumulated total. */
    val duration: (hours: Int, minutes: Int) -> String = { h, m -> if (h > 0) "${h}h ${m}m" else "${m}m" },
    val sessionsCount: (Int) -> String = { "$it session${if (it == 1) "" else "s"}" },
    val tasksCount: (Int) -> String = { "$it task${if (it == 1) "" else "s"}" },
    val daysCount: (Int) -> String = { "$it day${if (it == 1) "" else "s"}" },

    // ---- Tabs ----
    val tabToday: String = "Today",
    val tabHistory: String = "History",
    val tabSettings: String = "Settings",

    // ---- Today ----
    val noTasksQueued: String = "No tasks queued",
    val plannedSummary: (tasks: String, duration: String) -> String = { t, d -> "$t · $d planned" },
    val newTask: String = "New Task",
    val favorites: String = "FAVORITES",
    val favoritesHint: String = "Tap for stats and to start it, press and hold to edit.",
    val todayFooter: (sessions: String, duration: String) -> String = { s, d -> "$s completed today · $d focused." },
    val editTask: String = "Edit Task",
    val taskFieldPlaceholder: String = "Task",
    val repeat: String = "Repeat",
    val repeatHint: String = "Stays in the list after you start it",
    val statsAction: String = "Stats",

    // ---- Quick Start ----
    val quickStart: String = "Quick Start",
    val whatFocusingOn: String = "WHAT ARE YOU FOCUSING ON?",
    val quickEntryPlaceholder: String = "e.g. Laundry 20",
    val quickEntryHint: (Int) -> String = { "Add a number for the minutes. No number starts a $it-minute session." },
    val recentSection: String = "RECENT",
    val optionsSection: String = "OPTIONS",
    val backgroundPhoto: String = "Background Photo",
    val repeatingTask: String = "Repeating Task",
    val repeatingTaskHint: String = "Keeps it on Today every time you run it, and tracks its stats",
    val startWithLabel: (label: String, minutes: String) -> String = { l, m -> "Start $l · $m" },
    val startPlain: (String) -> String = { "Start · $it" },
    val saveToToday: String = "Save to Today",

    // ---- Choose Photo ----
    val choosePhoto: String = "Choose Photo",
    val recentlyUsed: String = "RECENTLY USED",
    val chooseFromLibrary: String = "Choose from Library",
    val removePhoto: String = "Remove Photo",
    val photoPickerNote: String = "Photos are picked with the system picker — the app never asks for storage access.",

    // ---- Focus (timer) ----
    val endsAt: (String) -> String = { "Ends at $it" },
    val noteNowPlaceholder: String = "Add a note about right now…",
    val note: String = "Note",
    val pause: String = "Pause",
    val resume: String = "Resume",
    val finish: String = "Finish",
    val cancelSession: String = "Cancel Session",
    val toggleLandscape: String = "Toggle landscape",

    // ---- Session Complete ----
    val sessionComplete: String = "Session Complete",
    val stopAlarm: String = "Stop Alarm",
    val photoSection: String = "PHOTO",
    val addPhoto: String = "Add Photo",
    val changePhoto: String = "Change Photo",
    val noteSection: String = "NOTE",
    val noteSessionPlaceholder: String = "Add a note about this session…",
    val discard: String = "Discard",
    val publishFooter: String = "Saved sessions appear in History. Discarded sessions are never recorded.",

    // ---- Up Next / Break ----
    val sessionSaved: String = "Session saved",
    val upNext: String = "UP NEXT",
    val moreWaiting: (Int) -> String = { "$it more task${if (it == 1) "" else "s"} waiting" },
    val startNextSession: String = "Start Next Session",
    val takeABreak: String = "Take a Break",
    val restFor: String = "Rest for",
    val howManyMinutes: String = "how many minutes",
    val startBreak: String = "Start Break",
    val doneForNow: String = "Done for Now",
    val breakTitle: String = "Break",
    val addFiveMinutes: String = "Add 5 Minutes",
    val skipBreak: String = "Skip Break",
    val thenNextFocus: String = "Then: your next focus",

    // ---- History ----
    val historyTitle: String = "History",
    val thisWeek: String = "This week",
    val dayStreak: String = "Day streak",
    val showCommentsOnly: String = "Show comments only",
    val showGallery: String = "Show gallery",
    val relativeToday: String = "Today",
    val relativeYesterday: String = "Yesterday",
    val noNote: String = "No note",
    val emptyFeed: String = "No sessions yet.\nFinish your first focus and it'll show up here.",
    val historySubtitle: (month: String, sessions: String) -> String = { m, s -> "$m · $s" },
    val daySummary: (duration: String, sessions: String) -> String = { d, s -> "$d · $s" },
    /** Full month name for the header, and the "August 3" style day heading. */
    val monthName: (LocalDate) -> String = { it.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) },
    val monthDay: (LocalDate) -> String = {
        "${it.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)} ${it.dayOfMonth}"
    },

    // ---- Session Detail ----
    val detailsSection: String = "DETAILS",
    val planned: String = "Planned",
    val focused: String = "Focused",
    val editNote: String = "Edit Note",
    val share: String = "Share",
    val deleteSession: String = "Delete Session",
    val deleteSessionTitle: String = "Delete this session?",
    val deleteSessionBody: String = "This can't be undone.",
    val shareFailed: String = "Couldn't prepare this for sharing",
    val noPhoto: String = "No Photo",
    val focusPhoto: String = "Focus photo",

    // ---- Stats ----
    val statsTitle: String = "Stats",
    val allTime: String = "All time",
    val sessionsLabel: String = "Sessions",
    val byTask: String = "BY TASK",
    val emptyStats: String = "Nothing to count yet.\nFinish a session and its task shows up here.",
    val taskRowSummary: (sessions: String, days: String, average: String) -> String = { s, d, a -> "$s · $d · avg $a" },
    val totalsSection: String = "TOTALS",
    val total: String = "Total",
    val average: String = "Average",
    val daysDone: String = "Days done",
    val lastTime: String = "Last time",
    val last14Days: String = "LAST 14 DAYS",
    val sessionsSection: String = "SESSIONS",
    val start: String = "Start",
    val addToFavorites: String = "Add to Favorites",
    val noSessionsYet: String = "No sessions yet — finish one and its stats show up here.",
    val howLong: String = "How long?",

    // ---- Settings ----
    val settingsTitle: String = "Settings",
    val timerSection: String = "TIMER",
    val defaultDuration: String = "Default Duration",
    val keepRunningWhileNote: String = "Keep running while writing a note",
    val photosSection: String = "PHOTOS",
    val photoShape: String = "Photo Shape",
    val shapeSquare: String = "Square",
    val shapePortrait: String = "Portrait",
    val notificationsSection: String = "NOTIFICATIONS",
    val playSoundOnEnd: String = "Play a sound when a session ends",
    val vibrateOnEnd: String = "Vibrate when a session ends",
    val sound: String = "Sound",
    val systemDefault: String = "System default",
    val settingsFooter: String = "No storage permission is required. Photos are picked with the system picker and stay on this device.",
    val languageSection: String = "LANGUAGE",
    val language: String = "Language",
    val languageSystem: String = "System",
    val languageEnglish: String = "English",
    val languageKorean: String = "한국어",
)

val English = Strings()

val Korean = Strings(
    cancel = "취소",
    save = "저장",
    delete = "삭제",
    edit = "수정",
    remove = "제거",
    back = "뒤로",
    focusFallback = "집중",
    minLabel = "분",
    selected = "선택됨",
    none = "없음",
    optional = "선택",

    minutes = { "${it}분" },
    minutesCompact = { "${it}분" },
    duration = { h, m -> if (h > 0) "${h}시간 ${m}분" else "${m}분" },
    sessionsCount = { "${it}세션" },
    tasksCount = { "할 일 ${it}개" },
    daysCount = { "${it}일" },

    tabToday = "오늘",
    tabHistory = "기록",
    tabSettings = "설정",

    noTasksQueued = "예정된 할 일이 없어요",
    plannedSummary = { t, d -> "$t · $d 예정" },
    newTask = "새 할 일",
    favorites = "즐겨찾기",
    favoritesHint = "누르면 통계, 길게 누르면 수정할 수 있어요.",
    todayFooter = { s, d -> "오늘 $s 완료 · $d 집중했어요." },
    editTask = "할 일 수정",
    taskFieldPlaceholder = "할 일",
    repeat = "반복",
    repeatHint = "시작해도 목록에 계속 남아요",
    statsAction = "통계",

    quickStart = "빠른 시작",
    whatFocusingOn = "무엇에 집중할까요?",
    quickEntryPlaceholder = "예: 빨래 20",
    quickEntryHint = { "숫자를 붙이면 그 시간만큼 진행돼요. 숫자가 없으면 ${it}분으로 시작해요." },
    recentSection = "최근",
    optionsSection = "옵션",
    backgroundPhoto = "배경 사진",
    repeatingTask = "반복 할 일",
    repeatingTaskHint = "실행해도 오늘 목록에 남고, 통계가 쌓여요",
    startWithLabel = { l, m -> "$l · $m 시작" },
    startPlain = { "$it 시작" },
    saveToToday = "오늘에 저장",

    choosePhoto = "사진 선택",
    recentlyUsed = "최근 사용",
    chooseFromLibrary = "앨범에서 선택",
    removePhoto = "사진 제거",
    photoPickerNote = "시스템 사진 선택기를 사용해요. 저장소 권한은 요청하지 않아요.",

    endsAt = { "$it 에 종료" },
    noteNowPlaceholder = "지금 이 순간을 기록해 보세요…",
    note = "메모",
    pause = "일시정지",
    resume = "계속",
    finish = "완료",
    cancelSession = "세션 취소",
    toggleLandscape = "가로 모드 전환",

    sessionComplete = "세션 완료",
    stopAlarm = "알람 끄기",
    photoSection = "사진",
    addPhoto = "사진 추가",
    changePhoto = "사진 변경",
    noteSection = "메모",
    noteSessionPlaceholder = "이번 세션에 대해 기록해 보세요…",
    discard = "버리기",
    publishFooter = "저장한 세션은 기록에 남아요. 버린 세션은 기록되지 않아요.",

    sessionSaved = "세션 저장됨",
    upNext = "다음 할 일",
    moreWaiting = { "${it}개 더 대기 중" },
    startNextSession = "다음 세션 시작",
    takeABreak = "쉬어가기",
    restFor = "쉬는 시간",
    howManyMinutes = "몇 분",
    startBreak = "휴식 시작",
    doneForNow = "여기까지",
    breakTitle = "휴식",
    addFiveMinutes = "5분 더",
    skipBreak = "휴식 건너뛰기",
    thenNextFocus = "다음: 이어질 집중",

    historyTitle = "기록",
    thisWeek = "이번 주",
    dayStreak = "연속 일수",
    showCommentsOnly = "메모만 보기",
    showGallery = "갤러리 보기",
    relativeToday = "오늘",
    relativeYesterday = "어제",
    noNote = "메모 없음",
    emptyFeed = "아직 기록이 없어요.\n첫 집중을 마치면 여기에 나타나요.",
    historySubtitle = { m, s -> "$m · $s" },
    daySummary = { d, s -> "$d · $s" },
    monthName = { "${it.monthValue}월" },
    monthDay = { "${it.monthValue}월 ${it.dayOfMonth}일" },

    detailsSection = "상세",
    planned = "계획",
    focused = "집중",
    editNote = "메모 수정",
    share = "공유",
    deleteSession = "세션 삭제",
    deleteSessionTitle = "이 세션을 삭제할까요?",
    deleteSessionBody = "되돌릴 수 없어요.",
    shareFailed = "공유를 준비하지 못했어요",
    noPhoto = "사진 없음",
    focusPhoto = "집중 사진",

    statsTitle = "통계",
    allTime = "전체",
    sessionsLabel = "세션",
    byTask = "할 일별",
    emptyStats = "아직 집계할 게 없어요.\n세션을 마치면 그 할 일이 여기에 나타나요.",
    taskRowSummary = { s, d, a -> "$s · $d · 평균 $a" },
    totalsSection = "합계",
    total = "총 시간",
    average = "평균",
    daysDone = "진행한 날",
    lastTime = "마지막",
    last14Days = "최근 14일",
    sessionsSection = "세션 목록",
    start = "시작",
    addToFavorites = "즐겨찾기에 추가",
    noSessionsYet = "아직 기록이 없어요 — 한 번 마치면 통계가 나타나요.",
    howLong = "몇 분 동안?",

    settingsTitle = "설정",
    timerSection = "타이머",
    defaultDuration = "기본 시간",
    keepRunningWhileNote = "메모를 쓰는 동안 계속 진행",
    photosSection = "사진",
    photoShape = "사진 비율",
    shapeSquare = "정사각형",
    shapePortrait = "세로형",
    notificationsSection = "알림",
    playSoundOnEnd = "세션이 끝나면 소리 재생",
    vibrateOnEnd = "세션이 끝나면 진동",
    sound = "알림음",
    systemDefault = "시스템 기본값",
    settingsFooter = "저장소 권한이 필요하지 않아요. 사진은 시스템 선택기로 고르고 기기 안에만 보관돼요.",
    languageSection = "언어",
    language = "언어",
    languageSystem = "시스템 설정",
    // languageEnglish / languageKorean stay as-is: a language is named in its own language,
    // so they read the same whichever mode you're in.
)

/** "1h 15m" / "1시간 15분" from raw millis — the localized form of Formatters.focusDuration. */
fun Strings.durationOf(totalMs: Long): String {
    val totalMin = (totalMs / 60_000L).toInt()
    return duration(totalMin / 60, totalMin % 60)
}

/** "25 min" / "25분" from raw millis, rounded to the nearest minute — Formatters.focus. */
fun Strings.minutesOf(ms: Long): String = minutes(((ms + 30_000L) / 60_000L).toInt().coerceAtLeast(1))

fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ENGLISH -> English
    AppLanguage.KOREAN -> Korean
    AppLanguage.SYSTEM -> if (Locale.getDefault().language == "ko") Korean else English
}

val LocalStrings = staticCompositionLocalOf { English }
