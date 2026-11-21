package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StalkerMacTest {
    @Test
    fun acceptsDifferentPrefixesAndInputFormats() {
        assertEquals("00:2A:12:34:56:78", normalizePortalMac("00:2a:12:34:56:78"))
        assertEquals("00:1A:12:34:56:78", normalizePortalMac("00-1a-12-34-56-78"))
        assertEquals("00:1B:12:34:56:78", normalizePortalMac("001b12345678"))
        assertEquals("FE:00:12:34:56:78", normalizePortalMac("fe0012345678"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidCharacters() {
        normalizePortalMac("00:1G:12:34:56:78")
    }
}
