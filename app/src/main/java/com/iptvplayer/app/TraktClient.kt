package com.iptvplayer.app

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

internal const val TRAKT_CLIENT_ID = "OFqILSXgqkCjYQ_wYRIbfs3O6ngifeMysl-mcbi6XqA"

internal data class TraktSession(
    val clientId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val username: String
)

internal data class TraktDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val qrUrl: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

internal sealed interface TraktDevicePoll {
    data class Authorized(val session: TraktSession) : TraktDevicePoll
    data object Pending : TraktDevicePoll
    data object SlowDown : TraktDevicePoll
}

internal data class TraktMedia(
    val section: PortalSection,
    val name: String,
    val year: Int?,
    val imdbId: String?,
    val tmdbId: String?,
    val traktId: Long?,
    val watchedAtMillis: Long? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val progressPercent: Float? = null
) {
    fun asTitle(): PortalTitle = PortalTitle(name, year = year, imdbId = imdbId, tmdbId = tmdbId)

    fun ids(): JSONObject = JSONObject().apply {
        imdbId?.let { put("imdb", it) }
        tmdbId?.toLongOrNull()?.takeIf { it > 0 }?.let { put("tmdb", it) }
        traktId?.takeIf { it > 0 }?.let { put("trakt", it) }
    }
}

internal data class TraktLibrary(
    val watchlist: List<TraktMedia>,
    val watched: List<TraktMedia>,
    val playback: List<TraktMedia> = emptyList(),
    val watchedEpisodes: List<TraktMedia> = emptyList(),
    val seriesProgress: List<TraktSeriesProgress> = emptyList(),
    val seriesHistorySnapshot: List<TraktMedia>? = null,
    /** Identity keys of all watched films, for the watched status; null when not loaded. */
    val watchedMovieKeys: Set<String>? = null
)

/** Changed Trakt sections. A null baseline deliberately means a first/full sync. */
internal data class TraktActivityDelta(
    val full: Boolean,
    val watchlistMovies: Boolean,
    val watchlistShows: Boolean,
    val watchedMovies: Boolean,
    val watchedEpisodes: Boolean,
    val playbackMovies: Boolean,
    val playbackEpisodes: Boolean
)

internal data class TraktSeriesProgress(
    val show: TraktMedia,
    val completedEpisodes: Int,
    val airedEpisodes: Int
) {
    val isComplete: Boolean get() = airedEpisodes > 0 && completedEpisodes >= airedEpisodes
}

internal data class TraktUpcomingEpisode(val name: String?, val season: Int?, val number: Int?, val airDate: String?)
internal data class TraktEpisodeFeed(val airedKeys: Set<String>, val upcoming: TraktUpcomingEpisode?)
internal data class TraktTitleMetadata(
    val title: String,
    val year: Int?,
    val overview: String?,
    val rating: Double?
)

internal class TraktApi(private val clientId: String) {
    private data class Response(val status: Int, val body: String, val pageCount: Int?)

    init {
        require(clientId.isNotBlank())
    }

    suspend fun beginDeviceAuthorization(): TraktDeviceCode {
        val json = request("/oauth/device/code", "POST", JSONObject().put("client_id", clientId)).jsonObject()
        val code = json.optString("device_code").takeIf(String::isNotBlank) ?: error("Trakt hat keinen Gerätecode geliefert.")
        val userCode = json.optString("user_code").takeIf(String::isNotBlank) ?: error("Trakt hat keinen Bestätigungscode geliefert.")
        val completeFromApi = json.optString("verification_url_complete")
        val verify = sequenceOf(
            json.optString("verification_url"), completeFromApi.substringBefore('?')
        ).firstNotNullOfOrNull { it.takeIf(::isTrustedActivationUrl) }
            ?: "https://auth.trakt.tv/activate"
        val complete = completeFromApi.takeIf(::isTrustedActivationUrl) ?: verify
        return TraktDeviceCode(
            code, userCode, verify, complete,
            json.optInt("expires_in", 600).coerceAtLeast(1),
            json.optInt("interval", 5).coerceAtLeast(1)
        )
    }

    suspend fun pollDeviceAuthorization(code: TraktDeviceCode): TraktDevicePoll = try {
        val json = request(
            "/oauth/device/token", "POST",
            JSONObject().put("code", code.deviceCode).put("client_id", clientId)
        ).jsonObject()
        TraktDevicePoll.Authorized(json.session(clientId))
    } catch (error: TraktApiException) {
        when {
            // Trakt's device-token endpoint documents HTTP 400 as the normal
            // "waiting for user approval" response. It does not always include
            // an OAuth error code in the response body.
            error.status == 400 || error.apiCode == "authorization_pending" -> TraktDevicePoll.Pending
            error.status == 429 || error.apiCode == "slow_down" -> TraktDevicePoll.SlowDown
            error.status == 410 || error.apiCode == "expired_token" ->
                throw TraktApiException(error.status, error.apiCode, "Der Trakt-Bestätigungscode ist abgelaufen. Bitte erneut verbinden.")
            error.status == 418 || error.apiCode == "access_denied" ->
                throw TraktApiException(error.status, error.apiCode, "Die Trakt-Verknüpfung wurde abgelehnt.")
            else -> throw error
        }
    }

