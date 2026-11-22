package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideNavigationTest {
    @Test
    fun touchDirectionsDependOnTheOpenPanel() {
        val channels = GuidePanel.GRID.after(GuideInput.SWIPE_LEFT)
        val categories = channels.after(GuideInput.SWIPE_LEFT)
        assertEquals(GuidePanel.CHANNELS, channels)
        assertEquals(GuidePanel.CATEGORIES, categories)
        assertEquals(GuidePanel.CATEGORIES, channels.after(GuideInput.SWIPE_RIGHT))
        assertEquals(GuidePanel.MENU, categories.after(GuideInput.SWIPE_RIGHT))
        assertEquals(GuidePanel.DETAILS, GuidePanel.GRID.after(GuideInput.SWIPE_RIGHT))
    }

    @Test
    fun usesTheCurrentCategoryOrTheRunningSendersCategoryInTheAllChannelsGuide() {
        val news = StalkerChannel("News", "", null, null)
        val music = StalkerChannel("Music", "", null, null)
        val categories = mapOf("Nachrichten" to listOf(news), "Musik" to listOf(music))
        val visible = categories.keys.toList()
        assertEquals("Musik", guideChannelCategory("Musik", visible, categories, news))
        assertEquals("Nachrichten", guideChannelCategory(null, visible, categories, news))
        assertEquals("Musik", guideChannelCategory("Ausgeblendet", visible, categories, music))
    }

    @Test
    fun remoteMovesLeftThroughTheListsAndRightToDetailsFromTheGrid() {
        val channels = GuidePanel.GRID.after(GuideInput.REMOTE_LEFT)
        val categories = channels.after(GuideInput.REMOTE_LEFT)
        val menu = categories.after(GuideInput.REMOTE_LEFT)
        assertEquals(GuidePanel.MENU, menu)
        assertEquals(GuidePanel.CATEGORIES, menu.after(GuideInput.REMOTE_RIGHT))
        assertEquals(GuidePanel.DETAILS, GuidePanel.GRID.after(GuideInput.REMOTE_RIGHT))
        assertEquals(GuidePanel.GRID, GuidePanel.DETAILS.after(GuideInput.REMOTE_LEFT))
    }
}
