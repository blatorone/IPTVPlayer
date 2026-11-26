package com.iptvplayer.app

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/*
 * Timeshift for Live-TV (optional, off by default): the running channel is recorded into a ring of
 * files in the app's cache, and the player plays from there. Pausing keeps recording; seeking
 * restarts the player at the matching place in the recording. Only plain streams (no HLS).
 */

private const val TIMESHIFT_ENABLED = "timeshift_enabled"
private const val TIMESHIFT_MINUTES = "timeshift_minutes"
internal val TIMESHIFT_DURATIONS = listOf(15, 30, 60)
private const val SEGMENT_BYTES = 8L * 1024 * 1024
private const val MAX_TOTAL_BYTES = 3L * 1024 * 1024 * 1024
private const val KEEP_FREE_BYTES = 600L * 1024 * 1024
private const val TS_PACKET = 188

/** Monotonic milliseconds (also available in tests). */
internal fun timeshiftNow(): Long = System.nanoTime() / 1_000_000

/** Minutes of timeshift, or null when it is off. */
internal fun timeshiftMinutes(context: Context): Int? {
    val preferences = context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    if (!preferences.getBoolean(TIMESHIFT_ENABLED, false)) return null
    return preferences.getInt(TIMESHIFT_MINUTES, 30).takeIf { it in TIMESHIFT_DURATIONS } ?: 30
}

/**
 * Records one live stream. Bytes are addressed by their position since the start of the recording;
 * the oldest segments are deleted when the time window or the space limit is exceeded.
 */
