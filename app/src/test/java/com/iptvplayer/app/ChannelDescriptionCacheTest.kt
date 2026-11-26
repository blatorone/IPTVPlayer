package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelDescriptionCacheTest {
    private fun portal(channels: List<StalkerChannel>) =
        SavedPortal("https://one.example", "02:00:00:00:00:01", channels, emptyMap(), emptyMap())

    @Test
    fun savedDescriptionsGiveTheSameGuide() {
        val channels = listOf(
            StalkerChannel("| DE | RTL FHD", "a", null, "| DE | Allgemeines"),
            StalkerChannel("DE - Sky Cinema Action HD", "b", null, "DE Filme"),
            StalkerChannel("UK: BBC One HD", "c", null, "UK General")
        )
        val file = java.io.File.createTempFile("descriptions", ".txt")
        try {
            clearChannelDescriptions()
            val fresh = buildLiveChannelGroups(listOf(portal(channels)), combine = true).map { it.description }
            saveChannelDescriptions(file, "7")
            clearChannelDescriptions()
            loadChannelDescriptions(file, "7")
            val restored = buildLiveChannelGroups(listOf(portal(channels)), combine = true).map { it.description }
            assertEquals(fresh, restored)
        } finally {
            file.delete()
            clearChannelDescriptions()
        }
    }

    @Test
    fun fileOfAnotherVersionIsIgnored() {
        val file = java.io.File.createTempFile("descriptions", ".txt")
        try {
            file.writeText("channel-descriptions 1 6\nX\t\tWrong\tDE\tNEWS\t\n")
            clearChannelDescriptions()
            loadChannelDescriptions(file, "7")
            val description = describeChannel(StalkerChannel("X", "x", null, null), null)
            assertEquals("X", description.name)
        } finally {
            file.delete()
            clearChannelDescriptions()
        }
    }
}