    suspend fun refresh(session: TraktSession): TraktSession {
        val json = request(
            "/oauth/token", "POST",
            JSONObject().put("refresh_token", session.refreshToken)
                .put("client_id", clientId).put("grant_type", "refresh_token")
        ).jsonObject()
        return json.session(clientId, session.username)
    }

    suspend fun accountName(accessToken: String): String {
        val response = request("/users/settings", "GET", token = accessToken).jsonObject()
        return response.optJSONObject("user")?.optString("username")
            ?.takeIf(String::isNotBlank) ?: "Trakt"
    }

    /** Timestamp map used by Trakt clients to avoid downloading unchanged library sections. */
    suspend fun loadLastActivities(accessToken: String): String =
        request("/sync/last_activities", "GET", token = accessToken).body

    /** Returns aired episode keys and the closest future episode Trakt currently knows. */
    suspend fun episodeFeed(title: PortalTitle, accessToken: String): TraktEpisodeFeed? {
        val show = findShow(title, accessToken) ?: return null
        val traktId = show.optJSONObject("ids")?.optLong("trakt", 0)?.takeIf { it > 0 } ?: return null
        val seasons = request("/shows/$traktId/seasons?extended=episodes", "GET", token = accessToken)
            .body.arrayObjects()
        val today = LocalDate.now(java.time.ZoneId.systemDefault())
        val aired = linkedSetOf<String>()
        val upcoming = mutableListOf<Pair<LocalDate, TraktUpcomingEpisode>>()
        for (season in seasons) {
            val seasonNumber = season.optInt("number", -1).takeIf { it >= 0 } ?: continue
            val episodes = season.optJSONArray("episodes") ?: continue
            for (index in 0 until episodes.length()) {
                val episode = episodes.optJSONObject(index) ?: continue
                val number = episode.optInt("number", -1).takeIf { it > 0 } ?: continue
                val rawDate = episode.optString("first_aired").takeIf(String::isNotBlank) ?: continue
                val date = runCatching { Instant.parse(rawDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                    .getOrNull() ?: continue
                if (!date.isAfter(today)) aired += "$seasonNumber:$number"
                else upcoming += date to TraktUpcomingEpisode(
                    episode.optString("title").takeIf(String::isNotBlank), seasonNumber, number, date.toString()
                )
            }
        }
        return TraktEpisodeFeed(aired, upcoming.minByOrNull { it.first }?.second)
    }

    /** Localized episode summaries and screenshots keyed by season and episode number. */
    suspend fun episodeDescriptions(title: PortalTitle, accessToken: String): Map<Pair<Int, Int>, TmdbEpisodeDetails> {
        val show = findShow(title, accessToken) ?: return emptyMap()
        val traktId = show.optJSONObject("ids")?.optLong("trakt", 0)?.takeIf { it > 0 }
            ?: return emptyMap()
        // Trakt returns English summaries; translated ones are only included on request.
        val path = "/shows/$traktId/seasons?translations=${descriptionLanguage()}&extended="
        val seasons = try {
            request(path + "episodes,images", "GET", token = accessToken).body.arrayObjects()
        } catch (error: TraktApiException) {
            // Images are optional; keep the summaries if this combination is refused.
            request(path + "episodes", "GET", token = accessToken).body.arrayObjects()
        }
        return buildMap {
            for (season in seasons) {
                val seasonNumber = season.optInt("number", -1).takeIf { it >= 0 } ?: continue
                val episodes = season.optJSONArray("episodes") ?: continue
                for (index in 0 until episodes.length()) {
                    val episode = episodes.optJSONObject(index) ?: continue
                    val number = episode.optInt("number", -1).takeIf { it > 0 } ?: continue
                    val overview = translatedOverview(episode.optJSONArray("translations"))
                        ?: germanDescription(episode.optString("overview"))
                    val screenshot = traktImage(episode.optJSONObject("images"), "screenshot", "thumb")
                    if (overview != null || screenshot != null) put(seasonNumber to number, TmdbEpisodeDetails(overview, screenshot))
                }
            }
        }
    }

    /** First image of the given kinds; Trakt returns host paths without a scheme. */
    private fun traktImage(images: JSONObject?, vararg kinds: String): String? {
        images ?: return null
        for (kind in kinds) {
            val candidates = images.optJSONArray(kind) ?: continue
            for (index in 0 until candidates.length()) {
                val value = candidates.optString(index).trim().takeIf { it.isNotEmpty() && it != "null" } ?: continue
                val url = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
                if (runCatching { java.net.URI(url).host }.getOrNull() != null) return url.replaceFirst("http://", "https://")
            }
        }
        return null
    }

    /**
     * Picks the Trakt entry for a portal title. A title found by external ID or with the same
     * normalized name must be unambiguous. Portal titles are often localized ("Die Simpsons")
     * while Trakt lists the original title; then the best-ranked search result is used only if
     * its release year matches exactly.
     */
    private fun chooseTraktCandidate(
        candidates: List<JSONObject>, title: PortalTitle, byId: Boolean, yearTolerance: Int
    ): JSONObject? {
        val expected = normalizedLabel(displayVodTitle(title))
        val titleYear = title.year ?: vodReleaseYear(title)
        fun year(row: JSONObject) = row.optInt("year", 0).takeIf { it > 0 }
        val named = candidates.filter { row ->
            val year = year(row)
            (byId || normalizedLabel(row.optString("title")) == expected) &&
                (titleYear == null || year == null || kotlin.math.abs(titleYear - year) <= yearTolerance)
        }.distinctBy { it.optJSONObject("ids")?.optLong("trakt", 0) }
        if (named.isNotEmpty() || byId) return named.singleOrNull()
        val year = titleYear ?: return null
        return candidates.firstOrNull()?.takeIf { year(it) == year }
    }

    private fun descriptionLanguage(): String =
        java.util.Locale.getDefault().language.takeIf { it.matches(Regex("[a-z]{2}")) } ?: "de"

    /** Picks the translated overview for the device language, preferring that language's home country. */
    private fun translatedOverview(translations: JSONArray?): String? {
        val language = descriptionLanguage()
        val candidates = translations?.let { array -> List(array.length()) { array.optJSONObject(it) } }
            .orEmpty().filterNotNull().filter { it.optString("language") == language }
        return candidates.sortedByDescending { it.optString("country").equals(language, ignoreCase = true) }
            .firstNotNullOfOrNull { candidate ->
                candidate.optString("overview").trim().takeIf { it.isNotEmpty() && it != "null" }
                    ?.let(::germanDescription)
            }
    }

    suspend fun lookupTitleMetadata(
        section: PortalSection,
        title: PortalTitle,
        accessToken: String
    ): TraktTitleMetadata? {
        val kind = if (section == PortalSection.MOVIES) "movie" else "show"
        val externalId = traktIdSearchPath(title)
        val path = if (externalId != null) {
            "$externalId?type=$kind&extended=full"
        } else {
            val query = URLEncoder.encode(displayVodTitle(title), "UTF-8")
            "/search/$kind?query=$query&extended=full"
        }
        val candidates = request(path, "GET", token = accessToken).body.arrayObjects().mapNotNull { result ->
            result.optJSONObject(kind)
        }
        val match = chooseTraktCandidate(candidates, title, byId = externalId != null, yearTolerance = 1) ?: return null
        val original = match.optString("overview").takeIf(String::isNotBlank)
        // Search results carry the English summary; the translation is a separate request.
        val translated = if (descriptionLanguage() == "en") null else {
            match.optJSONObject("ids")?.optLong("trakt", 0)?.takeIf { it > 0 }?.let { traktId ->
                try {
                    translatedOverview(runCatching {
                        JSONArray(request("/${kind}s/$traktId/translations/${descriptionLanguage()}", "GET", token = accessToken).body)
                    }.getOrNull())
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            }
        }
        // A localized portal title stays visible when Trakt was matched by its original title.
        val traktName = match.optString("title").takeIf(String::isNotBlank)
        val shownName = traktName?.takeIf { normalizedLabel(it) == normalizedLabel(displayVodTitle(title)) }
            ?: displayVodTitle(title).ifBlank { title.name }
        return TraktTitleMetadata(
            shownName,
            match.optInt("year", 0).takeIf { it > 0 },
            translated ?: original,
            match.optDouble("rating").takeIf { it.isFinite() && it > 0 }
        )
    }

    /** Main cast of a film or series from Trakt, used when no TMDB access is configured. */
    suspend fun cast(section: PortalSection, title: PortalTitle, accessToken: String): List<CastMember> {
        val kind = if (section == PortalSection.MOVIES) "movie" else "show"
        val match = findHistoryTitle(title, kind, accessToken) ?: return emptyList()
        val traktId = match.optJSONObject("ids")?.optLong("trakt", 0)?.takeIf { it > 0 } ?: return emptyList()
        val body = try {
            request("/${kind}s/$traktId/people?extended=images", "GET", token = accessToken).body
        } catch (error: TraktApiException) {
            request("/${kind}s/$traktId/people", "GET", token = accessToken).body
        }
        val cast = runCatching { JSONObject(body).optJSONArray("cast") }.getOrNull() ?: return emptyList()
        return List(cast.length()) { cast.optJSONObject(it) }.filterNotNull().take(60).mapNotNull { row ->
            val person = row.optJSONObject("person") ?: return@mapNotNull null
            val name = person.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: return@mapNotNull null
            val ids = person.optJSONObject("ids")
            val role = row.optJSONArray("characters")?.optString(0)?.takeIf { it.isNotBlank() && it != "null" }
                ?: row.optString("character").takeIf { it.isNotBlank() && it != "null" }
            CastMember(
                name, role, traktImage(person.optJSONObject("images"), "headshot"),
                ids?.optLong("tmdb", 0)?.takeIf { it > 0 }, ids?.optLong("trakt", 0)?.takeIf { it > 0 }
            )
        }
    }

    /** Films and series of a Trakt person. */
    suspend fun personCredits(traktId: Long, accessToken: String): List<PersonCredit> = coroutineScope {
        listOf(PortalSection.MOVIES to "movie", PortalSection.SERIES to "show").map { (section, kind) ->
            async {
                val body = try {
                    request("/people/$traktId/${kind}s", "GET", token = accessToken).body
                } catch (error: TraktApiException) {
                    return@async emptyList()
                }
                val cast = runCatching { JSONObject(body).optJSONArray("cast") }.getOrNull() ?: return@async emptyList()
                List(cast.length()) { cast.optJSONObject(it)?.optJSONObject(kind) }.filterNotNull().mapNotNull { item ->
                    val name = item.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: return@mapNotNull null
                    val ids = item.optJSONObject("ids")
                    PersonCredit(
                        section, name, null, item.optInt("year", 0).takeIf { it > 0 },
                        ids?.optString("imdb")?.takeIf { it.startsWith("tt") },
                        ids?.optLong("tmdb", 0)?.takeIf { it > 0 }?.toString()
                    )
                }
            }
        }.awaitAll().flatten()
    }

    private suspend fun findShow(title: PortalTitle, accessToken: String): JSONObject? =
        findHistoryTitle(title, "show", accessToken)

    private suspend fun findHistoryTitle(title: PortalTitle, kind: String, accessToken: String): JSONObject? {
        val idPath = traktIdSearchPath(title)
        val candidates = if (idPath != null) {
            request("$idPath?type=$kind", "GET", token = accessToken)
                .body.arrayObjects().mapNotNull { it.optJSONObject(kind) }
        } else {
            val query = URLEncoder.encode(displayVodTitle(title), "UTF-8")
            request("/search/$kind?query=$query", "GET", token = accessToken)
                .body.arrayObjects().mapNotNull { it.optJSONObject(kind) }
        }
        // Portal years are often off by one (German release vs. premiere).
        return chooseTraktCandidate(candidates, title, byId = idPath != null, yearTolerance = 1)
    }

    suspend fun setHistoryStatus(
        accessToken: String,
        section: PortalSection,
        title: PortalTitle,
        episodes: Set<Pair<Int, Int>>,
        watched: Boolean
    ): Boolean {
        val movie = section == PortalSection.MOVIES
        val ids = TraktMedia(section, title.name, title.year, title.imdbId, title.tmdbId, null).ids()
            .takeIf { it.length() > 0 } ?: findHistoryTitle(title, if (movie) "movie" else "show", accessToken)
            ?.optJSONObject("ids") ?: return false
        if (!movie && episodes.isEmpty()) return false
        val item = JSONObject().put("ids", ids)
        if (!movie) {
            val seasons = JSONArray()
            episodes.groupBy({ it.first }, { it.second }).forEach { (season, numbers) ->
                val entries = JSONArray()
                numbers.sorted().forEach { entries.put(JSONObject().put("number", it)) }
                seasons.put(JSONObject().put("number", season).put("episodes", entries))
            }
            item.put("seasons", seasons)
        }
        val payload = JSONObject().put(if (movie) "movies" else "shows", JSONArray().put(item))
        val endpoint = if (watched) "/sync/history" else "/sync/history/remove"
        val result = JSONObject(request(endpoint, "POST", payload, token = accessToken).body)
        val missing = result.optJSONObject("not_found") ?: return false
        return missing.keys().asSequence().none { (missing.optJSONArray(it)?.length() ?: 0) > 0 }
    }

    private fun String.arrayObjects(): List<JSONObject> =
        runCatching { JSONArray(this) }.getOrNull()?.let { array ->
            List(array.length()) { array.optJSONObject(it) }.filterNotNull()
        }.orEmpty()

    suspend fun loadLibrary(
        accessToken: String,
        onProgress: suspend (String) -> Unit = {},
        previousActivities: String? = null,
        currentActivities: String? = null,
        reconcileSeriesHistory: Boolean = false,
        loadAllWatchedMovies: Boolean = false
    ): TraktLibrary = withContext(Dispatchers.IO) {
        val delta = activityDelta(previousActivities, currentActivities)
        val loadWatchedMovies = delta.full || delta.watchedMovies || loadAllWatchedMovies
        val loadSeriesHistory = reconcileSeriesHistory || delta.full || delta.watchedEpisodes || delta.playbackEpisodes
        // These endpoints are independent. Fetch them concurrently, but keep a
        // small cap so TV devices and Trakt's API are not flooded with requests.
        val requestLimit = kotlinx.coroutines.sync.Semaphore(3)
        suspend fun <T> limited(block: suspend () -> T): T = requestLimit.withPermit { block() }
        val (watchlist, watchedData, playback) = coroutineScope {
                val watchlistMovies = async {
                    if (delta.full || delta.watchlistMovies) limited {
                        loadItems("/sync/watchlist/movies", PortalSection.MOVIES, accessToken, onProgress)
                    } else emptyList()
                }
                val watchlistShows = async {
                    if (delta.full || delta.watchlistShows) limited {
                        loadItems("/sync/watchlist/shows", PortalSection.SERIES, accessToken, onProgress)
                    } else emptyList()
                }
                val watchedMovies = async {
                    // All watched films: their keys give the watched status; only the newest ten are
                    // imported into the visible film history (which may search the portals).
                    if (loadWatchedMovies) limited {
                        loadItems("/sync/watched/movies", PortalSection.MOVIES, accessToken, onProgress, maxItems = 20_000)
                    } else emptyList()
                }
                val watchedShows = async {
                    if (loadSeriesHistory) limited { loadWatchedShows(accessToken, onProgress) }
                    else Triple(emptyList(), emptyList(), emptyList())
                }
                val playbackMovies = async {
                    if (delta.full || delta.playbackMovies) limited {
                        loadPlaybackItems("/sync/playback/movies", PortalSection.MOVIES, accessToken, onProgress)
                    } else emptyList()
                }
                val playbackEpisodes = async {
                    if (loadSeriesHistory) limited {
                        loadPlaybackItems("/sync/playback/episodes", PortalSection.SERIES, accessToken, onProgress)
                    } else emptyList()
                }
                Triple(
                    watchlistMovies.await() + watchlistShows.await(),
                    watchedMovies.await() to watchedShows.await(),
                    playbackMovies.await() + playbackEpisodes.await()
                )
            }
        val watchedMovies = watchedData.first
        val watchedShowData = watchedData.second
        val (watchedShows, aggregatedEpisodes, seriesProgress) = watchedShowData
        val watchedEpisodes = if (delta.full || delta.watchedEpisodes || reconcileSeriesHistory) {
            aggregatedEpisodes.ifEmpty {
            // Keep compatibility with responses without season data, but only
            // inspect the newest 100 events and at most ten distinct shows.
                if (watchedShows.isEmpty()) emptyList() else loadRecentEpisodeHistory(accessToken, onProgress)
            }
        } else emptyList()
        val recentMovies = watchedMovies.sortedByDescending { it.watchedAtMillis ?: 0L }.take(10)
        val watched = recentMovies + if (delta.full) watchedShows else emptyList()
        val changedPlayback = playback.filter { it.section != PortalSection.SERIES || delta.full || delta.playbackEpisodes }
        TraktLibrary(
            watchlist.distinctBy(::mediaIdentity), watched.distinctBy(::mediaIdentity),
            changedPlayback.distinctBy { "${mediaIdentity(it)}:${it.seasonNumber}:${it.episodeNumber}" },
            watchedEpisodes.distinctBy { "${mediaIdentity(it)}:${it.seasonNumber}:${it.episodeNumber}:${it.watchedAtMillis}" },
            seriesProgress.takeIf { delta.full || delta.watchedEpisodes }.orEmpty(),
            if (loadSeriesHistory) (watchedShows + playback.filter { it.section == PortalSection.SERIES })
                .distinctBy(::mediaIdentity) else null,
            if (loadWatchedMovies) watchedMovies.flatMapTo(hashSetOf()) { movieIdentityKeys(it.name, it.year, it.imdbId, it.tmdbId) } else null
        )
    }

    private fun activityDelta(previous: String?, current: String?): TraktActivityDelta {
        if (previous.isNullOrBlank() || current.isNullOrBlank()) {
            return TraktActivityDelta(true, true, true, true, true, true, true)
        }
        val before = runCatching { JSONObject(previous) }.getOrNull()
        val after = runCatching { JSONObject(current) }.getOrNull()
        if (before == null || after == null) return TraktActivityDelta(true, true, true, true, true, true, true)
        fun changed(section: String, field: String): Boolean =
            before.optJSONObject(section)?.optString(field).orEmpty() !=
                after.optJSONObject(section)?.optString(field).orEmpty()
        return TraktActivityDelta(
            full = false,
            watchlistMovies = changed("watchlist", "movies"),
            watchlistShows = changed("watchlist", "shows"),
            watchedMovies = changed("movies", "watched_at"),
            watchedEpisodes = changed("episodes", "watched_at") || changed("shows", "watched_at"),
            playbackMovies = changed("movies", "paused_at"),
            playbackEpisodes = changed("episodes", "paused_at")
        )
    }

    private suspend fun loadPlaybackItems(
        path: String, section: PortalSection, token: String, onProgress: suspend (String) -> Unit
    ): List<TraktMedia> {
        val result = mutableListOf<TraktMedia>()
        var page = 1
        var pageCount: Int? = null
        var hasMore = true
        do {
            val kindLabel = if (section == PortalSection.MOVIES) "Filme" else "Folgen"
            onProgress("Lade Trakt-Wiedergabestände ($kindLabel)…")
            val response = request("$path?page=$page&limit=100", "GET", token = token)
            val array = response.jsonArray()
            pageCount = response.pageCount ?: pageCount
            onProgress("Lade Wiedergabestände ($kindLabel): Seite $page${pageCount?.let { " von $it" } ?: ""}")
            hasMore = array.length() == 100
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                val media = row.optJSONObject(if (section == PortalSection.MOVIES) "movie" else "show") ?: continue
                val ids = media.optJSONObject("ids") ?: JSONObject()
                val model = TraktMedia(
                    section,
                    media.optString("title").ifBlank { media.optString("name") },
                    media.optInt("year").takeIf { it > 0 },
                    ids.optString("imdb").takeIf { it.startsWith("tt") },
                    ids.optLong("tmdb", 0).takeIf { it > 0 }?.toString(),
                    ids.optLong("trakt", 0).takeIf { it > 0 },
                    row.optString("paused_at").toInstantMillis(),
                    row.optJSONObject("episode")?.optInt("season", -1)?.takeIf { it >= 0 },
                    row.optJSONObject("episode")?.optInt("number", -1)?.takeIf { it > 0 },
                    row.optDouble("progress", Double.NaN).takeIf { it.isFinite() }?.toFloat()?.coerceIn(0f, 100f)
                )
                if (model.name.isNotBlank()) result += model
            }
            page++
            if (page > 1_000) error("Trakt hat ungewöhnlich viele Wiedergabestände geliefert; der Import wurde begrenzt.")
        } while (pageCount?.let { page <= it } ?: hasMore)
        return result
    }

    suspend fun scrobble(action: String, item: TraktMedia, accessToken: String, progress: Float) {
        require(action in setOf("start", "pause", "stop"))
        require(item.ids().length() > 0) { "Für diesen Titel fehlen Trakt-, IMDb- oder TMDB-IDs." }
        val media = JSONObject().put("title", item.name).put("ids", item.ids())
        val body = JSONObject().put("progress", progress.coerceIn(0f, 100f))
        if (item.section == PortalSection.MOVIES) {
            body.put("movie", media)
        } else {
            require((item.seasonNumber ?: 0) >= 0 && (item.episodeNumber ?: 0) > 0) {
                "Für diese Folge fehlen Staffel- oder Folgennummer."
            }
            body.put("show", media).put(
                "episode", JSONObject().put("season", item.seasonNumber).put("number", item.episodeNumber)
            )
        }
        request("/scrobble/$action", "POST", body, accessToken)
    }

    suspend fun setWatchlistItem(accessToken: String, item: TraktMedia, saved: Boolean) {
        require(item.ids().length() > 0) { "Für diesen Titel fehlen IMDb-, TMDB- und Trakt-ID." }
        val collection = JSONObject().put(
            if (item.section == PortalSection.MOVIES) "movies" else "shows",
            JSONArray().put(JSONObject().put("ids", item.ids()))
        )
        request(
            if (saved) "/sync/watchlist/add" else "/sync/watchlist/remove",
            "POST", collection, accessToken
        )
    }

    suspend fun addWatchedMovie(accessToken: String, item: TraktMedia) {
        require(item.section == PortalSection.MOVIES && item.ids().length() > 0)
        request("/sync/history", "POST", JSONObject().put(
            "movies", JSONArray().put(JSONObject().put("watched_at", Instant.now().toString()).put("ids", item.ids()))
        ), accessToken)
    }

    suspend fun addWatchedEpisode(accessToken: String, show: TraktMedia, season: Int, episode: Int) {
        require(show.section == PortalSection.SERIES && show.ids().length() > 0 && season >= 0 && episode > 0)
        request("/sync/history", "POST", JSONObject().put(
            "shows", JSONArray().put(JSONObject().put("ids", show.ids()).put(
                "seasons", JSONArray().put(JSONObject().put("number", season).put(
                    "episodes", JSONArray().put(JSONObject().put("number", episode))
                ))
            ))
        ), accessToken)
    }

    private suspend fun loadItems(
        path: String,
        section: PortalSection,
        token: String,
        onProgress: suspend (String) -> Unit,
        maxItems: Int? = null
    ): List<TraktMedia> {
        val result = mutableListOf<TraktMedia>()
        var page = 1
        var pageCount: Int? = null
        do {
            val limit = maxItems?.coerceIn(1, 100) ?: 100
            val response = request("$path?page=$page&limit=$limit", "GET", token = token)
            val array = response.jsonArray()
            pageCount = response.pageCount ?: pageCount
            val label = when (path) {
                "/sync/watchlist/movies", "/sync/watchlist/shows" -> "Merkliste"
                "/sync/watched/movies", "/sync/watched/shows" -> "Gesehene Titel"
                else -> "Trakt-Daten"
            }
            onProgress("Lade $label: Seite $page${pageCount?.let { " von $it" } ?: ""}")
            for (entry in 0 until array.length()) {
                val row = array.optJSONObject(entry) ?: continue
                val media = row.optJSONObject(if (section == PortalSection.MOVIES) "movie" else "show")
                    ?: row.takeIf { it.has("title") }
                    ?: continue
                val ids = media.optJSONObject("ids") ?: JSONObject()
                val imdb = ids.optString("imdb").takeIf { it.startsWith("tt") }
                val tmdb = ids.optLong("tmdb", 0).takeIf { it > 0 }?.toString()
                val trakt = ids.optLong("trakt", 0).takeIf { it > 0 }
                val watchedAt = row.optString("last_watched_at").ifBlank {
                    row.optString("watched_at")
                }.toInstantMillis()
                val episode = row.optJSONObject("episode")
                result += TraktMedia(
                    section, media.optString("title").ifBlank { media.optString("name") },
                    media.optInt("year").takeIf { it > 0 }, imdb, tmdb, trakt, watchedAt,
                    episode?.optInt("season", -1)?.takeIf { it >= 0 },
                    episode?.optInt("number", -1)?.takeIf { it > 0 }
                ).takeIf { it.name.isNotBlank() } ?: continue
            }
            page++
            if (page > 1_000) error("Trakt hat ungewöhnlich viele Ergebnisse geliefert; der Import wurde begrenzt.")
        } while ((pageCount?.let { page <= it } ?: (array.length() == 100)) &&
            (maxItems == null || result.size < maxItems))
        return result
    }

    /** The watched-shows endpoint already contains the episode-level watched state.
     * Reusing it avoids paging through a user's entire repeated watch-event history. */
    private suspend fun loadWatchedShows(
        token: String,
        onProgress: suspend (String) -> Unit
    ): Triple<List<TraktMedia>, List<TraktMedia>, List<TraktSeriesProgress>> {
        val shows = mutableListOf<TraktMedia>()
        val episodes = mutableListOf<TraktMedia>()
        val progress = mutableListOf<TraktSeriesProgress>()
        var page = 1
        var pageCount: Int? = null
        do {
            val response = request("/sync/watched/shows?page=$page&limit=100&extended=progress", "GET", token = token)
            val rows = response.jsonArray()
            pageCount = response.pageCount ?: pageCount
            onProgress("Lade gesehene Serien: Seite $page${pageCount?.let { " von $it" } ?: ""}")
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val show = row.optJSONObject("show") ?: continue
                val ids = show.optJSONObject("ids") ?: JSONObject()
                val name = show.optString("title").ifBlank { show.optString("name") }
                if (name.isBlank()) continue
                val imdb = ids.optString("imdb").takeIf { it.startsWith("tt") }
                val tmdb = ids.optLong("tmdb", 0).takeIf { it > 0 }?.toString()
                val trakt = ids.optLong("trakt", 0).takeIf { it > 0 }
                val watchedAt = row.optString("last_watched_at").toInstantMillis()
                val showMedia = TraktMedia(
                    PortalSection.SERIES, name, show.optInt("year").takeIf { it > 0 },
                    imdb, tmdb, trakt, watchedAt
                )
                shows += showMedia
                val seasons = row.optJSONArray("seasons") ?: show.optJSONArray("seasons") ?: continue
                var completedEpisodes = 0
                for (seasonIndex in 0 until seasons.length()) {
                    val season = seasons.optJSONObject(seasonIndex) ?: continue
                    val seasonNumber = season.optInt("number", -1).takeIf { it >= 0 } ?: continue
                    val seasonEpisodes = season.optJSONArray("episodes") ?: continue
                    for (episodeIndex in 0 until seasonEpisodes.length()) {
                        val episode = seasonEpisodes.optJSONObject(episodeIndex) ?: continue
                        val episodeNumber = episode.optInt("number", -1).takeIf { it > 0 } ?: continue
                        val plays = episode.optInt("plays", 0)
                        val episodeWatchedAt = episode.optString("last_watched_at").toInstantMillis()
                        if (plays <= 0 && episodeWatchedAt == null) continue
                        completedEpisodes++
                        episodes += TraktMedia(
                            PortalSection.SERIES, name, show.optInt("year").takeIf { it > 0 },
                            imdb, tmdb, trakt,
                            episodeWatchedAt ?: watchedAt,
                            seasonNumber, episodeNumber
                        )
                    }
                }
                val aired = show.optInt("aired_episodes", 0).coerceAtLeast(completedEpisodes)
                if (aired > 0) progress += TraktSeriesProgress(showMedia, completedEpisodes, aired)
            }
            page++
            if (page > 1_000) error("Trakt hat ungewöhnlich viele Serien geliefert; der Import wurde begrenzt.")
        } while (pageCount?.let { page <= it } ?: (rows.length() == 100))
        return Triple(shows, episodes, progress)
    }

    private suspend fun loadSeriesProgress(
        shows: List<TraktMedia>, token: String, onProgress: suspend (String) -> Unit
    ): List<TraktSeriesProgress> = coroutineScope {
        val limiter = kotlinx.coroutines.sync.Semaphore(4)
        val unique = shows.distinctBy(::mediaIdentity).filter { it.traktId != null || it.imdbId != null || it.tmdbId != null }
        val completed = java.util.concurrent.atomic.AtomicInteger()
        unique.map { show ->
            async {
                limiter.withPermit {
                    val id = show.traktId?.toString() ?: show.imdbId ?: show.tmdbId?.let { "tmdb:$it" }
                    val result = runCatching {
                        val response = request(
                            "/shows/${URLEncoder.encode(id, "UTF-8")}/progress/watched?hidden=false&specials=false",
                            "GET", token = token
                        ).jsonObject()
                        TraktSeriesProgress(
                            show,
                            response.optInt("completed", -1).takeIf { it >= 0 } ?: return@runCatching null,
                            response.optInt("aired", -1).takeIf { it >= 0 } ?: return@runCatching null
                        )
                    }.getOrNull()
                    val done = completed.incrementAndGet()
                    if (done % 10 == 0 || done == unique.size) onProgress("Prüfe Serienfortschritt: $done / ${unique.size}")
                    result
                }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun loadRecentEpisodeHistory(
        token: String,
        onProgress: suspend (String) -> Unit
    ): List<TraktMedia> {
        val result = mutableListOf<TraktMedia>()
        val recentShows = linkedSetOf<String>()
        val response = request("/sync/history/episodes?page=1&limit=100", "GET", token = token)
        val rows = response.jsonArray()
        onProgress("Prüfe die letzten Trakt-Folgen")
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val show = row.optJSONObject("show") ?: continue
            val ids = show.optJSONObject("ids") ?: JSONObject()
            val name = show.optString("title").ifBlank { show.optString("name") }
            if (name.isBlank()) continue
            val imdb = ids.optString("imdb").takeIf { it.startsWith("tt") }
            val tmdb = ids.optLong("tmdb", 0).takeIf { it > 0 }?.toString()
            val trakt = ids.optLong("trakt", 0).takeIf { it > 0 }
            val identity = trakt?.let { "trakt:$it" } ?: imdb ?: tmdb ?: normalizedLabel(name)
            if (identity !in recentShows && recentShows.size >= 10) continue
            recentShows += identity
            val episode = row.optJSONObject("episode") ?: continue
            val season = episode.optInt("season", -1).takeIf { it >= 0 } ?: continue
            val number = episode.optInt("number", -1).takeIf { it > 0 } ?: continue
            result += TraktMedia(
                PortalSection.SERIES, name, show.optInt("year").takeIf { it > 0 },
                imdb, tmdb, trakt, row.optString("watched_at").toInstantMillis(), season, number
            )
        }
        return result
    }

    private suspend fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        token: String? = null
    ): Response = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("https://api.trakt.tv$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 20_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("trakt-api-version", "2")
                    setRequestProperty("trakt-api-key", clientId)
                    token?.let { setRequestProperty("Authorization", "Bearer $it") }
                    if (body != null) {
                        doOutput = true
                        outputStream.use { it.write(body.toString().encodeToByteArray()) }
                    }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(8_000_000) }.orEmpty()
                if (status == 429 && attempt++ < 2) {
                    val waitSeconds = connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceIn(1, 60) ?: 5
                    connection.disconnect()
                    delay(waitSeconds * 1_000)
                    continue
                }
                if (status !in 200..299) {
                    val error = runCatching { JSONObject(content) }.getOrNull()
                    val code = error?.optString("error")?.takeIf(String::isNotBlank)
                    val description = error?.optString("error_description")?.takeIf(String::isNotBlank)
                        ?: error?.optString("message")?.takeIf(String::isNotBlank)
                        ?: "HTTP $status"
                    throw TraktApiException(status, code, description)
                }
                return@withContext Response(status, content, connection.getHeaderField("X-Pagination-Page-Count")?.toIntOrNull())
            } catch (error: CancellationException) {
                throw error
            } catch (error: TraktApiException) {
                throw error
            } catch (error: Exception) {
                throw TraktApiException(null, null, "Trakt ist momentan nicht erreichbar: ${error.message.orEmpty()}")
            } finally {
                connection?.disconnect()
            }
        }
        error("Trakt-Anfrage fehlgeschlagen.")
    }

    private fun JSONObject.session(clientId: String, username: String = "Trakt"): TraktSession {
        val access = optString("access_token").takeIf(String::isNotBlank) ?: error("Trakt hat kein Zugriffstoken geliefert.")
        val refresh = optString("refresh_token").takeIf(String::isNotBlank) ?: error("Trakt hat kein Erneuerungstoken geliefert.")
        return TraktSession(
            clientId, access, refresh,
            System.currentTimeMillis() + optLong("expires_in", 0).coerceAtLeast(0) * 1_000L,
            username
        )
    }

    private fun Response.jsonObject(): JSONObject = JSONObject(body)
    private fun Response.jsonArray(): JSONArray = JSONArray(body)

    private fun String?.toInstantMillis(): Long? = this?.takeIf(String::isNotBlank)
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    private fun isTrustedActivationUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.host in setOf("trakt.tv", "www.trakt.tv", "auth.trakt.tv")
    }.getOrDefault(false)

    private fun mediaIdentity(item: TraktMedia): String =
        "${item.section}:${item.imdbId ?: item.tmdbId ?: item.traktId ?: normalizedLabel(item.name)}:${item.year ?: ""}"
}

internal class TraktApiException(
    val status: Int?,
    val apiCode: String?,
    message: String
) : IllegalStateException(message)

/** Keys that identify a film across Trakt, TMDB and portal titles. */
internal fun movieIdentityKeys(name: String, year: Int?, imdbId: String?, tmdbId: String?): Set<String> = buildSet {
    imdbId?.takeIf { it.startsWith("tt") }?.let { add("imdb:${it.lowercase()}") }
    tmdbId?.toLongOrNull()?.takeIf { it > 0 }?.let { add("tmdb:$it") }
    normalizedLabel(displayVodTitle(PortalTitle(name))).takeIf { it.isNotBlank() && year != null }?.let { add("title:$it:$year") }
}

/** Trakt's ID lookup: `/search/imdb/tt…` or `/search/tmdb/…` (not `tmdb:…`). */
internal fun traktIdSearchPath(title: PortalTitle): String? =
    title.imdbId?.takeIf { it.matches(Regex("tt[0-9]+")) }?.let { "/search/imdb/$it" }
        ?: title.tmdbId?.toLongOrNull()?.takeIf { it > 0 }?.let { "/search/tmdb/$it" }
