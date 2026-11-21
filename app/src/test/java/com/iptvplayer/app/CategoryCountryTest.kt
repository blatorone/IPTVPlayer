package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryCountryTest {
    @Test
    fun recognizesCodesWithPortalSeparatorsAndSpacing() {
        for (name in listOf("DE", "de", "|DE|", "| DE |", "| DE | Filme", "[de] Filme",
            "(DE) Serien", "DE | Live TV", "DE Filme", "Filme DE", "Filme / DE", "DE-Filme",
            "｜ＤＥ｜ Serien", "DEU: Bundesliga", "GER_SERIEN", "🇩🇪 Filme")
        ) {
            assertEquals(name, "DE", categoryCountry(name))
        }
    }

    @Test
    fun recognizesNamesAndAlternateCountryCodes() {
        assertEquals("DE", categoryCountry("Deutschland – Sport"))
        assertEquals("DE", categoryCountry("Germany | News"))
        assertEquals("TR", categoryCountry("Türkiye | Sport"))
        assertEquals("AT", categoryCountry("AT: Nachrichten"))
        assertEquals("GB", categoryCountry("UK | Movies"))
        assertEquals("US", categoryCountry("[USA] News"))
        assertEquals("AE", categoryCountry("UAE | News"))
        assertEquals("FR", categoryCountry("🇫🇷 Infos"))
        assertEquals("DE", categoryCountry("Movies", "| DE |"))
    }

    @Test
    fun keepsAmbiguousCategoriesUnassigned() {
        assertEquals(UNKNOWN_COUNTRY, categoryCountry("|DE| |AT| Nachrichten"))
        assertEquals(UNKNOWN_COUNTRY, categoryCountry("🇩🇪 🇦🇹 Sport"))
        assertEquals(UNKNOWN_COUNTRY, categoryCountry("TV HD"))
        assertEquals(UNKNOWN_COUNTRY, categoryCountry("English Movies"))
        assertEquals("DE", categoryCountry("|AT| Movies", "DEU"))
    }
}
