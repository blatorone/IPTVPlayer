package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogueIndexTest {
    private val catalogue = List(2_000) { index ->
        PortalTitle("Titel $index", id = "$index", year = 1990 + index % 30, imdbId = if (index % 7 == 0) "tt$index" else null)
    } + listOf(
        PortalTitle("One Piece", id = "op-de", year = 1999, categoryName = "DE | Anime"),
        PortalTitle("One Piece", id = "op-live", year = 2023),
        PortalTitle("Gleicher Name", id = "a"),
        PortalTitle("Gleicher Name", id = "b", year = 2001),
        PortalTitle("Gleicher Name", id = "c", year = 2002)
    )

    @Test
    fun indexedLookupMatchesFullScan() {
        val queries = listOf(
            PortalTitle("One Piece", year = 1999),
            PortalTitle("One Piece"),
            PortalTitle("anders", imdbId = "tt14"),
            PortalTitle("Titel 15", year = 2005),
            PortalTitle("Gleicher Name"),
            PortalTitle("Unbekannt", year = 2000)
        )
        for (query in queries) for (verified in listOf(true, false)) {
            // Reference result without the index: every title is compared on its own, and the
            // few individual matches (below the index threshold) are then resolved together.
            val expected = catalogue.filter { candidate ->
                equivalentCatalogueTitle(query, listOf(candidate), verified) != null
            }.let { matches -> equivalentCatalogueTitle(query, matches, verified) }
            assertEquals("$query / $verified", expected, equivalentCatalogueTitle(query, catalogue, verified))
        }
        assertEquals("op-de", equivalentCatalogueTitle(PortalTitle("One Piece", year = 1999), catalogue)?.id)
        assertNull(equivalentCatalogueTitle(PortalTitle("Gleicher Name"), catalogue))
    }
}
