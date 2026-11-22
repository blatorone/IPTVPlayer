package com.iptvplayer.app

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmlTvGuideTest {
    @Test
    fun acceptsXmlTvOffsetsAndProviderMilliseconds() {
        val expected = Instant.parse("2026-11-21T19:30:00Z").epochSecond
        assertEquals(expected, xmlTvTime("20261121203000 +0100"))
        assertEquals(expected, xmlTvTime("202611211930"))
        assertEquals(expected, portalProgrammeTime((expected * 1_000).toString()))
        assertNull(xmlTvTime("21.11.2026"))
    }

    @Test
    fun matchesEpgIdBeforeNamesAndRejectsAmbiguousNames() {
        val expected = listOf(PortalProgramme("Aktuell", 10))
        val guide = XmlTvGuide(mapOf("one" to expected), mapOf("news" to setOf("one", "two")))
        val channel = StalkerChannel("News", "test", null, null, epgIdentity = "one")
        assertEquals(expected, guide.programmes(channel, "News"))
        assertEquals(emptyList<PortalProgramme>(), guide.programmes(StalkerChannel("News", "test", null, null), "News"))
    }
}
