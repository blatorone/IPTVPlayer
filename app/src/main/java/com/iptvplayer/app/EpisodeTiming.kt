package com.iptvplayer.app

import org.json.JSONObject

internal data class EpisodeTiming(
    val introStartMs: Long? = null,
    val introEndMs: Long? = null,
    val creditsStartMs: Long? = null
) {
    fun intro(duration: Long): LongRange? {
        val from = introStartMs ?: return null
        val to = introEndMs ?: return null
        return (from until to).takeIf { from >= 0 && from < to && to < duration }
    }

    fun nextAt(duration: Long): Long =
        creditsStartMs?.takeIf { it > 0 && it < duration } ?: duration
}

internal fun JSONObject.episodeTiming(): EpisodeTiming {
    fun position(vararg fields: String): Long? {
        val containers = listOfNotNull(optJSONObject("markers"), this, optJSONObject("info"))
        for (field in fields) for (container in containers) {
            val ms = container.optDouble("${field}_ms")
            if (ms.isFinite() && ms in 0.0..86_400_000.0) return ms.toLong()
            val seconds = container.optDouble(field)
            if (seconds.isFinite() && seconds in 0.0..86_400.0) return (seconds * 1_000).toLong()
        }
        return null
    }
    return EpisodeTiming(
        position("intro_start", "intro_start_seconds"),
        position("intro_end", "intro_end_seconds"),
        position("credits_start", "outro_start")
    )
}

internal fun followingEpisode(
    seasons: List<PortalSeason>, season: PortalSeason, episode: PortalTitle
): Pair<PortalSeason, PortalTitle>? {
    fun ordered(items: List<PortalTitle>): List<PortalTitle> =
        if (items.all { it.episode?.toIntOrNull() != null }) items.sortedBy { it.episode?.toIntOrNull() } else items
    val episodes = ordered(season.episodes)
    val index = episodes.indexOf(episode)
    if (index < 0) return null
    episodes.getOrNull(index + 1)?.let { return season to it }
    val seasonIndex = seasons.indexOf(season)
    if (seasonIndex < 0) return null
    return seasons.drop(seasonIndex + 1).firstOrNull { it.episodes.isNotEmpty() }?.let {
        it to ordered(it.episodes).first()
    }
}
