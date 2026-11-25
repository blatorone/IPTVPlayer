package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalSearchTest {
    @Test
    fun matchesWordsRegardlessOfPunctuationAndCase() {
        val title = "Spider-Man: Brand New Day -"
        assertTrue(matchesSearch(title, "spider-man brand new"))
        assertTrue(matchesSearch(title, "spider man brand new"))
        assertTrue(matchesSearch(title, "Spiderman"))
        assertTrue(matchesSearch(title, "brand new spider"))
        assertTrue(matchesSearch("Die Simpsons", "simps"))
        assertFalse(matchesSearch(title, "spider-man homecoming"))
        assertFalse(matchesSearch(title, " - "))
    }

    @Test
    fun titlesLoseDanglingSeparatorsSoTheyMatchTraktAndTmdb() {
        assertEquals("Spider-Man: Brand New Day", displayVodTitle(PortalTitle("Spider-Man: Brand New Day - 2026")))
        assertEquals("Spider-Man: Brand New Day", displayVodTitle(PortalTitle("Spider-Man: Brand New Day -")))
        assertEquals("Frankenstein's Bride", displayVodTitle(PortalTitle("Frankenstein's Bride - (2026)")))
        assertEquals("Spider-Man", displayVodTitle(PortalTitle("Spider-Man")))
    }

    @Test
    fun sendsTheLongestWordToThePortal() {
        assertEquals("spider-man", portalSearchTerm("spider-man brand new"))
        assertEquals("Spider-Man", portalSearchTerm("  Spider-Man:  "))
        assertEquals("simpsons", portalSearchTerm("die simpsons"))
    }
}
