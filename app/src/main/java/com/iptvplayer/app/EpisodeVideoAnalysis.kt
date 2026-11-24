package com.iptvplayer.app

import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal data class EpisodeFrame(val timeMs: Long, val fingerprint: Long, val creditsText: Boolean)

internal data class EpisodeVideoAnalysis(
    val opening: List<EpisodeFrame> = emptyList(),
    val creditsStartMs: Long? = null
)

internal data class LearnedOpening(val range: LongRange, val frames: List<EpisodeFrame>) {
    fun encode(): String = "${range.first},${range.last};" +
        frames.joinToString(";") { "${it.timeMs},${it.fingerprint}" }
}

internal data class OpeningReference(val mediaId: String, val frames: List<EpisodeFrame>) {
    fun encode(): String = "$mediaId|" + frames.take(180).joinToString(";") {
        "${it.timeMs},${it.fingerprint}"
    }
}

internal fun decodeOpeningReference(encoded: String?): OpeningReference? {
    val value = encoded ?: return null
    val separator = value.indexOf('|')
    if (separator <= 0) return null
    val mediaId = value.substring(0, separator)
    val frames = value.substring(separator + 1).split(';').take(180).mapNotNull { row ->
        val columns = row.split(',').mapNotNull(String::toLongOrNull)
        if (columns.size != 2 || columns[0] !in 0..180_000) null
        else EpisodeFrame(columns[0], columns[1], false)
    }
    return frames.takeIf { it.size >= 4 }?.let { OpeningReference(mediaId, it) }
}

internal fun decodeLearnedOpening(encoded: String?): LearnedOpening? {
    val rows = encoded?.split(';') ?: return null
    val bounds = rows.first().split(',').mapNotNull(String::toLongOrNull)
    if (bounds.size != 2 || bounds[0] < 0 || bounds[1] <= bounds[0] || bounds[1] > 180_000) return null
    val frames = rows.drop(1).take(7).mapNotNull { row ->
        val columns = row.split(',').mapNotNull(String::toLongOrNull)
        if (columns.size != 2 || columns[0] !in bounds[0]..bounds[1]) null
        else EpisodeFrame(columns[0], columns[1], false)
    }
    return frames.takeIf { it.size >= 4 }?.let { LearnedOpening(bounds[0]..bounds[1], it) }
}

private const val FRAME_INTERVAL_MS = 3_000L
internal const val HLS_INTRO_FRAME_INTERVAL_MS = 1_000L

internal suspend fun sampleEpisodeSurface(surface: SurfaceView, timeMs: Long): EpisodeFrame? {
    if (!surface.holder.surface.isValid) return null
    return suspendCoroutine { continuation ->
        val bitmap = Bitmap.createBitmap(72, 40, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(surface, bitmap, { result ->
                val frame = if (result == PixelCopy.SUCCESS) {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    episodeFrame(timeMs, pixels, bitmap.width, bitmap.height)
                } else null
                bitmap.recycle()
                continuation.resume(frame)
            }, Handler(Looper.getMainLooper()))
        } catch (_: IllegalArgumentException) {
            bitmap.recycle()
            continuation.resume(null)
        }
    }
}

internal fun confirmKnownOpening(
    current: List<EpisodeFrame>, reference: List<EpisodeFrame>, intro: LongRange
): LongRange? {
    val prefix = reference.filter { it.timeMs in intro }.take(7)
    if (prefix.size < 4) return null
    val newest = current.lastOrNull()?.timeMs ?: return null
    val recent = current.takeLast(14).associateBy { it.timeMs }
    val offset = newest - prefix.last().timeMs
    if (intro.first + offset < 0) return null
    val matches = prefix.mapNotNull { referenceFrame ->
        recent[referenceFrame.timeMs + offset]?.takeIf {
            java.lang.Long.bitCount(it.fingerprint xor referenceFrame.fingerprint) <= 10
        }
    }
    if (matches.size >= maxOf(4, prefix.size - 1) &&
        matches.map(EpisodeFrame::fingerprint).distinct().size >= 3
    ) return (intro.first + offset)..(intro.last + offset)
    return null
}

