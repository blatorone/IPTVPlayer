package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbMatchingTest {
    private val title = TmdbTitle(10, "Beispielfilm", "Sample Film", 2025, "Beschreibung", null, 12.0, 7.0)

    @Test
    fun usesEverySelectedCountryForCinemaAndStreamingServices() {
        val selected = linkedSetOf("DE", "AT", "unknown", "CH")
        assertEquals(listOf("DE", "AT", "CH"), tmdbShelfRegions(VodCategory.CINEMA, selected, "US"))
        assertEquals(listOf("DE", "AT", "CH"), tmdbShelfRegions(VodCategory.NETFLIX, selected, "US"))
        assertEquals(listOf("US"), tmdbShelfRegions(VodCategory.POPULAR, selected, "US"))
        assertEquals(listOf("DE"), tmdbShelfRegions(VodCategory.CINEMA, emptySet(), ""))
    }

    @Test
    fun recognizesDecoratedPortalNamesAndKeepsTheStreamCommand() {
        val playable = PortalTitle("| DE | Sample Film (2025) FHD", id = "portal-id", command = "sample-command")
        val chosen = matchTmdbTitle(title, listOf(playable))
        assertEquals(playable, chosen)
        assertEquals("sample-command", title.applyTo(requireNotNull(chosen)).command)
    }

    @Test
    fun rejectsAConflictingIdAndAnotherRelease() {
        assertNull(matchTmdbTitle(title, listOf(PortalTitle("Beispielfilm", year = 2025, tmdbId = "11"))))
        assertNull(matchTmdbTitle(title, listOf(PortalTitle("Beispielfilm", year = 1995))))
    }
}
