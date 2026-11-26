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
    fun tooLittleSoundGivesNoResult() {
        assertNull(bestLag(List(100) { it.toDouble() }, List(10) { it.toDouble() }))
    }
}
