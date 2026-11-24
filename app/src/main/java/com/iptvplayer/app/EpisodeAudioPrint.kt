package com.iptvplayer.app

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * A 32-bit spectral fingerprint of ~93 ms of decoded audio (window start in
 * media time). [loud] is false for silence or near silence, which carries no
 * information for matching an opening theme.
 */
internal data class AudioPrint(val timeMs: Long, val bits: Int, val loud: Boolean)

private const val AUDIO_WINDOW = 512
private const val AUDIO_HOP = 256
private const val AUDIO_TARGET_RATE = 5_512.5
private const val AUDIO_BANDS = 33
private const val MAX_QUEUED_PRINTS = 12_000

/**
 * Receives decoded PCM from the player's audio sink and turns it into
 * [AudioPrint]s. Only active while an episode opening is analysed, so live TV
 * and films do not pay for it. Passthrough audio (for example AC-3 sent to an
 * AV receiver) is never decoded by the app and therefore yields no prints; the
 * video fingerprints remain available in that case.
 */
internal object EpisodeAudioTap {
    @Volatile var active: Boolean = false
        private set
    @Volatile private var generation = 0
    private val queue = ConcurrentLinkedQueue<Pair<Int, AudioPrint>>()

    // Fields below are only touched on the playback thread.
    private var appliedGeneration = -1
    private var sampleRate = 0
    private var factor = 1
    private var decimatedRate = AUDIO_TARGET_RATE
    private var nextExpectedUs = Long.MIN_VALUE
    private var accumulator = 0.0
    private var accumulated = 0
    private val ring = DoubleArray(AUDIO_WINDOW)
    private var written = 0
    private var sinceHop = 0
    private var previousBands: DoubleArray? = null
    private var bandEdges = IntArray(AUDIO_BANDS + 1)
    private val window = DoubleArray(AUDIO_WINDOW) { 0.5 - 0.5 * cos(2 * PI * it / (AUDIO_WINDOW - 1)) }
    private val real = DoubleArray(AUDIO_WINDOW)
    private val imaginary = DoubleArray(AUDIO_WINDOW)

    // Counters for diagnostics: whether decoded audio reaches the analysis at all.
    @Volatile private var buffers = 0
    @Volatile private var pcmBuffers = 0
    @Volatile private var failures = 0
    @Volatile private var lastEncoding = C.ENCODING_INVALID
    @Volatile private var lastMediaMs = Long.MIN_VALUE

    fun noteBuffer(pcm: Boolean, encoding: Int, mediaTimeUs: Long) {
        buffers++
        if (pcm) pcmBuffers++
        lastEncoding = encoding
        lastMediaMs = mediaTimeUs / 1_000
    }

    fun noteFailure() {
        failures++
    }

    /** Counts and the last media time, without content, for the diagnostics log. */
    fun summary(): String =
        "b:$buffers pcm:$pcmBuffers e:$lastEncoding x:$failures t:${if (lastMediaMs == Long.MIN_VALUE) "-" else lastMediaMs / 1_000}"

    /** Starts a new capture and returns its token; prints of earlier captures are discarded. */
    @Synchronized
    fun start(): Int {
        generation++
        queue.clear()
        buffers = 0
        pcmBuffers = 0
        failures = 0
        lastMediaMs = Long.MIN_VALUE
        active = true
        return generation
    }

    /** Stops the capture identified by [token]; a newer capture keeps running. */
    @Synchronized
    fun stop(token: Int) {
        if (token != generation) return
        active = false
        generation++
        queue.clear()
    }

    /** Returns the prints captured since the last call for the current capture. */
    fun drain(): List<AudioPrint> {
        val current = generation
        val result = mutableListOf<AudioPrint>()
        while (true) {
            val (printGeneration, print) = queue.poll() ?: break
            if (printGeneration == current) result += print
        }
        return result
    }

    fun discontinuity() {
        nextExpectedUs = Long.MIN_VALUE
    }

    fun accept(buffer: ByteBuffer, mediaTimeUs: Long, encoding: Int, channels: Int, rate: Int) {
        if (!active || channels <= 0 || rate <= 0) return
        val currentGeneration = generation
        if (currentGeneration != appliedGeneration || rate != sampleRate) {
            appliedGeneration = currentGeneration
            configure(rate)
        }
        if (nextExpectedUs == Long.MIN_VALUE || abs(mediaTimeUs - nextExpectedUs) > 100_000) resetWindow()
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = bytesPerSample * channels
        val frames = data.remaining() / frameBytes
        val start = data.position()
        for (frame in 0 until frames) {
            var sum = 0.0
            for (channel in 0 until channels) {
                val index = start + frame * frameBytes + channel * bytesPerSample
                sum += if (bytesPerSample == 4) data.getFloat(index).toDouble() else data.getShort(index) / 32_768.0
            }
            accumulator += sum / channels
            if (++accumulated == factor) {
                push(accumulator / factor, mediaTimeUs + frame * 1_000_000L / rate, currentGeneration)
                accumulator = 0.0
                accumulated = 0
            }
        }
        nextExpectedUs = mediaTimeUs + frames * 1_000_000L / rate
    }

