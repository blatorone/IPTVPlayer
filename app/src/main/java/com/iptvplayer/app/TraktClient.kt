package com.iptvplayer.app

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    val watchedEpisodes: List<TraktMedia> = emptyList()
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

    suspend fun loadLibrary(
        accessToken: String,
        onProgress: suspend (String) -> Unit = {}
    ): TraktLibrary = withContext(Dispatchers.IO) {
        val watchlist = loadItems("/sync/watchlist/movies", PortalSection.MOVIES, accessToken, onProgress) +
            loadItems("/sync/watchlist/shows", PortalSection.SERIES, accessToken, onProgress)
        val watchedMovies = loadItems(
            "/sync/watched/movies", PortalSection.MOVIES, accessToken, onProgress, maxItems = 10
        )
        val (watchedShows, aggregatedEpisodes) = loadWatchedShows(accessToken, onProgress)
        val watchedEpisodes = aggregatedEpisodes.ifEmpty {
            // Keep compatibility with responses without season data, but only
            // inspect the newest 100 events and at most ten distinct shows.
            loadRecentEpisodeHistory(accessToken, onProgress)
        }
        val watched = watchedMovies + watchedShows
        val playback = loadPlaybackProgress(accessToken, onProgress)
        TraktLibrary(
            watchlist.distinctBy(::mediaIdentity), watched.distinctBy(::mediaIdentity),
            playback.distinctBy { "${mediaIdentity(it)}:${it.seasonNumber}:${it.episodeNumber}" },
            watchedEpisodes.distinctBy { "${mediaIdentity(it)}:${it.seasonNumber}:${it.episodeNumber}:${it.watchedAtMillis}" }
        )
    }

    private suspend fun loadPlaybackProgress(
        accessToken: String,
        onProgress: suspend (String) -> Unit
    ): List<TraktMedia> = loadPlaybackItems("/sync/playback/movies", PortalSection.MOVIES, accessToken, onProgress) +
        loadPlaybackItems("/sync/playback/episodes", PortalSection.SERIES, accessToken, onProgress)

    private suspend fun loadPlaybackItems(
        path: String, section: PortalSection, token: String, onProgress: suspend (String) -> Unit
    ): List<TraktMedia> {
        val result = mutableListOf<TraktMedia>()
        var page = 1
        var pageCount: Int? = null
        var hasMore = true
        do {
            val response = request("$path?page=$page&limit=100", "GET", token = token)
            val array = response.jsonArray()
            pageCount = response.pageCount ?: pageCount
            onProgress("Lade Wiedergabest\u00e4nde: Seite $page${pageCount?.let { " von $it" } ?: ""}")
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
    ): Pair<List<TraktMedia>, List<TraktMedia>> {
        val shows = mutableListOf<TraktMedia>()
        val episodes = mutableListOf<TraktMedia>()
        run {
            val response = request("/sync/watched/shows?page=1&limit=10", "GET", token = token)
            val rows = response.jsonArray()
            onProgress("Lade die zehn zuletzt gesehenen Serien")
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
                shows += TraktMedia(
                    PortalSection.SERIES, name, show.optInt("year").takeIf { it > 0 },
                    imdb, tmdb, trakt, watchedAt
                )
                val seasons = row.optJSONArray("seasons") ?: show.optJSONArray("seasons") ?: continue
                for (seasonIndex in 0 until seasons.length()) {
                    val season = seasons.optJSONObject(seasonIndex) ?: continue
                    val seasonNumber = season.optInt("number", -1).takeIf { it >= 0 } ?: continue
                    val seasonEpisodes = season.optJSONArray("episodes") ?: continue
                    for (episodeIndex in 0 until seasonEpisodes.length()) {
                        val episode = seasonEpisodes.optJSONObject(episodeIndex) ?: continue
                        val episodeNumber = episode.optInt("number", -1).takeIf { it > 0 } ?: continue
                        episodes += TraktMedia(
                            PortalSection.SERIES, name, show.optInt("year").takeIf { it > 0 },
                            imdb, tmdb, trakt,
                            episode.optString("last_watched_at").toInstantMillis() ?: watchedAt,
                            seasonNumber, episodeNumber
                        )
                    }
                }
            }
        }
        return shows to episodes
    }

    private suspend fun loadRecentEpisodeHistory(
        token: String,
        onProgress: suspend (String) -> Unit
    ): List<TraktMedia> {
        val rows = request("/sync/history/episodes?page=1&limit=100", "GET", token = token).jsonArray()
        onProgress("Prüfe die letzten Trakt-Folgen")
        val result = mutableListOf<TraktMedia>()
        val recentShows = linkedSetOf<String>()
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
