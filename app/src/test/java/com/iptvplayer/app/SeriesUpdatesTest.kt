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
    fun upcomingListsSeasonStartsOfWatchedSeriesSoonestFirst() {
        val today = LocalDate.of(2026, 11, 25)
        val entries = listOf(
            finished.copy(key = "a", name = "B", nextSeasonDate = "2026-12-10"),
            finished.copy(key = "b", name = "A", nextSeasonDate = "2026-11-28"),
            finished.copy(key = "c", name = "Old", nextSeasonDate = "2026-11-01"),
            finished.copy(key = "d", name = "Unknown", nextSeasonDate = null, nextAirDate = "2026-11-29"),
            finished.copy(key = "e", name = "Hidden", nextSeasonDate = "2026-11-30", hiddenFromHistory = true),
            finished.copy(key = "f", name = "Not watched", viewed = false, saved = true, nextSeasonDate = "2026-11-30")
        )
        assertEquals(listOf("A", "B"), upcomingSeries(entries, today).map { it.first.name })
    }

    @Test
    fun newEpisodesInThePortalComeFirstForAWeek() {
        val day = 24 * 60 * 60 * 1000L
        val reported = finished.observe(setOf("7:1", "7:2", "8:1"), 10 * day)
        assertTrue(reported.portalCheckDue(10 * day))
        // Not in the portal yet: no mark, ordered by when it was watched.
        val missing = reported.withPortalEpisodes(setOf("7:1", "7:2"), 10 * day).copy(key = "m", name = "Missing", watchedAt = 3)
        assertTrue(missing.newInPortal(10 * day).isEmpty())
        assertFalse(missing.portalCheckDue(10 * day + 60_000))
        assertTrue(missing.portalCheckDue(10 * day + PORTAL_CHECK_MS))
        val older = reported.withPortalEpisodes(setOf("8:1"), 11 * day).copy(key = "o", name = "Older", watchedAt = 1)
        val newest = reported.withPortalEpisodes(setOf("8:1"), 12 * day).copy(key = "n", name = "Newest", watchedAt = 2)
        assertEquals(setOf("8:1"), newest.newInPortal(12 * day))
        val lastWatched = finished.copy(key = "w", name = "Watched last", isComplete = false, watchedAt = 900)
        assertEquals(
            listOf("Newest", "Older", "Watched last", "Missing"),
            listOf(missing, lastWatched, older, newest).continueOrder(12 * day).map { it.name }
        )
        // After a week the mark is gone and the order is the usual one again.
        assertTrue(newest.newInPortal(20 * day).isEmpty())
    }
}
