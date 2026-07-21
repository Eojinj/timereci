# Merci (Timereci)

> **"집중한 시간만큼만 발행되는 감성 영수증."**
> 집중 세션을 완주하면 한 장이 발행되고, 발행된 순간들이 날짜별 **피드**로 쌓여 나만의 집중 아카이브가 된다. 타이머는 기능이 아니라 **발행 조건**이다.

Android 전용 MVP. **Kotlin + Jetpack Compose + Room + Hilt.** PRD v0.1과 Claude Design 핸드오프
(`집중영수증.dc.html`, `PhotoCard.dc.html`)를 코드로 구현한 것.

---

## 구현 범위 (MVP)

핵심 루프 전체가 동작한다:

1. **타이머(가로)** — 할일 라벨 · 배경 사진(선택) · 시간 설정 후 집중 시작. 집중 중 코멘트 한 줄.
2. **완주 → 발행** — 완주 직후 발행 화면에서 사진/코멘트 마무리.
3. **피드** — 발행된 사진이 날짜별로 쌓인 세로 피드. 밀도로 집중량이 보인다.
4. **하루 / 상세** — 날짜를 펼쳐 회고, 한 장을 확대·내보내기.
5. **설정** — 타이머 기본값 · 영수증 길이 모드 · 코멘트 중 타이머 동작 · Pro 안내.

확정 결정 원장(PRD §10) 반영:
- 완주 시에만 발행, **포기 세션은 흔적을 남기지 않는다** (별도 히스토리 테이블 없음).
- 사진은 **시작 전/완주 후에만**, 집중 중에는 불가. **시스템 포토 피커만 사용(권한 0)**.
- 코멘트는 **단일 메모** (로그·2단계 아님), 세션 중·후 같은 칸을 편집.
- 코멘트 중 타이머 **기본값 = 계속 흐름**(정직한 집중시간), 일시정지는 옵트인.
- 무료: 무제한 타이머·발행·**로컬 피드 열람**·표준 해상도 단건 내보내기(워터마크 없음).
  Pro(placeholder): 고해상도 내보내기·테마·위젯 등.

## 아키텍처

```
com.timereci.focus
├─ data/            Room (ReceiptEntity, ActiveSessionEntity), DAO, Converters,
│                   PhotoStorage(앱 전용 저장소), FocusRepository, SettingsRepository(DataStore)
├─ di/              Hilt DataModule (DB/DAO 제공)
├─ timer/           FocusTimerController(단일 진실원) · FocusTimerService(포그라운드)
│                   · TimerAlarmScheduler/Receiver(백업 알림)
└─ ui/
   ├─ theme/        디자인 토큰(Color/Type/Theme) · PhotoTones(그라디언트 팔레트)
   ├─ components/   PhotoCard · Buttons
   ├─ feed/ day/ timer/ publish/ detail/ settings/   화면 + 각 ViewModel
   ├─ model/        FeedBuilder(영수증 → 날짜별 피드 그룹핑)
   └─ FocusApp · Routes · RootViewModel(내비게이션/복구)
```

### 타이머 신뢰성 (PRD 이슈 4 — 권장안 4-A)
- **종료시각 기반:** 실행 중에는 `SystemClock.elapsedRealtime` 목표 시각만 저장하고
  표시값은 `목표 − 현재`로 계산 → 틱을 세지 않아 드리프트가 없고 프로세스가 죽어도 정확.
- **크래시 복구:** 모든 전이를 `ActiveSessionEntity`(0/1건)에 반영. 재실행 시
  `FocusTimerController.restore()`가 상태를 복원하고, *앱이 죽은 사이 완주*한 경우까지 처리.
- **포그라운드 서비스 + 알람:** 실행 중 포그라운드 서비스가 진행 알림을 갱신하고,
  정확 알람이 프로세스가 사라져도 완주 알림을 보장.

### 이미지 저장 (이슈 5 — 권장안 5-A)
포토 피커로 고른 사진은 content Uri를 보관하지 않고 **앱 전용 저장소(`filesDir/photos`)에
JPEG로 복사**한다(EXIF 회전 보정 · 다운스케일). 영수증 메타는 Room, 사진 파일 목록은
영수증 행에 JSON으로 인라인 저장(과도한 정규화 회피, PRD §5).

### 영수증 길이 곡선 (미해결 신규 항목)
`ReceiptEntity.focusedMs`를 렌더링 입력값으로 보관. 현재 카드 UI는 고정 높이(피드/상세)
이며, `f(focusedMs)` 곡선(선형/로그/상한)은 설정의 "영수증 길이" 토글과 함께 다음 단계에서
확정 예정.

## 디자인 매핑

| 핸드오프 화면 | 구현 |
|---|---|
| FEED (by date) | `ui/feed/FeedScreen` — 날짜 헤더 + 3열 사진 그리드 + 하단 "집중 시작" |
| DAY | `ui/day/DayScreen` — 네이비 배경 풀블리드 카드 스트림 |
| TIMER (landscape) | `ui/timer/TimerScreen` — 가로 고정, 대형 모노 숫자, 코멘트 시트 |
| PUBLISH | `ui/publish/PublishScreen` — 방금 담긴 카드 + 사진/코멘트 마무리 |
| EXPORT | `ui/detail/DetailScreen` — 단건 확대 + 갤러리 저장(표준 PNG) |
| `PhotoCard.dc.html` | `ui/components/PhotoCard` |

디자인 토큰(색·팔레트)은 프로토타입에서 그대로 옮겼다(`ui/theme/`). 폰트는 자산 없이 빌드되도록
Pretendard→시스템 기본, IBM Plex Mono→`FontFamily.Monospace`로 근사했다. 실제 폰트 파일을
`res/font`에 넣고 `Type.kt`의 `MonoFamily`/`AppTypography`만 바꾸면 그대로 업그레이드된다.

## 빌드

Android Studio(최신)에서 열거나:

```bash
./gradlew :app:assembleDebug
```

- `compileSdk 35`, `minSdk 26`, JDK 17.
- 첫 빌드시 AGP/의존성 다운로드를 위해 네트워크가 필요하다.
- Room 스키마는 `app/schemas/`로 export 된다.

## 다음 단계 (범위 밖)
Google Play Billing 결제·게이팅, 크롭 엔진(Compose Canvas), 월간 스프레드/롤북, 홈 위젯,
클라우드 백업(v2). PRD §8~9 참고. 최대 리스크는 **범위** — MVP 라인 사수.
