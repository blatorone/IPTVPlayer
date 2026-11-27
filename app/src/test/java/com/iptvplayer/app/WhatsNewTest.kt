package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    private val changelog = """
        # Änderungen

        ## 2.0.0

        - Neu A
        - Neu B

        ## 1.0.98

        - Alt

        ## 1.0.97

        - Älter
    """.trimIndent()

    @Test
    fun readsTheNewestSectionsInOrder() {
        val sections = changelogSections(changelog, limit = 2)
        assertEquals(listOf("2.0.0", "1.0.98"), sections.map { it.version })
        assertEquals("- Neu A\n- Neu B", sections.first().text)
    }

    @Test
    fun pageTextHasAHeadingPerVersion() {
        val text = whatsNewText(changelogSections(changelog))
        assertTrue(text.startsWith("Version 2.0.0\n\n- Neu A"))
        assertTrue("Version 1.0.97\n\n- Älter" in text)
    }

    @Test
    fun noChangelogGivesNoSections() {
        assertEquals(emptyList<ChangelogSection>(), changelogSections(""))
    }
}
