package com.iptvplayer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeTimingTest {
    private fun randomFrames(random: java.util.Random, times: List<Long>) =
        times.map { EpisodeFrame(it, random.nextLong(), false, random.nextLong()) }

    @Test
    fun confirmsLearnedIntroEarlyDespiteDroppedAndNoisyFrames() {
        val random = java.util.Random(7)
        val intro = 10_000L until 50_000L
        val reference = randomFrames(random, (10_000L until 50_000L step 1_000L).toList())
        val leadIn = randomFrames(random, (0L until 25_000L step 1_000L).toList())
        val played = reference.take(9).mapIndexedNotNull { index, frame ->
            when (index) {
                3 -> null // dropped sample
                5 -> EpisodeFrame(frame.timeMs + 15_300, random.nextLong(), false, random.nextLong())
                else -> frame.copy(timeMs = frame.timeMs + 15_300)
            }
        }
        assertEquals(25_000L until 65_000L, confirmKnownOpening(leadIn + played, reference, intro))
        assertNull(confirmKnownOpening(leadIn + played.take(3), reference, intro))
        assertNull(confirmKnownOpening(randomFrames(random, (0L until 60_000L step 1_000L).toList()), reference, intro))
    }

    @Test
    fun findsSharedOpeningAcrossGapsAndSamplingPhase() {
        val random = java.util.Random(11)
        val shared = List(20) { random.nextLong() to random.nextLong() }
        val previous = randomFrames(random, (0L until 5_000L step 1_000L).toList()) +
            shared.mapIndexed { index, (difference, average) ->
                EpisodeFrame(5_000L + index * 1_000, difference, false, average)
            } +
            randomFrames(random, (25_000L until 35_000L step 1_000L).toList())
        val currentIntro = shared.mapIndexedNotNull { index, (difference, average) ->
            if (index == 7) null else EpisodeFrame(3_400L + index * 1_000, difference xor 0x3L, false, average)
        }
        val lead = randomFrames(random, listOf(400L, 1_400L, 2_400L))
        val tail = randomFrames(random, (23_400L until 28_400L step 1_000L).toList())
        assertEquals(3_400L until 22_900L,
            findRepeatedOpening(lead + currentIntro + tail, previous, HLS_INTRO_FRAME_INTERVAL_MS))
        // Without differing footage after the match its end is still unknown.
        assertNull(findRepeatedOpening(lead + currentIntro, previous, HLS_INTRO_FRAME_INTERVAL_MS))
    }

    @Test
    fun ignoresFlatAndStaticPictures() {
        val black = List(30) { EpisodeFrame(it * 1_000L, 0L, false, 0L, detail = false) }
        assertNull(findRepeatedOpening(black, black, HLS_INTRO_FRAME_INTERVAL_MS))
        val random = java.util.Random(13)
        val still = List(30) { EpisodeFrame(it * 1_000L, 0x1234_5678_9abcL, false, 0x0f0f_0f0fL) }
        val other = still + randomFrames(random, (30_000L until 36_000L step 1_000L).toList())
        assertNull(findRepeatedOpening(other, still + randomFrames(random, (30_000L until 36_000L step 1_000L).toList()),
            HLS_INTRO_FRAME_INTERVAL_MS))
    }

    private fun audio(random: java.util.Random, fromMs: Long, toMs: Long) =
        (fromMs until toMs step 47L).map { AudioPrint(it, random.nextInt(), true) }

    private fun noisy(random: java.util.Random, prints: List<AudioPrint>, shiftMs: Long) =
        prints.mapIndexed { index, print ->
            var bits = print.bits
            repeat(if (index % 2 == 0) 1 else 4) { bits = bits xor (1 shl random.nextInt(32)) }
            AudioPrint(print.timeMs + shiftMs, bits, true)
        }

    @Test
    fun findsSharedThemeMusicInAudio() {
        val random = java.util.Random(3)
        val theme = audio(random, 10_000, 40_000)
        val previous = audio(random, 0, 10_000) + theme + audio(random, 40_000, 60_000)
        val current = audio(random, 0, 4_000) + noisy(random, theme, -5_993) + audio(random, 34_100, 50_000)
        val range = requireNotNull(findRepeatedAudioOpening(current, previous))
        assertTrue(range.first in 3_000L..5_000L)
        assertTrue(range.last in 32_500L..34_500L)
        assertNull(findRepeatedAudioOpening(audio(random, 0, 60_000), previous))
        val silent = previous.map { it.copy(loud = false) }
        assertNull(findRepeatedAudioOpening(silent, silent))
    }

    @Test
    fun confirmsLearnedThemeMusicEarly() {
        val random = java.util.Random(5)
        val theme = audio(random, 10_000, 40_000)
        val intro = 10_000L until 40_000L
        val learned = theme.filter { it.timeMs < 30_000 }
        val current = audio(random, 0, 8_000) + noisy(random, theme.filter { it.timeMs < 18_000 }, -2_000)
        val range = requireNotNull(confirmKnownAudioOpening(current, learned, intro))
        assertTrue(kotlin.math.abs(range.first - 8_000L) <= 40)
        assertNull(confirmKnownAudioOpening(audio(random, 0, 30_000), learned, intro))
        assertNull(confirmKnownAudioOpening(current.take(200), learned, intro))
    }

    @Test
    fun audioCachesRoundTrip() {
        val prints = listOf(AudioPrint(0, 5, true), AudioPrint(47, -1, false))
        assertEquals("episode" to prints, decodeAudioReference(encodeAudioReference("episode", prints)))
        assertEquals((1_000L..9_000L) to prints, decodeLearnedAudio(encodeLearnedAudio(1_000L..9_000L, prints)))
        assertNull(decodeLearnedAudio(byteArrayOf(1, 2)))
    }

    @Test
    fun blockFingerprintsSeparateDetailFromFlatFrames() {
        val width = 144
        val height = 80
        val flat = IntArray(width * height) { 0xff101010.toInt() }
        assertFalse(episodeFrame(0, flat, width, height).detail)
        val pattern = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x / 16 + y / 10) % 2 == 0) 0xffe0e0e0.toInt() else 0xff202020.toInt()
        }
        val noisy = IntArray(pattern.size) { index ->
            val noise = (index * 7_919 % 23) - 11
            val channel = ((pattern[index] and 255) + noise).coerceIn(0, 255)
            (0xff shl 24) or (channel shl 16) or (channel shl 8) or channel
        }
        val first = episodeFrame(0, pattern, width, height)
        val second = episodeFrame(0, noisy, width, height)
        assertTrue(first.detail)
        assertTrue(java.lang.Long.bitCount(first.fingerprint xor second.fingerprint) <= 4)
    }

    @Test
    fun openingFrameCachesReadOldAndNewFingerprints() {
        val oldReference = decodeOpeningReference("episode-1|0,1;1000,2;2000,4;3000,8")
        assertEquals(4, oldReference?.frames?.size)
        assertEquals(2L, oldReference?.frames?.get(1)?.averageFingerprint)

        val reference = OpeningReference("episode-2", List(4) { index ->
            EpisodeFrame(index * 1_000L, 1L shl index, false, 7L shl index)
        })
        assertEquals(reference, decodeOpeningReference(reference.encode()))
    }

    @Test
    fun rejectsAnEarlyTextSequenceFollowedByPicture() {
        val frames = listOf(true, true, true, false, false, true, true, true).mapIndexed { index, text ->
            EpisodeFrame(index * 5_000L, 0, text)
        }
        assertEquals(25_000L, findCreditsStart(frames))
        assertNull(findCreditsStart(frames.take(5)))
    }

    @Test
    fun prefersCreditsAndFallsBackToRealEndForInvalidMarkers() {
        assertEquals(90_000L, EpisodeTiming(creditsStartMs = 90_000).nextAt(120_000))
        assertEquals(120_000L, EpisodeTiming(creditsStartMs = 140_000).nextAt(120_000))
        assertNull(EpisodeTiming(introStartMs = 20_000, introEndMs = 10_000).intro(120_000))
        assertEquals(19_999L, EpisodeTiming(10_000, 20_000).intro(120_000)?.last)
    }

    @Test
    fun doesNotOfferIntroTargetBehindPlaybackPosition() {
        val intro = EpisodeTiming(10_000, 20_000).intro(120_000)
        assertEquals(20_000L, futureIntroSkipTarget(intro, 15_000))
        assertNull(futureIntroSkipTarget(intro, 20_001))
    }

    @Test
    fun selectsNextNumberAndContinuesIntoTheNextSeason() {
        val first = PortalTitle("Eins", episode = "1")
        val second = PortalTitle("Zwei", episode = "2")
        val current = PortalSeason(1, PortalTitle("Staffel 1"), listOf(second, first))
        val upcoming = PortalSeason(2, PortalTitle("Staffel 2"), listOf(first))
        assertEquals(current to second, followingEpisode(listOf(current, upcoming), current, first))
        assertEquals(upcoming to first, followingEpisode(listOf(current, upcoming), current, second))
        assertNull(followingEpisode(listOf(current, upcoming), upcoming, first))
    }

    @Test
    fun selectsPreviousNumberAndContinuesIntoThePreviousSeason() {
        val first = PortalTitle("Eins", episode = "1")
        val second = PortalTitle("Zwei", episode = "2")
        val current = PortalSeason(1, PortalTitle("Staffel 1"), listOf(second, first))
        val previousSeason = PortalSeason(0, PortalTitle("Staffel 0"), listOf(second))
        assertEquals(current to first, precedingEpisode(listOf(previousSeason, current), current, second))
        assertEquals(previousSeason to second, precedingEpisode(listOf(previousSeason, current), current, first))
        assertNull(precedingEpisode(listOf(previousSeason, current), previousSeason, second))
    }
}
