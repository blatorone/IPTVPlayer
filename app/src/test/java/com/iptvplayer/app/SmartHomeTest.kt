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
    fun eachStateHasItsOwnSceneAndUnsetMeansUnchanged() {
        val rule = LightRule()
            .withSetting(LightState.PLAY, "light.sofa", LightSetting(true, brightness = 20, kelvin = 2_700))
            .withSetting(LightState.PAUSE, "light.sofa", LightSetting(true, brightness = 80))
            .withSetting(LightState.SMOKE, "cover.terrace", LightSetting(true))
        assertTrue(rule.scene(LightState.PLAY).getValue("light.sofa").brightness == 20)
        assertTrue(rule.scene(LightState.PAUSE).getValue("light.sofa").brightness == 80)
        assertTrue(rule.scene(LightState.STOP).isEmpty())
        // "nicht ändern" removes the light from that state only.
        val unchanged = rule.withSetting(LightState.PLAY, "light.sofa", null)
        assertTrue(unchanged.scene(LightState.PLAY).isEmpty())
        assertTrue(unchanged.scene(LightState.PAUSE).containsKey("light.sofa"))
        assertTrue(unchanged.scene(LightState.SMOKE).containsKey("cover.terrace"))
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
