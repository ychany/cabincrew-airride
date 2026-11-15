package com.cabincrew.airride.report

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CareType(val label: String) {
    SESSION("동승 모드"),
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

data class ConditionSample(val timeMillis: Long, val alertness: Int, val motionSickness: Int)

data class TripReport(
    val smoothnessScore: Int,
    val freshAirSwitches: Int,
    val careCount: Int,
    val restCount: Int,
    val events: List<CareEvent>,
    val samples: List<ConditionSample>,
    val startMillis: Long,
    val endMillis: Long,
)

/** Records care events during a trip and builds the end-of-trip report (A-10). */
class TripRecorder {

    private val _events = MutableStateFlow<List<CareEvent>>(emptyList())
    val events: StateFlow<List<CareEvent>> = _events.asStateFlow()

    private var motionSum = 0L
    private var motionSamples = 0L
    private var startMillis = System.currentTimeMillis()
    private val samples = ArrayList<ConditionSample>()

    fun record(type: CareType, detail: String, automatic: Boolean = false) {
        _events.update { it + CareEvent(System.currentTimeMillis(), type, detail, automatic) }
    }

    /** Feed condition samples (≈1/s) for the smoothness score and the report chart. */
    fun sample(alertness: Int, motionSickness: Int) {
        motionSum += motionSickness
        motionSamples++
        samples += ConditionSample(System.currentTimeMillis(), alertness, motionSickness)
        if (samples.size > MAX_SAMPLES) samples.removeAt(0)
    }

    fun buildReport(): TripReport {
        val list = _events.value
        val avgMotion = if (motionSamples == 0L) 0 else (motionSum / motionSamples).toInt()
        return TripReport(
            smoothnessScore = (100 - avgMotion).coerceIn(0, 100),
            freshAirSwitches = list.count { it.type == CareType.FRESH_AIR },
            careCount = list.count { it.type != CareType.SESSION && it.type != CareType.REST },
            restCount = list.count { it.type == CareType.REST },
            events = list,
            samples = samples.toList(),
            startMillis = startMillis,
            endMillis = System.currentTimeMillis(),
        )
    }

    fun reset() {
        _events.value = emptyList()
        motionSum = 0
        motionSamples = 0
        samples.clear()
        startMillis = System.currentTimeMillis()
    }

    private companion object {
        const val MAX_SAMPLES = 3_600
    }
}
