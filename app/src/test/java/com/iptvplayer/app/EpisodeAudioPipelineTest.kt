package com.iptvplayer.app

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs synthetic decoded audio through [EpisodeAudioTap] as the player would, with the
 * shared theme starting at an arbitrary sub-millisecond offset in each episode.
 */
class EpisodeAudioPipelineTest {
    private val rate = 48_000

    /** A deterministic melody: notes of 250 ms with harmonics, distinct per seed. */
    private fun music(seed: Int, seconds: Double): FloatArray {
        val random = java.util.Random(seed.toLong())
        val notes = IntArray((seconds * 4).toInt() + 1) { 48 + random.nextInt(30) }
        return FloatArray((seconds * rate).toInt()) { index ->
            val time = index.toDouble() / rate
            val note = notes[(time * 4).toInt()]
            val frequency = 440.0 * Math.pow(2.0, (note - 69) / 12.0)
            val envelope = 1.0 - (time * 4 % 1.0) * 0.6
            ((sin(2 * PI * frequency * time) * 0.5 + sin(4 * PI * frequency * time) * 0.25 +
                sin(6 * PI * frequency * time) * 0.12) * envelope * 0.5).toFloat()
        }
    }

    private fun noise(seed: Int, samples: Int, level: Float = 0.02f): FloatArray {
        val random = java.util.Random(seed.toLong())
        return FloatArray(samples) { (random.nextGaussian() * level).toFloat() }
    }

    private fun episode(leadIn: FloatArray, theme: FloatArray, after: FloatArray): List<AudioPrint> {
        val samples = leadIn + theme + after
        val token = EpisodeAudioTap.start()
        // Deliver stereo 16-bit PCM in decoder-sized buffers with their media time.
        val frames = 1_024
        var position = 0
        while (position < samples.size) {
            val count = minOf(frames, samples.size - position)
            val buffer = ByteBuffer.allocate(count * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (index in 0 until count) {
                val value = (samples[position + index].coerceIn(-1f, 1f) * 32_000).toInt().toShort()
                buffer.putShort(value)
                buffer.putShort(value)
            }
            buffer.flip()
            EpisodeAudioTap.accept(buffer, position * 1_000_000L / rate, C.ENCODING_PCM_16BIT, 2, rate)
            position += count
        }
        val prints = EpisodeAudioTap.drain()
        EpisodeAudioTap.stop(token)
        return prints
    }

    @Test
    fun findsThemeMusicDespiteSubWindowPhaseShift() {
        val theme = music(1, 60.0)
        // Episode A: 20 s scene, theme, other scene. Episode B: 31.0137 s scene (phase shift), theme, other scene.
        val first = episode(music(10, 20.0), theme, music(11, 30.0))
        val second = episode(music(20, 31.0137), theme, music(21, 30.0))
        assertTrue("prints were produced", first.size > 1_000 && second.size > 1_000)

        val range = findRepeatedAudioOpening(second, first)
        assertNotNull("theme was not found", range)
        range!!
        assertTrue("start ${range.first}", range.first in 30_000L..33_000L)
        assertTrue("end ${range.last}", range.last in 89_000L..92_000L)

        // A learned theme is recognised early in a third episode with yet another phase.
        val learned = first.filter { it.timeMs in 20_000L..50_000L }
        val third = episode(noise(30, (12.0071 * rate).toInt(), 0.05f), music(1, 15.0), FloatArray(0))
        val confirmed = confirmKnownAudioOpening(third, learned, 20_000L until 80_000L)
        assertNotNull("learned theme not confirmed", confirmed)
        assertTrue("confirmed start ${confirmed!!.first}", confirmed.first in 11_000L..13_000L)
    }
}
