package com.cabincrew.airride.engine

import com.cabincrew.airride.data.VehicleSignals

sealed interface Suggestion {
    /** "바깥 공기로 바꿀까요?" */
    data object FreshAir : Suggestion

    /** "○○휴게소에서 쉬어 갈까요?" — rest area name is fixed sample data for now. */
    data class Rest(
        val restAreaName: String,
        val distanceKm: Int,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val address: String = "",
    ) : Suggestion

    /** "곧 커브예요. 뒷자리를 위해 부드럽게 돌아가요" */
    data object CurveAhead : Suggestion
}

/** Maps condition → at most one suggestion at a time (driving-safe). Thresholds 튜닝 예정. */
object SuggestionPolicy {

    // Demo data (real place) until rest-area search is wired — 경부고속도로 망향휴게소(부산방향).
    private val SAMPLE_REST_AREA = Suggestion.Rest(
        restAreaName = "망향휴게소", distanceKm = 12,
        latitude = 36.8945, longitude = 127.1878,
        address = "충청남도 천안시 서북구 성거읍",
    )

    fun decide(c: Condition, s: VehicleSignals, dismissed: Set<Class<out Suggestion>> = emptySet()): Suggestion? {
        val candidates = buildList {
            if (c.alertness < 40 || (c.alertness < 70 && c.motionSickness > 60)) add(SAMPLE_REST_AREA)
            if (c.alertness < 70 && s.recirculationOn) add(Suggestion.FreshAir)
            if (c.motionSickness > 40 && s.curveAhead) add(Suggestion.CurveAhead)
        }
        return candidates.firstOrNull { it::class.java !in dismissed }
    }
}
