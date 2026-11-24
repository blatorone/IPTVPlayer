package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GermanDescriptionTest {
    @Test
    fun rejectsDutchOverviewInsteadOfShowingItAsGerman() {
        assertNull(germanDescription("De familie woont in een huis en de kinderen spelen samen."))
    }

    @Test
    fun keepsGermanOverview() {
        val german = "Die Familie erlebt mit ihren Kindern ein neues Abenteuer."
        assertEquals(german, germanDescription(german))
    }
}
