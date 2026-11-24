package com.iptvplayer.app

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PortalLibraryStorageTest {
    private val key: SecretKey = newEnvelopeDataKey()

    // Tests use the raw key as "wrapped" key; on devices it is wrapped by the Android Keystore.
    private fun seal(payload: ByteArray): ByteArray {
        val bytes = ByteArrayOutputStream()
        EnvelopeOutputStream(bytes, key, key.encoded).use { it.write(payload) }
        return bytes.toByteArray()
    }

    private fun open(sealed: ByteArray): ByteArray =
        EnvelopeInputStream(ByteArrayInputStream(sealed)) { SecretKeySpec(it, "AES") }.use { it.readBytes() }

    @Test
    fun envelopeRoundTripsAcrossManyChunks() {
        val payload = ByteArray(1_300_000) { (it * 31 % 251).toByte() }
        assertArrayEquals(payload, open(seal(payload)))
        assertArrayEquals(ByteArray(0), open(seal(ByteArray(0))))
    }

    @Test
    fun envelopeRejectsTamperedAndTruncatedFiles() {
        val sealed = seal(ByteArray(600_000) { it.toByte() })
        val tampered = sealed.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 1).toByte() }
        try {
            open(tampered)
            fail("tampered snapshot accepted")
        } catch (_: IOException) {
        }
        // Cutting the file after a complete chunk must not look like a shorter valid snapshot.
        val firstChunkEnd = 4 + 4 + key.encoded.size + 12 + 1 + 4 + 256 * 1024 + 16
        try {
            open(sealed.copyOf(firstChunkEnd))
            fail("truncated snapshot accepted")
        } catch (_: IOException) {
        }
    }

    private val sample = SavedPortals(
        listOf(
            SavedPortal(
                "http://portal.invalid/c/", "00:1A:79:00:00:01",
                listOf(
                    StalkerChannel("Das Erste HD", "ffmpeg cmd", "7", "DE | Allgemein", "11", null, true, "ard", 1),
                    StalkerChannel("ZDF", "cmd2", null, null)
                ),
                mapOf("7" to "DE | Allgemein"),
                mapOf(
                    PortalSection.SERIES to PortalCatalog(
                        listOf(
                            PortalTitle(
                                "One Piece", "42", categoryId = "9", categoryName = "DE | Anime", poster = "http://img.invalid/p.jpg",
                                year = 1999, addedAt = 1_700_000_000, description = "Ruffy", imdbId = "tt0388629",
                                tmdbId = "37854", popularity = 12.5, service = "Netflix"
                            ),
                            PortalTitle("Ohne Details")
                        ),
                        120, 3, listOf(PortalGroup("9", "DE | Anime", "DE"))
                    )
                ),
                setOf("11"), java.time.LocalDate.of(2027, 1, 31), enabled = false, reachable = true, autoDisabled = true
            )
        ),
        "active"
    )

    private fun roundTrip(library: SavedPortals): SavedPortals {
        val text = StringWriter()
        JsonWriter(text).use { writePortalLibrary(it, library) }
        return JsonReader(StringReader(text.toString())).use(::readPortalLibrary)
    }

    private fun assertSameLibrary(expected: SavedPortals, actual: SavedPortals) {
        assertEquals(expected.activeId, actual.activeId)
        assertEquals(expected.sources.size, actual.sources.size)
        expected.sources.zip(actual.sources).forEach { (a, b) ->
            assertEquals(a.identity, b.identity)
            assertEquals(a.genres, b.genres)
            assertEquals(a.catalogs, b.catalogs)
            assertEquals(a.epgChannelIds, b.epgChannelIds)
            assertEquals(a.subscriptionExpiry, b.subscriptionExpiry)
            assertEquals(listOf(a.enabled, a.reachable, a.autoDisabled), listOf(b.enabled, b.reachable, b.autoDisabled))
            assertEquals(a.channels.size, b.channels.size)
            a.channels.zip(b.channels).forEach { (x, y) ->
                assertEquals(
                    listOf(x.name, x.command, x.genreId, x.genreName, x.id, x.logo, x.direct, x.epgIdentity, x.number),
                    listOf(y.name, y.command, y.genreId, y.genreName, y.id, y.logo, y.direct, y.epgIdentity, y.number)
                )
            }
        }
    }

    @Test
    fun libraryRoundTripsThroughStreamingCodec() {
        assertSameLibrary(sample, roundTrip(sample))
    }

    @Test
    fun readsSnapshotsWrittenByEarlierVersions() {
        // Structure as produced by the org.json snapshot, including a legacy single source,
        // plain title names and numbers stored as strings.
        val legacy = """
            {"address":"http://portal.invalid/c/","mac":"00:1A:79:00:00:02","genres":{"1":"News"},
             "channels":[{"name":"Tagesschau","command":"c","genreId":"1","direct":false,"number":5}],
             "catalogs":{"MOVIES":{"titles":["Alter Film",{"name":"Neu","year":"2024","popularity":0}],
             "total":2,"groups":[{"id":"1","name":"Filme"}]}}}
        """.trimIndent()
        val library = JsonReader(StringReader(legacy)).use(::readPortalLibrary)
        val source = library.sources.single()
        assertEquals(source.identity, library.activeId)
        assertTrue(source.enabled)
        assertNull(source.reachable)
        assertEquals(5L, source.channels.single().number)
        val titles = source.catalogs.getValue(PortalSection.MOVIES).titles
        assertEquals(listOf("Alter Film", "Neu"), titles.map { it.name })
        assertEquals(2024, titles[1].year)
        assertNull(titles[1].popularity)
    }

    @Test
    fun encryptedLibraryRoundTrip() {
        val sealed = ByteArrayOutputStream().also { bytes ->
            val envelope = EnvelopeOutputStream(bytes, key, key.encoded)
            JsonWriter(java.io.OutputStreamWriter(envelope, Charsets.UTF_8)).use { writePortalLibrary(it, sample) }
        }.toByteArray()
        val restored = EnvelopeInputStream(ByteArrayInputStream(sealed)) { SecretKeySpec(it, "AES") }.use { input ->
            JsonReader(java.io.InputStreamReader(input, Charsets.UTF_8)).use(::readPortalLibrary)
        }
        assertSameLibrary(sample, restored)
    }
}
