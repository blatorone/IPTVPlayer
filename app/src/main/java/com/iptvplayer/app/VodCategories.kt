package com.iptvplayer.app

internal enum class VodCategory(val label: Int, terms: String) {
    ALL(R.string.all_titles, ""),
    RECENT(R.string.vod_category_recent, "neu|new|recent|latest|premieren"),
    THIS_YEAR(R.string.vod_category_this_year, ""),
    POPULAR(R.string.vod_category_popular, "beliebt|popular|trending|top[ -]?(?:filme|movies|serien|series|rated)"),
    CINEMA(R.string.vod_category_cinema, "kino|kinofilme|cinema|theatrical"),
    CINEMA_THIS_YEAR(R.string.vod_category_cinema_year, ""),
    CINEMA_LAST_YEAR(R.string.vod_category_cinema_year, ""),
    NETFLIX(R.string.vod_category_netflix, "netflix"),
    PRIME(R.string.vod_category_prime, "prime(?:[ -]?video)?|amazon[ -]?prime"),
    APPLE(R.string.vod_category_apple, "apple(?:[ -]?tv\\+?|[ -]?originals)?"),
    DISNEY(R.string.vod_category_disney, "disney(?:[ -]?plus|\\+)?"),
    MAX(R.string.vod_category_max, "hbo(?:[ -]?max)?|warner[ -]?max"),
    PARAMOUNT(R.string.vod_category_paramount, "paramount(?:[ -]?plus|\\+)?"),
    SKY(R.string.live_category_sky, "sky|wow"),
    PEACOCK(R.string.vod_category_peacock, "peacock"),
    FAMILY(R.string.vod_category_family, "kinder|kids|familie|family|familienfilme"),
    ANIMATION(R.string.vod_category_animation, "animation|anime|cartoons?|zeichentrick"),
    ACTION(R.string.vod_category_action, "action|adventure|abenteuer|martial arts"),
    COMEDY(R.string.vod_category_comedy, "comedy|kom[oö]die[n]?|humor|sitcom"),
    HORROR(R.string.vod_category_horror, "horror|grusel"),
    CRIME(R.string.vod_category_crime, "crime|krimi|kriminal|thriller|mystery"),
    FANTASY(R.string.vod_category_fantasy, "sci[ -]?fi|science fiction|fantasy|superhelden"),
    DOCUMENTARY(R.string.live_category_documentary, "documentary|documentaries|dokus?|wissen"),
    ROMANCE(R.string.vod_category_romance, "romance|romantik|liebe"),
    DRAMA(R.string.vod_category_drama, "drama|dramen"),
    OTHER(R.string.vod_category_other, "");

    val id: String get() = "vod:$name"
    val matcher = Regex("(?:^|[^\\p{L}\\p{N}])(?:$terms)(?=$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    val derived: Boolean get() = this in setOf(ALL, THIS_YEAR, CINEMA_THIS_YEAR, CINEMA_LAST_YEAR, OTHER)
    val service: Boolean get() = this in NETFLIX..PEACOCK
}

internal fun vodCategory(group: PortalGroup): VodCategory =
    serviceCategory(group.name) ?: VodCategory.entries.firstOrNull {
        !it.derived && !it.service && it.matcher.containsMatchIn(group.name)
    } ?: VodCategory.OTHER

internal fun serviceCategory(label: String): VodCategory? =
    VodCategory.entries.firstOrNull { it.service && it.matcher.containsMatchIn(label) }
