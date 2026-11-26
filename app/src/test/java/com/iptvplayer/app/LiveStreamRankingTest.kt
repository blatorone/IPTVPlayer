package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStreamRankingTest {
    @Test
    fun streamThatRanBestComesFirst() {
        val records = mapOf("b" to StreamRecord(good = 3), "a" to StreamRecord(bad = 1))
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").rankedByHistory(records) { it })
    }

    @Test
    fun withoutHistoryTheGivenOrderStays() {
        assertEquals(listOf("p", "a", "b"), listOf("p", "a", "b").rankedByHistory(emptyMap()) { it })
        // Equal scores keep the preferred portal in front.
        val even = mapOf("p" to StreamRecord(good = 1), "a" to StreamRecord(good = 1))
        assertEquals(listOf("p", "a", "b"), listOf("p", "a", "b").rankedByHistory(even) { it })
    }

    @Test
    fun failuresWeighMoreThanSuccesses() {
        assertEquals(-1, StreamRecord(good = 1, bad = 1).score)
        assertTrue(StreamRecord(good = 3, bad = 1).score > StreamRecord().score)
    }

    @Test
    fun historyStaysBoundedSoRecentBehaviourWins() {
        var record = StreamRecord()
        repeat(StreamRecord.MAX_EVENTS) { record = record.succeeded() }
        record = record.failed()
        assertTrue(record.good + record.bad <= StreamRecord.MAX_EVENTS)
        // A portal that kept failing lately falls behind one with no history.
        repeat(6) { record = record.failed() }
        assertTrue(record.score < 0)
    }
}
