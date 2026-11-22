package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeTimingTest {
    @Test
    fun confirmsLearnedIntroBeforeItsEndAndAdjustsForLeadIn() {
        val reference = List(7) { index ->
            EpisodeFrame(15_000L + index * 3_000, 255L shl (index * 8), false)
        }
        val current = reference.take(6).map { it.copy(timeMs = it.timeMs + 6_000) }
        assertEquals(21_000L until 39_000L, confirmKnownOpening(current, reference, 15_000L until 33_000L))
        assertNull(confirmKnownOpening(current.dropLast(1), reference, 15_000L until 33_000L))
        assertNull(confirmKnownOpening(current.map { it.copy(fingerprint = 0) }, reference, 15_000L until 33_000L))
    }

    @Test
    fun findsAnOpeningSharedByTwoEpisodesWithDifferentLeadIn() {
        val shared = List(7) { index -> 255L shl (index * 8) }
        fun frames(values: List<Long>) = values.mapIndexed { index, hash ->
            EpisodeFrame(index * 3_000L, hash, false)
        }
        val current = frames(listOf(-1L) + shared + listOf(-1L))
        val previous = frames(listOf(0L, 0L) + shared + listOf(0L))
        assertEquals(3_000L until 21_000L, findRepeatedOpening(current, previous))
        assertNull(findRepeatedOpening(frames(List(12) { 0L }), frames(List(12) { 0L })))
    }

    @Test
    fun rejectsAnEarlyTextSequenceFollowedByPicture() {
        val frames = listOf(true, true, true, false, false, true, true, true).mapIndexed { index, text ->
            EpisodeFrame(index * 5_000L, 0, text)
        }
        assertEquals(25_000L, findCreditsStart(frames))
        assertNull(findCreditsStart(frames.take(5)))
    }

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

    @Test
    fun selectsPreviousNumberAndContinuesIntoThePreviousSeason() {
        val first = PortalTitle("Eins", episode = "1")
        val second = PortalTitle("Zwei", episode = "2")
        val current = PortalSeason(1, PortalTitle("Staffel 1"), listOf(second, first))
        val previousSeason = PortalSeason(0, PortalTitle("Staffel 0"), listOf(second))
        assertEquals(current to first, precedingEpisode(listOf(previousSeason, current), current, second))
        assertEquals(previousSeason to second, precedingEpisode(listOf(previousSeason, current), current, first))
        assertNull(precedingEpisode(listOf(previousSeason, current), previousSeason, second))
    }
}
