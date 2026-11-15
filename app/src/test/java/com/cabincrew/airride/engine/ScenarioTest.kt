package com.cabincrew.airride.engine

import com.cabincrew.airride.data.DemoVehicleSource.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each README demo scenario must produce its intended suggestion. */
class ScenarioTest {

    private val profile = PassengerProfile()

    private fun run(s: Scenario): Pair<Condition, Suggestion?> {
        val c = ConditionEngine.evaluate(s.signals, profile)
        return c to SuggestionPolicy.decide(c, s.signals)
    }

    @Test fun calm_noSuggestion() {
        val (c, sug) = run(Scenario.CALM)
        assertEquals(Level.GOOD, c.alertnessLevel)
        assertEquals(Level.GOOD, c.motionLevel)
        assertNull(sug)
    }

    @Test fun stuffy_suggestsFreshAir() {
        val (c, sug) = run(Scenario.STUFFY)
        assertEquals(Level.CAUTION, c.alertnessLevel)
        assertEquals(Suggestion.FreshAir, sug)
    }

    @Test fun drowsy_suggestsRest() {
        val (c, sug) = run(Scenario.DROWSY)
        assertEquals(Level.DANGER, c.alertnessLevel)
        assertTrue(sug is Suggestion.Rest)
    }

    @Test fun carsick_suggestsCurveGuide() {
        val (c, sug) = run(Scenario.CARSICK)
        assertEquals(Level.DANGER, c.motionLevel)
        assertEquals(Suggestion.CurveAhead, sug)
    }

    @Test fun dismissedSuggestion_isSkipped() {
        val s = Scenario.STUFFY.signals
        val c = ConditionEngine.evaluate(s, profile)
        assertNull(SuggestionPolicy.decide(c, s, setOf(Suggestion.FreshAir::class.java)))
    }

    @Test fun freshAir_lowersCo2() {
        val s = Scenario.STUFFY.signals
        assertTrue(ConditionEngine.estimateCo2(s.copy(recirculationOn = false)) < ConditionEngine.estimateCo2(s))
    }

    @Test fun motionSensitive_raisesScore() {
        val s = Scenario.CALM.signals
        assertEquals(
            ConditionEngine.motionSickness(s, profile) + 15,
            ConditionEngine.motionSickness(s, PassengerProfile(motionSensitive = true)),
        )
    }
}
