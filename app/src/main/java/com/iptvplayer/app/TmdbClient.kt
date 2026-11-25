package com.iptvplayer.app

import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class TmdbTitle(
    val id: Int,
    val title: String,
    val originalTitle: String,
    val year: Int?,
    val overview: String?,
    val poster: String?,
    val popularity: Double?,
    val rating: Double?
) {
    fun applyTo(portal: PortalTitle): PortalTitle = portal.copy(
        name = title,
        year = year ?: portal.year,
        description = germanDescription(overview) ?: germanDescription(portal.description),
        poster = poster ?: portal.poster,
        popularity = popularity
    )

    fun matches(portal: PortalTitle): Boolean {
        val names = setOf(title, originalTitle).map(::normalizedLabel)
        if (normalizedLabel(displayVodTitle(portal)) !in names) return false
        val portalYear = vodReleaseYear(portal)
        return year == null || portalYear == null || kotlin.math.abs(year - portalYear) <= 1
    }
}

internal data class TmdbPage(val items: List<TmdbTitle>, val nextPage: Int?)
internal data class TmdbEpisodeDetails(val description: String?, val picture: String?)

internal fun VodCategory.usesTmdb(section: PortalSection): Boolean =
    this != VodCategory.ALL && this != VodCategory.OTHER &&
        (section == PortalSection.MOVIES ||
            this !in setOf(VodCategory.CINEMA, VodCategory.CINEMA_THIS_YEAR, VodCategory.CINEMA_LAST_YEAR))

internal class TmdbClient(private val token: String) {
    private val metadata = LruCache<String, TmdbTitle>(256)
    init {
        require(token.length in 16..2048 && token.all { it.isLetterOrDigit() || it in "-_." })
    }

    suspend fun validate() {
        request("configuration")
    }

    suspend fun lookup(section: PortalSection, title: PortalTitle): TmdbTitle? {
        val key = "${section.name}:${title.tmdbId}:${title.imdbId}:${displayVodTitle(title)}:${vodReleaseYear(title)}"
        return metadata.get(key) ?: lookupUncached(section, title)?.also { metadata.put(key, it) }
    }

    private suspend fun lookupUncached(section: PortalSection, title: PortalTitle): TmdbTitle? {
        val kind = mediaKind(section)
        title.tmdbId?.toIntOrNull()?.takeIf { it > 0 }?.let { id ->
            val direct = try {
                readTitle(request("$kind/$id"), section)
            } catch (error: TmdbHttpException) {
                if (error.status != 404) throw error
                null
            }
            if (direct?.matches(title) == true) return direct
        }
        val imdb = title.imdbId?.takeIf { it.matches(Regex("tt[0-9]+")) }
        if (imdb != null) {
            val field = if (section == PortalSection.MOVIES) "movie_results" else "tv_results"
            val result = request("find/$imdb", mapOf("external_source" to "imdb_id"))
                .optJSONArray(field).titles(section).filter { it.matches(title) }
            if (result.size == 1) return result.first()
        }
        val names = displayVodTitle(title)
        if (names.isBlank()) return null
        val params = mutableMapOf("query" to names, "include_adult" to "false")
        vodReleaseYear(title)?.let { year ->
            params[if (section == PortalSection.MOVIES) "year" else "first_air_date_year"] = year.toString()
        }
        val results = request("search/$kind", params).optJSONArray("results").titles(section)
        return results.filter { it.matches(title) }.distinctBy { it.id }.singleOrNull()
    }

