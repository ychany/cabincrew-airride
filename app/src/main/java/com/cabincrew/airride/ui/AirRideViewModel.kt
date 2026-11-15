package com.cabincrew.airride.ui

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.airride.R
import com.cabincrew.airride.control.CabinController
import com.cabincrew.airride.control.DrivingRestriction
import com.cabincrew.airride.control.NaviBridge
import com.cabincrew.airride.control.VoiceGuide
import com.cabincrew.airride.data.DemoVehicleSource
import com.cabincrew.airride.data.VehicleConnector
import com.cabincrew.airride.data.VehicleSignalSource
import com.cabincrew.airride.data.VehicleSignals
import com.cabincrew.airride.engine.Condition
import com.cabincrew.airride.engine.ConditionEngine
import com.cabincrew.airride.engine.PassengerProfile
import com.cabincrew.airride.engine.Suggestion
import com.cabincrew.airride.engine.SuggestionPolicy
import com.cabincrew.airride.notify.SuggestionNotifier
import android.widget.Toast
import com.cabincrew.airride.report.CareType
import com.cabincrew.airride.report.TripRecorder
import com.cabincrew.airride.report.TripReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Sample passenger profiles (A-01). Names/photos are demo data from the design. */
data class Passenger(val name: String, @DrawableRes val photo: Int?, val motionSensitive: Boolean)

val SAMPLE_PASSENGERS = listOf(
    Passenger("민준", R.drawable.passenger_minjun, motionSensitive = true),
    Passenger("서아", R.drawable.passenger_seoa, motionSensitive = false),
    Passenger("동승자", null, motionSensitive = false),
)

enum class Phase { BOARDING_WHO, BOARDING_CARE, MAIN, REST_STOP }

/** A-09 휴게소 도착 steps: 1 정차 확인 → 2 환기 → 3 쉰 뒤 상태 확인 */
enum class RestStep { VENTILATE, RECHECK }

/** A-05 curve care sequence (Figma 프로토타입: 대응 → 대응+ → 대응++ → 결과). */
enum class CurveStage { WARN, CONCERN, ADJUSTING_NOTICE, ADJUSTING, STABLE }

data class CareOptions(
    val rearFan: Boolean = true,
    val curveVoice: Boolean = false,
    /** 외기 전환 시 운전석 온도 1.5℃ 낮추기 */
    val coolDriver: Boolean = true,
    /** 미세먼지 모드: 외기를 2분만 열고 다시 내기순환 */
    val briefFreshAir: Boolean = false,
)

data class UiState(
    val connecting: Boolean = true,
    val demoMode: Boolean = true,
    val phase: Phase = Phase.BOARDING_WHO,
    val passenger: Passenger? = null,
    val pendingPassenger: Passenger? = null,
    val care: CareOptions = CareOptions(),
    val signals: VehicleSignals = VehicleSignals(),
    val condition: Condition = ConditionEngine.evaluate(VehicleSignals(), PassengerProfile()),
    val suggestion: Suggestion? = null,
    /** True while driving restrictions apply → minimal UI. */
    val restricted: Boolean = true,
    /** Short confirmation shown after an action (A-07). */
    val appliedMessage: String? = null,
    /** A-05: rear fan softened during the current curve. */
    val rearFanActive: Boolean = false,
    /** A-08 first step: orb + "피로가 쌓인 것 같아요" notice without buttons, then the card with buttons. */
    val restIntro: Boolean = false,
    /** Non-null while the A-05 curve sequence is on screen. */
    val curveStage: CurveStage? = null,
    val restAreaName: String? = null,
    val restStep: RestStep = RestStep.VENTILATE,
    val report: TripReport? = null,
    val demoPanelOpen: Boolean = false,
    /** AirRide is on screen (full or split). Off-screen suggestions go to a heads-up notification. */
    val visible: Boolean = true,
) {
    val profile get() = PassengerProfile(motionSensitive = passenger?.motionSensitive == true)
    val passengerName get() = passenger?.name ?: "동승자"
}

class AirRideViewModel(app: Application) : AndroidViewModel(app) {

    val demo = DemoVehicleSource()
    private val recorder = TripRecorder()
    private val restriction = DrivingRestriction(app)
    private val voice = VoiceGuide(app)
    private val navi = NaviBridge(app) { viewModelScope.launch { onArrivedAtRestArea() } }
    private val notifier = SuggestionNotifier(app)
    private var notified: Suggestion? = null
    private val prefs = app.getSharedPreferences("airride", android.content.Context.MODE_PRIVATE)

