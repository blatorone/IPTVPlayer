package com.iptvplayer.app

import android.content.SharedPreferences
import org.json.JSONObject

/**
 * How well a channel's stream from one portal ran so far: [good] counts runs that played steadily,
 * [bad] counts failed starts, freezes and stalls. Both are capped so that recent behaviour wins.
 */
internal data class StreamRecord(val good: Int = 0, val bad: Int = 0) {
    val score: Int get() = good - 2 * bad

    fun succeeded() = capped(good + 1, bad)
    fun failed() = capped(good, bad + 1)

    private fun capped(good: Int, bad: Int): StreamRecord =
        // Halving keeps the ratio but lets a portal that improved (or got worse) move again soon.
        if (good + bad > MAX_EVENTS) StreamRecord((good + 1) / 2, (bad + 1) / 2) else StreamRecord(good, bad)

    companion object {
        const val MAX_EVENTS = 12
    }
}

/**
 * Orders a channel's streams so that the one that ran best comes first. [variants] arrive already
 * ordered by the user's choice and preferred portal; streams without a history keep that order,
 * and among equal scores it is kept as well (stable sort).
 */
internal fun <T> List<T>.rankedByHistory(records: Map<String, StreamRecord>, sourceOf: (T) -> String?): List<T> =
    sortedByDescending { item -> sourceOf(item)?.let { records[it]?.score } ?: 0 }

/**
 * Remembers per Live-TV channel how each portal's stream ran (only on this device; the file is
 * excluded from backups). Keys are the app's own channel keys and portal ids, no addresses.
 */
internal class LiveStreamHistory(private val preferences: SharedPreferences?) {
    fun records(channelKey: String): Map<String, StreamRecord> {
        val raw = preferences?.getString(channelKey, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { source ->
                val entry = json.getJSONObject(source)
                StreamRecord(entry.optInt("g"), entry.optInt("b"))
            }
        }.getOrDefault(emptyMap())
    }

    fun recordSuccess(channelKey: String, sourceId: String) = update(channelKey, sourceId) { it.succeeded() }

    fun recordFailure(channelKey: String, sourceId: String) = update(channelKey, sourceId) { it.failed() }

    private fun update(channelKey: String, sourceId: String, change: (StreamRecord) -> StreamRecord) {
        val prefs = preferences ?: return
        val records = records(channelKey).toMutableMap()
        records[sourceId] = change(records[sourceId] ?: StreamRecord())
        val json = JSONObject()
        records.forEach { (source, record) -> json.put(source, JSONObject().put("g", record.good).put("b", record.bad)) }
        val editor = prefs.edit().putString(channelKey, json.toString())
        // Bounded: once too many channels are stored, some others are forgotten to make room.
        val stored = prefs.all.keys
        if (stored.size >= MAX_CHANNELS && channelKey !in stored) {
            stored.filter { it != channelKey }.take(stored.size - MAX_CHANNELS + 1).forEach(editor::remove)
        }
        editor.apply()
    }

    companion object {
        const val FILE = "live_stream_history"
        private const val MAX_CHANNELS = 800
    }
}
