package com.iptvplayer.app

import android.content.Context
import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.sqrt

/*
 * "Fließender Portalwechsel" (experimental, off by default). Stage 1 measures: the loudness of the
 * played sound is kept (10 values per second, last 60 s). After a switch to another portal, the
 * sound before and after the switch is compared to find by how many seconds the portals are apart.
 * The result goes to the diagnostic report; stage 2 will use it to join both streams at the same spot.
 */

private const val SMOOTH_SWITCH = "smooth_switch_enabled"
private const val STEP_MS = 100L
private const val HISTORY = 600

internal fun smoothSwitchEnabled(context: Context): Boolean =
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).getBoolean(SMOOTH_SWITCH, false)

internal fun setSmoothSwitchEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit().putBoolean(SMOOTH_SWITCH, enabled).apply()
    SmoothSwitch.enabled = enabled
}

/** Weak devices (little memory) get a hint; the option stays selectable at one's own risk. */
internal fun weakDevice(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager ?: return false
    val memory = android.app.ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
    return manager.isLowRamDevice || memory.totalMem < 2_500L * 1024 * 1024
}

internal object SmoothSwitch {
    @Volatile var enabled = false
    private val times = LongArray(HISTORY)
    private val values = DoubleArray(HISTORY)
    private var count = 0
    private var next = 0
    private var bucketStart = Long.MIN_VALUE
    private var sum = 0.0
    private var samples = 0
    /** Moment of the last portal switch, while its measurement is pending. */
    @Volatile var switchedAt = Long.MIN_VALUE
        private set

    /** Played sound from the player's audio sink (playback thread). */
    fun accept(buffer: ByteBuffer, encoding: Int, channels: Int) {
        if (!enabled || channels <= 0) return
        val now = timeshiftNow()
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else if (encoding == C.ENCODING_PCM_16BIT) 2 else return
        val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val start = data.position()
        val step = bytesPerSample * channels * 8 // every 8th frame is enough for loudness
        var index = start
        synchronized(this) {
            if (bucketStart == Long.MIN_VALUE) bucketStart = now
            while (index + bytesPerSample <= data.limit()) {
                val sample = if (bytesPerSample == 4) data.getFloat(index).toDouble() else data.getShort(index) / 32_768.0
                sum += sample * sample
                samples++
                index += step
            }
            if (now - bucketStart >= STEP_MS && samples > 0) {
                times[next] = bucketStart
                values[next] = ln(1e-6 + sqrt(sum / samples))
                next = (next + 1) % HISTORY
                if (count < HISTORY) count++
                bucketStart = now
                sum = 0.0
                samples = 0
            }
        }
    }

    fun markSwitch() {
        if (enabled) switchedAt = timeshiftNow()
    }

    private fun range(from: Long, to: Long): List<Pair<Long, Double>> = synchronized(this) {
        (0 until count).map { (next - count + it + HISTORY) % HISTORY }
            .map { times[it] to values[it] }.filter { it.first in from until to }
    }

    /**
     * Compares the sound before the switch with the first seconds after it. Returns the shift in
     * ms (positive: the new portal is behind and repeats what was already played) and how well it
     * matched (0..1), or null when there is not enough sound.
     */
    fun measure(): Pair<Long, Double>? {
        val at = switchedAt.takeIf { it != Long.MIN_VALUE } ?: return null
        switchedAt = Long.MIN_VALUE
        val before = range(at - 45_000, at).map { it.second }
        val after = range(at + 1_000, at + 13_000).map { it.second }
        return bestLag(before, after)?.let { (lagSteps, score) -> lagSteps * STEP_MS to score }
    }
}

/**
 * Finds where [probe] fits best into [history] (both 10 values per second), by normalised
 * correlation. Returns how many steps before the end of the history the probe starts, and the score.
 */
internal fun bestLag(history: List<Double>, probe: List<Double>): Pair<Int, Double>? {
    // The new portal may be only a few seconds behind: then only the start of the probe overlaps
    // the end of the history. At least 3 s must overlap.
    val minOverlap = 30
    if (probe.size < minOverlap || history.size < minOverlap) return null
    fun normalise(values: List<Double>): DoubleArray {
        val mean = values.average()
        val deviation = sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size).takeIf { it > 1e-9 } ?: return DoubleArray(0)
        return DoubleArray(values.size) { (values[it] - mean) / deviation }
    }
    var best = -2.0
    var bestWeighted = -2.0
    var bestStart = -1
    for (start in 0..history.size - minOverlap) {
        val overlap = minOf(probe.size, history.size - start)
        val a = normalise(history.subList(start, start + overlap)).takeIf { it.isNotEmpty() } ?: continue
        val b = normalise(probe.subList(0, overlap)).takeIf { it.isNotEmpty() } ?: continue
        var score = 0.0
        for (i in 0 until overlap) score += a[i] * b[i]
        score /= overlap
        // Short overlaps match by chance more easily: they count a little less.
        val weighted = score * sqrt(overlap.toDouble() / probe.size)
        if (weighted > bestWeighted) { bestWeighted = weighted; best = score; bestStart = start }
    }
    if (bestStart < 0) return null
    return (history.size - bestStart) to best
}
