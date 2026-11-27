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
    fun aStreamThatLoopedIsTriedLastOnlyForAWhile() {
        val now = 1_000_000L
        // "p#fhd" looped: it goes last, although it ran well before; "p#hd" of the same portal stays first.
        val records = mapOf(
            "p#fhd" to StreamRecord(good = 10).blocked(now),
            "p#hd" to StreamRecord(good = 1)
        )
        assertEquals(listOf("p#hd", "x", "p#fhd"), listOf("p#fhd", "p#hd", "x").rankedByHistory(records, now) { it })
        // After the block ends it is ranked by its history again.
        val later = now + StreamRecord.BLOCK_MS + 1
        assertEquals(listOf("p#fhd", "p#hd", "x"), listOf("p#fhd", "p#hd", "x").rankedByHistory(records, later) { it })
    }

    @Test
    fun theStandInComesFromAnotherPortal() {
        // Ranked A1, A2, B1, C1: after A1 comes B1 (another account), then A2, then C1.
        val ranked = listOf("A1", "A2", "B1", "C1").alternatingPortals { it.take(1) }
        assertEquals(listOf("A1", "B1", "A2", "C1"), ranked)
        // Only one portal: the order stays.
        assertEquals(listOf("A1", "A2", "A3"), listOf("A1", "A2", "A3").alternatingPortals { it.take(1) })
    }

    @Test
    fun equallyGoodStreamsAreMixedButAClearlyBetterOneStaysFirst() {
        val records = mapOf("A1" to StreamRecord(good = 8), "B1" to StreamRecord(good = 1), "C1" to StreamRecord(good = 1))
        val firsts = (0 until 40).map { seed ->
            listOf("B1", "C1", "A1").rankedByHistory(records, random = kotlin.random.Random(seed)) { it }
        }
        assertTrue(firsts.all { it.first() == "A1" })
        assertEquals(setOf("B1", "C1"), firsts.map { it[1] }.toSet())
    }

    @Test
    fun aBlockedStreamStaysLastWhenPortalsAlternate() {
        val now = 5_000L
        val records = mapOf("B1" to StreamRecord().blocked(now))
        val ranked = listOf("A1", "B1", "A2", "C1").rankedByHistory(records, now, portalOf = { it.take(1) }) { it }
        assertEquals(listOf("A1", "C1", "A2", "B1"), ranked)
    }

    @Test
    fun streamKeysSeparateVariantsOfOnePortal() {
        assertEquals("p#12", liveStreamKey("p", "12"))
        assertEquals("p", liveStreamKey("p", null))
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
