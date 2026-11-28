package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelEditsTest {
    @Test
    fun movesPastTheNextShownEntry() {
        val order = listOf("a", "hidden", "b", "c")
        val shown = listOf("a", "b", "c")
        assertEquals(listOf("hidden", "b", "a", "c"), moveAmong(order, shown, "a", up = false))
        assertEquals(listOf("b", "a", "hidden", "c"), moveAmong(order, shown, "b", up = true))
        assertEquals(listOf("a", "hidden", "c", "b"), moveAmong(order, shown, "c", up = true))
    }

    @Test
    fun staysAtTheEnds() {
        val order = listOf("a", "b")
        assertNull(moveAmong(order, order, "a", up = true))
        assertNull(moveAmong(order, order, "b", up = false))
        assertNull(moveAmong(order, order, "x", up = false))
    }
}
