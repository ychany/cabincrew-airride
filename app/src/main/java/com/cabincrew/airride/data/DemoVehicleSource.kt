package com.cabincrew.airride.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Demo signal source. The hidden demo panel edits these values directly
 * so judges can reproduce each scenario on the emulator.
 */
class DemoVehicleSource(initial: VehicleSignals = Scenario.CALM.signals) : VehicleSignalSource {

    private val _signals = MutableStateFlow(initial)
    override val signals: StateFlow<VehicleSignals> = _signals.asStateFlow()
    override val isDemo: Boolean = true

    fun update(transform: (VehicleSignals) -> VehicleSignals) = _signals.update(transform)

    fun apply(scenario: Scenario) {
        _signals.value = scenario.signals
    }

    /** Preset scenarios used in the README walkthrough. */
    enum class Scenario(val label: String, val signals: VehicleSignals) {
        CALM(
            "평온한 주행",
            VehicleSignals(recircMinutes = 10f, steeringCorrectionsPerMin = 2f, continuousDriveMinutes = 30f),
        ),
        STUFFY(
            "답답한 실내 (내기순환 오래)",
            VehicleSignals(recircMinutes = 25f, occupants = 3, steeringCorrectionsPerMin = 4f),
        ),
        DROWSY(
            "졸음 위험",
            VehicleSignals(
                recircMinutes = 40f, occupants = 3, steeringCorrectionsPerMin = 7f,
                laneWarningRecent = true, continuousDriveMinutes = 140f,
            ),
        ),
        CARSICK(
            "멀미 위험 (커브 구간)",
            VehicleSignals(accelChange = 3.2f, lateralSway = 3.0f, curveAhead = true, recirculationOn = false),
        ),
        PARKED(
            "정차 (P)",
            VehicleSignals(isDriving = false, speedKmh = 0f, accelChange = 0f, lateralSway = 0f),
        ),
    }
}
