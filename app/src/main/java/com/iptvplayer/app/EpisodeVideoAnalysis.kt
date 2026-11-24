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

/**
 * A sampled video frame. [fingerprint] is a block-averaged difference hash,
 * [averageFingerprint] a block-average hash. [detail] is false for black,
 * white or otherwise flat frames, which match nearly everything and must not
 * be used as evidence for a repeated opening.
 */
internal data class EpisodeFrame(
    val timeMs: Long,
    val fingerprint: Long,
    val creditsText: Boolean,
    val averageFingerprint: Long = fingerprint,
    val detail: Boolean = true
)

internal data class EpisodeVideoAnalysis(
    val opening: List<EpisodeFrame> = emptyList(),
    val creditsStartMs: Long? = null,
    val audio: List<AudioPrint> = emptyList()
)

/** Openings are searched within the first minutes; long cold opens are common. */
internal const val OPENING_SCAN_LIMIT_MS = 300_000L
private const val MAX_REFERENCE_FRAMES = 300
private const val MAX_LEARNED_FRAMES = 120
private const val SAMPLE_WIDTH = 144
private const val SAMPLE_HEIGHT = 80

internal data class LearnedOpening(val range: LongRange, val frames: List<EpisodeFrame>) {
    fun encode(): String = "${range.first},${range.last};" +
        frames.take(MAX_LEARNED_FRAMES).joinToString(";", transform = ::encodeFrame)
}

internal data class OpeningReference(val mediaId: String, val frames: List<EpisodeFrame>) {
    fun encode(): String = "$mediaId|" + frames.take(MAX_REFERENCE_FRAMES).joinToString(";", transform = ::encodeFrame)
}

private fun encodeFrame(frame: EpisodeFrame): String =
    "${frame.timeMs},${frame.fingerprint},${frame.averageFingerprint},${if (frame.detail) 1 else 0}"

private fun decodeFrame(row: String): EpisodeFrame? {
    val columns = row.split(',').mapNotNull(String::toLongOrNull)
    if (columns.size !in 2..4) return null
    return EpisodeFrame(
        columns[0], columns[1], false, columns.getOrElse(2) { columns[1] }, columns.getOrElse(3) { 1L } != 0L
    )
}

internal fun decodeOpeningReference(encoded: String?): OpeningReference? {
    val value = encoded ?: return null
    val separator = value.indexOf('|')
    if (separator <= 0) return null
    val mediaId = value.substring(0, separator)
    val frames = value.substring(separator + 1).split(';').take(MAX_REFERENCE_FRAMES).mapNotNull { row ->
        decodeFrame(row)?.takeIf { it.timeMs in 0..OPENING_SCAN_LIMIT_MS }
    }
    return frames.takeIf { it.size >= 4 }?.let { OpeningReference(mediaId, it) }
}

internal fun decodeLearnedOpening(encoded: String?): LearnedOpening? {
    val rows = encoded?.split(';') ?: return null
    val bounds = rows.first().split(',').mapNotNull(String::toLongOrNull)
    if (bounds.size != 2 || bounds[0] < 0 || bounds[1] <= bounds[0] || bounds[1] > OPENING_SCAN_LIMIT_MS) return null
    val frames = rows.drop(1).take(MAX_LEARNED_FRAMES).mapNotNull { row ->
        decodeFrame(row)?.takeIf { it.timeMs in bounds[0]..bounds[1] }
    }
    return frames.takeIf { it.size >= 4 }?.let { LearnedOpening(bounds[0]..bounds[1], it) }
}

internal const val FRAME_INTERVAL_MS = 3_000L
internal const val HLS_INTRO_FRAME_INTERVAL_MS = 1_000L