    private var source: VehicleSignalSource = demo
    private var controller = CabinController(null, null, demo)
    private val dismissed = mutableSetOf<Class<out Suggestion>>()
    private var curveHandled = false
    private var restShown = false
    private var restIntroJob: kotlinx.coroutines.Job? = null
    private var restPending: String? = null

    private val _state = MutableStateFlow(
        UiState(
            care = CareOptions(
                rearFan = prefs.getBoolean("rearFan", true),
                curveVoice = prefs.getBoolean("curveVoice", false),
                coolDriver = prefs.getBoolean("coolDriver", true),
                briefFreshAir = prefs.getBoolean("briefFreshAir", false),
            ),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = VehicleConnector.connect(app, demo, viewModelScope)
            source = result.source
            controller = CabinController(result.vehicle, result.hvacArea, demo)
            source.start()
            _state.update { it.copy(connecting = false, demoMode = source.isDemo) }
            launch { source.signals.collect { recompute(it) } }
            launch { restriction.restricted.collect { recompute(source.signals.value) } }
            // Buttons on the heads-up notification (A-06 / A-08 over the map).
            launch { SuggestionNotifier.actionFlow.collect { accept -> if (accept) acceptSuggestion() else dismissSuggestion() } }
            // Sample condition for the report once per second.
            launch {
                while (isActive) {
                    delay(1_000)
                    val st = _state.value
                    if (st.phase == Phase.MAIN && st.signals.isDriving) {
                        recorder.sample(st.condition.alertness, st.condition.motionSickness)
                    }
                }
            }
        }
    }

    private fun recompute(s: VehicleSignals) {
        _state.update { st ->
            val c = ConditionEngine.evaluate(s, st.profile)
            val suggestion = if (st.phase == Phase.MAIN && s.isDriving) {
                SuggestionPolicy.decide(c, s, dismissed)
                    .takeUnless { it == Suggestion.CurveAhead && st.passenger == null }
            } else null
            st.copy(
                signals = s,
                condition = c,
                suggestion = suggestion,
                // Demo mode: the demo panel decides driving/parked so judges can reproduce scenarios.
                restricted = if (source.isDemo) s.isDriving else restriction.restricted.value ?: s.isDriving,
                rearFanActive = st.rearFanActive && s.curveAhead,
            )
        }
        val st = _state.value
        val isRest = st.suggestion is Suggestion.Rest
        if (isRest && !restShown) {
            restShown = true
            restIntroJob?.cancel()
            _state.update { it.copy(restIntro = true) }
            restIntroJob = viewModelScope.launch {
                delay(REST_INTRO_MS)
                _state.update { it.copy(restIntro = false) }
            }
        } else if (!isRest && restShown) {
            restShown = false
            restIntroJob?.cancel()
            _state.update { it.copy(restIntro = false) }
        }
        syncNotification(_state.value)
        if (st.suggestion == Suggestion.CurveAhead) onCurve() else if (!st.signals.curveAhead) onCurveEnded()
        // Arrived at the rest area: parked after accepting the rest suggestion → A-09.
        val pending = restPending
        if (pending != null && !st.signals.isDriving && st.phase == Phase.MAIN) {
            restPending = null
            _state.update { it.copy(phase = Phase.REST_STOP, restAreaName = pending, restStep = RestStep.VENTILATE) }
        }
    }

    fun setVisible(visible: Boolean) {
        _state.update { it.copy(visible = visible) }
        syncNotification(_state.value)
    }

    /** Off-screen → post the suggestion as a heads-up card over the map; on-screen → in-app card only. */
    private fun syncNotification(st: UiState) {
        val s = st.suggestion
        val copy = s?.let { suggestionCopy(it, st) }
        if (st.visible || copy == null) {
            if (notified != null) notifier.cancel()
            notified = null
            return
        }
        if (notified == s) return
        notified = s
        notifier.show(copy.title, copy.body, copy.primary, copy.secondary)
    }

    private var curveJob: kotlinx.coroutines.Job? = null

