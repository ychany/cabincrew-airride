package com.cabincrew.airride.engine

import com.cabincrew.airride.data.VehicleSignals

enum class Level { GOOD, CAUTION, DANGER }

data class Condition(
    /** Driver alertness 0..100 (higher is better). */
    val alertness: Int,
    val alertnessLevel: Level,
    /** Passenger motion-sickness risk 0..100 (higher is worse). */
    val motionSickness: Int,
    val motionLevel: Level,
    val estimatedCo2Ppm: Int,
)

data class PassengerProfile(val motionSensitive: Boolean = false)

/**
 * Rule-based scoring. All constants are demo assumptions — 튜닝 예정.
 */
object ConditionEngine {

    // 실내 체적 상수를 반영한 단순 근사 (튜닝 예정)
    private const val CO2_BASE_PPM = 600f
    private const val CO2_PER_PERSON_MINUTE = 35f

    // 멀미 가중치 (튜닝 예정)
    private const val K_ACCEL = 12f
    private const val K_SWAY = 10f

    fun estimateCo2(s: VehicleSignals): Float {
        val minutes = if (s.recirculationOn) s.recircMinutes else 0f
        return CO2_BASE_PPM + minutes * s.occupants * CO2_PER_PERSON_MINUTE
    }

    fun alertness(s: VehicleSignals): Int {
        val co2 = estimateCo2(s)
        var score = 100f
        score -= ((co2 - 1000f) / 40f).coerceIn(0f, 40f)
        score -= (s.steeringCorrectionsPerMin * 3f).coerceAtMost(25f)
        if (s.laneWarningRecent) score -= 15f
        score -= ((s.continuousDriveMinutes - 90f).coerceAtLeast(0f) * 0.2f).coerceAtMost(20f)
        return score.toInt().coerceIn(0, 100)
    }

    fun motionSickness(s: VehicleSignals, profile: PassengerProfile): Int {
        var score = s.accelChange * K_ACCEL + s.lateralSway * K_SWAY
        if (s.curveAhead) score += 10f
        if (profile.motionSensitive) score += 15f
        return score.toInt().coerceIn(0, 100)
    }

    fun evaluate(s: VehicleSignals, profile: PassengerProfile): Condition {
        val a = alertness(s)
        val m = motionSickness(s, profile)
        return Condition(
            alertness = a,
            alertnessLevel = when {
                a >= 70 -> Level.GOOD
                a >= 40 -> Level.CAUTION
                else -> Level.DANGER
            },
            motionSickness = m,
            motionLevel = when {
                m < 40 -> Level.GOOD
                m < 70 -> Level.CAUTION
                else -> Level.DANGER
            },
            estimatedCo2Ppm = estimateCo2(s).toInt(),
        )
    }
}