internal class TimeshiftRecorder(
    private val directory: File,
    private val url: String,
    private val windowMs: Long
) {
    private val lock = Object()
    /** First byte still kept, and the number of bytes written so far. */
    @Volatile var firstOffset = 0L
        private set
    @Volatile var written = 0L
        private set
    /** Elapsed-realtime marks (time to byte position), one per second. */
    private val marks = ArrayDeque<Pair<Long, Long>>()
    @Volatile private var failure: IOException? = null
    @Volatile private var stopped = false
    private val thread = Thread(::record, "timeshift-recorder")
    val startedAt = timeshiftNow()

    fun start() {
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() }
        thread.isDaemon = true
        thread.start()
    }

    fun stop() {
        stopped = true
        thread.interrupt()
        synchronized(lock) { lock.notifyAll() }
        Thread { directory.listFiles()?.forEach { it.delete() } }.start()
    }

    private fun segment(index: Long) = File(directory, "seg-$index")

    private fun record() {
        val client = OkHttpClient.Builder().readTimeout(20, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS).build()
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val input = response.body?.byteStream() ?: throw IOException("no body")
                val buffer = ByteArray(64 * 1024)
                var file: RandomAccessFile? = null
                var fileIndex = -1L
                try {
                    while (!stopped) {
                        val count = input.read(buffer)
                        if (count < 0) throw IOException("stream ended")
                        var done = 0
                        while (done < count) {
                            val index = written / SEGMENT_BYTES
                            if (index != fileIndex) {
                                file?.close()
                                file = RandomAccessFile(segment(index), "rw")
                                fileIndex = index
                            }
                            val inSegment = (SEGMENT_BYTES - written % SEGMENT_BYTES).toInt()
                            val part = minOf(inSegment, count - done)
                            file!!.write(buffer, done, part)
                            done += part
                            synchronized(lock) {
                                written += part
                                lock.notifyAll()
                            }
                        }
                        mark()
                        trim()
                    }
                } finally {
                    file?.close()
                }
            }
        } catch (error: IOException) {
            if (!stopped) failure = error
        } catch (error: Exception) {
            if (!stopped) failure = IOException(error)
        } finally {
            synchronized(lock) { lock.notifyAll() }
        }
    }

    private fun mark() {
        val now = timeshiftNow()
        synchronized(marks) {
            if (marks.isEmpty() || now - marks.last().first >= 1_000) marks.addLast(now to written)
        }
    }

    /** Deletes whole segments that fall out of the time window or exceed the space limits. */
    private fun trim() {
        val now = timeshiftNow()
        while (true) {
            val firstIndex = firstOffset / SEGMENT_BYTES
            val lastIndex = written / SEGMENT_BYTES
            if (firstIndex >= lastIndex) return
            val oldestTime = timeAt(firstOffset + SEGMENT_BYTES)
            val tooOld = oldestTime != null && now - oldestTime > windowMs
            val tooBig = written - firstOffset > MAX_TOTAL_BYTES ||
                runCatching { directory.usableSpace < KEEP_FREE_BYTES }.getOrDefault(false)
            if (!tooOld && !tooBig) return
            segment(firstIndex).delete()
            firstOffset = (firstIndex + 1) * SEGMENT_BYTES
            synchronized(marks) { while (marks.size > 1 && marks.first().second < firstOffset) marks.removeFirst() }
        }
    }

    /** Elapsed realtime at which byte [offset] arrived (nearest mark). */
    fun timeAt(offset: Long): Long? = synchronized(marks) {
        marks.firstOrNull { it.second >= offset }?.first ?: marks.lastOrNull()?.first
    }

    /** Byte position for a moment in elapsed realtime, on a transport-stream packet boundary. */
    fun offsetAt(time: Long): Long {
        val offset = synchronized(marks) { marks.lastOrNull { it.first <= time }?.second ?: marks.firstOrNull()?.second ?: 0L }
        val clamped = offset.coerceIn(firstOffset, written)
        return clamped - clamped % TS_PACKET
    }

    /** Oldest kept moment and the live edge, in elapsed realtime. */
    fun window(): Pair<Long, Long> = (timeAt(firstOffset) ?: startedAt) to timeshiftNow()

    /** Reads at [offset]; waits for data at the live edge. Throws when recording failed or stalled. */
    fun read(offset: Long, target: ByteArray, at: Int, length: Int, isClosed: () -> Boolean): Int {
        val waitStarted = timeshiftNow()
        synchronized(lock) {
            while (offset >= written) {
                failure?.let { throw it }
                if (stopped || isClosed()) throw InterruptedIOException("timeshift closed")
                if (timeshiftNow() - waitStarted > 20_000) throw IOException("timeshift stalled")
                lock.wait(100)
            }
        }
        if (offset < firstOffset) throw IOException("timeshift data gone")
        val index = offset / SEGMENT_BYTES
        val inSegment = offset % SEGMENT_BYTES
        val available = minOf(written - offset, SEGMENT_BYTES - inSegment, length.toLong()).toInt()
        RandomAccessFile(segment(index), "r").use { file ->
            file.seek(inSegment)
            return file.read(target, at, available).coerceAtLeast(0)
        }
    }
}

/** Serves the recording to the player from [startOffset], like an endless live stream. */
internal class TimeshiftDataSource(private val recorder: TimeshiftRecorder, private val startOffset: Long) :
    BaseDataSource(false) {
    @Volatile private var closed = false
    private var position = 0L
    private var uri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        closed = false
        position = maxOf(startOffset, recorder.firstOffset) + dataSpec.position
        transferInitializing(dataSpec)
        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val count = recorder.read(position, buffer, offset, length) { closed }
        position += count
        bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (!closed) {
            closed = true
            transferEnded()
        }
    }
}