    suspend fun shelf(section: PortalSection, category: VodCategory, region: String, page: Int): TmdbPage {
        val kind = mediaKind(section)
        val today = LocalDate.now()
        val params = mutableMapOf("page" to page.toString())
        val path = when (category) {
            VodCategory.POPULAR -> "trending/$kind/week"
            VodCategory.CINEMA, VodCategory.CINEMA_THIS_YEAR, VodCategory.CINEMA_LAST_YEAR -> {
                require(section == PortalSection.MOVIES)
                val first = when (category) {
                    VodCategory.CINEMA -> today.minusDays(30)
                    VodCategory.CINEMA_THIS_YEAR -> today.withDayOfYear(1)
                    else -> today.minusYears(1).withDayOfYear(1)
                }
                val last = if (category == VodCategory.CINEMA_LAST_YEAR) today.withDayOfYear(1).minusDays(1) else today
                // Year categories use the original release date, so re-releases (for example an
                // anniversary screening of an old film) do not count as films of that year. The
                // film must still have had a theatrical release in the region.
                val dateField = if (category == VodCategory.CINEMA) "release_date" else "primary_release_date"
                params += mapOf(
                    "region" to region, "$dateField.gte" to first.toString(),
                    "$dateField.lte" to last.toString(), "with_release_type" to "3|2"
                )
                "discover/movie"
            }
            VodCategory.THIS_YEAR, VodCategory.RECENT -> {
                val first = if (category == VodCategory.RECENT) today.minusDays(90) else today.withDayOfYear(1)
                val field = if (section == PortalSection.MOVIES) "primary_release_date" else "first_air_date"
                params["$field.gte"] = first.toString()
                params["$field.lte"] = today.toString()
                params["sort_by"] = "$field.desc"
                "discover/$kind"
            }
            else -> {
                if (category.service) {
                    val services = request("watch/providers/$kind", mapOf("watch_region" to region))
                        .optJSONArray("results").objects()
                        .filter { serviceCategory(it.optString("provider_name")) == category }
                        .mapNotNull { it.optInt("provider_id").takeIf { id -> id > 0 } }
                    if (services.isEmpty()) return TmdbPage(emptyList(), null)
                    params["watch_region"] = region
                    params["with_watch_providers"] = services.distinct().joinToString("|")
                    params["with_watch_monetization_types"] = "flatrate"
                } else {
                    val genres = request("genre/$kind/list", mapOf("language" to "en-US"))
                        .optJSONArray("genres").objects()
                        .filter { category.matcher.containsMatchIn(it.optString("name")) }
                        .mapNotNull { it.optInt("id").takeIf { id -> id > 0 } }
                    if (genres.isEmpty()) return TmdbPage(emptyList(), null)
                    params["with_genres"] = genres.joinToString("|")
                }
                "discover/$kind"
            }
        }
        val result = request(path, params + ("include_adult" to "false"))
        val items = result.optJSONArray("results").titles(section)
        return TmdbPage(items, (page + 1).takeIf { it <= result.optInt("total_pages") && it <= 500 })
    }

    suspend fun airedEpisodes(title: PortalTitle): Set<String>? {
        val item = lookup(PortalSection.SERIES, title) ?: return null
        val series = request("tv/${item.id}")
        val last = series.optJSONObject("last_episode_to_air") ?: return emptySet()
        val number = last.optInt("season_number", -1)
        if (number < 0) return emptySet()
        val today = LocalDate.now()
        return request("tv/${item.id}/season/$number").optJSONArray("episodes").objects().mapNotNull { episode ->
            val date = runCatching { LocalDate.parse(episode.optString("air_date")) }.getOrNull()
            val index = episode.optInt("episode_number", -1)
            if (date != null && !date.isAfter(today) && index >= 0) "$number:$index" else null
        }.toSet()
    }

    suspend fun episodeDetails(seriesId: Int, season: Int): Map<Int, TmdbEpisodeDetails> =
        request("tv/$seriesId/season/$season").optJSONArray("episodes").objects()
            .mapNotNull { episode ->
                val number = episode.optInt("episode_number", -1).takeIf { it >= 0 } ?: return@mapNotNull null
                val picture = episode.optString("still_path").takeIf { it.matches(Regex("/[A-Za-z0-9_.-]+")) }
                    ?.let { "https://image.tmdb.org/t/p/w300$it" }
                number to TmdbEpisodeDetails(germanDescription(episode.value("overview")), picture)
            }.toMap()

