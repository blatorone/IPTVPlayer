package com.iptvplayer.app

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * A learned opening of a series. A series can have several: a long and a short
 * version, or a new opening from a later season. [range] is in the episode the
 * opening was learned from; frames and prints are its image and theme fingerprints.
 */
internal data class IntroVariant(
    val range: LongRange,
    val frames: List<EpisodeFrame>,
    val prints: List<AudioPrint>,
    val lastUsed: Long
)

internal const val MAX_INTRO_VARIANTS = 4
private const val VARIANT_FORMAT = 1

/** Margin by which another opening must continue past a shorter one to rule the shorter one out. */
private const val REFUTE_MARGIN_MS = 2_000L

internal fun encodeIntroVariants(variants: List<IntroVariant>): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { output ->
        output.writeInt(VARIANT_FORMAT)
        output.writeInt(variants.size)
        for (variant in variants) {
            output.writeLong(variant.range.first)
            output.writeLong(variant.range.last)
            output.writeLong(variant.lastUsed)
            val video = LearnedOpening(variant.range, variant.frames).encode().encodeToByteArray()
            output.writeInt(video.size)
            output.write(video)
            val audio = encodeAudioPrints(variant.prints)
            output.writeInt(audio.size)
            output.write(audio)
        }
    }
    return bytes.toByteArray()
}

internal fun decodeIntroVariants(data: ByteArray?): List<IntroVariant> {
    if (data == null) return emptyList()
    return runCatching {
        DataInputStream(data.inputStream()).use { input ->
            if (input.readInt() != VARIANT_FORMAT) return emptyList()
            val count = input.readInt()
            if (count !in 0..MAX_INTRO_VARIANTS) return emptyList()
            List(count) {
                val first = input.readLong()
                val last = input.readLong()
                val lastUsed = input.readLong()
                val video = ByteArray(input.readInt().also { require(it in 0..2_000_000) }).also(input::readFully)
                val audio = ByteArray(input.readInt().also { require(it in 0..2_000_000) }).also(input::readFully)
                require(first >= 0 && last > first && last <= OPENING_SCAN_LIMIT_MS)
                IntroVariant(
                    first..last,
                    decodeLearnedOpening(video.decodeToString())?.frames.orEmpty(),
                    decodeAudioPrints(audio),
                    lastUsed
                )
            }
        }
    }.getOrDefault(emptyList()).filter { it.frames.size >= 4 || it.prints.count(AudioPrint::loud) >= 40 }
}

/**
 * Chooses the skip range from the openings recognised so far. Openings the episode
 * has left are ignored. While several remain possible, the earliest end is used so a
 * skip never lands inside the episode; a shorter opening is ruled out once a longer
 * one keeps matching beyond its end.
 */
internal fun chooseIntro(observations: List<OpeningObservation>): LongRange? {
    val consistent = observations.filterNot(OpeningObservation::diverged)
    val alive = consistent.filter { candidate ->
        consistent.none { other ->
            other !== candidate && other.range.last > candidate.range.last + REFUTE_MARGIN_MS &&
                (other.lastMatchMs ?: Long.MIN_VALUE) > candidate.range.last + REFUTE_MARGIN_MS
        }
    }
    if (alive.isEmpty()) return null
    val end = alive.minOf { it.range.last }
    val start = alive.filter { it.range.last >= end }.minOf { it.range.first }
    return start..end
}

/**
 * Stores the opening found by comparing two episodes. If it is a known opening that
 * this episode still followed, that variant is replaced by the longer, current one;
 * otherwise it becomes a new variant, dropping the least recently used beyond the limit.
 */
internal fun learnIntroVariant(
    variants: List<IntroVariant>,
    observations: List<Pair<IntroVariant, OpeningObservation>>,
    found: LongRange,
    frames: List<EpisodeFrame>,
    prints: List<AudioPrint>,
    now: Long
): List<IntroVariant> {
    val learned = IntroVariant(
        found,
        frames.filter { it.timeMs in found }.take(120),
        prints.filter { it.timeMs in found.first..minOf(found.last, found.first + 30_000L) },
        now
    )
    if (learned.frames.size < 4 && learned.prints.count(AudioPrint::loud) < 40) return variants
    // The same opening: this episode still followed it, it starts at the same place and the
    // comparison did not end clearly earlier (a version that ends earlier makes the episode diverge).
    val same = observations.firstOrNull { (_, seen) ->
        !seen.diverged && seen.range.first <= found.last && found.first <= seen.range.last &&
            kotlin.math.abs(seen.range.first - found.first) <= 10_000L && found.last >= seen.range.last - 15_000L
    }?.first?.let { known -> variants.firstOrNull { it === known } }
    val updated = if (same != null) {
        if (found.last - found.first >= same.range.last - same.range.first) variants - same + learned
        else variants - same + same.copy(lastUsed = now)
    } else variants + learned
    return updated.sortedByDescending(IntroVariant::lastUsed).take(MAX_INTRO_VARIANTS)
}

/** The closing part of an episode that is compared between episodes, counted back from its end. */
internal const val CLOSING_WINDOW_MS = 300_000L // the last 5 minutes (the opening search reaches further)

/** Footage after recurring end credits for at least this long counts as a post-credits scene. */
private const val POST_CREDITS_MIN_MS = 20_000L

internal data class ClosingDecision(
    /** Credits followed by more footage: offered as "skip credits", the next episode follows at the end. */
    val skip: LongRange?,
    /** Credits running until the end: the next episode is offered when they begin. */
    val creditsStart: Long?
)

/**
 * Interprets the end credits found in the closing window, whose time 0 is
 * `duration - CLOSING_WINDOW_MS` (or the episode start for short episodes).
 */
internal fun closingDecision(range: LongRange, windowStart: Long, duration: Long): ClosingDecision {
    val start = windowStart + range.first
    val end = windowStart + range.last + 1
    return if (duration - end >= POST_CREDITS_MIN_MS) ClosingDecision(start until end, null)
    else ClosingDecision(null, start)
}
