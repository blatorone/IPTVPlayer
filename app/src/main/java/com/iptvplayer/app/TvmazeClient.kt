package com.iptvplayer.app

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class TvmazeTitleMetadata(val title: String, val year: Int?, val overview: String?, val rating: Double?)

internal object TvmazeClient {
    suspend fun lookupTitleMetadata(title: PortalTitle): TvmazeTitleMetadata? = withContext(Dispatchers.IO) {
        val imdb = title.imdbId?.takeIf { it.matches(Regex("tt[0-9]+")) }
        val name = displayVodTitle(title)
        val show = if (imdb != null) {
            request("lookup/shows?imdb=$imdb")?.let(::JSONObject)
        } else {
            val found = request("search/shows?q=${URLEncoder.encode(name, "UTF-8")}") ?: return@withContext null
            val entries = JSONArray(found)
            (0 until entries.length()).mapNotNull { index ->
                entries.optJSONObject(index)?.optJSONObject("show")?.takeIf { row ->
                    val year = row.optString("premiered").take(4).toIntOrNull()
                    normalizedLabel(row.optString("name")) == normalizedLabel(name) &&
                        (title.year == null || title.year == year)
                }
            }.singleOrNull()
        } ?: return@withContext null
        val summary = show.optString("summary").takeIf(String::isNotBlank)?.let { html ->
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
        }
        TvmazeTitleMetadata(
            show.optString("name").takeIf(String::isNotBlank) ?: title.name,
            show.optString("premiered").take(4).toIntOrNull(), summary,
            show.optJSONObject("rating")?.optDouble("average")?.takeIf { it.isFinite() && it > 0 }
        )
    }

    /** Aired episodes plus the next episode and the next season start (same shape as Trakt's feed). */
    suspend fun episodeFeed(title: PortalTitle): TraktEpisodeFeed? = withContext(Dispatchers.IO) {
        val imdb = title.imdbId?.takeIf { it.matches(Regex("tt[0-9]+")) }
        val name = displayVodTitle(title)
        val show = if (imdb != null) {
            request("lookup/shows?imdb=$imdb")?.let(::JSONObject)
        } else {
            val found = request("search/shows?q=${URLEncoder.encode(name, "UTF-8")}") ?: return@withContext null
            val entries = JSONArray(found)
            (0 until entries.length()).mapNotNull { index ->
                entries.optJSONObject(index)?.optJSONObject("show")?.takeIf { row ->
                    val year = row.optString("premiered").take(4).toIntOrNull()
                    normalizedLabel(row.optString("name")) == normalizedLabel(name) &&
                        (title.year == null || title.year == year)
                }
            }.singleOrNull()
        }
        val id = show?.optInt("id")?.takeIf { it > 0 } ?: return@withContext null
        val body = request("shows/$id/episodes") ?: return@withContext null
        val episodes = JSONArray(body)
        val today = LocalDate.now()
        val aired = linkedSetOf<String>()
        val upcoming = mutableListOf<Pair<LocalDate, TraktUpcomingEpisode>>()
        for (index in 0 until episodes.length()) {
            val row = episodes.optJSONObject(index) ?: continue
            val airdate = runCatching { LocalDate.parse(row.optString("airdate")) }.getOrNull() ?: continue
            val season = row.optInt("season", -1)
            val number = row.optInt("number", -1)
            if (season < 0 || number < 0) continue
            if (!airdate.isAfter(today)) aired += "$season:$number"
            else upcoming += airdate to TraktUpcomingEpisode(row.optString("name").ifBlank { null }, season, number, airdate.toString())
        }
        TraktEpisodeFeed(
            aired, upcoming.minByOrNull { it.first }?.second,
            upcoming.filter { (_, episode) -> episode.number == 1 && (episode.season ?: 0) >= 1 }.minByOrNull { it.first }?.second
        )
    }

    private fun request(path: String): String? = request(URI("https://api.tvmaze.com/$path"), 0)

    private fun request(uri: URI, redirects: Int): String? {
        require(uri.scheme == "https" && uri.host == "api.tvmaze.com" && redirects <= 2)
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 7_000
            connection.readTimeout = 7_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "IPTVPlayer")
            val status = connection.responseCode
            if (status in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location") ?: return null
                return request(uri.resolve(location), redirects + 1)
            }
            if (status == 404) return null
            check(status == 200)
            return connection.inputStream.use { input ->
                val result = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(result.size() + count <= 4 * 1024 * 1024)
                    result.write(buffer, 0, count)
                }
                result.toString("UTF-8")
            }
        } finally {
            connection.disconnect()
        }
    }
}
