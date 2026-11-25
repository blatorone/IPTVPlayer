package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideScheduleTest {
    @Test
    fun overlappingEntriesEndWhereTheNextShowStarts() {
        val cleaned = normalizeSchedule(listOf(
            PortalProgramme("Die drei Musketiere", 5_400, 11_700),
            PortalProgramme("SOKO Potsdam", 10_200, 10_800),
            PortalProgramme("Vesper Chronicles", 10_800, 14_400)
        ))
        assertEquals(listOf(5_400L to 10_200L, 10_200L to 10_800L, 10_800L to 14_400L),
            cleaned.map { it.startSeconds to it.endSeconds })
    }

    @Test
    fun duplicateStartsKeepTheMostDetailedEntry() {
        val cleaned = normalizeSchedule(listOf(
            PortalProgramme("Auf Streife", 100, 200),
            PortalProgramme("Auf Streife", 100, 300, "Polizisten im Einsatz"),
            PortalProgramme("Criminal Minds", 300, 400)
        ))
        assertEquals(listOf("Auf Streife", "Criminal Minds"), cleaned.map { it.title })
        assertEquals("Polizisten im Einsatz", cleaned.first().description)
    }

    @Test
    fun untimedEntriesAreKept() {
        val untimed = PortalProgramme("Programm", null)
        assertEquals(listOf(untimed), normalizeSchedule(listOf(untimed)))
    }
}
