package com.iptvplayer.app

import androidx.core.content.edit

import android.content.SharedPreferences

import org.json.JSONArray
import org.json.JSONObject

internal data class SeriesPortalRef(val sourceId: String, val portalTitleId: String)

internal data class EpisodeResume(
    val episodeKey: String,
    val season: Int?,
    val episode: String?,
    val name: String?,
    val progressKey: String
)

internal data class WatchedMovie(
    val name: String,
    val year: Int?,
    val imdb: String?,
    val tmdb: String?,
    val refs: List<SeriesPortalRef>,
    val progressKey: String,
    val watchedAt: Long
)

internal data class TrackedSeries(
    val key: String,
    val name: String,
    val refs: List<SeriesPortalRef>,
    val year: Int? = null,
    val imdb: String? = null,
    val tmdb: String? = null,
    val saved: Boolean = false,
    val viewed: Boolean = false,
    val known: Set<String> = emptySet(),
    val unseen: Set<String> = emptySet(),
    val checkedAt: Long = 0,
    val usedTmdb: Boolean = false,
    val episodeProvider: String? = null,
    val watchedAt: Long = 0,
    val lastEpisode: EpisodeResume? = null,
    val nextEpisodeName: String? = null,
    val nextAirDate: String? = null,
    val watchedEpisodes: Set<String> = emptySet(),
    val isComplete: Boolean? = null,
    val localViewed: Boolean = false,
    val hiddenFromHistory: Boolean = false
,
    /** When episodes not yet seen were last found (0: never or before this was recorded). */
    val newAt: Long = 0
,
    /** Episodes not yet seen that the portal already offers, and when the last of them showed up there. */
    val portalNew: Set<String> = emptySet(),
    val portalNewAt: Long = 0,
    val portalCheckedAt: Long = 0,
    /** Start of the next season (Trakt), for "Bald verfügbar". */
    val nextSeason: Int? = null,
    val nextSeasonDate: String? = null
)

internal fun seriesEpisodeKey(season: PortalSeason, episode: PortalTitle): String {
    val seasonKey = season.number?.toString() ?: numberedSeason.find(season.title.name)?.groupValues?.get(1)
        ?.toIntOrNull()?.toString() ?: normalizedLabel(season.title.name)
    val episodeKey = episode.episode?.let { raw ->
        (raw.trim().toIntOrNull() ?: numberedEpisode.matchEntire(raw.trim())?.groupValues?.get(1)?.toIntOrNull())
            ?.toString() ?: normalizedLabel(raw)
    }
        ?: normalizedLabel(episode.name)
    return "$seasonKey:$episodeKey"
}

private val numberedSeason = Regex("""(?i)\b(?:staffel|season)\s+(\d+)\b""")
private val numberedEpisode = Regex("""(?i)(?:s\d+\s*e|e|episode\s+|folge\s+)(\d+)""")

internal fun SharedPreferences.saveInterruptedPlayback(key: String, position: Long, duration: Long) {
    edit {
        putLong(key, position)
        putLong("progress_updated:$key", System.currentTimeMillis())
        putBoolean("completed:$key", false)
        remove("manual_watched:$key")
        if (duration > 0) putLong("duration:$key", duration)
    }
}

internal fun SharedPreferences.isPlaybackCompleted(key: String, fallback: Boolean): Boolean =
    if (contains("manual_watched:$key")) getBoolean("manual_watched:$key", false)
    else getBoolean("completed:$key", fallback)

internal fun resumeEpisode(seasons: List<PortalSeason>, resume: EpisodeResume): Pair<PortalSeason, PortalTitle>? {
    for (season in seasons) {
        val episode = season.episodes.firstOrNull { seriesEpisodeKey(season, it) == resume.episodeKey }
        if (episode != null) return season to episode
    }
    return null
}