    /**
     * A-05: curve ahead → voice prompt, then (if 뒷좌석 바람 조절 is on) gentle rear airflow + fresh air.
     * No buttons while driving; each step stays on screen for a few seconds.
     */
    private fun onCurve() {
        if (curveHandled) return
        curveHandled = true
        val care = _state.value.care
        curveJob?.cancel()
        curveJob = viewModelScope.launch {
            setCurveStage(CurveStage.WARN)
            if (care.curveVoice) voice.speak("곧 커브예요. 급가속과 급제동을 줄여주세요")
            delay(CURVE_STEP_MS)
            setCurveStage(CurveStage.CONCERN)
            if (!care.rearFan) {
                recorder.record(CareType.CURVE_GUIDE, "커브 구간 · 부드러운 주행 안내", automatic = true)
                return@launch
            }
            delay(CURVE_STEP_MS)
            setCurveStage(CurveStage.ADJUSTING_NOTICE)
            if (care.curveVoice) voice.speak("뒷좌석을 조금 더 시원하게, 바깥 공기로 바꿀게요")
            controller.softenRearFan { applied ->
                recorder.record(CareType.CURVE_GUIDE, if (applied) "커브 구간 · 뒷좌석 바람 조절" else "커브 구간 · 뒷좌석 바람 조절 (데모)", automatic = true)
            }
            if (_state.value.signals.recirculationOn) {
                controller.switchToFreshAir { applied ->
                    recorder.record(CareType.FRESH_AIR, if (applied) "커브 구간 · 외기로 전환" else "커브 구간 · 외기로 전환 (데모)", automatic = true)
                }
            }
            _state.update { it.copy(rearFanActive = true) }
            delay(CURVE_STEP_MS)
            setCurveStage(CurveStage.ADJUSTING)
        }
    }

    /** Curve passed → "상태가 안정되어 바람 조절을 마쳤어요", then back to the normal screen. */
    private fun onCurveEnded() {
        if (!curveHandled) return
        curveHandled = false
        curveJob?.cancel()
        val wasAdjusting = _state.value.curveStage == CurveStage.ADJUSTING || _state.value.curveStage == CurveStage.ADJUSTING_NOTICE
        if (!wasAdjusting) {
            _state.update { it.copy(curveStage = null) }
            return
        }
        curveJob = viewModelScope.launch {
            setCurveStage(CurveStage.STABLE)
            delay(CURVE_STABLE_MS)
            _state.update { it.copy(curveStage = null, rearFanActive = false) }
        }
    }

    private fun setCurveStage(stage: CurveStage) = _state.update { it.copy(curveStage = stage) }

    // ---- A-01 / A-02 boarding ----

    fun choosePassenger(p: Passenger?) {
        if (p == null) {
            _state.update { it.copy(passenger = null, phase = Phase.MAIN) }
            recompute(source.signals.value)
        } else {
            _state.update { it.copy(pendingPassenger = p, phase = Phase.BOARDING_CARE) }
        }
    }

    fun setCare(options: CareOptions) = _state.update { it.copy(care = options) }

    fun backToWho() = _state.update { it.copy(phase = Phase.BOARDING_WHO) }

    fun startCare() {
        val p = _state.value.pendingPassenger ?: return
        recorder.record(CareType.SESSION, "${p.name} 동승 모드 시작")
        _state.update { it.copy(passenger = p, phase = Phase.MAIN) }
        recompute(source.signals.value)
    }

    // ---- suggestions ----

    fun acceptSuggestion() {
        when (val s = _state.value.suggestion) {
            Suggestion.FreshAir -> {
                val care = _state.value.care
                controller.switchToFreshAir(
                    brief = care.briefFreshAir,
                    onRestored = { applied ->
                        recorder.record(CareType.CABIN_ADJUST, if (applied) "미세먼지 모드 · 다시 내기순환" else "미세먼지 모드 · 다시 내기순환 (데모)", automatic = true)
                    },
                ) { applied ->
                    recorder.record(CareType.FRESH_AIR, if (applied) "외기로 전환" else "외기로 전환 (데모)")
                }
                if (care.coolDriver) {
                    controller.coolDriverSeat { celsius ->
                        recorder.record(
                            CareType.CABIN_ADJUST,
                            if (celsius != null) "운전석 온도 %.1f℃로 낮춤".format(celsius) else "운전석 온도 낮춤 (데모)",
                        )
                    }
                }
                showApplied(
                    when {
                        care.briefFreshAir -> "외기로 2분간 환기할게요"
                        care.coolDriver -> "외기로 바꾸고 운전석을 시원하게 했어요"
                        else -> "외기로 바꿨어요"
                    }
                )
            }
            is Suggestion.Rest -> {
                // A-08-2: hand the rest area to the navigation app (NaviHelper SDK).
                val sent = navi.addRestStop(NaviBridge.Place(s.restAreaName, s.latitude, s.longitude, s.address))
                recorder.record(CareType.REST, if (sent) "${s.restAreaName} 경유지 추가" else "${s.restAreaName} 경유지 추가 (데모)")
                dismissed += Suggestion.Rest::class.java
                restPending = s.restAreaName
                showApplied("${s.restAreaName}를 경유지로 추가했어요")
            }
            Suggestion.CurveAhead, null -> Unit
        }
        recompute(source.signals.value)
    }

