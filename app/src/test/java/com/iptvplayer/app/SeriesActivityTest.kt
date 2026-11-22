package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeriesActivityTest {
    @Test
    fun selectsSeasonOneEpisodeOneEvenWhenThePortalReturnsAnotherOrder() {
        val firstEpisode = PortalTitle("Erste Folge", episode = "01")
        val first = PortalSeason(1, PortalTitle("Staffel 1"), listOf(
            PortalTitle("Zweite Folge", episode = "2"), firstEpisode
        ))
        val second = PortalSeason(2, PortalTitle("Staffel 2"), listOf(PortalTitle("Folge", episode = "1")))
        val special = PortalSeason(0, PortalTitle("Extras"), listOf(PortalTitle("Extra", episode = "1")))

        assertEquals(first to firstEpisode, firstPlayableEpisode(listOf(special, second, first)))
        assertEquals(second to second.episodes.first(), firstPlayableEpisode(listOf(special, second)))
        assertNull(firstPlayableEpisode(listOf(first.copy(episodes = emptyList()))))
    }

    @Test
    fun recordsFirstCheckAsBaselineAndKeepsLaterEpisodesUnread() {
        val series = TrackedSeries("series", "Beispiel", emptyList(), saved = true)
        val first = series.observe(setOf("1:1", "1:2"), now = 1)
        val second = first.observe(setOf("1:1", "1:3"), now = 2)
        assertEquals(emptySet<String>(), first.unseen)
        assertEquals(setOf("1:3"), second.unseen)
        assertEquals(setOf("1:1", "1:2", "1:3"), second.known)
    }

    @Test
    fun restoresTheExactSeasonEvenWhenEpisodeNumbersRepeat() {
        val first = PortalSeason(1, PortalTitle("Staffel 1"), listOf(PortalTitle("Folge eins", episode = "1")))
        val second = PortalSeason(2, PortalTitle("Staffel 2"), listOf(PortalTitle("Folge eins", episode = "01")))
        val resume = EpisodeResume("2:1", 2, "1", null, "position")
        assertEquals(second to second.episodes.first(), resumeEpisode(listOf(first, second), resume))
    }

    @Test
    fun migratesAnUnambiguousPositionAndDoesNotGuessBetweenOldEpisodes() {
        val series = TrackedSeries("tmdb:88", "Beispiel", emptyList())
        val key = "SERIES:catalogue:tmdb:88:3:2"
        assertEquals("3:2", recoverEpisodeResume(series, mapOf(key to 25_000L))?.episodeKey)
        assertNull(recoverEpisodeResume(series, mapOf(key to 25_000L, "SERIES:catalogue:tmdb:88:1:1" to 1L)))
    }
}
