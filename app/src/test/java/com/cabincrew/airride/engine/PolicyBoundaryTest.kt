package com.cabincrew.airride.engine

import com.cabincrew.airride.data.VehicleSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Threshold edges of the suggestion rules (README "제안 조건"). */
class PolicyBoundaryTest {

    private fun cond(alert: Int, motion: Int) = Condition(alert, Level.GOOD, motion, Level.GOOD, 600)

    @Test fun freshAir_onlyBelow70_andOnlyWhileRecirculating() {
        val recirc = VehicleSignals(recirculationOn = true)
        assertNull(SuggestionPolicy.decide(cond(70, 0), recirc))
        assertEquals(Suggestion.FreshAir, SuggestionPolicy.decide(cond(69, 0), recirc))
        assertNull(SuggestionPolicy.decide(cond(69, 0), recirc.copy(recirculationOn = false)))
    }

    @Test fun rest_below40_orLowAlertnessWithHighMotion() {
        val s = VehicleSignals(recirculationOn = false)
        assertNull(SuggestionPolicy.decide(cond(40, 0), s))
        assertTrue(SuggestionPolicy.decide(cond(39, 0), s) is Suggestion.Rest)
        assertNull(SuggestionPolicy.decide(cond(69, 60), s))
        assertTrue(SuggestionPolicy.decide(cond(69, 61), s) is Suggestion.Rest)
    }

    @Test fun rest_takesPriorityOverFreshAirAndCurve() {
        val s = VehicleSignals(recirculationOn = true, curveAhead = true)
        assertTrue(SuggestionPolicy.decide(cond(30, 80), s) is Suggestion.Rest)
    }

    @Test fun curve_needsMotionAbove40_andCurveAhead() {
        val s = VehicleSignals(recirculationOn = false, curveAhead = true)
        assertNull(SuggestionPolicy.decide(cond(90, 40), s))
        assertEquals(Suggestion.CurveAhead, SuggestionPolicy.decide(cond(90, 41), s))
        assertNull(SuggestionPolicy.decide(cond(90, 80), s.copy(curveAhead = false)))
    }

    @Test fun scores_areClampedTo0_100() {
        val worst = VehicleSignals(
            recirculationOn = true, recircMinutes = 999f, occupants = 5, steeringCorrectionsPerMin = 99f,
            laneWarningRecent = true, continuousDriveMinutes = 999f, accelChange = 99f, lateralSway = 99f, curveAhead = true,
        )
        val c = ConditionEngine.evaluate(worst, PassengerProfile(motionSensitive = true))
        assertEquals(0, c.alertness)
        assertEquals(100, c.motionSickness)
        assertEquals(Level.DANGER, c.alertnessLevel)
        assertEquals(Level.DANGER, c.motionLevel)
    }

    @Test fun co2_isBaselineOnFreshAir() {
        assertEquals(600f, ConditionEngine.estimateCo2(VehicleSignals(recirculationOn = false, recircMinutes = 60f)), 0.01f)
        assertEquals(600f + 10 * 2 * 35, ConditionEngine.estimateCo2(VehicleSignals(recirculationOn = true, recircMinutes = 10f, occupants = 2)), 0.01f)
    }
}
