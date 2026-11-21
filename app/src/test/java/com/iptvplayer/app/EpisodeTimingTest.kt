package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeTimingTest {
    @Test
    fun prefersCreditsAndFallsBackToRealEndForInvalidMarkers() {
        assertEquals(90_000L, EpisodeTiming(creditsStartMs = 90_000).nextAt(120_000))
        assertEquals(120_000L, EpisodeTiming(creditsStartMs = 140_000).nextAt(120_000))
        assertNull(EpisodeTiming(introStartMs = 20_000, introEndMs = 10_000).intro(120_000))
        assertEquals(19_999L, EpisodeTiming(10_000, 20_000).intro(120_000)?.last)
    }

    @Test
    fun selectsNextNumberAndContinuesIntoTheNextSeason() {
        val first = PortalTitle("Eins", episode = "1")
        val second = PortalTitle("Zwei", episode = "2")
        val current = PortalSeason(1, PortalTitle("Staffel 1"), listOf(second, first))
        val upcoming = PortalSeason(2, PortalTitle("Staffel 2"), listOf(first))
        assertEquals(current to second, followingEpisode(listOf(current, upcoming), current, first))
        assertEquals(upcoming to first, followingEpisode(listOf(current, upcoming), current, second))
        assertNull(followingEpisode(listOf(current, upcoming), upcoming, first))
    }
}
