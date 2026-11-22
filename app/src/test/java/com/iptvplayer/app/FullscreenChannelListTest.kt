package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenChannelListTest {
    @Test
    fun picksTheCurrentShowAndCalculatesItsProgress() {
        val earlier = PortalProgramme("Früher", 100, 200)
        val current = PortalProgramme("Jetzt", 200, 300)
        val later = PortalProgramme("Danach", 300, 400)
        val schedule = listOf(earlier, current, later)

        assertEquals(current, currentGuideProgramme(schedule, 250))
        assertEquals(0.5f, guideProgrammeProgress(current, 250))
        assertEquals(later, currentGuideProgramme(schedule, 300))
        assertEquals(null, guideProgrammeProgress(current, 300))
    }
}
