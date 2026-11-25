package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroVariantsTest {
    private val random = java.util.Random(23)
    private fun frames(from: Long, count: Int) = List(count) { index ->
        EpisodeFrame(from + index * 1_000L, random.nextLong(), false, random.nextLong())
    }
    private fun shifted(frames: List<EpisodeFrame>, by: Long) = frames.map { it.copy(timeMs = it.timeMs + by) }

    // A 60 s opening; its short version shares the first 20 s and then the episode starts.
    private val longOpening = frames(10_000, 60)
    private val longVariant = IntroVariant(10_000L until 70_000L, longOpening, emptyList(), 1)
    private val shortVariant = IntroVariant(10_000L until 30_000L, longOpening.take(20), emptyList(), 2)

    private fun observe(current: List<EpisodeFrame>, variant: IntroVariant) =
        observeKnownOpening(current, variant.frames, variant.range)

    @Test
    fun offersOnlyTheSafeEndWhileLongAndShortVersionAreBothPossible() {
        // 12 s into the opening both versions match.
        val current = frames(0, 5) + shifted(longOpening.take(12), -5_000)
        val seen = listOf(observe(current, longVariant)!!, observe(current, shortVariant)!!)
        assertEquals(5_000L..24_999L, chooseIntro(seen))
    }

    @Test
    fun movesToTheLongEndOnceTheEpisodeContinuesPastTheShortVersion() {
        val current = frames(0, 5) + shifted(longOpening.take(30), -5_000)
        val seen = listOf(observe(current, longVariant)!!, observe(current, shortVariant)!!)
        assertEquals(5_000L..64_999L, chooseIntro(seen))
    }

    @Test
    fun withdrawsTheLongVersionWhenTheEpisodeLeavesIt() {
        // Short version: after 20 s of the opening, the episode continues with other footage.
        val current = frames(0, 5) + shifted(longOpening.take(20), -5_000) + frames(25_000, 15)
        val long = observe(current, longVariant)
        assertNotNull(long)
        assertTrue(long!!.diverged)
        val short = observe(current, shortVariant)!!
        assertFalse(short.diverged)
        assertEquals(5_000L..24_999L, chooseIntro(listOf(long, short)))
        // Without the short variant nothing is offered rather than a skip into the episode.
        assertNull(chooseIntro(listOf(long)))
    }

    @Test
    fun learnsAShortVersionAsNewVariantAndExtendsTheSameOpening() {
        val current = frames(0, 5) + shifted(longOpening.take(20), -5_000) + frames(25_000, 15)
        val seenLong = longVariant to observe(current, longVariant)!!
        val learned = learnIntroVariant(listOf(longVariant), listOf(seenLong), 5_000L until 24_500L, current, emptyList(), 10)
        assertEquals(2, learned.size)
        assertTrue(learned.any { it.range == 5_000L until 24_500L })

        // The same opening found longer than learned replaces the earlier, too short variant.
        val tooShort = IntroVariant(10_000L until 40_000L, longOpening.take(30), emptyList(), 1)
        val full = frames(0, 5) + shifted(longOpening, -5_000) + frames(65_000, 10)
        val seen = tooShort to observe(full, tooShort)!!
        val corrected = learnIntroVariant(listOf(tooShort), listOf(seen), 5_000L until 64_500L, full, emptyList(), 10)
        assertEquals(listOf(5_000L until 64_500L), corrected.map { it.range })
    }

    @Test
    fun keepsAtMostFourVariantsDroppingTheLeastRecentlyUsed() {
        val variants = List(4) { index -> IntroVariant(10_000L until 40_000L, frames(10_000, 30), emptyList(), index.toLong()) }
        val newOpening = frames(100_000, 30)
        val learned = learnIntroVariant(variants, emptyList(), 100_000L until 130_000L, newOpening, emptyList(), 99)
        assertEquals(4, learned.size)
        assertTrue(learned.none { it.lastUsed == 0L })
        assertEquals(99L, learned.first().lastUsed)
    }

    @Test
    fun creditsWithPostCreditsSceneAreSkippedOtherwiseTheNextEpisodeFollows() {
        // 22 min episode; closing window starts five minutes before the end (at 17:00).
        val duration = 22 * 60_000L
        val windowStart = duration - CLOSING_WINDOW_MS
        // Credits from 18:00 to 18:40, then a 2:20 post-credits scene.
        val withScene = closingDecision(60_000L until 100_000L, windowStart, duration)
        assertEquals((windowStart + 60_000L) until (windowStart + 100_000L), withScene.skip)
        assertNull(withScene.creditsStart)
        // Credits running until ten seconds before the end: offer the next episode instead.
        val plain = closingDecision(240_000L until 290_000L, windowStart, duration)
        assertNull(plain.skip)
        assertEquals(windowStart + 240_000L, plain.creditsStart)
    }

    @Test
    fun variantsRoundTrip() {
        val prints = List(50) { AudioPrint(10_000L + it * 47, it, true) }
        val variants = listOf(longVariant.copy(prints = prints), shortVariant)
        val decoded = decodeIntroVariants(encodeIntroVariants(variants))
        assertEquals(variants.map { it.range }, decoded.map { it.range })
        assertEquals(variants.map { it.lastUsed }, decoded.map { it.lastUsed })
        assertEquals(prints, decoded.first().prints)
        assertEquals(longOpening.map { it.timeMs }, decoded.first().frames.map { it.timeMs })
        assertTrue(decodeIntroVariants(byteArrayOf(0, 0, 0, 9)).isEmpty())
    }
}
