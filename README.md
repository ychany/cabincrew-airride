# AirRide (에어라이드)

**운전자는 졸지 않게, 동승자는 멀미하지 않게 — 차가 알아서 실내 환경을 맞춰 주는 Pleos Connect 앱**

하나의 엔진이 두 지수를 계산합니다.

- **운전석 각성 지수** (0~100): 추정 CO₂(내기순환 지속 시간 × 탑승 인원), 조향 미세 보정 빈도, 차선 경고, 연속 주행 시간
- **동승석 멀미 지수** (0~100): 가감속 변화, 좌우 흔들림, 전방 커브, 동승자 멀미 민감도

지수에 따라 **제안 → 1탭 승인 → 공조 제어** 순서로 동작합니다. 기본은 제안형입니다.

| 상황 | 제안 |
|---|---|
| 각성 < 70 이고 내기순환 중 | "바깥 공기로 바꿀까요?" → HVAC 외기 전환 |
| 각성 < 40, 또는 각성 < 70 이면서 멀미 > 60 | "○○휴게소에서 쉬어 갈까요?" → 경유지 추가 |
| 멀미 > 40 이고 전방 커브 | "곧 커브예요. 뒷자리를 위해 부드럽게 돌아가요" |

---

## APK

- 위치: [`apk/CabinCrew_AirRide_release.apk`](apk/CabinCrew_AirRide_release.apk) (서명된 release 빌드, minSdk 28)
- 다운로드: https://github.com/ychany/cabincrew-airride/raw/main/apk/CabinCrew_AirRide_release.apk

## 실행 방법

### 1. 설치

```bash
# Pleos Connect 에뮬레이터를 실행한 상태에서
adb install -r apk/CabinCrew_AirRide_release.apk
```

런처에서 **AirRide**를 실행합니다.

**내비와 나란히 보기 (주행 중 화면 A-04~A-08 배치)**: AirRide 실행 중에 화면 오른쪽 끝의 `⋮` 손잡이를 왼쪽으로 끌어 화면을 나눈 뒤, 하단 바의 **내비게이션** 아이콘을 누르면 가운데에 내비, 오른쪽에 AirRide(840px)가 표시됩니다. 분할 상태에서는 마지막으로 연 앱이 가운데로 옵니다. AirRide는 창 폭에 맞춰 넓은 화면(캐빈 뷰 + 앱 패널)과 분할 화면(앱 패널만)을 자동으로 전환합니다.

### 2. 차량 연결 모드 / 데모 모드

앱은 시작할 때 Pleos Vehicle SDK 연결을 시도합니다.

- **차량 연결됨**: Pleos Connect 에뮬레이터에서는 Vehicle Service에 연결되어 기어(P/D), HVAC 내기순환 상태, 조향각을 실제로 받습니다. [외기로 바꾸기]를 누르면 `setHvacRecirculation(false)`가 차량 HVAC에 실제로 적용됩니다.
- **데모 모드**: SDK 연결에 실패하거나 HVAC capability를 확인할 수 없으면 자동으로 데모 모드로 전환됩니다. 차량에 연결된 상태라도 데모 패널을 조작하면 데모 값이 우선하며, 상단 칩이 `데모 모드`로 바뀝니다.

- **데모 패널 열기**: 앱 상단의 **`에어라이드` 글자를 길게 누르기**
- 패널에서 시나리오 버튼을 누르거나, 슬라이더로 내기순환 시간·조향 보정·가감속·좌우 흔들림·연속 주행 시간·주행/정차를 직접 바꿀 수 있습니다.

> 에뮬레이터에서 지원하지 않는 신호(커브, 차선 경고, 탑승 인원)는 데모 값으로 동작합니다. 차속·가감속은 Virtual Console로 바꿀 수 있습니다.

**AirRide가 화면에 없을 때 (내비 전체 화면)**: 제안은 같은 문구의 Android 알림(버튼 2개)으로 전달되고, 적용 결과는 토스트로 안내합니다. 단, 현재 Pleos Connect 에뮬레이터는 서드파티 앱의 헤즈업 알림을 화면에 띄우지 않으므로, 에뮬레이터 시연은 위의 분할 화면(가운데 내비 + 오른쪽 AirRide)으로 진행합니다.