    fun dismissSuggestion() {
        _state.value.suggestion?.let { dismissed += it::class.java }
        recompute(source.signals.value)
        // Ask again later.
        viewModelScope.launch {
            delay(DISMISS_COOLDOWN_MS)
            dismissed.clear()
            recompute(source.signals.value)
        }
    }

    private fun showApplied(msg: String) {
        // Over the map (A-07 / A-08-2): system toast at the bottom.
        if (!_state.value.visible) Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        _state.update { it.copy(appliedMessage = msg) }
        viewModelScope.launch {
            delay(3_000)
            _state.update { if (it.appliedMessage == msg) it.copy(appliedMessage = null) else it }
        }
    }

    // ---- A-09 rest stop ----

    /** Navigation app reported arrival → start the rest-stop flow if a rest was planned. */
    private fun onArrivedAtRestArea() {
        val name = restPending ?: return
        restPending = null
        _state.update { it.copy(phase = Phase.REST_STOP, restAreaName = name, restStep = RestStep.VENTILATE) }
    }

    fun ventilate(doIt: Boolean) {
        val name = _state.value.restAreaName ?: "휴게소"
        if (doIt) {
            controller.switchToFreshAir { applied ->
                recorder.record(CareType.FRESH_AIR, if (applied) "$name · 실내 환기" else "$name · 실내 환기 (데모)")
            }
        }
        // Rest resets the fatigue factors (demo assumption — 튜닝 예정).
        demo.update { it.copy(continuousDriveMinutes = 0f, steeringCorrectionsPerMin = 2f, laneWarningRecent = false) }
        _state.update { it.copy(restStep = RestStep.RECHECK) }
    }

    fun finishRest() {
        dismissed.clear()
        _state.update { it.copy(phase = Phase.MAIN, restAreaName = null) }
        recompute(source.signals.value)
    }

    // ---- report ----

    fun endTrip() {
        _state.update { it.copy(report = recorder.buildReport()) }
    }

    /** A-10 "다음 여행에도 이 설정 쓰기" */
    fun saveSettingsForNextTrip() {
        val care = _state.value.care
        prefs.edit()
            .putBoolean("rearFan", care.rearFan).putBoolean("curveVoice", care.curveVoice)
            .putBoolean("coolDriver", care.coolDriver).putBoolean("briefFreshAir", care.briefFreshAir)
            .apply()
        Toast.makeText(getApplication(), "다음 여행에도 이 설정을 쓸게요", Toast.LENGTH_SHORT).show()
        closeReport()
    }

    fun closeReport() {
        recorder.reset()
        dismissed.clear()
        _state.update { it.copy(report = null, phase = Phase.BOARDING_WHO, passenger = null) }
    }

    // ---- demo ----

    fun toggleDemoPanel(open: Boolean = !_state.value.demoPanelOpen) {
        _state.update { it.copy(demoPanelOpen = open) }
    }

    fun applyScenario(scenario: DemoVehicleSource.Scenario) {
        enterDemoOverride()
        dismissed.clear()
        if (!scenario.signals.curveAhead) onCurveEnded()
        demo.apply(scenario)
    }

    fun updateDemo(transform: (VehicleSignals) -> VehicleSignals) {
        enterDemoOverride()
        demo.update(transform)
    }

    private fun enterDemoOverride() {
        if (source.isDemo) return
        source.setDemoOverride(true)
        _state.update { it.copy(demoMode = true) }
    }

    override fun onCleared() {
        source.stop()
        controller.release()
        restriction.release()
        voice.release()
        navi.release()
        notifier.cancel()
    }

    private companion object {
        const val DISMISS_COOLDOWN_MS = 60_000L
        const val CURVE_STEP_MS = 3_000L
        const val REST_INTRO_MS = 3_000L
        const val CURVE_STABLE_MS = 4_000L
    }
}