/**
 * The one running timeshift. [Player] wraps the app's player for the controls: it reports the
 * recorded span as a seekable timeline, so the normal time bar and seek buttons work for Live-TV.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object Timeshift {
    @Volatile var recorder: TimeshiftRecorder? = null
        private set
    /** Elapsed realtime of the place where the player's current source starts. */
    @Volatile private var sourceStart = 0L
    private var mediaItem: MediaItem? = null

    fun stop() {
        recorder?.stop()
        recorder = null
        mediaItem = null
    }

    /** Starts recording [url] and plays it from the recording on [player]. */
    fun play(context: Context, player: ExoPlayer, url: String, media: MediaItem, minutes: Int) {
        stop()
        val next = TimeshiftRecorder(File(context.cacheDir, "timeshift"), url, minutes * 60_000L).also { it.start() }
        recorder = next
        mediaItem = media
        sourceStart = timeshiftNow()
        player.setMediaSource(ProgressiveMediaSource.Factory { TimeshiftDataSource(next, 0L) }.createMediaSource(media))
    }

    /** Continues playback at [time] (elapsed realtime) of the recording. */
    private fun jump(player: Player, time: Long) {
        val active = recorder ?: return
        val media = mediaItem ?: return
        val exo = player as? ExoPlayer ?: return
        val (oldest, live) = active.window()
        val target = time.coerceIn(oldest, live)
        val offset = active.offsetAt(target)
        sourceStart = target
        val playing = exo.playWhenReady
        exo.setMediaSource(ProgressiveMediaSource.Factory { TimeshiftDataSource(active, offset) }.createMediaSource(media))
        exo.prepare()
        exo.playWhenReady = playing
    }

    /** How far behind live playback is (ms), or null without a running timeshift. */
    fun behindLive(player: Player?): Long? {
        val active = recorder ?: return null
        val position = player?.let { (it as? ForwardingPlayer)?.wrappedPlayer ?: it }?.currentPosition ?: return null
        return (active.window().second - (sourceStart + position.coerceAtLeast(0L))).coerceAtLeast(0L)
    }

    /** Back to the live edge of the recording. */
    fun goLive(player: Player?) {
        val exo = (player as? ForwardingPlayer)?.wrappedPlayer ?: player ?: return
        jump(exo, timeshiftNow())
    }

    /** The player for the controls: plain [player] without timeshift. */
    fun controls(player: ExoPlayer?): Player? {
        if (player == null) return null
        return object : ForwardingPlayer(player) {
            private fun active() = recorder != null
            private fun nowPlaying() = sourceStart + player.currentPosition.coerceAtLeast(0L)

            override fun isCurrentMediaItemSeekable(): Boolean = active() || super.isCurrentMediaItemSeekable()
            override fun isCurrentMediaItemLive(): Boolean = if (active()) false else super.isCurrentMediaItemLive()
            override fun isCurrentMediaItemDynamic(): Boolean = if (active()) false else super.isCurrentMediaItemDynamic()
            override fun getDuration(): Long {
                val active = recorder ?: return super.getDuration()
                val (oldest, live) = active.window()
                return (live - oldest).coerceAtLeast(1L)
            }
            override fun getCurrentPosition(): Long {
                val active = recorder ?: return super.getCurrentPosition()
                val (oldest, live) = active.window()
                return (nowPlaying() - oldest).coerceIn(0L, live - oldest)
            }
            override fun getContentPosition(): Long = currentPosition
            override fun getContentDuration(): Long = duration
            override fun getBufferedPosition(): Long = if (active()) duration else super.getBufferedPosition()
            override fun getContentBufferedPosition(): Long = bufferedPosition
            // Without the timeline command the control view takes the length and the seeks from
            // this player (the recorded span); a live timeline has no length, so the bar stayed empty.
            override fun getAvailableCommands(): Player.Commands =
                if (!active()) super.getAvailableCommands()
                else super.getAvailableCommands().buildUpon()
                    .addAll(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD)
                    .remove(Player.COMMAND_GET_TIMELINE)
                    .build()
            override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)
            override fun seekTo(positionMs: Long) {
                val active = recorder ?: return super.seekTo(positionMs)
                jump(player, active.window().first + positionMs)
            }
            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                if (!active()) return super.seekTo(mediaItemIndex, positionMs)
                seekTo(positionMs)
            }
            override fun seekBack() = if (active()) jump(player, nowPlaying() - seekBackIncrement) else super.seekBack()
            override fun seekForward() = if (active()) jump(player, nowPlaying() + seekForwardIncrement) else super.seekForward()
            override fun seekToDefaultPosition() = if (active()) jump(player, timeshiftNow()) else super.seekToDefaultPosition()
        }
    }
}
