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
        assertEquals(3_200L, smoothHoldMs(3_200L, 0L))
        // The picture already stood still for a while: that much less to wait.
        assertEquals(1_200L, smoothHoldMs(3_200L, 2_000L))
        assertNull(smoothHoldMs(null, 0L))          // not known yet: switch at once
        assertNull(smoothHoldMs(-2_000L, 0L))       // ahead: nothing to wait for
        assertNull(smoothHoldMs(3_200L, 8_000L))    // stood still longer than the distance
        assertNull(smoothHoldMs(60_000L, 0L))       // implausibly far: no long frozen picture
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
    fun tooLittleSoundGivesNoResult() {
        assertNull(bestLag(List(100) { it.toDouble() }, List(10) { it.toDouble() }))
    }
}
