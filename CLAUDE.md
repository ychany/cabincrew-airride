# AirRide — 작업 규칙

Pleos Connect 차량용 앱 공모전(현대자동차그룹) 1차 접수 가산점용 데모 앱.
운전자 각성 지수 + 동승자 멀미 지수 → 실내 환경(공조·시트·창문) 제안.

## 절대 규칙

1. **마감 2026-11-15 23:59 KST.** 23:30까지 release APK가 먼저. 기능이 덜 돼도 APK부터.
2. **실명·연락처·이메일 금지.** 코드, 주석, README, 커밋 메시지, 커밋 author 어디에도 넣지 않는다.
   - git author는 로컬 설정 `CabinCrew <...@users.noreply.github.com>`을 그대로 쓴다. 바꾸지 않는다.
   - 루트의 `*.docx`(접수 양식, 실명 포함)는 커밋 금지 — .gitignore 처리됨.
3. **커밋 메시지에 AI 표기 금지.** `Co-Authored-By`, "Generated with …", "with Claude" 같은 줄을 넣지 않는다.
   (.git/hooks/commit-msg 가 막는다. 우회 금지)
4. **커밋은 작업 단위로 나눈다.** 한 커밋 = 한 가지 목적. Conventional Commits(`feat:`, `fix:`, `build:`, `docs:`, `chore:`).
5. **Pleos SDK API는 추측 금지.** 패키지명·메서드명·권한 문자열은 공식 문서/샘플에서 확인한 것만 쓴다.
   - 샘플: https://github.com/Pleos-Playground/pleos-sample-vehicle-app
   - 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
6. **막히는 SDK 기능은 데모 모드(가짜 신호)로 대체**하고 README에 적는다. 오래 붙잡지 않는다.
7. 키스토어(`*.jks`, `*.keystore`)와 `keystore.properties`는 커밋 금지.

## 확인된 환경 값 (문서/샘플 기준)

| 항목 | 값 |
|---|---|
| Maven repo | `https://nexus-playground.pleos.ai/repository/maven-releases/` |
| Vehicle SDK | `ai.pleos.playground:Vehicle:2.3.2` |
| 초기화 | `val vehicle = Vehicle(context); vehicle.initialize(); vehicle.getXxx()` → 끝나면 `vehicle.release()` |
| API 타입 | Get / Set / Callback / Capability, 결과는 `onComplete` / `onFailed` 비동기 |
| 권한 | `pleos.car.permission.*` (예: `CAR_CLIMATE`, `CONTROL_CAR_CLIMATE`) — 실제 쓰는 것만 선언 |
| 주행 규제 | AAOS `CarUxRestrictionsManager` → `onUxRestrictionsChanged()` |
| 에뮬레이터 | Pleos Connect v2.1 AVD (Android 14 / API 34, Automotive), RAM 4GB, Cold boot |
| 빌드 | AGP 8.8.2, Gradle 8.10.2, Kotlin 2.0.0, JDK 17+, compile/target 35, **minSdk 28** |

JDK는 Android Studio 내장 JBR 사용:
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`

## 제출 규정

- APK: `apk/CabinCrew_AirRide_release.apk` (release 빌드, 서명)
- README 필수: 앱 이름·한 줄 소개 / 실행 방법 / 테스트 환경·설정 / **에뮬레이터 버전·화면 규격** / APK 위치
- 11/16(월)까지 Public 전환, 11/23까지 유지. 심사는 메일에 적은 최종 커밋 해시 기준.

## 주행 안전 (UI 규칙)

- Activity에 Distraction Optimized 선언.
- 주행 중: 게이지 2개 + 제안 카드(버튼 최대 2개)만. 목록·긴 텍스트·리포트 상세는 정차 중에만.
- 큰 터치 영역, 짧은 문구, 애니메이션 최소화. 영상·미디어 재생 없음.

## 구조

```
app/src/main/java/com/cabincrew/airride/
  data/     VehicleSignalSource, PleosVehicleSource, DemoVehicleSource
  engine/   ConditionEngine, SuggestionPolicy   (수치는 데모용 가정, "튜닝 예정")
  control/  CabinController   (미지원/실패 시 로그 + UI만 반영)
  ui/       HomeScreen, SuggestionCard, RestSuggestion, ReportScreen, DemoPanel
  report/   TripRecorder
```

Pleos SDK 연결 실패 또는 capability 없음 → 자동으로 DemoVehicleSource 폴백, 상단 칩에 "데모 모드".
