package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalControlsTest {
    @Test
    fun ratingsBecomeAges() {
        assertEquals(12, ageFromRating("12"))
        assertEquals(16, ageFromRating("FSK 16"))
        assertEquals(6, ageFromRating("PG"))
        assertEquals(12, ageFromRating("PG-13"))
        assertEquals(16, ageFromRating("TV-MA"))
        assertEquals(null, ageFromRating("NR"))
        assertEquals(null, ageFromRating(null))
    }

    @Test
    fun recognisesAdultAndKidsCategories() {
        assertTrue(isAdultCategory("DE| EROTIK"))
        assertTrue(isAdultCategory("XXX 18+"))
        assertFalse(isAdultCategory("Sexy Beast Filme"))
        assertTrue(isKidsCategory("DE| KINDER"))
        assertTrue(isKidsCategory("Family & Kids"))
        assertFalse(isKidsCategory("DE| SPORT"))
    }

    @Test
    fun kidsModeShowsOnlyKidsCategoriesAndNeverAdultOnes() {
        val parental = ParentalControls(null)
        parental.updateKidsMode(true)
        parental.updateKidsAge(6)
        assertTrue(parental.allowsCategory("live:kinder", "DE| KINDER", kidsFriendlyId = false))
        assertFalse(parental.allowsCategory("live:sport", "DE| SPORT", kidsFriendlyId = false))
        assertTrue(parental.allowsCategory("movies:vod:FAMILY", "Familie", kidsFriendlyId = true))
        parental.updateKidsAge(16)
        assertTrue(parental.allowsCategory("live:sport", "DE| SPORT", kidsFriendlyId = false))
        assertFalse(parental.allowsCategory("live:erotik", "DE| EROTIK", kidsFriendlyId = false))
        assertFalse(parental.allowsAge(null))
        assertTrue(parental.allowsAge(16))
        assertFalse(parental.allowsAge(18))
    }
}
