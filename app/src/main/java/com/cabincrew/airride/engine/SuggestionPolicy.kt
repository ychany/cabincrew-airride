package com.cabincrew.airride.engine

import com.cabincrew.airride.data.VehicleSignals

sealed interface Suggestion {
    /** "바깥 공기로 바꿀까요?" */
    data object FreshAir : Suggestion

    /** "○○휴게소에서 쉬어 갈까요?" — rest area name is fixed sample data for now. */
    data class Rest(val restAreaName: String, val distanceKm: Int) : Suggestion

    /** "곧 커브예요. 뒷자리를 위해 부드럽게 돌아가요" */
    data object CurveAhead : Suggestion
}

/** Maps condition → at most one suggestion at a time (driving-safe). Thresholds 튜닝 예정. */
object SuggestionPolicy {

    private val SAMPLE_REST_AREA = Suggestion.Rest(restAreaName = "망향휴게소", distanceKm = 12)

    fun decide(c: Condition, s: VehicleSignals, dismissed: Set<Class<out Suggestion>> = emptySet()): Suggestion? {
        val candidates = buildList {
            if (c.alertness < 40 || (c.alertness < 70 && c.motionSickness > 60)) add(SAMPLE_REST_AREA)
            if (c.alertness < 70 && s.recirculationOn) add(Suggestion.FreshAir)
            if (c.motionSickness > 40 && s.curveAhead) add(Suggestion.CurveAhead)
        }
        return candidates.firstOrNull { it::class.java !in dismissed }
    }
}