internal fun nextEpisodeAfter(
    seasons: List<PortalSeason>,
    resume: EpisodeResume,
    isCompleted: (PortalSeason, PortalTitle) -> Boolean
): Pair<PortalSeason, PortalTitle>? {
    val ordered = seasons.sortedWith(compareBy(
        { it.number?.takeIf { number -> number >= 0 } ?: Int.MAX_VALUE },
        { normalizedLabel(it.title.name) }
    )).flatMap { season ->
        season.episodes.sortedWith(compareBy(
            { it.episode?.toIntOrNull()?.takeIf { number -> number > 0 } ?: Int.MAX_VALUE },
            { normalizedLabel(it.name) }
        )).map { season to it }
    }
    val current = ordered.indexOfFirst { (season, episode) -> seriesEpisodeKey(season, episode) == resume.episodeKey }
    if (current < 0) return null
    return ordered.drop(current + 1).firstOrNull { (season, episode) -> !isCompleted(season, episode) }
}

internal fun firstPlayableEpisode(seasons: List<PortalSeason>): Pair<PortalSeason, PortalTitle>? {
    val available = seasons.filter { it.episodes.isNotEmpty() }
    val season = available.firstOrNull { it.number == 1 }
        ?: available.minByOrNull { it.number?.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }
        ?: return null
    val episode = season.episodes.minByOrNull {
        it.episode?.toIntOrNull()?.takeIf { number -> number > 0 } ?: Int.MAX_VALUE
    } ?: return null
    return season to episode
}

internal fun recoverEpisodeResume(entry: TrackedSeries, positions: Map<String, *>): EpisodeResume? {
    val identity = when {
        entry.imdb?.startsWith("tt") == true -> "imdb:${entry.imdb}"
        entry.tmdb?.isNotBlank() == true -> "tmdb:${entry.tmdb}"
        else -> entry.key.takeIf { it.startsWith("imdb:") || it.startsWith("tmdb:") }
    }
    val prefixes = listOfNotNull(identity?.let { "SERIES:catalogue:$it:" }) + entry.refs.map {
        "SERIES:${it.sourceId}:${it.portalTitleId}:"
    }
    val activePositions = positions.mapNotNull { (key, value) ->
        if (value !is Long || value <= 0) return@mapNotNull null
        if (positions["completed:$key"] == true) return@mapNotNull null
        val prefix = prefixes.firstOrNull(key::startsWith) ?: return@mapNotNull null
        val position = key.removePrefix(prefix).split(':')
        if (position.size != 2) return@mapNotNull null
        val season = position.first().toIntOrNull() ?: return@mapNotNull null
        val episode = position.last().toIntOrNull() ?: return@mapNotNull null
        val updated = positions["progress_updated:$key"] as? Long ?: 0L
        updated to EpisodeResume("$season:$episode", season, episode.toString(), null, key)
    }
    val distinctEpisodes = activePositions.distinctBy { it.second.episodeKey }
    val activePosition = when (distinctEpisodes.size) {
        0 -> null
        1 -> activePositions.maxByOrNull { it.first }?.second
        else -> {
            val ordered = distinctEpisodes.sortedByDescending { it.first }
            ordered.first().takeIf { it.first > ordered[1].first }?.second
        }
    }
    return activePosition ?: entry.lastEpisode
}

internal fun TrackedSeries.observe(episodes: Set<String>, now: Long): TrackedSeries {
    val added = if (checkedAt == 0L) emptySet() else episodes - known - watchedEpisodes
    return copy(
        known = known + episodes,
        unseen = unseen + added,
        checkedAt = now,
        newAt = if (added.isNotEmpty()) now else newAt,
        // A finished series with new episodes is in progress again ("Weiter schauen").
        isComplete = if (added.isNotEmpty()) false else isComplete
    )
}

/** Shown under "Weiter schauen": unfinished, or finished with episodes added since. */
internal fun TrackedSeries.continuable(): Boolean =
    viewed && !hiddenFromHistory && (isComplete != true || unseen.isNotEmpty())

private const val DAY_MS = 24 * 60 * 60 * 1000L
/** "Neu diese Woche": how long an episode found in the portal counts as new. */
internal const val PORTAL_NEW_MS = 7 * DAY_MS
/** The portal is asked again after this long while a new episode is still missing there. */
internal const val PORTAL_CHECK_MS = 3 * 60 * 60 * 1000L