internal suspend fun sampleEpisodeSurface(surface: SurfaceView, timeMs: Long): EpisodeFrame? {
    if (!surface.holder.surface.isValid) return null
    return suspendCoroutine { continuation ->
        val bitmap = Bitmap.createBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT, Bitmap.Config.ARGB_8888)
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

/** How the current episode relates to a learned opening. Times are in the current episode. */
internal data class OpeningObservation(
    val range: LongRange,
    /** The footage stopped matching before the learned opening ended (for example a short version). */
    val diverged: Boolean,
    /** Time of the last frame that still matched the learned opening. */
    val lastMatchMs: Long?
)

/**
 * Recognises a learned opening while it is still running and keeps checking it as
 * playback continues. Frames are aligned by offset voting with a tolerance of one
 * sample, so dropped samples, buffering and phase differences do not break the match;
 * a clear majority of the already played frames must match. A sustained run of
 * differing frames inside the learned range marks the opening as diverged.
 */
internal fun observeKnownOpening(
    current: List<EpisodeFrame>,
    reference: List<EpisodeFrame>,
    intro: LongRange,
    stepMs: Long = HLS_INTRO_FRAME_INTERVAL_MS
): OpeningObservation? {
    val prefix = reference.filter { it.timeMs in intro && it.detail }.sortedBy(EpisodeFrame::timeMs)
    if (prefix.size < 4 || current.isEmpty()) return null
    val sortedCurrent = current.sortedBy(EpisodeFrame::timeMs)
    val currentEnd = sortedCurrent.last().timeMs
    for (offset in bestOffsets(sortedCurrent, prefix, stepMs)) {
        val start = intro.first - offset
        if (start < 0) continue
        var observed = 0
        var run = 0
        var diverged = false
        var lastMatch: Long? = null
        val matched = mutableListOf<EpisodeFrame>()
        for (referenceFrame in prefix) {
            val target = referenceFrame.timeMs - offset
            if (target > currentEnd) break
            val near = framesNear(sortedCurrent, target, stepMs).filter(EpisodeFrame::detail)
            if (near.isEmpty()) continue
            observed++
            if (near.any { sameEpisodeFrame(it, referenceFrame) }) {
                matched += referenceFrame
                lastMatch = target
                run = 0
            } else if (++run >= OPENING_END_MISMATCHES) {
                diverged = true
                break
            }
        }
        // The majority rule applies to the part before a divergence.
        val considered = observed - run
        if (matched.size >= 5 && matched.size * 10 >= considered * 7 && distinctScenes(matched) >= 3) {
            return OpeningObservation(start..(intro.last - offset), diverged, lastMatch)
        }
    }
    return null
}

/** A learned opening recognised early in the current episode, unless the episode left it. */
internal fun confirmKnownOpening(
    current: List<EpisodeFrame>,
    reference: List<EpisodeFrame>,
    intro: LongRange,
    stepMs: Long = HLS_INTRO_FRAME_INTERVAL_MS
): LongRange? = observeKnownOpening(current, reference, intro, stepMs)?.takeUnless { it.diverged }?.range

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
                    timeMs * 1_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, SAMPLE_WIDTH, SAMPLE_HEIGHT
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
                for (timeMs in 0L until minOf(OPENING_SCAN_LIMIT_MS, durationMs / 3) step FRAME_INTERVAL_MS) {
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

internal fun episodeFrame(timeMs: Long, pixels: IntArray, width: Int, height: Int): EpisodeFrame {
    val luma = IntArray(width * height) { index ->
        val pixel = pixels[index]
        ((pixel shr 16 and 255) * 3 + (pixel shr 8 and 255) * 6 + (pixel and 255)) / 10
    }
    // Area means are far less sensitive to compression noise and scaling than
    // single pixels, which matters when variants differ in bitrate or resolution.
    fun blockMean(column: Int, columns: Int, row: Int, rows: Int): Int {
        val x0 = column * width / columns
        val x1 = maxOf(x0 + 1, (column + 1) * width / columns).coerceAtMost(width)
        val y0 = row * height / rows
        val y1 = maxOf(y0 + 1, (row + 1) * height / rows).coerceAtMost(height)
        var total = 0L
        for (y in y0 until y1) for (x in x0 until x1) total += luma[y * width + x]
        return (total / ((x1 - x0) * (y1 - y0)).coerceAtLeast(1)).toInt()
    }
    var fingerprint = 0L
    val differenceGrid = IntArray(72)
    for (row in 0 until 8) for (column in 0 until 9) differenceGrid[row * 9 + column] = blockMean(column, 9, row, 8)
    for (row in 0 until 8) for (column in 0 until 8) {
        if (differenceGrid[row * 9 + column] > differenceGrid[row * 9 + column + 1]) {
            fingerprint = fingerprint or (1L shl (row * 8 + column))
        }
    }
    val grid = IntArray(64)
    for (row in 0 until 8) for (column in 0 until 8) grid[row * 8 + column] = blockMean(column, 8, row, 8)
    val average = grid.average()
    var averageFingerprint = 0L
    grid.forEachIndexed { index, value ->
        if (value > average) averageFingerprint = averageFingerprint or (1L shl index)
    }
    val detail = grid.max() - grid.min() >= 18

    var dark = 0
    var textRows = 0
    val minimumBright = maxOf(3, width / 24)
    for (y in 0 until height) {
        var bright = 0
        for (x in 0 until width) {
            val level = luma[y * width + x]
            if (level < 35) dark++
            if (level >= 130) bright++
        }
        if (bright in minimumBright..(width / 2)) textRows++
    }
    return EpisodeFrame(
        timeMs, fingerprint, dark * 100 >= luma.size * 80 && textRows >= maxOf(3, height * 3 / 40),
        averageFingerprint, detail
    )
}

private fun sameEpisodeFrame(first: EpisodeFrame, second: EpisodeFrame): Boolean {
    val differenceHashDistance = java.lang.Long.bitCount(first.fingerprint xor second.fingerprint)
    if (differenceHashDistance <= 10) return true
    // A block-average hash is less sensitive to compression, scaling, and overlays.
    val averageHashDistance = java.lang.Long.bitCount(first.averageFingerprint xor second.averageFingerprint)
    return differenceHashDistance <= 22 && averageHashDistance <= 8
}

/** Counts visually different scenes so a single static image is not mistaken for an opening. */
private fun distinctScenes(frames: List<EpisodeFrame>): Int {
    val scenes = mutableListOf<EpisodeFrame>()
    for (frame in frames) {
        if (scenes.none { java.lang.Long.bitCount(it.fingerprint xor frame.fingerprint) <= 6 &&
                java.lang.Long.bitCount(it.averageFingerprint xor frame.averageFingerprint) <= 6 }
        ) scenes += frame
    }
    return scenes.size
}

/** Frames of [sorted] within [toleranceMs] of [targetMs]. */
private fun framesNear(sorted: List<EpisodeFrame>, targetMs: Long, toleranceMs: Long): List<EpisodeFrame> {
    var low = 0
    var high = sorted.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (sorted[middle].timeMs < targetMs - toleranceMs) low = middle + 1 else high = middle
    }
    val result = mutableListOf<EpisodeFrame>()
    var index = low
    while (index < sorted.size && sorted[index].timeMs <= targetMs + toleranceMs) result += sorted[index++]
    return result
}

/**
 * Votes for time offsets (reference time minus current time) at which detailed
 * frames match. Neighbouring offsets support each other to absorb the sampling
 * phase difference between two playbacks.
 */
private fun bestOffsets(
    current: List<EpisodeFrame>, reference: List<EpisodeFrame>, stepMs: Long, limit: Int = 4
): List<Long> {
    val votes = HashMap<Long, Int>()
    val references = reference.filter(EpisodeFrame::detail)
    for (frame in current) {
        if (!frame.detail) continue
        for (candidate in references) if (sameEpisodeFrame(frame, candidate)) {
            val offset = Math.round((candidate.timeMs - frame.timeMs).toDouble() / stepMs) * stepMs
            votes[offset] = (votes[offset] ?: 0) + 1
        }
    }
    return votes.keys.map { offset ->
        offset to votes.getValue(offset) + (votes[offset - stepMs] ?: 0) + (votes[offset + stepMs] ?: 0)
    }.filter { it.second >= 4 }
        .sortedWith(compareByDescending<Pair<Long, Int>> { it.second }.thenByDescending { votes[it.first] ?: 0 })
        .map { it.first }
        .take(limit)
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

/**
 * Finds a sequence of the current episode that also appears in a previous episode
 * of the same series. The sequence must be followed by footage that differs from
 * the previous episode, so its end is known and a skip never cuts into content.
 */
internal fun findRepeatedOpening(
    current: List<EpisodeFrame>, previous: List<EpisodeFrame>, stepMs: Long = FRAME_INTERVAL_MS
): LongRange? {
    val minimumSpan = if (stepMs == HLS_INTRO_FRAME_INTERVAL_MS) 10_000L else 15_000L
    // Larger holes mean sampling stopped (pause, seek or buffering); the run is then unknown.
    val maximumHole = maxOf(8_000L, stepMs * 3)
    val sortedCurrent = current.sortedBy(EpisodeFrame::timeMs)
    val sortedPrevious = previous.sortedBy(EpisodeFrame::timeMs)
    var best: LongRange? = null
    for (offset in bestOffsets(sortedCurrent, sortedPrevious, stepMs)) {
        var first: EpisodeFrame? = null
        var lastMatch: EpisodeFrame? = null
        var firstMismatchAfter: EpisodeFrame? = null
        val matched = mutableListOf<EpisodeFrame>()
        var mismatches = 0
        var trailingMismatches = 0
        var lastTime: Long? = null

        fun close(completed: Boolean) {
            val start = first
            val end = lastMatch
            val inner = mismatches - trailingMismatches
            if (completed && start != null && end != null && end.timeMs - start.timeMs >= minimumSpan &&
                matched.size * 10 >= (matched.size + inner) * 6 && distinctScenes(matched) >= 5
            ) {
                // The real boundary lies between the last match and the first differing frame.
                val boundary = firstMismatchAfter?.let { (end.timeMs + it.timeMs) / 2 } ?: end.timeMs
                val candidate = start.timeMs until boundary
                if (candidate.last - candidate.first > (best?.let { it.last - it.first } ?: 0)) best = candidate
            }
            first = null
            lastMatch = null
            firstMismatchAfter = null
            matched.clear()
            mismatches = 0
            trailingMismatches = 0
        }

        for (frame in sortedCurrent) {
            if (lastTime != null && frame.timeMs - lastTime > maximumHole) close(completed = false)
            lastTime = frame.timeMs
            val counterparts = framesNear(sortedPrevious, frame.timeMs + offset, stepMs)
            if (counterparts.isEmpty()) {
                // The previous episode was not sampled here; nothing is known about the end.
                if (first != null) close(completed = false)
                continue
            }
            if (!frame.detail) continue
            if (counterparts.any { it.detail && sameEpisodeFrame(frame, it) }) {
                if (first == null) first = frame
                lastMatch = frame
                matched += frame
                firstMismatchAfter = null
                trailingMismatches = 0
            } else if (first != null) {
                if (firstMismatchAfter == null) firstMismatchAfter = frame
                mismatches++
                trailingMismatches++
                if (trailingMismatches >= OPENING_END_MISMATCHES) close(completed = true)
            }
        }
        // The end is only certain after several differing frames; fast cuts inside an opening
        // briefly differ and must not end it early.
        close(completed = trailingMismatches >= OPENING_END_MISMATCHES)
    }
    return best
}

/** Consecutive differing 1 s samples after which a repeated opening is considered over. */
internal const val OPENING_END_MISMATCHES = 6

/** Combines the image and audio results; overlapping ranges describe the same opening. */
internal fun mergeOpeningRanges(video: LongRange?, audio: LongRange?): LongRange? = when {
    video == null -> audio
    audio == null -> video
    video.first <= audio.last && audio.first <= video.last ->
        minOf(video.first, audio.first)..maxOf(video.last, audio.last)
    audio.last - audio.first > video.last - video.first -> audio
    else -> video
}