    private fun configure(rate: Int) {
        sampleRate = rate
        factor = maxOf(1, Math.round(rate / AUDIO_TARGET_RATE).toInt())
        decimatedRate = rate.toDouble() / factor
        val top = minOf(2_000.0, decimatedRate / 2 - 50)
        bandEdges = IntArray(AUDIO_BANDS + 1) { band ->
            val frequency = exp(ln(300.0) + (ln(top) - ln(300.0)) * band / AUDIO_BANDS)
            (frequency * AUDIO_WINDOW / decimatedRate).toInt().coerceIn(1, AUDIO_WINDOW / 2 - 1)
        }
        resetWindow()
    }

    private fun resetWindow() {
        written = 0
        sinceHop = 0
        accumulator = 0.0
        accumulated = 0
        previousBands = null
    }

    private fun push(sample: Double, timeUs: Long, printGeneration: Int) {
        ring[written % AUDIO_WINDOW] = sample
        written++
        if (written < AUDIO_WINDOW || ++sinceHop < AUDIO_HOP) return
        sinceHop = 0
        var energy = 0.0
        for (index in 0 until AUDIO_WINDOW) {
            val value = ring[(written + index) % AUDIO_WINDOW]
            energy += value * value
            real[index] = value * window[index]
            imaginary[index] = 0.0
        }
        fft(real, imaginary)
        val bands = DoubleArray(AUDIO_BANDS) { band ->
            var total = 1e-12
            for (bin in bandEdges[band] until maxOf(bandEdges[band] + 1, bandEdges[band + 1])) {
                total += real[bin] * real[bin] + imaginary[bin] * imaginary[bin]
            }
            total
        }
        val previous = previousBands
        previousBands = bands
        if (previous == null) return
        var bits = 0
        for (band in 0 until AUDIO_BANDS - 1) {
            val difference = (bands[band] - bands[band + 1]) - (previous[band] - previous[band + 1])
            if (difference > 0) bits = bits or (1 shl band)
        }
        val windowStartUs = timeUs - ((AUDIO_WINDOW - 1) * 1_000_000.0 / decimatedRate).roundToLong()
        if (queue.size < MAX_QUEUED_PRINTS) {
            queue.add(printGeneration to AudioPrint(windowStartUs / 1_000, bits, energy / AUDIO_WINDOW > 1e-5))
        }
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val size = re.size
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var length = 2
        while (length <= size) {
            val angle = -2 * PI / length
            val stepRe = cos(angle)
            val stepIm = sin(angle)
            var offset = 0
            while (offset < size) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until length / 2) {
                    val a = offset + k
                    val b = a + length / 2
                    val tRe = re[b] * wRe - im[b] * wIm
                    val tIm = re[b] * wIm + im[b] * wRe
                    re[b] = re[a] - tRe
                    im[b] = im[a] - tIm
                    re[a] += tRe
                    im[a] += tIm
                    val nextRe = wRe * stepRe - wIm * stepIm
                    wIm = wRe * stepIm + wIm * stepRe
                    wRe = nextRe
                }
                offset += length
            }
            length = length shl 1
        }
    }
}

/** Forwards everything to [sink] and hands decoded PCM with its media time to [EpisodeAudioTap]. */
@OptIn(UnstableApi::class)
internal class EpisodeAudioTapSink(sink: AudioSink) : ForwardingAudioSink(sink) {
    private var pcm = false
    private var encoding = C.ENCODING_INVALID
    private var channels = 0
    private var rate = 0
    private var streamOffsetUs = 0L
    private var lastBuffer: ByteBuffer? = null
    private var lastPresentationUs = Long.MIN_VALUE

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        encoding = inputFormat.pcmEncoding
        pcm = inputFormat.sampleMimeType == MimeTypes.AUDIO_RAW &&
            (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT)
        channels = inputFormat.channelCount
        rate = inputFormat.sampleRate
        EpisodeAudioTap.discontinuity()
        super.configure(inputFormat, specifiedBufferSize, outputChannels)
    }

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        streamOffsetUs = outputStreamOffsetUs
        super.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        // The sink receives the same buffer again until it is fully consumed; analyse it once.
        if (EpisodeAudioTap.active && (buffer !== lastBuffer || presentationTimeUs != lastPresentationUs)) {
            lastBuffer = buffer
            lastPresentationUs = presentationTimeUs
            EpisodeAudioTap.noteBuffer(pcm, encoding, presentationTimeUs - streamOffsetUs)
            if (pcm) try {
                EpisodeAudioTap.accept(buffer, presentationTimeUs - streamOffsetUs, encoding, channels, rate)
            } catch (_: RuntimeException) {
                EpisodeAudioTap.noteFailure()
            }
        }
        return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }

    override fun flush() {
        lastBuffer = null
        lastPresentationUs = Long.MIN_VALUE
        EpisodeAudioTap.discontinuity()
        super.flush()
    }
}