/** Episodes not yet seen that the portal already has, if one of them came up there this week. */
internal fun TrackedSeries.newInPortal(now: Long): Set<String> =
    if (now - portalNewAt > PORTAL_NEW_MS) emptySet() else portalNew intersect unseen

/** Whether the portal should be asked for the episodes Trakt reported as new (only for two weeks). */
internal fun TrackedSeries.portalCheckDue(now: Long): Boolean =
    (unseen - portalNew).isNotEmpty() && newAt > 0 && now - newAt <= 2 * PORTAL_NEW_MS &&
        now - portalCheckedAt >= PORTAL_CHECK_MS

/** Records which new episodes the portal offers ([offered]: episode keys found in the portal). */
internal fun TrackedSeries.withPortalEpisodes(offered: Set<String>, now: Long): TrackedSeries {
    val available = unseen intersect offered
    return copy(
        portalNew = available,
        portalNewAt = if ((available - portalNew).isNotEmpty()) now else portalNewAt,
        portalCheckedAt = now
    )
}

/**
 * Order of "Weiter schauen": series with a new episode in the portal this week first, the most
 * recent first; then the others by when they were watched last ("Verlauf" keeps that order for all).
 */
internal fun List<TrackedSeries>.continueOrder(now: Long): List<TrackedSeries> = sortedWith(
    compareByDescending<TrackedSeries> { it.newInPortal(now).isNotEmpty() }
        .thenByDescending { if (it.newInPortal(now).isNotEmpty()) it.portalNewAt else it.watchedAt }
)

/**
 * "Bald verfügbar": series that are watched whose next season has a start date today or later,
 * soonest first. Only a hint: whether the portal will have it is not known yet.
 */
internal fun upcomingSeries(entries: List<TrackedSeries>, today: java.time.LocalDate): List<Pair<TrackedSeries, java.time.LocalDate>> =
    entries.filter { it.viewed && !it.hiddenFromHistory }
        .mapNotNull { entry ->
            entry.nextSeasonDate?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() }
                ?.takeIf { !it.isBefore(today) }?.let { entry to it }
        }
        .sortedWith(compareBy({ it.second }, { it.first.name }))

internal fun encodeSeriesActivity(entries: List<TrackedSeries>): String = JSONArray().apply {
    entries.forEach { series ->
        put(JSONObject().apply {
            put("key", series.key)
            put("name", series.name)
            put("year", series.year)
            put("imdb", series.imdb)
            put("tmdb", series.tmdb)
            put("refs", encodeRefs(series.refs))
            put("saved", series.saved)
            put("viewed", series.viewed)
            put("localViewed", series.localViewed)
            put("hiddenFromHistory", series.hiddenFromHistory)
            put("known", JSONArray(series.known))
            put("unseen", JSONArray(series.unseen))
            put("watchedEpisodes", JSONArray(series.watchedEpisodes))
            series.isComplete?.let { put("isComplete", it) }
            put("checkedAt", series.checkedAt)
            put("usedTmdb", series.usedTmdb)
            put("episodeProvider", series.episodeProvider)
            put("watchedAt", series.watchedAt)
            if (series.newAt > 0) put("newAt", series.newAt)
            if (series.portalNew.isNotEmpty()) put("portalNew", JSONArray(series.portalNew))
            if (series.portalNewAt > 0) put("portalNewAt", series.portalNewAt)
            if (series.portalCheckedAt > 0) put("portalCheckedAt", series.portalCheckedAt)
            series.nextSeason?.let { put("nextSeason", it) }
            series.nextSeasonDate?.let { put("nextSeasonDate", it) }
            series.nextEpisodeName?.let { put("nextEpisodeName", it) }
            series.nextAirDate?.let { put("nextAirDate", it) }
            series.lastEpisode?.let { episode ->
                put("lastEpisode", JSONObject()
                    .put("key", episode.episodeKey).put("season", episode.season)
                    .put("episode", episode.episode).put("name", episode.name)
                    .put("progressKey", episode.progressKey))
            }
        })
    }
}.toString()