internal fun analyzeEpisodeVideo(
    uri: Uri,
    durationMs: Long,
    scanOpening: Boolean,
    scanCredits: Boolean
)= channelFlow {
    if (Build.VERSION.SDK_INT < 27 || uri.scheme !in listOf("http", "https") ||
        uri.lastPathSegment.orEmpty().endsWith(".m3u8", ignoreCase = true)) return@channelFlow
    withTimeoutOrNull(25_000) { runInterruptible(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(uri.toString(), emptyMap())
            fun frame(timeMs: Long): EpisodeFrame? {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                val bitmap = retriever.getScaledFrameAtTime(
                    timeMs * 1_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 72, 40
                ) ?: return null
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    return episodeFrame(timeMs, pixels, bitmap.width, bitmap.height)
                } finally {
                    bitmap.recycle()
                }
            }
            val opening = mutableListOf<EpisodeFrame>()
            if (scanOpening) {
                for (timeMs in 0L until minOf(180_000L, durationMs / 3) step FRAME_INTERVAL_MS) {
                    frame(timeMs)?.let {
                        opening += it
                        if (opening.size >= 7) trySend(EpisodeVideoAnalysis(opening.toList()))
                    }
                }
            }
            if (scanCredits && durationMs >= 300_000) {
                val credits = (maxOf(durationMs * 3 / 4, durationMs - 240_000) until durationMs - 2_000 step 5_000)
                    .mapNotNull(::frame)
                findCreditsStart(credits)?.let { trySend(EpisodeVideoAnalysis(creditsStartMs = it)) }
            }
        } catch (_: RuntimeException) {
            // Playback continues when the platform extractor cannot read this stream.
        } finally {
            runCatching { retriever.release() }
        }
    } }
}

private fun episodeFrame(timeMs: Long, pixels: IntArray, width: Int, height: Int): EpisodeFrame {
    fun luminance(x: Int, y: Int): Int {
        val pixel = pixels[y * width + x]
        return ((pixel shr 16 and 255) * 3 + (pixel shr 8 and 255) * 6 + (pixel and 255)) / 10
    }
    var fingerprint = 0L
    for (y in 0..7) for (x in 0..7) {
        val left = luminance((x + 1) * width / 10, (y + 1) * height / 9)
        val right = luminance((x + 2) * width / 10, (y + 1) * height / 9)
        if (left > right) fingerprint = fingerprint or (1L shl (y * 8 + x))
    }
    var dark = 0
    var textRows = 0
    for (y in 0 until height) {
        var bright = 0
        for (x in 0 until width) {
            val level = luminance(x, y)
            if (level < 35) dark++
            if (level >= 130) bright++
        }
        if (bright in 3..(width / 2)) textRows++
    }
    return EpisodeFrame(timeMs, fingerprint, dark * 100 >= pixels.size * 80 && textRows >= 3)
}

internal fun findCreditsStart(frames: List<EpisodeFrame>): Long? {
    // Credits often end with a bright logo or a short black transition. Looking
    // only at the final frames therefore misses an otherwise stable credit roll.
    // Require a sustained sequence to avoid treating one dark scene as the outro.
    val candidates = frames.windowed(3).mapIndexedNotNull { index, run ->
        if (run.all(EpisodeFrame::creditsText) && run.last().timeMs - run.first().timeMs >= 10_000L) {
            index to run.first().timeMs
        } else null
    }
    // Prefer the final sustained text segment. Permit one closing transition/logo
    // frame after it, but reject text that is followed by ordinary episode footage.
    return candidates.lastOrNull { (index, _) -> frames.size - (index + 3) <= 1 }?.second
}

internal fun findRepeatedOpening(
    current: List<EpisodeFrame>, previous: List<EpisodeFrame>, stepMs: Long = FRAME_INTERVAL_MS
): LongRange? {
    var best: LongRange? = null
    for (start in current.indices) for (reference in previous.indices) {
        var count = 0
        var matched = 0
        var lastMatch = 0
        val distinct = mutableSetOf<Long>()
        while (start + count < current.size && reference + count < previous.size) {
            val a = current[start + count]
            val b = previous[reference + count]
            if (a.timeMs - current[start].timeMs != count * stepMs ||
                b.timeMs - previous[reference].timeMs != count * stepMs) break
            if (java.lang.Long.bitCount(a.fingerprint xor b.fingerprint) <= 10) {
                distinct += a.fingerprint
                matched++
                lastMatch = count
            } else if (stepMs == FRAME_INTERVAL_MS || count == 0 || count - lastMatch > 2) break
            count++
        }
        // A boundary after the match is necessary to offer a safe, conservative skip.
        val minimumSpan = if (stepMs == HLS_INTRO_FRAME_INTERVAL_MS) 10_000 else 15_000
        if (lastMatch * stepMs < minimumSpan || matched * 4 < (lastMatch + 1) * 3 ||
            distinct.size < 5 || start + count >= current.size ||
            reference + count >= previous.size) continue
        val candidate = current[start].timeMs until current[start + lastMatch].timeMs
        if (candidate.last - candidate.first > (best?.let { it.last - it.first } ?: 0)) best = candidate
    }
    return best
}
