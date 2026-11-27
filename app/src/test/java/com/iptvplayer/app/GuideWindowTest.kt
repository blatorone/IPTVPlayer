package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideWindowTest {
    private fun show(title: String, startHours: Int, lengthHours: Int = 1) =
        PortalProgramme(title, startSeconds = NOW + startHours * 3_600L, endSeconds = NOW + (startHours + lengthHours) * 3_600L)

    @Test
    fun keepsOneDayBackAndTheChosenHoursAhead() {
        val shows = listOf(
            show("too old", -26), show("yesterday evening", -20), show("now", 0),
            show("tomorrow morning", 20), show("too far", 30)
        )
        assertEquals(
            listOf("yesterday evening", "now", "tomorrow morning"),
            shows.guideWindow(NOW, 24).map { it.title }
        )
    }

    private companion object {
        const val NOW = 1_800_000_000L
    }
}
