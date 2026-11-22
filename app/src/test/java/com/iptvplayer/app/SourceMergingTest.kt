package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceMergingTest {
    private val first = SavedPortal(
        "https://first.example", "02:00:00:00:00:01", emptyList(), emptyMap(), emptyMap()
    )
    private val second = SavedPortal(
        "https://second.example", "02:00:00:00:00:02", emptyList(), emptyMap(), emptyMap()
    )

    @Test
    fun groupsLiveVariantsByPortalEpgIdentifierInSourceOrder() {
        val firstChannel = StalkerChannel("Sender HD", "first", null, null, epgIdentity = "sender.example")
        val secondChannel = StalkerChannel("Sender", "second", null, null, epgIdentity = "SENDER.example")
        val items = listOf(SourceItem(first, firstChannel), SourceItem(second, secondChannel))
        assertEquals(listOf(items), mergeSourceItems(items, ::channelIdentity))
    }

    @Test
    fun localIdsAndNamesAloneKeepChannelsSeparate() {
        val items = listOf(
            SourceItem(first, StalkerChannel("Nachrichten", "first", null, null, id = "1")),
            SourceItem(second, StalkerChannel("Nachrichten", "second", null, null, id = "1"))
        )
        assertEquals(2, mergeSourceItems(items, ::channelIdentity).size)
    }

    @Test
    fun titleAndYearDistinguishRemakes() {
        val items = listOf(
            SourceItem(first, PortalTitle(" Beispielfilm ", year = 1990)),
            SourceItem(second, PortalTitle("BEISPIELFILM", year = 1990)),
            SourceItem(second, PortalTitle("Beispielfilm", year = 2024))
        )
        assertEquals(listOf(2, 1), mergeSourceItems(items, ::titleIdentity).map { it.size })
    }

    @Test
    fun combinesVodTitlesDespitePortalLabelsOrMetadataDifferences() {
        val items = listOf(
            SourceItem(first, PortalTitle("| DE | Beispielfilm (2022) FHD", imdbId = "tt123")),
            SourceItem(second, PortalTitle("Beispielfilm 4K+", year = 2022))
        )
        assertEquals(listOf(items), mergeCatalogueTitles(items))
        assertEquals("Beispielfilm", displayVodTitle(items.first().value))
    }

    @Test
    fun keepsAmbiguousReleasesSeparateWithoutAYear() {
        val items = listOf(
            SourceItem(first, PortalTitle("Beispielfilm", year = 1990)),
            SourceItem(first, PortalTitle("Beispielfilm", year = 2024)),
            SourceItem(second, PortalTitle("Beispielfilm"))
        )
        assertEquals(3, mergeCatalogueTitles(items).size)
        assertEquals(null, equivalentCatalogueTitle(items.last().value, items.take(2).map { it.value }))
    }

    @Test
    fun keepsGhostsAdaptationsAndUnknownSeriesSeparate() {
        val american = PortalTitle("Ghosts", year = 2021, imdbId = "tt11379026")
        val german = PortalTitle("Ghosts", year = 2025)
        val unidentified = PortalTitle("Ghosts")
        val americanVariant = PortalTitle("Ghosts (2021)")
        val items = listOf(
            SourceItem(first, american), SourceItem(second, german),
            SourceItem(second, unidentified), SourceItem(second, americanVariant)
        )
        assertEquals(listOf(2, 1, 1), mergeCatalogueTitles(items, requireVerifiedIdentity = true).map { it.size })
        assertEquals(
            americanVariant,
            equivalentCatalogueTitle(american, listOf(german, unidentified, americanVariant), requireVerifiedIdentity = true)
        )
        assertEquals(null, equivalentCatalogueTitle(american, listOf(unidentified), requireVerifiedIdentity = true))
    }

    @Test
    fun readsOnlyValidPortalExpiryDates() {
        val expected = java.time.LocalDate.of(2026, 12, 31)
        assertEquals(expected, parsePortalExpiry("2026-12-31 18:30:00"))
        assertEquals(expected, parsePortalExpiry("1798675200"))
        assertEquals(null, parsePortalExpiry("0000-00-00"))
        assertEquals(null, parsePortalExpiry("2026-02-30"))
        assertEquals(null, parsePortalExpiry("0"))
    }

    @Test
    fun mapsDifferentProviderGenreLabelsToTheSameCategory() {
        assertEquals(VodCategory.ACTION, vodCategory(PortalGroup("one", "| DE | Action & Abenteuer")))
        assertEquals(VodCategory.ACTION, vodCategory(PortalGroup("two", "Adventure Movies")))
    }

    @Test
    fun recognizesCinemaAndStreamingServicesFromPortalLabels() {
        assertEquals(VodCategory.CINEMA, vodCategory(PortalGroup("kino", "| DE | Kino Filme")))
        assertEquals(VodCategory.NETFLIX, vodCategory(PortalGroup("n", "| DE | Netflix Serien")))
        assertEquals(VodCategory.PRIME, serviceCategory("Amazon Prime Video"))
        assertEquals(VodCategory.APPLE, serviceCategory("Apple TV+"))
    }
}