internal fun decodeSeriesActivity(data: String?): List<TrackedSeries> = runCatching {
    val entries = JSONArray(data ?: "[]")
    List(entries.length()) { index ->
        val entry = entries.getJSONObject(index)
        TrackedSeries(
            key = entry.getString("key"),
            name = entry.getString("name"),
            refs = decodeRefs(entry.getJSONArray("refs")),
            year = entry.optInt("year").takeIf { it > 0 },
            imdb = entry.optString("imdb").ifEmpty { null },
            tmdb = entry.optString("tmdb").ifEmpty { null },
            saved = entry.optBoolean("saved"),
            viewed = entry.optBoolean("viewed"),
            localViewed = entry.optBoolean("localViewed"),
            hiddenFromHistory = entry.optBoolean("hiddenFromHistory"),
            known = entry.optJSONArray("known").readStrings(),
            unseen = entry.optJSONArray("unseen").readStrings(),
            watchedEpisodes = entry.optJSONArray("watchedEpisodes").readStrings(),
            isComplete = if (entry.has("isComplete")) entry.optBoolean("isComplete") else null,
            checkedAt = entry.optLong("checkedAt"),
            usedTmdb = entry.optBoolean("usedTmdb"),
            episodeProvider = entry.optString("episodeProvider").ifBlank { null },
            watchedAt = entry.optLong("watchedAt"),
            newAt = entry.optLong("newAt"),
            portalNew = entry.optJSONArray("portalNew").readStrings(),
            portalNewAt = entry.optLong("portalNewAt"),
            portalCheckedAt = entry.optLong("portalCheckedAt"),
            nextSeason = if (entry.has("nextSeason")) entry.optInt("nextSeason") else null,
            nextSeasonDate = entry.optString("nextSeasonDate").ifBlank { null },
            nextEpisodeName = entry.optString("nextEpisodeName").ifBlank { null },
            nextAirDate = entry.optString("nextAirDate").ifBlank { null },
            lastEpisode = entry.optJSONObject("lastEpisode")?.let {
                EpisodeResume(
                    it.getString("key"), it.optInt("season", -1).takeIf { value -> value >= 0 },
                    it.optString("episode").ifEmpty { null }, it.optString("name").ifEmpty { null },
                    it.getString("progressKey")
                )
            }
        )
    }
}.getOrDefault(emptyList())

private fun JSONArray?.readStrings(): Set<String> =
    if (this == null) emptySet() else List(length()) { getString(it) }.toSet()

internal fun encodeMovieHistory(entries: List<WatchedMovie>): String = JSONArray().apply {
    entries.forEach { movie ->
        put(JSONObject().put("name", movie.name).put("year", movie.year)
            .put("imdb", movie.imdb).put("tmdb", movie.tmdb)
            .put("refs", encodeRefs(movie.refs)).put("progressKey", movie.progressKey)
            .put("watchedAt", movie.watchedAt))
    }
}.toString()

internal fun decodeMovieHistory(data: String?): List<WatchedMovie> = runCatching {
    val entries = JSONArray(data ?: "[]")
    List(entries.length()) { index ->
        val entry = entries.getJSONObject(index)
        WatchedMovie(
            entry.getString("name"), entry.optInt("year").takeIf { it > 0 },
            entry.optString("imdb").ifEmpty { null }, entry.optString("tmdb").ifEmpty { null },
            decodeRefs(entry.getJSONArray("refs")), entry.getString("progressKey"), entry.optLong("watchedAt")
        )
    }
}.getOrDefault(emptyList())

private fun encodeRefs(refs: List<SeriesPortalRef>): JSONArray = JSONArray().apply {
    refs.forEach { ref -> put(JSONObject().put("source", ref.sourceId).put("title", ref.portalTitleId)) }
}

private fun decodeRefs(refs: JSONArray): List<SeriesPortalRef> = List(refs.length()) { index ->
    val ref = refs.getJSONObject(index)
    SeriesPortalRef(ref.getString("source"), ref.getString("title"))
}
