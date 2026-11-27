package com.iptvplayer.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesUpdatesTest {
    private val finished = TrackedSeries(
        "key", "Rick and Morty", emptyList(), viewed = true,
        known = setOf("7:1", "7:2"), watchedEpisodes = setOf("7:1", "7:2"), checkedAt = 1, isComplete = true
    )

    @Test
    fun newEpisodesReopenAFinishedSeries() {
        assertFalse(finished.continuable())
        val updated = finished.observe(setOf("7:1", "7:2", "8:1"), 2)
        assertEquals(setOf("8:1"), updated.unseen)
        assertEquals(false, updated.isComplete)
        assertTrue(updated.continuable())
        // Nothing new: the series stays finished.
        assertEquals(true, finished.observe(setOf("7:1", "7:2"), 2).isComplete)
    }

    @Test
    fun episodesAlreadyWatchedAreNotReportedAsNew() {
        val updated = finished.copy(watchedEpisodes = finished.watchedEpisodes + "8:1").observe(setOf("8:1"), 2)
        assertTrue(updated.unseen.isEmpty())
    }

    @Test
    fun upcomingListsKnownFutureAirDatesSoonestFirst() {
        val today = LocalDate.of(2026, 11, 25)
        val entries = listOf(
            finished.copy(key = "a", name = "B", nextAirDate = "2026-12-10"),
            finished.copy(key = "b", name = "A", nextAirDate = "2026-11-28"),
            finished.copy(key = "c", name = "Old", nextAirDate = "2026-11-01"),
            finished.copy(key = "d", name = "Unknown", nextAirDate = null),
            finished.copy(key = "e", name = "Hidden", nextAirDate = "2026-11-30", hiddenFromHistory = true),
            finished.copy(key = "f", name = "Not watched", viewed = false, nextAirDate = "2026-11-30")
        )
        assertEquals(listOf("A", "B"), upcomingSeries(entries, today).map { it.first.name })
    }

    @Test
    fun seriesWithNewEpisodesComeFirstNewestFirst() {
        val lastWatched = finished.copy(key = "a", name = "Watched last", isComplete = false, watchedAt = 900)
        val olderNews = finished.observe(setOf("7:1", "7:2", "8:1"), 100).copy(key = "b", name = "Older news", watchedAt = 10)
        val newestNews = finished.observe(setOf("7:1", "7:2", "8:1"), 500).copy(key = "c", name = "Newest news", watchedAt = 5)
        assertEquals(500L, newestNews.newAt)
        assertEquals(
            listOf("Newest news", "Older news", "Watched last"),
            listOf(lastWatched, olderNews, newestNews).continueOrder().map { it.name }
        )
    }
}
