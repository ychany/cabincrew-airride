# AirRide (에어라이드)

> **[필수 항목 바로가기]**
> 1. [앱 이름 및 한 줄 소개](#1-앱-이름-및-한-줄-소개) · 2. [실행 방법](#2-실행-방법) · 3. [테스트 환경 및 필요한 설정](#3-테스트-환경-및-필요한-설정) · 4. [Pleos Connect 에뮬레이터 버전과 화면 규격](#4-pleos-connect-에뮬레이터-버전과-화면-규격) · 5. [APK 파일 위치](#5-apk-파일-위치)

# 필수 항목 (공모전 제출 규정)

## 1. 앱 이름 및 한 줄 소개

- **앱 이름**: 에어라이드 (AirRide) / 캐빈 컨디션 매니저 — 팀 Cabin Crew
- **한 줄 소개**: 운전자는 졸지 않게, 동승자는 멀미하지 않게 차가 알아서 실내 환경을 맞춰 주는 Pleos Connect 앱

## 2. 실행 방법

### 2-1. 설치와 실행

```bash
# Pleos Connect 에뮬레이터를 실행한 상태에서
adb install -r apk/CabinCrew_AirRide_release.apk

# 또는 시연 환경 한 번에 준비 (GPS 서울, Virtual Console 포트, 설치, 권한 부여)
./scripts/setup-emulator-demo.sh
```

런처에서 **AirRide**를 실행합니다. 정차 상태에서 **탑승 확인(A-01)** 화면이 먼저 나옵니다.

> 처음 실행하면 권한 창이 뜹니다. 모두 **[허용]**을 눌러 주세요.
> - **위치 정보**: 차속(`android.car.permission.CAR_SPEED`) 권한입니다. AAOS가 차속 권한을 "위치" 그룹으로 묶어 표시하며, 앱은 위치를 수집하지 않고 차속만 읽어 가감속(멀미 지수)을 계산합니다.
> - **알림**: 앱이 가려져 있을 때 제안을 알림으로 보냅니다.
> - `setup-emulator-demo.sh`로 설치하면 권한이 미리 부여됩니다.

### 2-2. 데모 모드 진입

- **데모 패널 열기**: 앱 상단의 **`에어라이드` 글자를 길게 누르기** (커브 안내 화면에서는 화면 아무 곳이나 길게 누르기)
- 패널의 시나리오 버튼이나 슬라이더(내기순환 시간, 조향 보정, 가감속, 좌우 흔들림, 연속 주행, 주행/정차 등)로 상황을 재현합니다.
- 앱은 시작할 때 Pleos Vehicle SDK에 연결합니다. 연결에 실패하면 자동으로 데모 모드로 동작하고, 데모 패널을 조작하면 데모 값이 실제 차량 신호보다 우선합니다.

### 2-3. 시나리오 재현 순서

| # | 조작 | 기대 화면 |
|---|---|---|
| 1 | 동승자 `민준` 선택 → 케어 항목 선택 → [케어 시작] | **A-01 탑승 확인 → A-02 동승 모드** → 홈 |
| 2 | 데모 패널 `평온한 주행` | 운전자 각성 **좋음** / 민준 멀미 **편안함**, 제안 없음 |
| 3 | 데모 패널 `답답한 실내 (내기순환 오래)` → [외기로 전환] | **A-06** "바깥 공기를 들일까요?" → **A-07** 적용 완료. 차량 HVAC 외기 전환 (옵션: 운전석 1.5℃ 낮춤, 미세먼지 모드 2분 후 내기 복귀) |
| 4 | 데모 패널 `멀미 위험 (커브 구간)` → 데모 패널에서 `전방 커브` 끄기 | **A-05** Gleo 오브와 함께 "곧 커브예요…" → "멀미가 우려돼요" → "바깥 공기로 바꿀게요"(뒷좌석 풍량 조절·외기 전환) → "뒷좌석 바람 조절 중" → 커브가 끝나면 "상태가 안정되어 바람 조절을 마쳤어요" |
| 5 | 데모 패널 `졸음 위험` → [경유지 추가] | **A-08** "피로가 쌓인 것 같아요" 안내 → "망향휴게소에서 잠시 쉬어 갈까요?" → **A-08-2** NaviHelper로 내비에 경로 요청, 안내 시작 |
| 6 | 데모 패널 `정차 (P)` → [환기하기] → [다시 출발하기] | **A-09 휴게소 도착**: 정차 확인 → 환기 → 쉰 뒤 상태 확인 |
| 7 | 정차 홈에서 [주행 종료 리포트] | **A-10** 부드러운 주행 점수, 케어 적용·휴식 횟수, 구간별 컨디션 그래프, 차가 대신한 케어 기록 |

## 3. 테스트 환경 및 필요한 설정

| 항목 | 값 |
|---|---|
| Android Studio | 2025.2 |
| JDK | 17 이상 (Android Studio 내장 JBR 사용) |
| Gradle / AGP / Kotlin | 8.10.2 / 8.8.2 / 2.0.0 |
| minSdk / targetSdk | 28 / 35 |
| Pleos Vehicle SDK | `ai.pleos.playground:Vehicle:2.3.2` |
| Pleos NaviHelper SDK | `ai.pleos.playground:NaviHelper:3.0.5` |
| AVD 설정 | RAM 4GB, Cold boot |

**필요한 설정 (권한)** — Pleos 권한은 Manifest 선언만으로 동작하고, 실제로 사용하는 API의 권한만 선언했습니다.

| 권한 | 용도 |
|---|---|
| `pleos.car.permission.CAR_CLIMATE` | HVAC 내기순환 상태 조회·구독 |
| `pleos.car.permission.CONTROL_CAR_CLIMATE` | 외기 전환, 뒷좌석 풍량, 운전석 온도 |
| `pleos.car.permission.CAR_DRIVE` | 기어(P/D) → 주행/정차, 조향각 → 미세 보정 빈도 |
| `pleos.car.permission.NAVI_ROUTE`, `NAVI_ROUTE_SEARCH` | 휴게소 경로·경유지 요청 |
| `android.car.permission.CAR_SPEED` | 차속 → 가감속 변화 (실행 시 권한 요청) |
| `android.permission.POST_NOTIFICATIONS` | 앱이 가려져 있을 때 제안 알림 (실행 시 권한 요청) |

## 4. Pleos Connect 에뮬레이터 버전과 화면 규격

| 항목 | 값 |
|---|---|
| **에뮬레이터 버전** | **Pleos Connect v2.1.0** (`release_connect_v2.1.0`, Android 14, API 34, Automotive, arm64-v8a) |
| **화면 규격** | **2560 × 1440 px, 12.8인치, 160 dpi (mdpi), 가로** |

## 5. APK 파일 위치

- **위치**: [`apk/CabinCrew_AirRide_release.apk`](apk/CabinCrew_AirRide_release.apk) (서명된 release 빌드, minSdk 28)
- **다운로드**: https://github.com/ychany/cabincrew-airride/raw/main/apk/CabinCrew_AirRide_release.apk
- **시연 영상**
  - [`demo-videos/CabinCrew_AirRide_01_화면시안_프로토타입.mp4`](demo-videos/CabinCrew_AirRide_01_화면시안_프로토타입.mp4) — 화면 시안 프로토타입 (94초, Figma)
  - [`demo-videos/CabinCrew_AirRide_02_에뮬레이터_시연.mp4`](demo-videos/CabinCrew_AirRide_02_에뮬레이터_시연.mp4) — 에뮬레이터 시연 (66초, A-01~A-10 실제 앱 동작)

---

# 참고 (추가 정보)

## 핵심 개념

하나의 엔진이 두 지수를 계산하고, 지수에 따라 **제안 → 1탭 승인 → 공조·내비 제어** 순서로 동작합니다.

- **운전석 각성 지수** (0~100): 추정 CO₂(내기순환 지속 시간 × 탑승 인원), 조향 미세 보정 빈도, 차선 경고, 연속 주행 시간
- **동승석 멀미 지수** (0~100): 가감속 변화, 좌우 흔들림, 전방 커브, 동승자 멀미 민감도

| 상황 | 제안 |
|---|---|
| 각성 < 70 이고 내기순환 중 | "바깥 공기를 들일까요?" → HVAC 외기 전환 |
| 각성 < 40, 또는 각성 < 70 이면서 멀미 > 60 | "망향휴게소에서 잠시 쉬어 갈까요?" → 경유지 추가 |
| 멀미 > 40 이고 전방 커브 | 커브 안내 → 뒷좌석 바람 조절 (자동) |

## 케어 설정 (정차 중 홈 화면)

뒷좌석 바람 조절, 커브 전 음성 안내, 운전석 시원하게(외기 전환 시 운전석 1.5℃↓), 미세먼지 모드(외기를 2분만 열고 다시 내기순환 — 한국교통안전공단 권고: 내기순환 중 1~2시간마다 1~2분 외기 전환). A-10의 [다음 여행에도 이 설정 쓰기]로 저장됩니다.

## 내비와 나란히 보기 (주행 중 화면 A-04~A-08 배치)

AirRide 실행 중에 화면 오른쪽 끝의 `⋮` 손잡이를 왼쪽으로 끌어 화면을 나눈 뒤, 하단 바의 **내비게이션** 아이콘을 누르면 가운데에 내비, 오른쪽에 AirRide(840px)가 표시됩니다. AirRide는 창 폭에 맞춰 넓은 화면(캐빈 뷰 + 앱 패널)과 분할 화면(앱 패널만)을 자동으로 전환합니다.

AirRide가 화면에 없을 때(내비 전체 화면)는 제안을 같은 문구의 Android 알림(버튼 2개)으로 보내고 결과는 토스트로 안내합니다. 단, 현재 에뮬레이터는 서드파티 앱의 헤즈업 알림을 띄우지 않으므로 시연은 분할 화면으로 진행합니다.

## 실제 차량 신호로 확인 (Pleos Connect Virtual Console)

`adb forward tcp:8088 tcp:8088` 후 PC 브라우저에서 `http://127.0.0.1:8088/`을 엽니다 (탭을 열어 둔 상태에서 동작). Motion에서 기어를 **D**로 바꾸면 AirRide가 SDK 기어 신호로 주행 모드로 전환되고, 차속을 바꾸면 AAOS 차속(`PERF_VEHICLE_SPEED`)으로 계산한 가감속이 멀미 지수에 반영됩니다. Climate의 내기순환·풍량 변경도 앱에 반영됩니다.

## 에뮬레이터 제약과 데모 데이터

- 에뮬레이터에서 제공되지 않는 신호(전방 커브, 차선 경고, 탑승 인원)는 데모 값으로 동작합니다.
- 에뮬레이터 내비는 경로선·턴바이턴 안내는 표시하지만 바탕 지도 타일은 표시하지 않습니다. GPS 기본 위치가 미국이므로 `setup-emulator-demo.sh`로 서울로 옮긴 뒤 시연하세요.
- `adb install`로 설치한 앱은 앱 마켓 설치가 아니어서 AAOS가 Distraction Optimized 선언을 적용하지 않습니다. **차속이 0보다 크면 시스템이 앱 화면을 가리므로** 시연은 기어 D·차속 0 상태 또는 데모 패널로 진행합니다.
- **휴게소 데이터**: 휴식 제안의 휴게소는 데모용으로 **경부고속도로 망향휴게소(부산방향)** 하나로 고정되어 있습니다 (좌표는 부근 근사값 36.8945, 127.1878). 카드의 거리·시간은 데모 고정값이고, 실제 거리와 소요 시간은 내비가 계산합니다 (서울 강남 출발 시 약 63km, 54분).

## 사용한 SDK / API

| SDK | API | 사용처 |
|---|---|---|
| Vehicle – HVAC | `checkHvacRecirculationCapabilities`, `getHvacRecirculationState`, `registerHvacRecirculationState`, `setHvacRecirculation` | 내기순환 지속 시간 → 추정 CO₂, 외기 전환 |
| Vehicle – HVAC | `checkHvacFanSpeedCapabilities`, `setHvacFanSpeed` | 커브 구간 뒷좌석 바람 조절 |
| Vehicle – HVAC | `checkHvacTemperatureCapabilities`, `getHvacTemperature`, `setHvacTemperature` | 외기 전환 시 운전석 온도 1.5℃ 낮춤 |
| Vehicle – Gear | `registerGearSelection` | 주행/정차 |
| Vehicle – SteeringWheel | `registerSteeringWheelAngle` | 조향 미세 보정 빈도 |
| NaviHelper | `requestRoute`, `addWaypoint`, `getRouteStateInfo`, `onWayPointArrived` / `onDestinationArrived` | 휴게소 경유지 추가, 도착 시 A-09 전환 |
| AAOS | `CarUxRestrictionsManager` | 주행 중 UI 제한 |
| AAOS | `CarPropertyManager` · `PERF_VEHICLE_SPEED` | 차속 → 가감속 변화 (멀미 지수) |
| Android | `TextToSpeech` | 커브 전 음성 안내 |
| Android | `NotificationManager` (heads-up, 액션 2개) | 앱이 가려져 있을 때 외기·휴식 제안 |

## 주행 안전

- Activity에 `distractionOptimized` 선언
- `CarUxRestrictions` 기준으로 **주행 중에는 게이지 2개와 제안 카드(버튼 최대 2개)만** 표시합니다. 동승자 설정, 케어 설정, 리포트는 정차 중에만 보입니다.
- 큰 터치 영역, 짧은 문구, 애니메이션 최소화, 미디어 재생 없음

## 소스에서 빌드

```bash
./gradlew assembleDebug                 # 디버그
./gradlew testDebugUnitTest             # 단위 테스트
./scripts/build-release.sh              # release (keystore.properties 필요, 저장소에 미포함)
```

## 폴더 구조

```
app/src/main/java/com/cabincrew/airride/
  data/      VehicleSignals, VehicleSignalSource, DemoVehicleSource,
             PleosVehicleSource, VehicleConnector, CarSpeedMonitor
  engine/    ConditionEngine (각성·멀미 지수), SuggestionPolicy
  control/   CabinController (HVAC 제어), DrivingRestriction (CarUxRestrictions),
             VoiceGuide (TTS), NaviBridge (NaviHelper 경유지)
  report/    TripRecorder (케어 기록 → 주행 종료 리포트)
  notify/    SuggestionNotifier (화면 밖 제안 알림)
  ui/        BoardingScreen (A-01·A-02), HomeScreen (A-03~A-08), CurveFlow (A-05),
             RestStopScreen (A-09), ReportScreen (A-10), CabinTopView, Components,
             DemoPanel, AirRideViewModel
apk/         CabinCrew_AirRide_release.apk
scripts/     build-release.sh, setup-emulator-demo.sh
```
