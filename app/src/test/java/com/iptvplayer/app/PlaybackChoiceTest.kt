package com.iptvplayer.app

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackChoiceTest {
    @Test
    fun chosenPortalComesFirstThenPreferredThenTheRest() {
        val portals = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "b", "a", "d"), portals.preferringSource("c", "b") { it })
        assertEquals(listOf("b", "a", "c", "d"), portals.preferringSource(null, "b") { it })
        assertEquals(portals, portals.preferringSource(null, null) { it })
        assertEquals(portals, portals.preferringSource("x", "y") { it })
    }

    @Test
    fun recognisesForcedSubtitles() {
        assertTrue(isForcedSubtitle(C.SELECTION_FLAG_FORCED, null, null))
        assertTrue(isForcedSubtitle(0, "Deutsch (Forced)", null))
        assertTrue(isForcedSubtitle(0, null, "3:Deutsch erzwungen"))
        assertFalse(isForcedSubtitle(C.SELECTION_FLAG_DEFAULT, "Deutsch", "2"))
    }
}