internal fun encodeAudioPrints(prints: List<AudioPrint>): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { output ->
        output.writeInt(prints.size)
        prints.forEach {
            output.writeInt(it.timeMs.toInt())
            output.writeInt(it.bits)
            output.writeBoolean(it.loud)
        }
    }
    return bytes.toByteArray()
}

internal fun decodeAudioPrints(data: ByteArray?): List<AudioPrint> {
    if (data == null || data.size < 4) return emptyList()
    return runCatching {
        DataInputStream(data.inputStream()).use { input ->
            val count = input.readInt()
            if (count !in 0..20_000) return emptyList()
            List(count) { AudioPrint(input.readInt().toLong(), input.readInt(), input.readBoolean()) }
        }
    }.getOrDefault(emptyList())
}

/** Audio prints of the opening minutes of one episode, used as reference for the next one. */
internal fun encodeAudioReference(mediaId: String, prints: List<AudioPrint>): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { output ->
        output.writeUTF(mediaId.take(2_000))
        output.write(encodeAudioPrints(prints))
    }
    return bytes.toByteArray()
}

internal fun decodeAudioReference(data: ByteArray?): Pair<String, List<AudioPrint>>? {
    if (data == null) return null
    return runCatching {
        DataInputStream(data.inputStream()).use { input ->
            val mediaId = input.readUTF()
            mediaId to decodeAudioPrints(input.readBytes())
        }
    }.getOrNull()?.takeIf { it.second.isNotEmpty() }
}

/** A learned theme: its range in the learning episode and the prints at its beginning. */
internal fun encodeLearnedAudio(range: LongRange, prints: List<AudioPrint>): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { output ->
        output.writeLong(range.first)
        output.writeLong(range.last)
        output.write(encodeAudioPrints(prints))
    }
    return bytes.toByteArray()
}

internal fun decodeLearnedAudio(data: ByteArray?): Pair<LongRange, List<AudioPrint>>? {
    if (data == null) return null
    return runCatching {
        DataInputStream(data.inputStream()).use { input ->
            val first = input.readLong()
            val last = input.readLong()
            if (first < 0 || last <= first || last > OPENING_SCAN_LIMIT_MS) return null
            (first..last) to decodeAudioPrints(input.readBytes())
        }
    }.getOrNull()?.takeIf { it.second.isNotEmpty() }
}

private enum class SecondState { MATCH, MISMATCH, NEUTRAL, MISSING }

private fun nearestPrint(sorted: List<AudioPrint>, targetMs: Long, toleranceMs: Long): AudioPrint? {
    var low = 0
    var high = sorted.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (sorted[middle].timeMs < targetMs) low = middle + 1 else high = middle
    }
    return listOfNotNull(sorted.getOrNull(low - 1), sorted.getOrNull(low))
        .filter { abs(it.timeMs - targetMs) <= toleranceMs }
        .minByOrNull { abs(it.timeMs - targetMs) }
}

/**
 * Votes for offsets (reference time minus current time) from exactly and
 * nearly (one bit) equal prints. Equal prints are rare for unrelated audio but
 * common for the same theme music, even after different encoding.
 */
private fun audioOffsets(current: List<AudioPrint>, reference: List<AudioPrint>, limit: Int = 3): List<Long> {
    val index = HashMap<Int, MutableList<Long>>()
    reference.filter(AudioPrint::loud).forEach { index.getOrPut(it.bits) { mutableListOf() } += it.timeMs }
    if (index.isEmpty()) return emptyList()
    val votes = HashMap<Long, Int>()
    for (print in current) {
        if (!print.loud) continue
        for (flip in -1 until 32) {
            val key = if (flip < 0) print.bits else print.bits xor (1 shl flip)
            index[key]?.forEach { time ->
                val bucket = Math.round((time - print.timeMs) / 20.0)
                votes[bucket] = (votes[bucket] ?: 0) + 1
            }
        }
    }
    return votes.keys.map { bucket ->
        bucket to votes.getValue(bucket) + (votes[bucket - 1] ?: 0) + (votes[bucket + 1] ?: 0)
    }.filter { it.second >= 8 }
        .sortedByDescending { it.second }
        .map { it.first * 20 }
        .fold(mutableListOf<Long>()) { chosen, offset ->
            if (chosen.none { abs(it - offset) < 200 }) chosen += offset
            chosen
        }
        .take(limit)
}

