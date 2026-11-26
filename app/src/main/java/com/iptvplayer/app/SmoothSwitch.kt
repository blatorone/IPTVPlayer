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

    // Picture brightness, 5 values per second, last 60 s: used when the sound is not available
    // to the app (Dolby passed through to the TV or receiver).
    private val videoTimes = LongArray(300)
    private val videoValues = DoubleArray(300)
    private var videoCount = 0
    private var videoNext = 0

    fun acceptVideo(brightness: Double) {
        if (!enabled) return
        synchronized(this) {
            videoTimes[videoNext] = timeshiftNow()
            videoValues[videoNext] = brightness
            videoNext = (videoNext + 1) % videoTimes.size
            if (videoCount < videoTimes.size) videoCount++
        }
    }

    private fun videoRange(from: Long, to: Long): List<Double> = synchronized(this) {
        (0 until videoCount).map { (videoNext - videoCount + it + videoTimes.size) % videoTimes.size }
            .filter { videoTimes[it] in from until to }.map { videoValues[it] }
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
    fun measure(): Measurement? {
        val at = switchedAt.takeIf { it != Long.MIN_VALUE } ?: return null
        switchedAt = Long.MIN_VALUE
        val before = range(at - 45_000, at).map { it.second }
        val after = range(at + 1_000, at + 13_000).map { it.second }
        bestLag(before, after)?.let { (steps, score) -> return Measurement("audio", steps * STEP_MS, score) }
        // No sound to compare (e.g. Dolby passthrough): the picture brightness instead.
        val videoBefore = videoRange(at - 45_000, at)
        val videoAfter = videoRange(at + 1_000, at + 13_000)
        return bestLag(videoBefore, videoAfter, minOverlap = 20)?.let { (steps, score) -> Measurement("video", steps * 200L, score) }
    }
}

internal data class Measurement(val source: String, val lagMs: Long, val score: Double)


/**
 * Finds where [probe] fits best into [history] (both 10 values per second), by normalised
 * correlation. Returns how many steps before the end of the history the probe starts, and the score.
 */
internal fun bestLag(history: List<Double>, probe: List<Double>, minOverlap: Int = 30): Pair<Int, Double>? {
    // The new portal may be only a few seconds behind: then only the start of the probe overlaps
    // the end of the history. At least minOverlap values (3 s of sound, 4 s of picture) must overlap.
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

/**
 * Brightness of the current picture from a tiny 32 x 18 copy of [surface] (scaled on the GPU),
 * or null when the copy failed (e.g. protected video). Called on the main thread.
 */
internal suspend fun pictureBrightness(surface: android.view.SurfaceView): Double? {
    if (!surface.holder.surface.isValid) return null
    val bitmap = android.graphics.Bitmap.createBitmap(32, 18, android.graphics.Bitmap.Config.ARGB_8888)
    return try {
        val ok = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            android.view.PixelCopy.request(surface, bitmap, { result ->
                if (continuation.isActive) continuation.resumeWith(Result.success(result == android.view.PixelCopy.SUCCESS))
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
        if (!ok) null else {
            val pixels = IntArray(32 * 18).also { bitmap.getPixels(it, 0, 32, 0, 0, 32, 18) }
            pixels.sumOf { (0.299 * (it shr 16 and 0xFF) + 0.587 * (it shr 8 and 0xFF) + 0.114 * (it and 0xFF)) } / pixels.size
        }
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    } finally {
        bitmap.recycle()
    }
}
