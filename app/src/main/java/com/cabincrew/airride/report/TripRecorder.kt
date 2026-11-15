package com.cabincrew.airride.report

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CareType(val label: String) {
    FRESH_AIR("외기 전환"),
    REST("휴식 경유지"),
    CURVE_GUIDE("커브 안내"),
    CABIN_ADJUST("실내 환경 조정"),
}

data class CareEvent(
    val timeMillis: Long,
    val type: CareType,
    val detail: String,
    /** True when the car applied it automatically (opt-in auto mode). */
    val automatic: Boolean,
)

data class TripReport(
    val smoothnessScore: Int,
    val freshAirSwitches: Int,
    val careCount: Int,
    val events: List<CareEvent>,
)

/** Records care events during a trip and builds the end-of-trip report (A-10). */
class TripRecorder {

    private val _events = MutableStateFlow<List<CareEvent>>(emptyList())
    val events: StateFlow<List<CareEvent>> = _events.asStateFlow()

    private var motionSum = 0L
    private var motionSamples = 0L

    fun record(type: CareType, detail: String, automatic: Boolean = false) {
        _events.update { it + CareEvent(System.currentTimeMillis(), type, detail, automatic) }
    }

    /** Feed motion-sickness samples to compute the smoothness score. */
    fun sampleMotion(motionSickness: Int) {
        motionSum += motionSickness
        motionSamples++
    }

    fun buildReport(): TripReport {
        val list = _events.value
        val avgMotion = if (motionSamples == 0L) 0 else (motionSum / motionSamples).toInt()
        return TripReport(
            smoothnessScore = (100 - avgMotion).coerceIn(0, 100),
            freshAirSwitches = list.count { it.type == CareType.FRESH_AIR },
            careCount = list.size,
            events = list,
        )
    }

    fun reset() {
        _events.value = emptyList()
        motionSum = 0
        motionSamples = 0
    }
}
