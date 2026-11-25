package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CastBrowserTest {
    private val portal = SavedPortal(
        "http://portal.invalid/c/", "00:1A:79:00:00:03", emptyList(), emptyMap(),
        mapOf(
            PortalSection.MOVIES to PortalCatalog(
                listOf(
                    PortalTitle("Spider-Man: Brand New Day - 2026", id = "1", year = 2026),
                    PortalTitle("Spider-Man: No Way Home", id = "2", year = 2021),
                    PortalTitle("Dune", id = "3", year = 2021)
                ), null, null
            ),
            PortalSection.SERIES to PortalCatalog(listOf(PortalTitle("Euphoria", id = "9", year = 2019)), null, null)
        )
    )

    @Test
    fun listsOnlyWorkAvailableInThePortalsNewestFirst() {
        val credits = listOf(
            PersonCredit(PortalSection.MOVIES, "Spider-Man: No Way Home", null, 2021, null, null),
            PersonCredit(PortalSection.MOVIES, "Spider-Man: Brand New Day", null, 2026, null, null),
            PersonCredit(PortalSection.MOVIES, "Challengers", null, 2024, null, null),
            PersonCredit(PortalSection.SERIES, "Euphoria", null, 2019, null, null)
        )
        val matched = matchPersonCredits(credits, listOf(portal)) { _, items -> items }
        assertEquals(listOf("1", "2", "9"), matched.map { it.second.id })
        assertEquals(PortalSection.SERIES, matched.last().first)
    }

    @Test
    fun asksThePortalForTitlesMissingFromTheStoredCatalogue() = kotlinx.coroutines.runBlocking {
        val credits = listOf(
            PersonCredit(PortalSection.SERIES, "Stranger Things", null, 2016, null, "66732"),
            PersonCredit(PortalSection.MOVIES, "Dune", null, 2021, null, null),
            PersonCredit(PortalSection.MOVIES, "Unbekannt", null, 2020, null, null)
        )
        val asked = mutableListOf<String>()
        val resolved = resolvePersonCredits(credits, listOf(portal), { _, items -> items }, lookup = { _, section, reference ->
            asked += reference.name
            if (section == PortalSection.SERIES && reference.name == "Stranger Things") {
                PortalTitle("Stranger Things", id = "st", year = 2016)
            } else null
        })
        assertEquals(listOf("3", "st"), resolved.map { it.second.id })
        // Titles already in the stored catalogue are not looked up again.
        assertEquals(listOf("Stranger Things", "Unbekannt"), asked.sorted())
    }

    @Test
    fun appliesTheCountryFilter() {
        val credits = listOf(PersonCredit(PortalSection.MOVIES, "Dune", null, 2021, null, null))
        assertEquals(emptyList<Pair<PortalSection, PortalTitle>>(),
            matchPersonCredits(credits, listOf(portal)) { _, _ -> emptyList() })
    }
}
