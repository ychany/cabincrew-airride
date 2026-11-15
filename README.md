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

- 위치: [`apk/CabinCrew_AirRide_release.apk`](apk/CabinCrew_AirRide_release.apk) (서명된 release 빌드)

## 실행 방법

### 1. 설치

```bash
# Pleos Connect 에뮬레이터를 실행한 상태에서
adb install -r apk/CabinCrew_AirRide_release.apk
```

런처에서 **AirRide**를 실행합니다. 화면은 가로 방향 기준입니다.

### 2. 차량 연결 모드 / 데모 모드

앱은 시작할 때 Pleos Vehicle SDK 연결을 시도합니다.

- **차량 연결됨**: Pleos Connect 에뮬레이터에서는 Vehicle Service에 연결되어 기어(P/D), HVAC 내기순환 상태, 조향각을 실제로 받습니다. [외기로 바꾸기]를 누르면 `setHvacRecirculation(false)`가 차량 HVAC에 실제로 적용됩니다.
- **데모 모드**: SDK 연결에 실패하거나 HVAC capability를 확인할 수 없으면 자동으로 데모 모드로 전환됩니다. 차량에 연결된 상태라도 데모 패널을 조작하면 데모 값이 우선하며, 상단 칩이 `데모 모드`로 바뀝니다.

- **데모 패널 열기**: 화면 왼쪽 위 **`AirRide` 글자를 길게 누르기**
- 패널에서 시나리오 버튼을 누르거나, 슬라이더로 내기순환 시간·조향 보정·가감속·좌우 흔들림·연속 주행 시간·주행/정차를 직접 바꿀 수 있습니다.

> 에뮬레이터에서 지원하지 않는 신호(차속, 가감속, 커브, 차선 경고, 탑승 인원)는 데모 값으로 동작합니다.

### 3. 시나리오 재현 순서

| # | 데모 패널 조작 | 기대 화면 |
|---|---|---|
| 1 | `평온한 주행` | 각성 높음 / 멀미 낮음, "편안하게 가고 있어요" |
| 2 | `답답한 실내 (내기순환 오래)` | 각성 **주의** → **외기 제안 카드** → [외기로 바꾸기] → 적용 완료, 칩이 `외기`로 변경 |
| 3 | `졸음 위험` | 각성 **위험** → **휴식 제안 카드** → [경유지 추가] |
| 4 | `멀미 위험 (커브 구간)` | 멀미 **위험** → **커브 안내 카드** |
| 5 | `정차 (P)` | 동승자 설정(멀미 민감) + [주행 종료 리포트] → 부드러움 점수 / 외기 전환 횟수 / 케어 기록 |

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

### 권한

Pleos Vehicle 권한은 실행 중 승인 창 없이 Manifest 선언만으로 동작합니다. 실제로 사용하는 API의 권한만 선언했습니다.

| 권한 | 용도 |
|---|---|
| `pleos.car.permission.CAR_CLIMATE` | HVAC 내기순환 상태 조회·구독 |
| `pleos.car.permission.CONTROL_CAR_CLIMATE` | 외기 전환 (`setHvacRecirculation`) |
| `pleos.car.permission.CAR_DRIVE` | 기어 상태(P/D) → 주행/정차 판단, 조향각 → 미세 보정 빈도 |

### 소스에서 빌드

```bash
./gradlew assembleDebug                 # 디버그
./scripts/build-release.sh              # release (keystore.properties 필요, 저장소에 미포함)
```

## 사용한 SDK / API

| SDK | API | 사용처 |
|---|---|---|
| Vehicle – HVAC | `checkHvacRecirculationCapabilities`, `getHvacRecirculationState`, `registerHvacRecirculationState`, `setHvacRecirculation` | 내기순환 지속 시간 → 추정 CO₂, 외기 전환 |
| Vehicle – Gear | `registerGearSelection` | 주행/정차 |
| Vehicle – SteeringWheel | `registerSteeringWheelAngle` | 조향 미세 보정 빈도 |
| AAOS | `CarUxRestrictionsManager` | 주행 중 UI 제한 |

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
  ui/        HomeScreen, SuggestionCard, ReportScreen, DemoPanel, AirRideViewModel
apk/         CabinCrew_AirRide_release.apk
```