**실제 차량 신호로 확인 (Pleos Connect Virtual Console)**: `adb forward tcp:8088 tcp:8088` 후 PC 브라우저에서 `http://127.0.0.1:8088/`을 엽니다. Motion에서 기어를 **D**로 바꾸면 AirRide가 SDK 기어 신호로 주행 모드(케어 중, 설정·리포트 숨김)로 전환되고, 차속을 바꾸면 AAOS 차속(`PERF_VEHICLE_SPEED`)으로 계산한 가감속이 멀미 지수에 반영됩니다. Climate의 내기순환·풍량 변경도 앱에 반영됩니다.

> 참고: 에뮬레이터 내비는 경로선·턴바이턴 안내는 표시하지만 바탕 지도(도로·건물) 타일은 표시하지 않습니다. 에뮬레이터 GPS 기본 위치가 미국이므로 `adb emu geo fix 127.0276 37.4979`로 서울로 옮긴 뒤 시연하세요.

> 참고: `adb install`로 설치한 앱은 설치 출처가 앱 마켓이 아니어서 AAOS가 Distraction Optimized 선언을 적용하지 않습니다. 그래서 에뮬레이터에서 **차속이 0보다 커지면 시스템이 앱 화면을 "주행 중에는 사용할 수 없습니다"로 가립니다.** 앱 마켓으로 배포되면 선언이 적용됩니다. 에뮬레이터 시연은 기어 D·차속 0 상태 또는 데모 패널로 진행합니다.

### 3. 시나리오 재현 순서

앱을 실행하면 정차 상태에서 **탑승 확인(A-01)** 화면이 먼저 나옵니다.

| # | 조작 | 기대 화면 |
|---|---|---|
| 1 | 동승자 `민준` 선택 → 케어 항목(뒷좌석 바람 조절 / 커브 전 음성 안내) 선택 → [케어 시작] | **A-01 탑승 확인 → A-02 동승 모드** → 홈 |
| 2 | 데모 패널 `평온한 주행` | 운전자 각성 **좋음** / 민준 멀미 **편안함**, 제안 없음 |
| 3 | 데모 패널 `답답한 실내 (내기순환 오래)` | **A-06** "바깥 공기를 들일까요? / 내기순환이 N분째 유지되고 있어요" → [외기로 전환] → 적용 완료, 캐빈 칩 `외기 유입` (차량 연결 시 실제 HVAC 내기순환 OFF) |
| 4 | 데모 패널 `멀미 위험 (커브 구간)` | **A-05** "곧 커브예요 / 급가속과 급제동을 줄여주세요" 음성 안내 카드 + 멀미 게이지 + `뒷좌석 바람 조절 중` (차량 연결 시 실제 HVAC 풍량 조절, 음성 안내는 TTS) |
| 5 | 데모 패널 `졸음 위험` | **A-08** "○○휴게소에서 잠시 쉬어 갈까요? / 약 N분 후 현재 경로에 있어요" → [경유지 추가] → **NaviHelper SDK로 내비에 경유지(경로가 없으면 목적지)를 요청**, 내비가 경로 안내를 시작 (A-08-2) |
| 6 | 데모 패널 `정차 (P)` (또는 내비의 휴게소 도착 이벤트) | **A-09 휴게소 도착**: 정차 확인 → [환기하기] → 쉰 뒤 상태 확인 → [다시 출발하기] |
| 7 | 정차 홈에서 [주행 종료 리포트] | **A-10** "민준이와 함께한 주행을 마쳤어요": 부드러운 주행 점수, 케어 적용·휴식 횟수, 구간별 컨디션 그래프(실제 기록), 차가 대신한 케어 기록, [다음 여행에도 이 설정 쓰기] |

## 테스트 환경 및 설정

| 항목 | 값 |
|---|---|
| 에뮬레이터 | **Pleos Connect v2.1.0** (`release_connect_v2.1.0`, Android 14, API 34, Automotive, arm64-v8a) |
| 화면 규격 | **2560 × 1440 px, 12.8인치, 160 dpi (mdpi), 가로** |
| AVD 설정 | RAM 4GB, Cold boot |
| Android Studio | 2025.2 |
| JDK | 17 이상 (Android Studio 내장 JBR 사용) |
| Gradle / AGP / Kotlin | 8.10.2 / 8.8.2 / 2.0.0 |
| minSdk / targetSdk | 28 / 35 |
| Pleos Vehicle SDK | `ai.pleos.playground:Vehicle:2.3.2` |
| Pleos NaviHelper SDK | `ai.pleos.playground:NaviHelper:3.0.5` |

