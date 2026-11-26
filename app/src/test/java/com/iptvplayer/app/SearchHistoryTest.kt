package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryTest {
    @Test
    fun newestTermComesFirstWithoutDuplicates() {
        val history = updatedSearchHistory(listOf("Tatort", "Dune", "Arte"), "  dune ")
        assertEquals(listOf("dune", "Tatort", "Arte"), history)
    }

    @Test
    fun veryShortTermsAreNotKept() {
        assertEquals(listOf("Dune"), updatedSearchHistory(listOf("Dune"), " a "))
    }

    @Test
    fun keepsOnlyTheNewestEntries() {
        val full = List(10) { "Titel $it" }
        val history = updatedSearchHistory(full, "Neu")
        assertEquals(10, history.size)
        assertEquals("Neu", history.first())
        assertEquals("Titel 8", history.last())
        assertEquals(5, updatedSearchHistory(full, "Neu", max = 5).size)
    }

    @Test
    fun collapsesInnerSpaces() {
        assertEquals(listOf("Star Wars"), updatedSearchHistory(emptyList(), "Star    Wars"))
    }
}