/** Compares each second of the current episode with the reference at [offset]. */
private fun audioSeconds(
    current: List<AudioPrint>, reference: List<AudioPrint>, offset: Long, include: (Long) -> Boolean = { true }
): List<Pair<Long, SecondState>> {
    val first = reference.firstOrNull()?.timeMs ?: return emptyList()
    val last = reference.last().timeMs
    return current.filter { include(it.timeMs + offset) }.groupBy { it.timeMs / 1_000 }.toSortedMap().map { (second, prints) ->
        val targets = prints.map { it.timeMs + offset }
        if (targets.all { it < first - 50 || it > last + 50 }) return@map second to SecondState.MISSING
        var distance = 0
        var pairs = 0
        for (print in prints) {
            if (!print.loud) continue
            val counterpart = nearestPrint(reference, print.timeMs + offset, 35) ?: continue
            if (!counterpart.loud) continue
            distance += Integer.bitCount(print.bits xor counterpart.bits)
            pairs++
        }
        val state = when {
            pairs < 5 -> SecondState.NEUTRAL
            distance <= pairs * 32 * 33 / 100 -> SecondState.MATCH
            distance >= pairs * 32 * 40 / 100 -> SecondState.MISMATCH
            else -> SecondState.NEUTRAL
        }
        second to state
    }
}

/**
 * Finds theme music shared with a previous episode. Like the image comparison,
 * the matching section must be followed by differing audio so that its end is known.
 */
internal fun findRepeatedAudioOpening(current: List<AudioPrint>, previous: List<AudioPrint>): LongRange? {
    val sortedCurrent = current.sortedBy(AudioPrint::timeMs)
    val sortedPrevious = previous.sortedBy(AudioPrint::timeMs)
    var best: LongRange? = null
    for (offset in audioOffsets(sortedCurrent, sortedPrevious)) {
        var first: Long? = null
        var lastMatch: Long? = null
        var matched = 0
        var mismatches = 0
        var trailing = 0
        var lastSecond: Long? = null
        fun close(completed: Boolean) {
            val start = first
            val end = lastMatch
            if (completed && start != null && end != null && (end - start + 1) * 1_000 >= 10_000 &&
                matched * 10 >= (matched + mismatches - trailing) * 6
            ) {
                val candidate = (start * 1_000) until (end * 1_000 + 500)
                if (candidate.last - candidate.first > (best?.let { it.last - it.first } ?: 0)) best = candidate
            }
            first = null
            lastMatch = null
            matched = 0
            mismatches = 0
            trailing = 0
        }
        for ((second, state) in audioSeconds(sortedCurrent, sortedPrevious, offset)) {
            if (lastSecond != null && second - lastSecond > 8) close(completed = false)
            lastSecond = second
            when (state) {
                SecondState.MISSING -> if (first != null) close(completed = false)
                SecondState.NEUTRAL -> Unit
                SecondState.MATCH -> {
                    if (first == null) first = second
                    lastMatch = second
                    matched++
                    trailing = 0
                }
                SecondState.MISMATCH -> if (first != null) {
                    mismatches++
                    if (++trailing >= OPENING_END_SECONDS) close(completed = true)
                }
            }
        }
        close(completed = trailing >= OPENING_END_SECONDS)
    }
    return best
}

/** Recognises a previously learned theme early, while it is still playing. */
internal fun confirmKnownAudioOpening(
    current: List<AudioPrint>, learned: List<AudioPrint>, intro: LongRange
): LongRange? {
    val reference = learned.filter { it.timeMs in intro }.sortedBy(AudioPrint::timeMs)
    if (reference.count(AudioPrint::loud) < 40 || current.isEmpty()) return null
    val sortedCurrent = current.sortedBy(AudioPrint::timeMs)
    for (offset in audioOffsets(sortedCurrent, reference)) {
        val start = intro.first - offset
        if (start < 0) continue
        val seconds = audioSeconds(sortedCurrent, reference, offset) { it in intro }
        val matched = seconds.count { it.second == SecondState.MATCH }
        val observed = matched + seconds.count { it.second == SecondState.MISMATCH }
        if (matched >= 5 && matched * 10 >= observed * 7) return start..(intro.last - offset)
    }
    return null
}

/** Consecutive differing seconds after which shared theme music is considered over. */
internal const val OPENING_END_SECONDS = 4
