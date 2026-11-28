package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchedPercentTest {
    @Test
    fun countsFinishedFullyAndStartedByPosition() {
        val progress = listOf(
            EpisodeCardProgress(completed = true, position = 0, duration = 0),
            EpisodeCardProgress(completed = false, position = 30, duration = 60),
            EpisodeCardProgress(completed = false, position = 0, duration = 60),
            EpisodeCardProgress(completed = false, position = 0, duration = 0)
        )
        assertEquals(37, watchedPercent(progress))
        assertEquals(0, watchedPercent(emptyList()))
    }
}
