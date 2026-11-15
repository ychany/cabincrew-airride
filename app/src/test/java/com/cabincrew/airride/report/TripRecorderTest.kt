package com.cabincrew.airride.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A-10 report numbers. */
class TripRecorderTest {

    @Test fun counts_excludeSessionStart_andSeparateRests() {
        val r = TripRecorder()
        r.record(CareType.SESSION, "민준 동승 모드 시작")
        r.record(CareType.FRESH_AIR, "외기로 전환")
        r.record(CareType.CURVE_GUIDE, "커브 구간 · 뒷좌석 바람 조절", automatic = true)
        r.record(CareType.REST, "망향휴게소 경유지 추가")
        r.record(CareType.FRESH_AIR, "망향휴게소 · 실내 환기")
        val rep = r.buildReport()
        assertEquals(3, rep.careCount)
        assertEquals(1, rep.restCount)
        assertEquals(2, rep.freshAirSwitches)
        assertEquals(5, rep.events.size)
    }

    @Test fun smoothness_is100MinusAverageMotion() {
        val r = TripRecorder()
        listOf(10, 30, 50).forEach { r.sample(alertness = 80, motionSickness = it) }
        val rep = r.buildReport()
        assertEquals(70, rep.smoothnessScore)
        assertEquals(3, rep.samples.size)
    }

    @Test fun emptyTrip_isPerfectlySmooth() {
        assertEquals(100, TripRecorder().buildReport().smoothnessScore)
    }

    @Test fun reset_clearsEverything() {
        val r = TripRecorder()
        r.record(CareType.FRESH_AIR, "x")
        r.sample(50, 50)
        r.reset()
        val rep = r.buildReport()
        assertTrue(rep.events.isEmpty())
        assertTrue(rep.samples.isEmpty())
        assertEquals(100, rep.smoothnessScore)
    }
}
