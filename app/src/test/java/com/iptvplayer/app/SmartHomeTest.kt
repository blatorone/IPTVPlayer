package com.iptvplayer.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartHomeTest {
    @Test
    fun hoursAcrossMidnight() {
        // 18:00 to 06:00
        assertTrue(inHours(22 * 60, 18 * 60, 6 * 60))
        assertTrue(inHours(2 * 60, 18 * 60, 6 * 60))
        assertFalse(inHours(12 * 60, 18 * 60, 6 * 60))
        assertFalse(inHours(6 * 60, 18 * 60, 6 * 60))
    }

    @Test
    fun hoursWithinOneDay() {
        assertTrue(inHours(9 * 60, 8 * 60, 17 * 60))
        assertFalse(inHours(20 * 60, 8 * 60, 17 * 60))
        // Same start and end: the whole day.
        assertTrue(inHours(3 * 60, 0, 0))
    }

    @Test
    fun ruleAppliesOnlyToChosenAreas() {
        val rule = LightRule(movies = true, series = false, live = false)
        assertTrue(rule.applies(PortalSection.MOVIES))
        assertFalse(rule.applies(PortalSection.SERIES))
        assertFalse(rule.applies(PortalSection.LIVE))
        assertFalse(rule.applies(null))
    }
}
