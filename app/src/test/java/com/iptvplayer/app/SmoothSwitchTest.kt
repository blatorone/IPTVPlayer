package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SmoothSwitchTest {
    @Test
    fun findsHowFarTheNewPortalIsBehind() {
        val random = Random(7)
        val signal = List(450) { random.nextDouble() }      // 45 s of loudness, 10 values per second
        val history = signal.subList(0, 400)                 // played before the switch
        val probe = signal.subList(370, 490.coerceAtMost(signal.size)) // new portal starts 3 s back
        val (steps, score) = bestLag(history, probe)!!
        assertEquals(30, steps)
        assertTrue(score > 0.99)
    }

    @Test
    fun holdsOnlyWhenTheNewPortalIsKnownToBeBehind() {
        assertEquals(2_500L, smoothHoldMs(2_500L, 0L))
        // The picture already stood still for a while: that much less to wait.
        assertEquals(1_200L, smoothHoldMs(3_200L, 2_000L))
        assertNull(smoothHoldMs(null, 0L))          // not known yet: switch at once
        assertNull(smoothHoldMs(-2_000L, 0L))       // ahead: nothing to wait for
        assertNull(smoothHoldMs(3_200L, 8_000L))    // stood still longer than the distance
        assertEquals(MAX_HOLD_MS, smoothHoldMs(9_000L, 0L)) // never longer than 3 s
    }

    @Test
    fun theStandInStartsWhereTheOldStreamWasLastSeen() {
        // New portal 4 s ahead: what was seen at the switch was recorded 4 s earlier.
        assertEquals(96_000L, standbyStart(100_000L, 0L, -4_000L))
        // 3 s behind and the picture stood 1 s: it is sent 2 s from now (held until then).
        assertEquals(102_000L, standbyStart(100_000L, 1_000L, 3_000L))
        // Distance unknown: taken as none.
        assertEquals(99_000L, standbyStart(100_000L, 1_000L, null))
    }

    @Test
    fun aDistanceCountsOnlyOnceTwoMeasurementsAgree() {
        val first = nextLagRecord(null, 5_000L)
        assertEquals(LagRecord(5_000L, false), first)
        val second = nextLagRecord(first, 6_000L)          // within 2 s: confirmed, averaged
        assertEquals(LagRecord(5_500L, true), second)
        val outlier = nextLagRecord(second, 17_000L)       // far off: starts over, not used
        assertEquals(LagRecord(17_000L, false), outlier)
        assertEquals(second, LagRecord.parse(second.format()))
    }

    @Test
    fun tooLittleSoundGivesNoResult() {
        assertNull(bestLag(List(100) { it.toDouble() }, List(10) { it.toDouble() }))
    }
}
