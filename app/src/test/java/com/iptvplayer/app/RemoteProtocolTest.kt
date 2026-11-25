package com.iptvplayer.app

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProtocolTest {
    private val epoch = "e1"

    private fun decode(json: String) = RemoteProtocol.decodeCommand(json, epoch)

    @Test
    fun rejectsCommandsOfAnotherEpoch() {
        assertNull(RemoteProtocol.decodeCommand("""{"t":"hello","epoch":"old"}""", epoch))
        assertNull(decode("""{"t":"hello"}"""))
        assertEquals(RemoteCommand.Hello, decode("""{"t":"hello","epoch":"e1"}"""))
    }

    @Test
    fun parsesCategoryAndCountryCommands() {
        val categories = decode("""{"t":"categories","epoch":"e1","section":"MOVIES","ids":["a","b"],"hidden":true}""")
        assertEquals(RemoteCommand.Categories(PortalSection.MOVIES, setOf("a", "b"), true), categories)
        assertNull(decode("""{"t":"categories","epoch":"e1","section":"RADIO","ids":["a"],"hidden":true}"""))
        val countries = decode("""{"t":"countries","epoch":"e1","codes":["DE","AT","unknown"]}""")
        assertEquals(RemoteCommand.Countries(listOf("DE", "AT", "unknown")), countries)
        assertNull(decode("""{"t":"countries","epoch":"e1","codes":["Deutschland"]}"""))
    }

    @Test
    fun validatesPortalsAndPins() {
        assertEquals(
            RemoteCommand.PortalAdd("http://portal.example/c/", "00:1A:79:00:00:01"),
            decode("""{"t":"portalAdd","epoch":"e1","address":" http://portal.example/c/ ","mac":"00:1A:79:00:00:01"}""")
        )
        assertNull(decode("""{"t":"portalAdd","epoch":"e1","address":"javascript:alert(1)","mac":"x"}"""))
        assertEquals(RemoteCommand.Pin("1234", null), decode("""{"t":"pin","epoch":"e1","pin":"1234"}"""))
        assertNull(decode("""{"t":"pin","epoch":"e1","pin":"12a4"}"""))
        assertNull(decode("""{"t":"pin","epoch":"e1","pin":"123"}"""))
        assertEquals(RemoteCommand.KidsPin(null, "1234"), decode("""{"t":"kidsPin","epoch":"e1","pin":null,"current":"1234"}"""))
        assertNull(decode("""{"t":"parental","epoch":"e1","field":"somethingElse","value":true}"""))
    }

    @Test
    fun parsesCountryConfirmationAndPortalEdit() {
        assertEquals(
            RemoteCommand.Countries(listOf("DE"), confirm = true),
            decode("""{"t":"countries","epoch":"e1","codes":["DE"],"confirm":true}""")
        )
        assertEquals(RemoteCommand.CountriesLater, decode("""{"t":"countriesLater","epoch":"e1"}"""))
        assertEquals(RemoteCommand.Finish, decode("""{"t":"finish","epoch":"e1"}"""))
        assertEquals(
            RemoteCommand.PortalEdit("id1", "https://new.example/c/", "00:1A:79:00:00:02"),
            decode("""{"t":"portalEdit","epoch":"e1","id":"id1","address":"https://new.example/c/","mac":"00:1A:79:00:00:02"}""")
        )
        assertNull(decode("""{"t":"portalEdit","epoch":"e1","id":"id1","address":"ftp://x","mac":"00:1A:79:00:00:02"}"""))
    }

    @Test
    fun ignoresGarbage() {
        assertNull(decode("not json"))
        assertNull(decode("""{"t":"unknown","epoch":"e1"}"""))
        assertNull(decode("""["t"]"""))
    }

    @Test
    fun appliesOnlyValidSettingValues() {
        var flag = false
        var choice = "A"
        var number = 10
        val entries = listOf(
            RemoteEntry.Toggle("flag", "S", "Flag", { flag }) { flag = it },
            RemoteEntry.Choice("choice", "S", "Choice", listOf(RemoteOption("A", "a"), RemoteOption("B", "b")), { choice }) { choice = it },
            RemoteEntry.Number("number", "S", "Number", 5, 60, 5, "s", { number }) { number = it }
        )
        assertTrue(entries.applyRemoteValue("flag", JsonPrimitive(true)))
        assertTrue(flag)
        assertFalse(entries.applyRemoteValue("flag", JsonPrimitive("yes")))
        assertTrue(entries.applyRemoteValue("choice", JsonPrimitive("B")))
        assertEquals("B", choice)
        assertFalse(entries.applyRemoteValue("choice", JsonPrimitive("C")))
        assertTrue(entries.applyRemoteValue("number", JsonPrimitive(25)))
        assertEquals(25, number)
        assertFalse(entries.applyRemoteValue("number", JsonPrimitive(27)))
        assertFalse(entries.applyRemoteValue("number", JsonPrimitive(65)))
        assertFalse(entries.applyRemoteValue("number", JsonPrimitive(2.5)))
        assertFalse(entries.applyRemoteValue("missing", JsonPrimitive(true)))
    }

    @Test
    fun encodesStateForThePage() {
        var flag = true
        val state = RemoteState(
            device = "StreamWatch (TV)",
            settings = listOf(RemoteEntry.Toggle("flag", "Allgemein", "Flag", { flag }) { flag = it }),
            portals = listOf(RemotePortal("id1", "Portal 1 · host", "http://host/c/", "00:1A:79:00:00:01", true, null, null, true)),
            categories = mapOf("LIVE" to listOf(RemoteCategory("News", "News", hidden = false, locked = true))),
            countries = listOf(RemoteCountry("DE", "Deutschland", true)),
            parental = RemoteParental(hasPin = true, separateKidsPin = false, kidsMode = false),
            busy = null, error = null
        )
        val json = JsonParser.parseString(RemoteProtocol.encodeState(state, epoch)).asJsonObject
        assertEquals("state", json["t"].asString)
        assertEquals(epoch, json["epoch"].asString)
        assertTrue(json["settings"].asJsonArray[0].asJsonObject["value"].asBoolean)
        assertTrue(json["categories"].asJsonObject["LIVE"].asJsonArray[0].asJsonObject["locked"].asBoolean)
        assertFalse(json["portals"].asJsonArray[0].asJsonObject.has("reachable"))
    }
}
