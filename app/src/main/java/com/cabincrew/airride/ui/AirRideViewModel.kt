package com.cabincrew.airride.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.airride.control.CabinController
import com.cabincrew.airride.control.DrivingRestriction
import com.cabincrew.airride.data.DemoVehicleSource
import com.cabincrew.airride.data.VehicleConnector
import com.cabincrew.airride.data.VehicleSignalSource
import com.cabincrew.airride.data.VehicleSignals
import com.cabincrew.airride.engine.Condition
import com.cabincrew.airride.engine.ConditionEngine
import com.cabincrew.airride.engine.PassengerProfile
import com.cabincrew.airride.engine.Suggestion
import com.cabincrew.airride.engine.SuggestionPolicy
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

data class UiState(
    val connecting: Boolean = true,
    val demoMode: Boolean = true,
    val signals: VehicleSignals = VehicleSignals(),
    val condition: Condition = ConditionEngine.evaluate(VehicleSignals(), PassengerProfile()),
    val profile: PassengerProfile = PassengerProfile(),
    val suggestion: Suggestion? = null,
    /** True while driving restrictions apply → minimal UI. */
    val restricted: Boolean = true,
    /** Short confirmation shown after an action (A-07). */
    val appliedMessage: String? = null,
    val report: TripReport? = null,
    val demoPanelOpen: Boolean = false,
)

class AirRideViewModel(app: Application) : AndroidViewModel(app) {

    val demo = DemoVehicleSource()
    private val recorder = TripRecorder()
    private val restriction = DrivingRestriction(app)

    private var source: VehicleSignalSource = demo
    private var controller = CabinController(null, null, demo)
    private val dismissed = mutableSetOf<Class<out Suggestion>>()

    private val _state = MutableStateFlow(UiState())
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
            // Sample motion score for the report once per second.
            launch {
                while (isActive) {
                    delay(1_000)
                    if (_state.value.signals.isDriving) recorder.sampleMotion(_state.value.condition.motionSickness)
                }
            }
        }
    }

    private fun recompute(s: VehicleSignals) {
        _state.update { st ->
            val c = ConditionEngine.evaluate(s, st.profile)
            st.copy(
                signals = s,
                condition = c,
                suggestion = SuggestionPolicy.decide(c, s, dismissed),
                // Demo mode: the demo panel decides driving/parked so judges can reproduce scenarios.
                restricted = if (source.isDemo) s.isDriving else restriction.restricted.value ?: s.isDriving,
            )
        }
    }

    fun setMotionSensitive(sensitive: Boolean) {
        _state.update { it.copy(profile = PassengerProfile(motionSensitive = sensitive)) }
        recompute(source.signals.value)
    }

    fun acceptSuggestion() {
        when (val s = _state.value.suggestion) {
            Suggestion.FreshAir -> {
                controller.switchToFreshAir { applied ->
                    recorder.record(CareType.FRESH_AIR, if (applied) "내기 → 외기 (차량 적용)" else "내기 → 외기 (데모)")
                }
                showApplied("바깥 공기로 바꿨어요")
            }
            is Suggestion.Rest -> {
                recorder.record(CareType.REST, "${s.restAreaName} 경유지 추가")
                dismissed += Suggestion.Rest::class.java
                showApplied("${s.restAreaName}를 경유지로 추가했어요")
            }
            Suggestion.CurveAhead -> {
                recorder.record(CareType.CURVE_GUIDE, "커브 전 부드러운 주행 안내")
                dismissed += Suggestion.CurveAhead::class.java
                showApplied("뒷자리를 위해 부드럽게 돌아가요")
            }
            null -> Unit
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
        _state.update { it.copy(appliedMessage = msg) }
        viewModelScope.launch {
            delay(3_000)
            _state.update { if (it.appliedMessage == msg) it.copy(appliedMessage = null) else it }
        }
    }

    fun endTrip() {
        _state.update { it.copy(report = recorder.buildReport()) }
    }

    fun closeReport() {
        recorder.reset()
        dismissed.clear()
        _state.update { it.copy(report = null) }
    }

    fun toggleDemoPanel(open: Boolean = !_state.value.demoPanelOpen) {
        _state.update { it.copy(demoPanelOpen = open) }
    }

    fun applyScenario(scenario: DemoVehicleSource.Scenario) {
        dismissed.clear()
        demo.apply(scenario)
    }

    override fun onCleared() {
        source.stop()
        controller.release()
        restriction.release()
    }

    private companion object {
        const val DISMISS_COOLDOWN_MS = 60_000L
    }
}
