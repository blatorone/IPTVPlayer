package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideNavigationTest {
    @Test
    fun seriesOverlayMovesBetweenEpisodesSeasonsAndMenuInBothDirections() {
        val episodes = GuidePanel.GRID.afterSeriesOverlay(GuideInput.REMOTE_LEFT)
        val seasons = episodes.afterSeriesOverlay(GuideInput.SWIPE_LEFT)
        val menu = seasons.afterSeriesOverlay(GuideInput.REMOTE_LEFT)
        assertEquals(GuidePanel.CHANNELS, episodes)
        assertEquals(GuidePanel.CATEGORIES, seasons)
        assertEquals(GuidePanel.MENU, menu)
        assertEquals(seasons, menu.afterSeriesOverlay(GuideInput.BACK))
        assertEquals(episodes, seasons.afterSeriesOverlay(GuideInput.REMOTE_RIGHT))
        assertEquals(GuidePanel.GRID, episodes.afterSeriesOverlay(GuideInput.SWIPE_RIGHT))
        assertEquals(episodes, episodes.afterSeriesOverlay(GuideInput.REMOTE_DOWN))
    }

    @Test
    fun movingACategoryKeepsTheRelativeOrderOfTheOthers() {
        val categories = listOf("Nachrichten", "Sport", "Filme", "Kinder")
        assertEquals(listOf("Filme", "Nachrichten", "Sport", "Kinder"), categories.moveCategory("Filme", CategoryMove.TOP))
        assertEquals(listOf("Sport", "Filme", "Kinder", "Nachrichten"), categories.moveCategory("Nachrichten", CategoryMove.BOTTOM))
        assertEquals(categories, categories.moveCategory("Unbekannt", CategoryMove.UP))
    }

    @Test
    fun leftOpensTheChannelListOnlyInFullscreen() {
        for (input in listOf(GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT)) {
            assertEquals(GuidePanel.CHANNELS, GuidePanel.GRID.after(input))
            val guideCategories = GuidePanel.GRID.afterProgrammeGuide(input)
            assertEquals(GuidePanel.CATEGORIES, guideCategories)
            assertEquals(GuidePanel.MENU, guideCategories.afterProgrammeGuide(input))
            val fullscreenCategories = GuidePanel.CHANNELS.after(input)
            assertEquals(GuidePanel.CATEGORIES, fullscreenCategories)
            assertEquals(GuidePanel.MENU, fullscreenCategories.after(input))
            assertEquals(GuidePanel.GRID, GuidePanel.DETAILS.afterProgrammeGuide(input))
        }
    }

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
