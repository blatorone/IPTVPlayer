package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveChannelCatalogueTest {
    @Test
    fun mergesPortalCategoriesAndQualitiesIntoOneRtlEntry() {
        val first = source("one", listOf(
            channel("| DE | RTL FHD", "| DE | Allgemeines", "one-fhd"),
            channel("DE - RTL RAW", "| DE | Allgemeines", "one-raw")
        ))
        val second = source("two", listOf(
            channel("(DE) RTL HD+", "DE- Deutschland", "two-hd")
        ))
        val group = buildLiveChannelGroups(listOf(first, second), combine = true).single()
        assertEquals("RTL", group.description.name)
        assertEquals("DE", group.description.country)
        assertEquals(LiveCategory.GENERAL, group.description.category)
        assertEquals(listOf("FHD", "RAW", "HD+"), group.variants.flatMap { it.description.qualities })
        assertEquals(listOf("one-fhd", "one-raw", "two-hd"), group.variants.map { it.item.value.command })
    }

    @Test
    fun preservesDifferentCountriesAndStationNamesWithPlus() {
        val first = source("one", listOf(
            channel("| DE | Allgemeines | DE | RTL FHD", "|DE| Allgemeines", "rtl-de"),
            channel("DE RTL+ FHD", "|DE| Allgemeines", "rtl-plus")
        ))
        val second = source("two", listOf(channel("AT RTL HD", "AT – TV", "rtl-at")))
        val result = buildLiveChannelGroups(listOf(first, second), combine = true)
        assertEquals(listOf("RTL", "RTL+", "RTL"), result.map { it.description.name })
        assertEquals(listOf("DE", "DE", "AT"), result.map { it.description.country })
    }

    @Test
    fun keepsPortalVariantsSeparateWhenCombiningIsOff() {
        val first = source("one", listOf(channel("DE RTL RAW", "DE", "first")))
        val second = source("two", listOf(channel("DE RTL HD", "DE", "second")))
        assertEquals(2, buildLiveChannelGroups(listOf(first, second), combine = false).size)
    }

    @Test
    fun groupsFourKPlusAndCodecTagsWithTheSameSender() {
        val first = source("one", listOf(channel("DE | RTL 4K+", "DE Allgemeines", "uhd")))
        val second = source("two", listOf(channel("(DE) RTL FHD HEVC", "DE", "fhd")))
        val group = buildLiveChannelGroups(listOf(first, second), combine = true).single()
        assertEquals("RTL", group.description.name)
        assertEquals(setOf("4K+", "FHD", "HEVC"), group.variants.flatMap { it.description.qualities }.toSet())
    }

    @Test
    fun classifiesSkyAccordingToTheChannelName() {
        assertEquals(LiveCategory.FOOTBALL, describeChannel(channel("DE Sky Bundesliga HD", "Allgemeines", "a"), "Allgemeines").category)
        assertEquals(LiveCategory.MOVIES, describeChannel(channel("DE Sky Cinema FHD", "Allgemeines", "b"), "Allgemeines").category)
        assertEquals(LiveCategory.SKY, describeChannel(channel("DE Sky One HD", "Allgemeines", "c"), "Allgemeines").category)
    }

    @Test
    fun keepsSpecificSportsAndProviderNamesSeparate() {
        assertEquals(LiveCategory.DAZN, describeChannel(channel("DAZN Tennis HD", "Sport", "a"), "Sport").category)
        assertEquals(LiveCategory.MAGENTA, describeChannel(channel("MagentaSport 2 HD", "Sport", "b"), "Sport").category)
        assertEquals(LiveCategory.TENNIS, describeChannel(channel("Tennis Channel", "Sport", "c"), "Sport").category)
    }

    private fun source(name: String, channels: List<StalkerChannel>) = SavedPortal(
        "https://$name.example", "02:00:00:00:00:01", channels, emptyMap(), emptyMap()
    )

    private fun channel(name: String, category: String, command: String) =
        StalkerChannel(name, command, null, category)
}