    /** Main cast of a film or series, in billing order. */
    suspend fun cast(section: PortalSection, id: Int): List<CastMember> {
        val movie = section == PortalSection.MOVIES
        return request(if (movie) "movie/$id/credits" else "tv/$id/aggregate_credits")
            .optJSONArray("cast").objects().take(60).mapNotNull { row ->
                val name = row.value("name") ?: return@mapNotNull null
                val role = if (movie) row.value("character")
                else row.optJSONArray("roles").objects().firstNotNullOfOrNull { it.value("character") }
                CastMember(name, role, profileImage(row), row.optLong("id").takeIf { it > 0 }, null)
            }.distinctBy { it.tmdbId ?: it.name }
    }

    /** Films and series a person appeared in. */
    suspend fun personCredits(personId: Long): List<PersonCredit> =
        request("person/$personId/combined_credits").optJSONArray("cast").objects().mapNotNull { row ->
            val section = when (row.value("media_type")) {
                "movie" -> PortalSection.MOVIES
                "tv" -> PortalSection.SERIES
                else -> return@mapNotNull null
            }
            val movie = section == PortalSection.MOVIES
            val title = row.value(if (movie) "title" else "name") ?: return@mapNotNull null
            PersonCredit(
                section, title, row.value(if (movie) "original_title" else "original_name"),
                row.value(if (movie) "release_date" else "first_air_date")?.take(4)?.toIntOrNull(),
                null, row.optInt("id").takeIf { it > 0 }?.toString()
            )
        }.distinctBy { "${it.section}:${it.tmdbId}" }

    private fun profileImage(row: JSONObject): String? =
        row.value("profile_path")?.takeIf { it.matches(Regex("/[A-Za-z0-9_.-]+")) }
            ?.let { "https://image.tmdb.org/t/p/w185$it" }

    private suspend fun request(path: String, parameters: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val query = (mapOf("language" to "de-DE") + parameters).entries.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }
            val connection = URI("https://api.themoviedb.org/3/$path?$query").toURL()
                .openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 7_000
                connection.readTimeout = 7_000
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")
                val status = connection.responseCode
                if (status != 200) throw TmdbHttpException(status)
                val data = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        check(output.size() + count <= 4 * 1024 * 1024)
                        output.write(buffer, 0, count)
                    }
                    output.toString("UTF-8")
                }
                JSONObject(data)
            } finally {
                connection.disconnect()
            }
        }
}

private class TmdbHttpException(val status: Int) : IllegalStateException()

private fun mediaKind(section: PortalSection) = if (section == PortalSection.MOVIES) "movie" else "tv"

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else List(length()) { optJSONObject(it) }.filterNotNull()

private fun JSONArray?.titles(section: PortalSection): List<TmdbTitle> =
    objects().mapNotNull { readTitle(it, section) }

private fun JSONObject.value(name: String): String? =
    opt(name)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }

private fun readTitle(row: JSONObject, section: PortalSection): TmdbTitle? {
    val id = row.optInt("id").takeIf { it > 0 } ?: return null
    val movie = section == PortalSection.MOVIES
    val title = row.value(if (movie) "title" else "name") ?: return null
    val original = row.value(if (movie) "original_title" else "original_name") ?: title
    val year = row.value(if (movie) "release_date" else "first_air_date")?.take(4)?.toIntOrNull()
    val image = row.value("poster_path")?.takeIf { it.matches(Regex("/[A-Za-z0-9_.-]+")) }
        ?.let { "https://image.tmdb.org/t/p/w500$it" }
    return TmdbTitle(
        id, title, original, year, row.value("overview"), image,
        row.optDouble("popularity").takeIf { it.isFinite() && it > 0 },
        row.optDouble("vote_average").takeIf { it.isFinite() && it > 0 }
    )
}