### 권한

Pleos Vehicle 권한은 실행 중 승인 창 없이 Manifest 선언만으로 동작합니다. 실제로 사용하는 API의 권한만 선언했습니다.

| 권한 | 용도 |
|---|---|
| `pleos.car.permission.CAR_CLIMATE` | HVAC 내기순환 상태 조회·구독 |
| `pleos.car.permission.CONTROL_CAR_CLIMATE` | 외기 전환 (`setHvacRecirculation`), 뒷좌석 바람 조절 (`setHvacFanSpeed`) |
| `pleos.car.permission.CAR_DRIVE` | 기어 상태(P/D) → 주행/정차 판단, 조향각 → 미세 보정 빈도 |
| `pleos.car.permission.NAVI_ROUTE`, `NAVI_ROUTE_SEARCH` | 휴게소 경로·경유지 요청 |
| `android.car.permission.CAR_SPEED` | 차속 → 가감속 변화 (실행 시 권한 요청) |
| `android.permission.POST_NOTIFICATIONS` | 앱이 가려져 있을 때 제안 알림 (실행 시 권한 요청) |

### 소스에서 빌드

```bash
./gradlew assembleDebug                 # 디버그
./scripts/build-release.sh              # release (keystore.properties 필요, 저장소에 미포함)
```

## 사용한 SDK / API

| SDK | API | 사용처 |
|---|---|---|
| Vehicle – HVAC | `checkHvacRecirculationCapabilities`, `getHvacRecirculationState`, `registerHvacRecirculationState`, `setHvacRecirculation` | 내기순환 지속 시간 → 추정 CO₂, 외기 전환 |
| Vehicle – HVAC | `checkHvacFanSpeedCapabilities`, `setHvacFanSpeed` | 커브 구간 뒷좌석 바람 조절 |
| Vehicle – Gear | `registerGearSelection` | 주행/정차 |
| Vehicle – SteeringWheel | `registerSteeringWheelAngle` | 조향 미세 보정 빈도 |
| NaviHelper | `requestRoute`, `addWaypoint`, `getRouteStateInfo`, `onWayPointArrived` / `onDestinationArrived` | 휴게소 경유지 추가, 도착 시 A-09 전환 |
| AAOS | `CarUxRestrictionsManager` | 주행 중 UI 제한 |
| AAOS | `CarPropertyManager` · `PERF_VEHICLE_SPEED` | 차속 → 가감속 변화 (멀미 지수) |
| Android | `TextToSpeech` | 커브 전 음성 안내 |
| Android | `NotificationManager` (heads-up, 액션 2개) | 앱이 가려져 있을 때 외기·휴식 제안 |

## 주행 안전

- Activity에 `distractionOptimized` 선언
- `CarUxRestrictions` 기준으로 **주행 중에는 게이지 2개와 제안 카드(버튼 최대 2개)만** 표시합니다. 동승자 설정과 리포트 상세는 정차 중에만 보입니다.
- 큰 터치 영역, 짧은 문구, 애니메이션 없음, 미디어 재생 없음

## 폴더 구조

```
app/src/main/java/com/cabincrew/airride/
  data/      VehicleSignals, VehicleSignalSource, DemoVehicleSource,
             PleosVehicleSource, VehicleConnector
  engine/    ConditionEngine (각성·멀미 지수), SuggestionPolicy
  control/   CabinController (HVAC 제어), DrivingRestriction (CarUxRestrictions)
  report/    TripRecorder (케어 기록 → 주행 종료 리포트)
  control/   VoiceGuide (TTS), NaviBridge (NaviHelper 경유지)
  notify/    SuggestionNotifier (화면 밖 제안 알림)
  ui/        BoardingScreen (A-01·A-02), HomeScreen (A-03~A-08), RestStopScreen (A-09),
             ReportScreen (A-10), CabinTopView, Components, DemoPanel, AirRideViewModel
apk/         CabinCrew_AirRide_release.apk
```
