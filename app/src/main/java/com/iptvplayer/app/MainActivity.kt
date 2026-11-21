package com.iptvplayer.app

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.util.TypedValue
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private val storage by lazy { PortalStorage(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (player.isPlaying ||
                    (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING)
                ) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        })
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = playerColors) {
                PlayerHome(
                    player = player,
                    storage = storage,
                    onFullscreen = { fullscreen ->
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        val bars = WindowInsetsCompat.Type.systemBars()
                        if (fullscreen) controller.hide(bars) else controller.show(bars)
                    },
                    canPlay = { lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
                )
            }
        }
    }

    override fun onStop() {
        player.pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

private enum class Destination(val label: Int) {
    LIVE(R.string.destination_live),
    GUIDE(R.string.destination_guide),
    MOVIES(R.string.destination_movies),
    SERIES(R.string.destination_series),
    SEARCH(R.string.search),
    SETTINGS(R.string.destination_settings),
    CATEGORIES(R.string.category_settings),
    SOURCES(R.string.destination_sources)
}

private val mainDestinations = Destination.entries.filter {
    it != Destination.SOURCES && it != Destination.CATEGORIES
}

private val playerColors = darkColorScheme(
    primary = Color(0xFF8BB8FF),
    surface = Color(0xFF0E121B),
    surfaceContainer = Color(0xFF171E2B),
    secondaryContainer = Color(0xFF253650),
    onSecondaryContainer = Color(0xFFD7E7FF),
    tertiaryContainer = Color(0xFFB1CFFF),
    onTertiaryContainer = Color(0xFF12243D)
)

private data class PlaybackSelection(
    val source: String,
    val section: PortalSection,
    val id: String,
    val episode: String? = null,
    val seriesId: String? = null
)

private data class PlaybackVariant(
    val label: String,
    val timing: EpisodeTiming = EpisodeTiming(),
    val resolve: suspend () -> String
)
private data class PlaybackAttempt(
    val selection: PlaybackSelection,
    val channel: StalkerChannel,
    val variants: List<PlaybackVariant>,
    val index: Int,
    val positionMs: Long,
    val progressKey: String?,
    val mediaId: String = ""
)

private enum class SeriesActivityView { HISTORY, WATCHLIST }

private fun liveSelection(channel: StalkerChannel, group: LiveChannelGroup, combine: Boolean): PlaybackSelection {
    val source = group.variants.first().item.source.identity
    val key = group.identity ?: channel.id ?: channel.command
    return PlaybackSelection(if (combine && group.identity != null) "catalogue" else source, PortalSection.LIVE, key)
}

private fun playbackTime(positionMs: Long): String {
    val seconds = positionMs / 1_000
    val minutes = seconds / 60
    val remainder = (seconds % 60).toString().padStart(2, '0')
    return if (minutes < 60) "$minutes:$remainder"
    else "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:$remainder"
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerHome(
    player: ExoPlayer? = null,
    storage: PortalStorage? = null,
    onFullscreen: (Boolean) -> Unit = {},
    canPlay: () -> Boolean = { false }
) {
    var selected by rememberSaveable { mutableStateOf<Destination?>(null) }
    var lastFocused by rememberSaveable { mutableStateOf(Destination.LIVE) }
    val initialFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preview = LocalInspectionMode.current
    val preferences = remember(context, preview) {
        if (preview) null else context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    }
    val progressPreferences = remember(context, preview) {
        if (preview) null else context.getSharedPreferences("vod_progress", Context.MODE_PRIVATE)
    }
    var seriesActivity by remember(progressPreferences) {
        mutableStateOf(decodeSeriesActivity(progressPreferences?.getString("series_activity", null)))
    }
    var seriesActivityView by rememberSaveable { mutableStateOf<SeriesActivityView?>(null) }
    var movieHistoryOpen by rememberSaveable { mutableStateOf(false) }
    var movieHistory by remember(progressPreferences) {
        mutableStateOf(decodeMovieHistory(progressPreferences?.getString("movie_history", null)))
    }
    var metadataClient by remember { mutableStateOf<TmdbClient?>(null) }
    var metadataSettingsOpen by remember { mutableStateOf(false) }
    var episodeSettingsOpen by remember { mutableStateOf(false) }
    var useTvmaze by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("use_tvmaze", false) ?: false)
    }
    var autoNextEpisode by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("auto_next_episode", true) ?: true)
    }
    var autoSkipIntro by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("auto_skip_intro", false) ?: false)
    }
    var episodeNoticeSeconds by remember(preferences) {
        mutableStateOf((preferences?.getInt("episode_notice_seconds", 15) ?: 15).coerceIn(5, 60))
    }
    var nextEpisodeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var nextEpisodeName by remember { mutableStateOf("") }
    var nextEpisodeSeconds by remember { mutableStateOf<Int?>(null) }
    var skipIntroTo by remember { mutableStateOf<Long?>(null) }
    var metadataBusy by remember { mutableStateOf(false) }
    var metadataError by remember { mutableStateOf(false) }
    var pendingSeriesEpisode by remember { mutableStateOf<Pair<PortalTitle, EpisodeResume>?>(null) }
    var pendingMovie by remember { mutableStateOf<WatchedMovie?>(null) }
    var countries by remember(preferences) {
        mutableStateOf(
            preferences?.getString("countryOrder", null)?.split(',')?.filter { it.isNotBlank() }?.toSet()
                ?: preferences?.getStringSet("countries", emptySet()).orEmpty().toSet()
        )
    }
    var automaticCountries by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("autoCountries", true) ?: true)
    }
    var askForCountries by remember { mutableStateOf(false) }
    var choosingCountries by remember { mutableStateOf(false) }
    var hiddenCategories by remember(preferences) {
        mutableStateOf(preferences?.getStringSet("hidden", emptySet()).orEmpty().toSet())
    }
    var showHiddenCategories by remember { mutableStateOf(false) }
    var mergeSources by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("mergeSources", true) ?: true)
    }
    var categoryMenu by remember { mutableStateOf<Pair<PortalSection, String?>?>(null) }
    var details by remember { mutableStateOf<Pair<PortalSection, PortalTitle>?>(null) }
    var favorites by remember(preferences) {
        mutableStateOf(preferences?.getStringSet("favorites", emptySet()).orEmpty().toSet())
    }
    var watchlist by remember(preferences) {
        mutableStateOf(preferences?.getStringSet("watchlist", emptySet()).orEmpty().toSet())
    }
    var searchQuery by remember { mutableStateOf("") }
    var liveSession by remember { mutableStateOf<Pair<StalkerPortal, String?>?>(null) }
    var channels by remember { mutableStateOf(emptyList<StalkerChannel>()) }
    var genres by remember { mutableStateOf(emptyMap<String, String>()) }
    var category by remember { mutableStateOf<String?>(null) }
    var lastLiveCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreLiveFocus by remember { mutableStateOf(false) }
    val categoryScroll = rememberLazyListState()
    val guideScroll = rememberLazyListState()
    val guideState = rememberSaveableStateHolder()
    val channelScrollByCategory = remember { mutableMapOf<String, LazyListState>() }
    val channelScroll = remember(category) {
        category?.let { channelScrollByCategory.getOrPut(it) { LazyListState() } }
            ?: LazyListState()
    }
    val contentScroll = rememberScrollState()
    val menuScroll = rememberScrollState()
    var playing by remember { mutableStateOf<StalkerChannel?>(null) }
    var playbackSelection by remember { mutableStateOf<PlaybackSelection?>(null) }
    var playbackAttempt by remember { mutableStateOf<PlaybackAttempt?>(null) }
    var attemptSerial by remember { mutableStateOf(0L) }
    var playbackSourceBadge by remember { mutableStateOf<Pair<String, String>?>(null) }
    var lastBadgedAttempt by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(false) }
    var programmeChannel by remember { mutableStateOf<StalkerChannel?>(null) }
    var epgChannelIds by remember { mutableStateOf(emptySet<String>()) }
    var starting by remember { mutableStateOf<StalkerChannel?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var lastBack by remember { mutableStateOf(0L) }
    var triedHls by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var batchSummary by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var sourceError by remember { mutableStateOf<Int?>(null) }
    var importStatuses by remember { mutableStateOf(emptyMap<PortalSection, ImportStatus>()) }
    var catalogSession by remember { mutableStateOf<Pair<StalkerPortal, String?>?>(null) }
    var catalogs by remember { mutableStateOf(emptyMap<PortalSection, PortalCatalog>()) }
    var selectedGroup by remember { mutableStateOf<Pair<PortalSection, PortalGroup>?>(null) }
    var groupCatalog by remember { mutableStateOf<PortalCatalog?>(null) }
    var metadataNextPages by remember { mutableStateOf(emptyMap<String, Int>()) }
    var metadataTitles by remember {
        mutableStateOf(emptyMap<Int, Pair<PortalTitle, List<SourceItem<PortalTitle>>>>())
    }
    var groupUsesTmdb by remember { mutableStateOf(false) }
    var groupPages by remember { mutableStateOf(emptyMap<String, PortalCatalog>()) }
    var groupVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var searchedVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var groupLoading by remember { mutableStateOf(false) }
    var groupError by remember { mutableStateOf(false) }
    var groupJob by remember { mutableStateOf<Job?>(null) }
    var catalogLoading by remember { mutableStateOf(emptySet<PortalSection>()) }
    var catalogErrors by remember { mutableStateOf(emptySet<PortalSection>()) }
    var playbackError by remember { mutableStateOf<Int?>(null) }
    var vodSection by remember { mutableStateOf<PortalSection?>(null) }
    var openedSeries by remember { mutableStateOf<PortalTitle?>(null) }
    var seasons by remember { mutableStateOf<List<PortalSeason>?>(null) }
    var openedSeason by remember { mutableStateOf<PortalSeason?>(null) }
    var episodeVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var episodeError by remember { mutableStateOf(false) }
    var resumeEpisodeMissing by remember { mutableStateOf(false) }
    var episodeJob by remember { mutableStateOf<Job?>(null) }
    var address by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var storedSource by remember { mutableStateOf<SavedPortal?>(null) }
    var savedSources by remember { mutableStateOf(emptyList<SavedPortal>()) }
    val secondarySessions = remember { mutableMapOf<String, StalkerPortal>() }
    val sourcesForBrowsing = remember(savedSources, storedSource, mergeSources) {
        val active = storedSource
        if (mergeSources) savedSources.ifEmpty { listOfNotNull(active) } else listOfNotNull(active)
    }
    val combine = mergeSources
    var editing by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(storage != null) }
    var storageError by remember { mutableStateOf<Int?>(null) }
    val detailMetadata by produceState<TmdbTitle?>(null, metadataClient, details) {
        value = null
        val client = metadataClient ?: return@produceState
        val entry = details ?: return@produceState
        value = try {
            client.lookup(entry.first, entry.second)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(storage) {
        try {
            metadataClient = storage?.readMetadataToken()?.let(::TmdbClient)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            metadataError = true
        }
    }

    fun updateMetadataConnection(token: String?) {
        if (metadataBusy) return
        metadataBusy = true
        metadataError = false
        scope.launch {
            try {
                val client = token?.let(::TmdbClient)
                client?.validate()
                storage?.writeMetadataToken(token.orEmpty())
                metadataClient = client
                metadataSettingsOpen = false
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                metadataError = true
            } finally {
                metadataBusy = false
            }
        }
    }
    val fallbackGroup = stringResource(R.string.uncategorized_channels)
    val uncategorized = stringResource(R.string.uncategorized_channels)
    val categoryNames = LiveCategory.entries.associateWith { stringResource(it.label) }
    val displayChannels = remember { mutableMapOf<StalkerChannel, StalkerChannel>() }
    val liveGroups = remember(sourcesForBrowsing, combine) {
        val groups = buildLiveChannelGroups(sourcesForBrowsing, combine)
        val originals = groups.map { it.variants.first().item.value }.toSet()
        displayChannels.keys.retainAll(originals)
        groups.associateBy { group ->
            val original = group.variants.first().item.value
            val name = group.description.name
            displayChannels.getOrPut(original) {
                StalkerChannel(
                    name, original.command, original.genreId, original.genreName,
                    original.id, original.logo, original.direct, original.epgIdentity, original.number
                )
            }
        }
    }
    val liveVariants = remember(liveGroups) {
        liveGroups.mapValues { (_, group) -> group.variants.map { it.item } }
    }
    val allLiveCategories = remember(liveGroups, categoryNames) {
        liveGroups.keys.groupBy { channel ->
            categoryNames.getValue(liveGroups.getValue(channel).description.category)
        }
    }
    val liveCategories = remember(allLiveCategories, liveGroups, countries) {
        allLiveCategories.mapValues { (_, items) ->
            if (countries.isEmpty()) items else items.filter {
                liveGroups.getValue(it).description.country in countries
            }
        }.filterValues { it.isNotEmpty() }
    }
    val currentYear = remember { java.time.Year.now().value }
    val vodCategoryNames = VodCategory.entries.associate {
        val label = when {
            metadataClient == null && it == VodCategory.POPULAR -> stringResource(R.string.portal_popular)
            metadataClient == null && it == VodCategory.CINEMA -> stringResource(R.string.portal_cinema)
            else -> when (it) {
            VodCategory.CINEMA_THIS_YEAR -> stringResource(it.label, currentYear)
            VodCategory.CINEMA_LAST_YEAR -> stringResource(it.label, currentYear - 1)
            VodCategory.THIS_YEAR -> stringResource(it.label, currentYear)
            else -> stringResource(it.label)
            }
        }
        it.id to label
    }
    val vodGroups = remember(sourcesForBrowsing, combine, fallbackGroup, countries, metadataClient) {
        listOf(PortalSection.MOVIES, PortalSection.SERIES).associateWith { section ->
            val entries = sourcesForBrowsing.flatMap { source ->
                source.catalogs[section]?.availableGroups(fallbackGroup).orEmpty().map { SourceItem(source, it) }
            }
            if (combine) {
                val groups = entries.groupBy { vodCategory(it.value).id }.toMutableMap()
                val all = if (countries.isEmpty()) sourcesForBrowsing.filter { section in it.catalogs }.map {
                    SourceItem(it, PortalGroup(ALL_CATALOG_ITEMS, fallbackGroup))
                } else entries
                groups[VodCategory.ALL.id] = all
                groups[VodCategory.THIS_YEAR.id] = all
                val directPopular = groups[VodCategory.POPULAR.id].orEmpty()
                groups[VodCategory.POPULAR.id] = (directPopular + all).distinct()
                if (section == PortalSection.MOVIES) {
                    val cinema = groups[VodCategory.CINEMA.id].orEmpty()
                    groups[VodCategory.CINEMA_THIS_YEAR.id] = cinema
                    groups[VodCategory.CINEMA_LAST_YEAR.id] = cinema
                }
                for (service in VodCategory.entries.filter { it.service }) {
                    val taggedSources = sourcesForBrowsing.filter { source ->
                        source.catalogs[section]?.titles.orEmpty().any {
                            serviceCategory(it.service.orEmpty()) == service
                        }
                    }.map { it.identity }.toSet()
                    val targets = groups[service.id].orEmpty()
                    if (taggedSources.isNotEmpty()) {
                        groups[service.id] = (targets + all.filter { it.source.identity in taggedSources }).distinct()
                    }
                }
                if (sourcesForBrowsing.isNotEmpty()) {
                    for (category in VodCategory.entries.filter { it.usesTmdb(section) }) {
                        if (category.requiresTmdb() || metadataClient != null) {
                            if (groups[category.id].isNullOrEmpty()) groups[category.id] = all
                        }
                    }
                }
                groups
            } else entries.associate { it.value.id to listOf(it) }
        }
    }
    fun sessionFor(source: SavedPortal): StalkerPortal =
        if (source.identity == storedSource?.identity) {
            liveSession?.first ?: secondarySessions.getOrPut(source.identity) {
                StalkerPortal(source.address, source.mac)
            }
        } else secondarySessions.getOrPut(source.identity) { StalkerPortal(source.address, source.mac) }

    fun playbackSourceLabel(source: SavedPortal, portal: StalkerPortal): String {
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        return if (index < 0) portal.sourceLabel else
            context.getString(R.string.saved_portal_label, index + 1, portal.sourceLabel)
    }

    fun programmeKey(channel: StalkerChannel): String? {
        val item = liveVariants[channel]?.firstOrNull() ?: return null
        return item.value.id?.let { "${item.source.identity}:$it" }
    }

    val pinnedProgrammeKeys = remember(liveVariants) {
        liveVariants.keys.filter { channel ->
            val item = liveVariants.getValue(channel).first()
            item.value.id in item.source.epgChannelIds
        }.mapNotNull(::programmeKey).toSet()
    }

    fun groupOwners(section: PortalSection, id: String): List<SourceItem<PortalGroup>> =
        vodGroups[section]?.get(id).orEmpty().filter {
            !combine || countries.isEmpty() || categoryCountry(it.value.name, it.value.country) in countries
        }

    fun browsingGroups(section: PortalSection): List<PortalGroup> =
        vodGroups[section].orEmpty().mapNotNull { (id, entries) ->
            val category = VodCategory.entries.firstOrNull { it.id == id }
            if (groupOwners(section, id).isEmpty() && category?.requiresTmdb() != true) null
            else if (combine) PortalGroup(id, vodCategoryNames.getValue(id))
            else entries.first().value
        }

    val knownCountries = remember(liveGroups, vodGroups) {
        (liveGroups.values.map { it.description.country } +
            vodGroups.values.flatMap { groups ->
                groups.filterKeys { id ->
                    VodCategory.entries.none { it.id == id && it in listOf(VodCategory.ALL, VodCategory.THIS_YEAR) }
                }.values.flatten().map {
                    categoryCountry(it.value.name, it.value.country)
                }
            }).toSet()
    }

    fun saveCountries(selection: Set<String>) {
        countries = selection
        preferences?.edit()?.putString("countryOrder", countries.joinToString(","))?.apply()
    }

    fun setCategoryVisibility(section: PortalSection, ids: Set<String>, hidden: Boolean) {
        val keys = ids.map { categoryKey(section, it) }.toSet()
        hiddenCategories = if (hidden) hiddenCategories + keys else hiddenCategories - keys
        preferences?.edit()?.putStringSet("hidden", hiddenCategories)?.apply()
    }

    fun countryPriority(name: String, country: String? = null): Int =
        countries.indexOf(categoryCountry(name, country)).takeIf { it >= 0 } ?: Int.MAX_VALUE

    fun titleKey(section: PortalSection, title: PortalTitle): String =
        "${catalogSession?.first?.sourceLabel.orEmpty()}:${section.name}:${title.id ?: title.name}"

    fun seriesKey(title: PortalTitle): String =
        titleIdentity(title) ?: titleKey(PortalSection.SERIES, title)
    fun trackedSeries(title: PortalTitle): TrackedSeries? =
        seriesActivity.firstOrNull { it.key == seriesKey(title) }
    fun isTitleSaved(section: PortalSection, title: PortalTitle): Boolean =
        titleKey(section, title) in watchlist ||
            (section == PortalSection.SERIES && trackedSeries(title)?.saved == true)

    fun saveSeriesActivity(entries: List<TrackedSeries>) {
        seriesActivity = entries
        progressPreferences?.edit()?.putString("series_activity", encodeSeriesActivity(entries))?.apply()
    }

    fun updateTrackedSeries(title: PortalTitle, saved: Boolean? = null, watchedEpisode: EpisodeResume? = null) {
        val key = seriesKey(title)
        val existing = seriesActivity.firstOrNull { it.key == key }
        val variants = searchedVariants[title] ?: groupVariants[title] ?: listOfNotNull(
            storedSource?.let { SourceItem(it, title) }
        )
        val refs = variants.mapNotNull { item ->
            item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
        }
        val initial = existing ?: TrackedSeries(
            key, displayVodTitle(title), refs, vodReleaseYear(title), title.imdbId, title.tmdbId
        )
        val updated = initial.copy(
            refs = (initial.refs + refs).distinct(),
            saved = saved ?: initial.saved,
            viewed = initial.viewed || watchedEpisode != null,
            known = if (watchedEpisode != null && initial.checkedAt > 0) {
                initial.known + watchedEpisode.episodeKey
            } else initial.known,
            unseen = if (watchedEpisode == null) initial.unseen else initial.unseen - watchedEpisode.episodeKey,
            watchedAt = if (watchedEpisode == null) initial.watchedAt else System.currentTimeMillis(),
            lastEpisode = watchedEpisode ?: initial.lastEpisode
        )
        val remaining = seriesActivity.filterNot { it.key == key }
        saveSeriesActivity(if (updated.saved || updated.viewed) remaining + updated else remaining)
    }

    fun observeEpisodeProvider(key: String, episodes: Set<String>, provider: String) {
        saveSeriesActivity(seriesActivity.map {
            if (it.key != key) it
            else if ((it.episodeProvider ?: if (it.usedTmdb) "tmdb" else null) == provider) {
                it.observe(episodes, System.currentTimeMillis())
            } else it.copy(episodeProvider = provider, known = episodes, checkedAt = System.currentTimeMillis())
        })
    }

    val trackedSources = savedSources.map { it.identity }
    val trackedTitles = seriesActivity.map { it.key to it.refs }
    LaunchedEffect(metadataClient, useTvmaze, trackedSources, trackedTitles) {
        val client = metadataClient
        if (client == null && !useTvmaze) return@LaunchedEffect
        val provider = if (useTvmaze) "tvmaze" else "tmdb"
        while (trackedSources.isNotEmpty() && trackedTitles.isNotEmpty()) {
            if (canPlay()) {
                val now = System.currentTimeMillis()
                for (tracked in seriesActivity.filter {
                    (it.episodeProvider ?: if (it.usedTmdb) "tmdb" else null) != provider ||
                        now - it.checkedAt >= 30 * 60 * 1000L
                }) {
                    try {
                        val title = PortalTitle(
                            tracked.name, year = tracked.year, imdbId = tracked.imdb, tmdbId = tracked.tmdb
                        )
                        val keys = if (useTvmaze) TvmazeClient.airedEpisodes(title) else client?.airedEpisodes(title)
                        if (keys != null && seriesActivity.any { it.key == tracked.key }) {
                            observeEpisodeProvider(tracked.key, keys, provider)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // Keep the last verified episode list when TMDB is unavailable.
                    }
                }
            }
            delay(60_000)
        }
    }

    fun toggleSavedTitle(section: PortalSection, title: PortalTitle, favorite: Boolean) {
        val key = titleKey(section, title)
        if (favorite) {
            favorites = if (key in favorites) favorites - key else favorites + key
            preferences?.edit()?.putStringSet("favorites", favorites)?.apply()
        } else {
            val saved = !isTitleSaved(section, title)
            watchlist = if (saved) watchlist + key else watchlist - key
            preferences?.edit()?.putStringSet("watchlist", watchlist)?.apply()
            if (section == PortalSection.SERIES) updateTrackedSeries(title, saved = saved)
        }
    }
    fun visibleCategory(section: PortalSection, id: String, label: String, country: String? = null): Boolean {
        val hidden = categoryKey(section, id) in hiddenCategories
        return (showHiddenCategories || !hidden) &&
            (section == PortalSection.LIVE || combine || countries.isEmpty() || categoryCountry(label, country) in countries ||
                (showHiddenCategories && hidden))
    }

    fun closeGroup() {
        groupJob?.cancel()
        selectedGroup = null
        groupCatalog = null
        metadataNextPages = emptyMap()
        metadataTitles = emptyMap()
        groupUsesTmdb = false
        groupPages = emptyMap()
        groupVariants = emptyMap()
        groupError = false
        groupLoading = false
    }

    fun loadGroup(section: PortalSection, group: PortalGroup, next: Boolean = false) {
        val category = VodCategory.entries.firstOrNull { it.id == group.id }
        val client = metadataClient
        if (category?.usesTmdb(section) == true && client != null) {
            groupJob?.cancel()
            val pages = if (next) metadataNextPages else tmdbShelfRegions(
                category, countries, java.util.Locale.getDefault().country
            ).associateWith { 1 }
            if (pages.isEmpty()) return
            val previous = if (next) metadataTitles else emptyMap()
            metadataNextPages = pages
            metadataTitles = previous
            selectedGroup = section to group
            groupCatalog = null
            groupVariants = previous.values.toMap()
            groupUsesTmdb = true
            groupError = false
            groupLoading = true
            groupJob = scope.launch {
                try {
                    val result = loadTmdbPortalShelf(
                        client, section, category, pages, sourcesForBrowsing, countries, ::sessionFor
                    )
                    metadataTitles = previous + result.titles
                    groupVariants = metadataTitles.values.toMap()
                    metadataNextPages = result.nextPages
                    groupError = result.incomplete
                    groupCatalog = PortalCatalog(groupVariants.keys.toList(), null, 1.takeIf { metadataNextPages.isNotEmpty() })
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    groupError = true
                    groupCatalog = PortalCatalog(groupVariants.keys.toList(), null, 1)
                } finally {
                    if (kotlin.coroutines.coroutineContext.isActive) groupLoading = false
                }
            }
            return
        }
        val owners = groupOwners(section, group.id)
        if (owners.isEmpty()) return
        val previous = if (next) groupPages else emptyMap()
        groupJob?.cancel()
        selectedGroup = section to group
        groupUsesTmdb = false
        if (!next) {
            groupCatalog = null
            groupVariants = emptyMap()
        }
        groupError = false
        groupLoading = true
        groupJob = scope.launch {
            try {
                val requests = Semaphore(4)
                fun pageKey(item: SourceItem<PortalGroup>) = "${item.source.identity}:${item.value.id}"
                val pages = coroutineScope {
                    owners.map { item ->
                        async {
                            val key = pageKey(item)
                            val before = previous[key]
                            val page = if (next) before?.nextPage else 1
                            if (page == null) return@async key to before
                            try {
                                key to requests.withPermit {
                                    val portal = sessionFor(item.source)
                                    portal.loadCatalog(section, portal.openSession(), page, before, item.value.id)
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                groupError = true
                                key to before
                            }
                        }
                    }.map { it.await() }.mapNotNull { (id, page) -> page?.let { id to it } }.toMap()
                }
                val target = VodCategory.entries.firstOrNull { it.id == group.id }
                val entries = owners.flatMap { item ->
                    val providerCategory = vodCategory(item.value)
                    pages[pageKey(item)]?.titles.orEmpty().filter { title ->
                        when (target) {
                            VodCategory.THIS_YEAR, VodCategory.CINEMA_THIS_YEAR -> vodReleaseYear(title) == currentYear
                            VodCategory.CINEMA_LAST_YEAR -> vodReleaseYear(title) == currentYear - 1
                            VodCategory.POPULAR -> providerCategory == VodCategory.POPULAR || title.popularity != null
                            else -> target?.service != true || providerCategory == target ||
                                serviceCategory(title.service.orEmpty()) == target
                        }
                    }.map { SourceItem(item.source, it) }
                }
                val variants = if (combine) mergeCatalogueTitles(entries.distinct()) else entries.map(::listOf)
                groupPages = pages
                groupVariants = variants.associateBy { it.first().value }
                groupCatalog = PortalCatalog(
                    groupVariants.keys.toList(),
                    if (combine) null else pages.values.singleOrNull()?.total,
                    if (pages.values.any { it.nextPage != null }) 1 else null
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                groupError = true
            } finally {
                if (kotlin.coroutines.coroutineContext.isActive) groupLoading = false
            }
        }
    }
    LaunchedEffect(countries) {
        val group = selectedGroup
        if (groupUsesTmdb && group != null) loadGroup(group.first, group.second)
    }
    var epgCache by remember(liveSession) {
        mutableStateOf(emptyMap<String, List<PortalProgramme>>())
    }
    var epgFetchedAt by remember(liveSession) { mutableStateOf(emptyMap<String, Long>()) }
    val pinnedProgramme = programmeChannel?.let(::programmeKey) in pinnedProgrammeKeys
    val focusedProgrammes by produceState<List<PortalProgramme>?>(
        null, programmeChannel, sourcesForBrowsing, pinnedProgramme
    ) {
        value = null
        val channel = programmeChannel ?: return@produceState
        val item = liveVariants[channel]?.firstOrNull() ?: return@produceState
        val session = sessionFor(item.source)
        if (pinnedProgramme) return@produceState
        val id = programmeKey(channel) ?: run {
            value = emptyList()
            return@produceState
        }
        val previous = epgCache[id]
        value = previous
        val fetchedAt = epgFetchedAt[id]
        if (fetchedAt != null && SystemClock.elapsedRealtime() - fetchedAt < 15 * 60 * 1000L) {
            return@produceState
        }
        delay(250)
        value = try {
            session.loadProgramme(item.value, session.openSession()).also {
                epgCache = epgCache + (id to it)
                epgFetchedAt = epgFetchedAt + (id to SystemClock.elapsedRealtime())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            previous.orEmpty()
        }
    }
    val programmes = if (pinnedProgramme) {
        epgCache[programmeChannel?.let(::programmeKey)]
    } else focusedProgrammes

    LaunchedEffect(liveSession, pinnedProgrammeKeys) {
        if (pinnedProgrammeKeys.isEmpty()) return@LaunchedEffect
        while (true) {
            for (channel in liveVariants.keys) {
                val id = programmeKey(channel)?.takeIf { it in pinnedProgrammeKeys } ?: continue
                val item = liveVariants.getValue(channel).first()
                val portal = sessionFor(item.source)
                try {
                    epgCache = epgCache + (id to portal.loadProgramme(item.value, portal.openSession()))
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    if (id !in epgCache) epgCache = epgCache + (id to emptyList())
                }
            }
            delay(6 * 60 * 60 * 1000L)
        }
    }

    LaunchedEffect(storage) {
        try {
            storage?.read()?.let { library ->
                savedSources = library.sources
                val saved = library.sources.firstOrNull { it.identity == library.activeId }
                    ?: library.sources.firstOrNull()
                if (saved != null) {
                    val session = StalkerPortal(saved.address, saved.mac) to null
                    storedSource = saved
                    liveSession = session
                    catalogSession = session
                    address = saved.address
                    mac = saved.mac
                    channels = saved.channels
                    genres = saved.genres
                    catalogs = saved.catalogs
                    epgChannelIds = saved.epgChannelIds
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = R.string.portal_restore_failed
        } finally {
            restoring = false
        }
    }

    suspend fun save(source: SavedPortal, activate: Boolean = true) {
        if (activate || storedSource?.identity == source.identity) storedSource = source
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        savedSources = if (index < 0) savedSources + source else {
            savedSources.toMutableList().apply { this[index] = source }
        }
        try {
            storage?.write(SavedPortals(savedSources, storedSource?.identity ?: source.identity))
            storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = R.string.portal_save_failed
        }
    }

    fun toggleProgrammeChannel(channel: StalkerChannel) {
        val item = liveVariants[channel]?.firstOrNull() ?: return
        val id = item.value.id ?: return
        val source = item.source
        val ids = if (id in source.epgChannelIds) source.epgChannelIds - id else source.epgChannelIds + id
        if (storedSource?.identity == source.identity) epgChannelIds = ids
        val snapshot = SavedPortal(source.address, source.mac, source.channels, source.genres, source.catalogs, ids)
        scope.launch { save(snapshot, activate = false) }
    }

    fun savePlaybackPosition(onFailure: Boolean = false) {
        val attempt = playbackAttempt ?: return
        val key = attempt.progressKey ?: return
        val activePlayer = player ?: return
        if (activePlayer.currentMediaItem?.mediaId != attempt.mediaId || starting != null ||
            activePlayer.playbackState == Player.STATE_ENDED
        ) return
        val position = activePlayer.currentPosition
        if (position >= 0 && (!onFailure || position > 0)) {
            progressPreferences?.edit()?.putLong(key, position)?.apply()
        }
    }

    fun resumeFor(entry: TrackedSeries): EpisodeResume? =
        recoverEpisodeResume(entry, progressPreferences?.all.orEmpty())

    fun stopPlayback() {
        savePlaybackPosition()
        playbackJob?.cancel()
        playbackAttempt = null
        playbackSourceBadge = null
        pendingSeriesEpisode = null
        pendingMovie = null
        nextEpisodeAction = null
        nextEpisodeSeconds = null
        skipIntroTo = null
        player?.stop()
        player?.clearMediaItems()
        playing = null
        playbackSelection = null
        starting = null
        playbackError = null
        vodSection = null
        fullscreen = false
        lastBack = 0
    }

    fun reopenPlayback(selection: PlaybackSelection, matchSeries: Boolean = false): Boolean {
        val current = playbackSelection ?: return false
        val matches = if (matchSeries) {
            current.source == selection.source && current.section == PortalSection.SERIES &&
                current.seriesId == selection.id
        } else current == selection
        val activePlayer = player ?: return false
        if (!matches || playbackError != null) return false
        if (playing == null) return starting != null
        if (activePlayer.playbackState != Player.STATE_READY &&
            activePlayer.playbackState != Player.STATE_BUFFERING
        ) return false
        lastBack = 0
        fullscreen = true
        if (canPlay()) activePlayer.play()
        return true
    }

    fun runPlayback(plan: PlaybackAttempt) {
        val activePlayer = player ?: return
        playbackJob?.cancel()
        playbackAttempt = null
        activePlayer.stop()
        activePlayer.clearMediaItems()
        playing = null
        playbackError = null
        playbackJob = scope.launch {
            var message = R.string.playback_link_error
            for (index in plan.index until plan.variants.size) {
                val attempt = plan.copy(index = index, mediaId = "playback-${++attemptSerial}")
                playbackAttempt = attempt
                triedHls = false
                starting = attempt.channel
                try {
                    val stream = attempt.variants[index].resolve()
                    if (playbackAttempt !== attempt) return@launch
                    playing = attempt.channel
                    val media = MediaItem.Builder().setUri(stream).setMediaId(attempt.mediaId).build()
                    if (attempt.progressKey == null) activePlayer.setMediaItem(media)
                    else activePlayer.setMediaItem(media, attempt.positionMs)
                    activePlayer.prepare()
                    activePlayer.playWhenReady = canPlay()
                    if (!canPlay()) starting = null
                    return@launch
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    message = if (error is PortalRequestException && error.status in listOf(401, 403)) {
                        R.string.playback_portal_denied
                    } else R.string.playback_link_error
                }
            }
            starting = null
            playbackError = if (plan.variants.size > 1) R.string.playback_all_variants_failed else message
        }
    }

    fun startPlayback(
        channel: StalkerChannel,
        selection: PlaybackSelection,
        variants: List<PlaybackVariant>,
        progressKey: String? = null
    ) {
        if (player == null || variants.isEmpty() || reopenPlayback(selection)) return
        stopPlayback()
        playbackSelection = selection
        vodSection = selection.section.takeUnless { it == PortalSection.LIVE }
        val resume = progressKey?.let { progressPreferences?.getLong(it, 0L) } ?: 0L
        runPlayback(PlaybackAttempt(selection, channel, variants, 0, resume, progressKey))
    }

    fun failPlayback(attempt: PlaybackAttempt, message: Int) {
        if (playbackAttempt !== attempt) return
        if (attempt.index + 1 >= attempt.variants.size) {
            savePlaybackPosition(onFailure = true)
            playbackJob?.cancel()
            playbackAttempt = null
            player?.stop()
            starting = null
            playbackError = if (attempt.variants.size > 1) R.string.playback_all_variants_failed else message
            return
        }
        savePlaybackPosition(onFailure = true)
        val position = attempt.progressKey?.let { progressPreferences?.getLong(it, attempt.positionMs) }
            ?: attempt.positionMs
        runPlayback(attempt.copy(index = attempt.index + 1, positionMs = position))
    }

    fun playChannel(channel: StalkerChannel) {
        val group = liveGroups[channel] ?: return
        programmeChannel = channel
        startPlayback(
            channel,
            liveSelection(channel, group, combine),
            group.variants.map { variant ->
                val item = variant.item
                val portal = sessionFor(item.source)
                val quality = variant.description.qualities.joinToString(" / ")
                val sourceLabel = playbackSourceLabel(item.source, portal)
                val label = if (quality.isBlank()) sourceLabel else "$quality · $sourceLabel"
                PlaybackVariant(label) { portal.resolve(item.value, portal.openSession()) }
            }
        )
    }

    fun closeSeries() {
        episodeJob?.cancel()
        openedSeries = null
        seasons = null
        openedSeason = null
        episodeVariants = emptyMap()
        episodeError = false
        resumeEpisodeMissing = false
    }

    fun switchSource(source: SavedPortal) {
        if (connecting || source.identity == storedSource?.identity) return
        closeGroup()
        closeSeries()
        details = null
        searchedVariants = emptyMap()
        category = null
        lastLiveCategory = null
        channelScrollByCategory.clear()
        categoryScroll.requestScrollToItem(0)
        guideScroll.requestScrollToItem(0)
        programmeChannel = null
        importStatuses = emptyMap()
        sourceError = null
        catalogErrors = emptySet()
        catalogLoading = emptySet()
        val session = StalkerPortal(source.address, source.mac) to null
        liveSession = session
        catalogSession = session
        channels = source.channels
        genres = source.genres
        catalogs = source.catalogs
        epgChannelIds = source.epgChannelIds
        address = source.address
        mac = source.mac
        scope.launch { save(source) }
    }

    fun titleEntries(section: PortalSection, title: PortalTitle): List<SourceItem<PortalTitle>> {
        val known = searchedVariants[title] ?: groupVariants[title]
        if (known != null) return known
        val source = storedSource ?: return emptyList()
        if (!combine) return listOf(SourceItem(source, title))
        return listOf(SourceItem(source, title)) + sourcesForBrowsing
            .filter { it.identity != source.identity }.mapNotNull { other ->
                equivalentCatalogueTitle(title, other.catalogs[section]?.titles.orEmpty())
                    ?.let { SourceItem(other, it) }
            }
    }

    suspend fun findEquivalent(
        source: SavedPortal, section: PortalSection, title: PortalTitle
    ): PortalTitle? {
        val portal = sessionFor(source)
        val search = displayVodTitle(title)
        if (search.isBlank()) return null
        return equivalentCatalogueTitle(title, portal.loadCatalog(
            section, portal.openSession(), search = search
        ).titles)
    }

    fun titleSelection(section: PortalSection, title: PortalTitle): PlaybackSelection {
        val root = openedSeries.takeIf { section == PortalSection.SERIES }
        val canonical = titleIdentity(root ?: title)
        val source = titleEntries(section, root ?: title).firstOrNull()?.source?.identity.orEmpty()
        val seriesId = root?.let { titleIdentity(it) ?: it.id ?: it.name }
        val id = if (root != null) {
            val episode = title.episode?.toIntOrNull()?.toString() ?: title.episode ?: title.id ?: title.name
            "${openedSeason?.number ?: openedSeason?.title?.name}:$episode"
        } else canonical ?: title.id ?: title.name
        return PlaybackSelection(
            if (canonical != null) "catalogue" else source, section, id, title.episode, seriesId
        )
    }

    fun showTitle(section: PortalSection, title: PortalTitle) {
        if (reopenPlayback(
                titleSelection(section, title),
                matchSeries = section == PortalSection.SERIES
            )
        ) return
        closeSeries()
        details = section to title
        if (section == PortalSection.SERIES && isTitleSaved(section, title) && trackedSeries(title) == null) {
            updateTrackedSeries(title, saved = true)
        }
    }

    fun openTrackedSeries(entry: TrackedSeries, onOpen: ((PortalTitle) -> Unit)? = null) {
        val variants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.SERIES]?.titles?.firstOrNull { it.id == ref.portalTitleId }
                ?: PortalTitle(entry.name, id = ref.portalTitleId, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
            SourceItem(source, title)
        }
        val title = variants.firstOrNull()?.value ?: return
        searchedVariants = mapOf(title to variants)
        if (onOpen == null) showTitle(PortalSection.SERIES, title) else onOpen(title)
    }

    fun selectTitle(
        section: PortalSection,
        title: PortalTitle,
        resume: EpisodeResume? = null,
        progressOverride: String? = null
    ) {
        if (storedSource == null) return
        val selection = titleSelection(section, title)
        if (reopenPlayback(selection, matchSeries = section == PortalSection.SERIES && openedSeries == null)) {
            return
        }
        if (section == PortalSection.SERIES && openedSeries == null) {
            openedSeries = title
            episodeError = false
            resumeEpisodeMissing = false
            seasons = null
            openedSeason = null
            episodeVariants = emptyMap()
            episodeJob = scope.launch {
                try {
                    val known = titleEntries(section, title).distinctBy { it.source.identity }
                        .associateBy { it.source.identity }
                    val owners = if (combine) {
                        known.values.map { it.source } + sourcesForBrowsing.filter { it.identity !in known }
                    } else known.values.map { it.source }
                    val results = coroutineScope {
                        owners.map { source ->
                            async {
                                try {
                                    val item = known[source.identity]?.value
                                        ?: findEquivalent(source, section, title) ?: return@async emptyList()
                                    val portal = sessionFor(source)
                                    portal.loadSeasons(item, portal.openSession()).map { SourceItem(source, it) }
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                        }.flatMap { it.await() }
                    }
                    val variants = mutableMapOf<PortalTitle, List<SourceItem<PortalTitle>>>()
                    val grouped = if (combine) {
                        mergeSourceItems(results) { it.number?.let { number -> "season:$number" } }
                    } else results.map(::listOf)
                    seasons = grouped.map { season ->
                        val items = season.flatMap { entry -> entry.value.episodes.map { SourceItem(entry.source, it) } }
                        val episodes = if (season.size > 1) {
                            mergeSourceItems(items) { it.episode?.toIntOrNull()?.let { number -> "episode:$number" } }
                        } else items.map(::listOf)
                        for (episode in episodes) variants[episode.first().value] = episode
                        season.first().value.copy(episodes = episodes.map { it.first().value })
                    }
                    episodeVariants = variants
                    if (resume != null) {
                        val item = resumeEpisode(seasons.orEmpty(), resume)
                        if (item != null) {
                            val (season, episode) = item
                            openedSeason = season
                            selectTitle(section, episode, progressOverride = resume.progressKey)
                        } else resumeEpisodeMissing = true
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    episodeError = true
                }
            }
        } else {
            val known = if (section == PortalSection.SERIES) episodeVariants[title].orEmpty()
            else titleEntries(section, title)
            val knownBySource = known.distinctBy { it.source.identity }.associateBy { it.source.identity }
            val owners = if (combine && section == PortalSection.MOVIES) {
                knownBySource.values.map { it.source } + sourcesForBrowsing.filter { it.identity !in knownBySource }
            } else knownBySource.values.map { it.source }
            val variants = owners.map { source ->
                val portal = sessionFor(source)
                PlaybackVariant(playbackSourceLabel(source, portal), knownBySource[source.identity]?.value?.timing ?: EpisodeTiming()) {
                    val knownTitle = knownBySource[source.identity]?.value
                    val item = knownTitle?.takeIf { it.command != null } ?: run {
                        val found = portal.loadCatalog(section, portal.openSession(), search = displayVodTitle(title))
                            .titles.filter { it.command != null }
                        found.firstOrNull { knownTitle?.id != null && it.id == knownTitle.id }
                            ?: equivalentCatalogueTitle(title, found) ?: error("No equivalent title")
                    }
                    portal.resolveVod(item, portal.openSession())
                }
            }
            if (variants.isNotEmpty()) {
                val key = progressOverride ?:
                    "${selection.section}:${selection.source}:${selection.seriesId.orEmpty()}:${selection.id}"
                startPlayback(
                    StalkerChannel(title.name, title.command.orEmpty(), null, null),
                    selection, variants, progressKey = key
                )
                if (section == PortalSection.SERIES) {
                    val series = openedSeries
                    val season = openedSeason
                    if (series != null && season != null) {
                        pendingSeriesEpisode = series to EpisodeResume(
                            seriesEpisodeKey(season, title), season.number, title.episode, title.name, key
                        )
                        val seasonList = seasons.orEmpty()
                        val availableVariants = episodeVariants
                        followingEpisode(seasonList, season, title)?.let { (followingSeason, following) ->
                            nextEpisodeName = following.episode?.let {
                                context.getString(R.string.episode_number, it)
                            } ?: following.name
                            nextEpisodeAction = {
                                openedSeries = series
                                openedSeason = followingSeason
                                seasons = seasonList
                                episodeVariants = availableVariants
                                selectTitle(PortalSection.SERIES, following)
                                progressPreferences?.edit()?.remove(key)?.apply()
                            }
                        }
                    }
                } else if (section == PortalSection.MOVIES) {
                    pendingMovie = WatchedMovie(
                        displayVodTitle(title), vodReleaseYear(title), title.imdbId, title.tmdbId,
                        known.map { SeriesPortalRef(it.source.identity, it.value.id.orEmpty()) }.distinct(),
                        key, System.currentTimeMillis()
                    )
                }
            } else {
                vodSection = section
                playbackError = R.string.vod_missing_link
            }
        }
    }

    fun openWatchedMovie(entry: WatchedMovie, resume: Boolean) {
        val variants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.MOVIES]?.titles?.firstOrNull {
                ref.portalTitleId.isNotBlank() && it.id == ref.portalTitleId
            } ?: PortalTitle(
                entry.name, id = ref.portalTitleId.ifBlank { null }, year = entry.year,
                imdbId = entry.imdb, tmdbId = entry.tmdb
            )
            SourceItem(source, title)
        }
        val title = variants.firstOrNull()?.value ?: return
        searchedVariants = mapOf(title to variants)
        if (resume) {
            details = PortalSection.MOVIES to title
            selectTitle(PortalSection.MOVIES, title, progressOverride = entry.progressKey)
        } else showTitle(PortalSection.MOVIES, title)
    }

    suspend fun refreshPortal(enteredAddress: String, enteredMac: String, activate: Boolean): Boolean {
        sourceError = null
        importStatuses = emptyMap()
        try {
            val source = StalkerPortal(enteredAddress, enteredMac)
            val token = source.openSession()
            val previous = savedSources.firstOrNull {
                it.address == enteredAddress && normalizePortalMac(it.mac) == normalizePortalMac(enteredMac)
            }
            var refreshedChannels = previous?.channels.orEmpty()
            var refreshedGenres = previous?.genres.orEmpty()
            var refreshedCatalogs = previous?.catalogs.orEmpty()
            importStatuses = PortalSection.entries.associateWith { ImportStatus.Loading }
            coroutineScope {
                for (section in PortalSection.entries) launch {
                    val status = try {
                        if (section == PortalSection.LIVE) {
                            val (imported, labels) = coroutineScope {
                                val labels = async {
                                    try {
                                        source.loadGenres(token)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        emptyMap()
                                    }
                                }
                                val imported = source.loadChannels(token) { loaded, total ->
                                    withContext(Dispatchers.Main) {
                                        importStatuses = importStatuses +
                                            (section to ImportStatus.Progress(loaded, total))
                                    }
                                }
                                imported to labels.await()
                            }
                            refreshedChannels = if (imported.partial) {
                                (imported.channels + refreshedChannels).distinctBy { it.id ?: it.command }
                            } else imported.channels
                            refreshedGenres = refreshedGenres + labels
                            if (imported.partial) ImportStatus.Partial(imported.channels.size)
                            else ImportStatus.Count(imported.channels.size)
                        } else {
                            val catalog = coroutineScope {
                                val groups = async {
                                    try {
                                        source.loadCatalogGroups(section, token)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                }
                                val firstPage = source.loadCatalog(section, token)
                                firstPage.copy(groups = groups.await().ifEmpty {
                                    previous?.catalogs?.get(section)?.groups.orEmpty()
                                })
                            }
                            refreshedCatalogs = refreshedCatalogs + (section to catalog)
                            catalog.total?.let { ImportStatus.Count(it) }
                                ?: ImportStatus.Loaded(catalog.titles.size)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        when {
                            section != PortalSection.LIVE -> ImportStatus.Failed
                            error is PortalRequestException && error.status in listOf(401, 403) ->
                                ImportStatus.Error(R.string.portal_live_access_denied)
                            else -> ImportStatus.Error(R.string.portal_live_list_invalid)
                        }
                    }
                    importStatuses = importStatuses + (section to status)
                }
            }
            val changed = importStatuses.values.any {
                it is ImportStatus.Count || it is ImportStatus.Loaded || it is ImportStatus.Partial
            }
            if (!changed) return false
            val snapshot = SavedPortal(
                enteredAddress, enteredMac, refreshedChannels, refreshedGenres,
                refreshedCatalogs,
                savedSources.firstOrNull { it.identity == previous?.identity }?.epgChannelIds.orEmpty()
            )
            val sameSource = snapshot.identity == storedSource?.identity
            if (activate || sameSource) {
                if (!sameSource) {
                    closeSeries()
                    closeGroup()
                    details = null
                    searchedVariants = emptyMap()
                    category = null
                    lastLiveCategory = null
                    channelScrollByCategory.clear()
                    categoryScroll.requestScrollToItem(0)
                    guideScroll.requestScrollToItem(0)
                    programmeChannel = null
                }
                val session = source to token
                liveSession = session
                catalogSession = session
                catalogLoading = emptySet()
                catalogErrors = emptySet()
                channels = refreshedChannels
                genres = refreshedGenres
                catalogs = refreshedCatalogs
                epgChannelIds = snapshot.epgChannelIds
            } else {
                secondarySessions[snapshot.identity] = source
            }
            save(snapshot, activate)
            return importStatuses.values.all { it is ImportStatus.Count || it is ImportStatus.Loaded } &&
                storageError == null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            sourceError = when (error) {
                is InvalidPortalAddressException -> R.string.portal_invalid_address
                is PortalAuthenticationException -> R.string.portal_auth_failed
                is PortalHandshakeException -> R.string.portal_handshake_failed
                is IllegalArgumentException -> R.string.portal_invalid_mac
                else -> R.string.portal_error
            }
            return false
        }
    }

    fun importPortal() {
        editing = false
        val enteredAddress = address.trim()
        val enteredMac = mac.trim()
        connecting = true
        batchSummary = null
        scope.launch {
            try {
                refreshPortal(enteredAddress, enteredMac, activate = true)
                if (automaticCountries && importStatuses.values.any {
                    it is ImportStatus.Count || it is ImportStatus.Loaded || it is ImportStatus.Partial
                }) askForCountries = true
            } finally {
                connecting = false
            }
        }
    }

    fun refreshAllSources() {
        val sources = savedSources
        if (connecting || sources.isEmpty()) return
        connecting = true
        batchSummary = null
        scope.launch {
            var completed = 0
            try {
                for ((index, source) in sources.withIndex()) {
                    batchProgress = (index + 1) to sources.size
                    if (refreshPortal(source.address, source.mac, activate = false)) completed++
                }
                batchSummary = completed to (sources.size - completed)
                if (completed > 0 && automaticCountries) askForCountries = true
            } finally {
                batchProgress = null
                connecting = false
            }
        }
    }

    fun loadMore(section: PortalSection) {
        val session = catalogSession ?: return
        val previous = catalogs[section] ?: return
        val page = previous.nextPage ?: return
        if (connecting || section in catalogLoading) return
        catalogLoading = catalogLoading + section
        catalogErrors = catalogErrors - section
        scope.launch {
            try {
                val token = session.second ?: session.first.openSession()
                val catalog = session.first.loadCatalog(section, token, page, previous)
                if (catalogSession === session) {
                    catalogs = catalogs + (section to catalog)
                    storedSource?.let { saved ->
                        save(SavedPortal(saved.address, saved.mac, channels, genres, catalogs, epgChannelIds))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (catalogSession === session) catalogErrors = catalogErrors + section
            } finally {
                if (catalogSession === session) catalogLoading = catalogLoading - section
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    pendingSeriesEpisode?.let { (series, episode) ->
                        pendingSeriesEpisode = null
                        updateTrackedSeries(series, watchedEpisode = episode)
                    }
                    pendingMovie?.let { movie ->
                        pendingMovie = null
                        movieHistory = (listOf(movie) + movieHistory.filterNot { it.progressKey == movie.progressKey }).take(200)
                        progressPreferences?.edit()?.putString("movie_history", encodeMovieHistory(movieHistory))?.apply()
                    }
                    playbackAttempt?.let { attempt ->
                        if (lastBadgedAttempt != attempt.mediaId) {
                            lastBadgedAttempt = attempt.mediaId
                            playbackSourceBadge = attempt.mediaId to attempt.variants[attempt.index].label
                        }
                    }
                    val openFullscreen = when (vodSection) {
                        null -> selected == null || selected == Destination.LIVE || selected == Destination.GUIDE
                        PortalSection.MOVIES -> selected == Destination.MOVIES
                        PortalSection.SERIES -> selected == Destination.SERIES
                        PortalSection.LIVE -> false
                    }
                    if (starting != null && openFullscreen) fullscreen = true
                    starting = null
                } else savePlaybackPosition(onFailure = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                val attempt = playbackAttempt ?: return
                if (playbackState == Player.STATE_ENDED && player?.currentMediaItem?.mediaId == attempt.mediaId) {
                    if (attempt.progressKey == null) {
                        scope.launch { failPlayback(attempt, R.string.playback_error) }
                    } else progressPreferences?.edit()?.remove(attempt.progressKey)?.apply()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val item = player?.currentMediaItem
                val attempt = playbackAttempt ?: return
                if (item?.mediaId != attempt.mediaId) return
                val startingChannel = starting
                val uri = item?.localConfiguration?.uri
                val formatError = error.errorCode in setOf(
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                )
                if (uri != null && startingChannel != null && formatError &&
                    !triedHls && !uri.path.orEmpty().endsWith(".m3u8", ignoreCase = true)
                ) {
                    triedHls = true
                    scope.launch {
                        if (starting === startingChannel && player.currentMediaItem == item) {
                            val hls = item.buildUpon().setMimeType(MimeTypes.APPLICATION_M3U8).build()
                            if (attempt.progressKey == null) player.setMediaItem(hls)
                            else player.setMediaItem(hls, attempt.positionMs)
                            player.prepare()
                            player.playWhenReady = canPlay()
                        }
                    }
                    return
                }
                val httpStatus = (error.cause as? HttpDataSource.InvalidResponseCodeException)
                    ?.responseCode
                val message = when {
                    httpStatus == 401 || httpStatus == 403 -> R.string.playback_stream_denied
                    formatError -> R.string.playback_format_error
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        R.string.playback_stream_http_error
                    else -> R.string.playback_error
                }
                scope.launch { failPlayback(attempt, message) }
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(playbackAttempt, starting, buffering) {
        val attempt = playbackAttempt
        if (attempt != null && (starting != null || buffering)) {
            delay(25_000)
            if (canPlay() && (starting != null || player?.playWhenReady == true)) {
                failPlayback(attempt, R.string.playback_timeout)
            }
        }
    }
    LaunchedEffect(playbackSelection) {
        if (playbackSelection?.section == PortalSection.LIVE || playbackSelection == null) return@LaunchedEffect
        while (true) {
            delay(3_000)
            if (player?.isPlaying == true) savePlaybackPosition()
        }
    }
    LaunchedEffect(playbackSourceBadge) {
        if (playbackSourceBadge != null) {
            delay(4_000)
            playbackSourceBadge = null
        }
    }

    val insideCategory = (selected == null || selected == Destination.LIVE) && category != null
    fun returnToCategories() {
        lastLiveCategory = category
        category = null
        restoreLiveFocus = true
    }
    DisposableEffect(fullscreen) {
        onFullscreen(fullscreen)
        onDispose { if (fullscreen) onFullscreen(false) }
    }
    LaunchedEffect(lastBack) {
        if (lastBack != 0L) {
            delay(1_500)
            lastBack = 0
        }
    }
    BackHandler(enabled = fullscreen || selected != null || insideCategory) {
        if (fullscreen) {
            val now = SystemClock.elapsedRealtime()
            if (lastBack != 0L && now - lastBack <= 1_500) {
                savePlaybackPosition()
                restoreLiveFocus = vodSection == null &&
                    (selected == null || selected == Destination.LIVE)
                fullscreen = false
                lastBack = 0
            } else lastBack = now
        } else if (insideCategory) returnToCategories()
        else if (selected == Destination.SERIES && openedSeason != null) openedSeason = null
        else if (selected == Destination.SERIES && openedSeries != null) closeSeries()
        else if (details != null && selected in listOf(Destination.SERIES, Destination.MOVIES)) details = null
        else if (selected == Destination.SERIES && seriesActivityView != null) seriesActivityView = null
        else if (selected == Destination.MOVIES && movieHistoryOpen) movieHistoryOpen = false
        else if (
            (selected == Destination.MOVIES && selectedGroup?.first == PortalSection.MOVIES) ||
            (selected == Destination.SERIES && selectedGroup?.first == PortalSection.SERIES)
        ) closeGroup()
        else selected = if (selected == Destination.SOURCES || selected == Destination.CATEGORIES) {
            Destination.SETTINGS
        } else null
    }
    LaunchedEffect(playbackAttempt, autoNextEpisode, autoSkipIntro, episodeNoticeSeconds) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val active = player ?: return@LaunchedEffect
        if (attempt.selection.section != PortalSection.SERIES) return@LaunchedEffect
        var introSkipped = false
        while (playbackAttempt === attempt) {
            val duration = active.duration
            if (active.currentMediaItem?.mediaId == attempt.mediaId && duration > 0) {
                val timing = attempt.variants[attempt.index].timing
                val position = if (active.playbackState == Player.STATE_ENDED) duration else active.currentPosition
                val intro = timing.intro(duration)
                skipIntroTo = if (!introSkipped && intro != null && position in intro &&
                    active.isCurrentMediaItemSeekable) intro.last + 1 else null
                if (autoSkipIntro && active.isPlaying) skipIntroTo?.let {
                    active.seekTo(it)
                    skipIntroTo = null
                    introSkipped = true
                }
                val remaining = (timing.nextAt(duration) - position).coerceAtLeast(0)
                nextEpisodeSeconds = if (nextEpisodeAction != null && remaining <= episodeNoticeSeconds * 1_000L) {
                    ((remaining + 999) / 1_000).toInt()
                } else null
                if (autoNextEpisode && (active.isPlaying || active.playbackState == Player.STATE_ENDED) &&
                    remaining == 0L && canPlay()
                ) {
                    val advance = nextEpisodeAction
                    if (advance != null) {
                        advance()
                        return@LaunchedEffect
                    }
                }
            }
            delay(250)
        }
    }
    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PlayerFrame(player, active = true, fullscreen = true, onStop = ::stopPlayback)
            if (skipIntroTo != null || nextEpisodeSeconds != null) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(20.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        skipIntroTo?.let { position ->
                            MenuButton(stringResource(R.string.skip_intro), Modifier) {
                                player?.seekTo(position)
                                skipIntroTo = null
                            }
                        }
                        nextEpisodeSeconds?.let { seconds ->
                            Text(
                                if (autoNextEpisode) stringResource(R.string.episode_countdown, nextEpisodeName, seconds)
                                else stringResource(R.string.next_episode_ready, nextEpisodeName)
                            )
                            Row {
                                MenuButton(stringResource(R.string.play_next_episode), Modifier) {
                                    nextEpisodeAction?.invoke()
                                }
                                MenuButton(stringResource(R.string.stop_playback), Modifier, onClick = ::stopPlayback)
                            }
                        }
                    }
                }
            }
            playbackSourceBadge?.let { (_, label) ->
                Text(
                    stringResource(R.string.playback_source_badge, label),
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            playbackAttempt?.takeIf { it.index > 0 && starting != null }?.let { attempt ->
                val portal = attempt.variants[attempt.index].label
                Text(
                    if (attempt.positionMs > 0) {
                        stringResource(R.string.playback_switching_source_at, portal, playbackTime(attempt.positionMs))
                    } else stringResource(R.string.playback_switching_source, portal),
                    Modifier.align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface).padding(12.dp)
                )
            }
            if (lastBack != 0L || playbackError != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        stringResource(playbackError ?: R.string.fullscreen_back_hint),
                        Modifier.padding(12.dp)
                    )
                }
            }
        }
        return
    }
    categoryMenu?.let { (section, first) ->
        val groups = if (section == PortalSection.LIVE) {
            allLiveCategories.keys.sorted().map { PortalGroup(it, it) }
        } else browsingGroups(section)
        CategoryDialog(
            groups, first,
            isHidden = { categoryKey(section, it.id) in hiddenCategories },
            onApply = { ids, hidden ->
                setCategoryVisibility(section, ids, hidden)
                categoryMenu = null
            },
            onDismiss = { categoryMenu = null }
        )
    }
    if (askForCountries) {
        AlertDialog(
            onDismissRequest = { askForCountries = false },
            title = { Text(stringResource(R.string.select_countries)) },
            text = { Text(stringResource(R.string.ask_countries)) },
            confirmButton = {
                TextButton(onClick = {
                    askForCountries = false
                    choosingCountries = true
                }) { Text(stringResource(R.string.select_countries)) }
            },
            dismissButton = {
                TextButton(onClick = { askForCountries = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
    if (choosingCountries) {
        CountryDialog(
            knownCountries, countries,
            onApply = { saveCountries(it); choosingCountries = false },
            onDismiss = { choosingCountries = false }
        )
    }
    if (metadataSettingsOpen) {
        MetadataSettings(
            connected = metadataClient != null,
            busy = metadataBusy,
            error = metadataError,
            onConnect = { updateMetadataConnection(it) },
            onDisconnect = { updateMetadataConnection(null) },
            onDismiss = { metadataSettingsOpen = false }
        )
    }
    if (episodeSettingsOpen) AlertDialog(
        onDismissRequest = { episodeSettingsOpen = false },
        title = { Text(stringResource(R.string.episode_settings)) },
        text = {
            Column {
                MenuButton(stringResource(R.string.auto_next_episode), Modifier, selected = autoNextEpisode) {
                    autoNextEpisode = !autoNextEpisode
                    preferences?.edit()?.putBoolean("auto_next_episode", autoNextEpisode)?.apply()
                }
                MenuButton(stringResource(R.string.auto_skip_intro), Modifier, selected = autoSkipIntro) {
                    autoSkipIntro = !autoSkipIntro
                    preferences?.edit()?.putBoolean("auto_skip_intro", autoSkipIntro)?.apply()
                }
                Text(stringResource(R.string.episode_notice_seconds, episodeNoticeSeconds))
                Row {
                    MenuButton("−5", Modifier) {
                        episodeNoticeSeconds = (episodeNoticeSeconds - 5).coerceAtLeast(5)
                        preferences?.edit()?.putInt("episode_notice_seconds", episodeNoticeSeconds)?.apply()
                    }
                    MenuButton("+5", Modifier) {
                        episodeNoticeSeconds = (episodeNoticeSeconds + 5).coerceAtMost(60)
                        preferences?.edit()?.putInt("episode_notice_seconds", episodeNoticeSeconds)?.apply()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { episodeSettingsOpen = false }) { Text(stringResource(android.R.string.ok)) }
        }
    )
    Scaffold(
        modifier = Modifier.fillMaxSize().then(if (selected == Destination.SEARCH) Modifier.imePadding() else Modifier),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            val destination = selected
            val menu: @Composable () -> Unit = {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                mainDestinations.forEach { item ->
                    MenuButton(
                        label = stringResource(item.label),
                        modifier = if (
                            item == lastFocused &&
                            destination != Destination.SETTINGS &&
                            destination != Destination.SOURCES && destination != Destination.CATEGORIES
                        ) {
                            Modifier.fillMaxWidth().focusRequester(initialFocus)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        selected = item == (destination ?: Destination.LIVE) ||
                            (item == Destination.SETTINGS &&
                                destination in listOf(Destination.SOURCES, Destination.CATEGORIES)),
                        onClick = {
                            lastFocused = item
                            selected = item
                        }
                    )
                }
            }
            val content: @Composable () -> Unit = {
                Text(
                    text = stringResource(destination?.label ?: R.string.destination_live),
                    style = MaterialTheme.typography.headlineSmall
                )
                when (destination) {
                    null, Destination.LIVE -> LiveOverview(
                        player = player,
                        groups = liveCategories,
                        visibleGroups = liveCategories.keys.filter {
                            visibleCategory(PortalSection.LIVE, it, it)
                        }.sortedBy { categoryNames.values.indexOf(it) },
                        hiddenGroups = hiddenCategories,
                        onManageGroups = { categoryMenu = PortalSection.LIVE to it },
                        category = category,
                        onCategory = {
                            if (it == null) returnToCategories()
                            else {
                                lastLiveCategory = it
                                category = it
                            }
                        },
                        categoryScroll = categoryScroll,
                        channelScroll = channelScroll,
                        lastCategory = lastLiveCategory,
                        restoreFocus = restoreLiveFocus,
                        onFocusRestored = { restoreLiveFocus = false },
                        playing = playing.takeIf { vodSection == null },
                        starting = starting.takeIf { vodSection == null },
                        programmeChannel = programmeChannel,
                        programmes = programmes,
                        variants = programmeChannel?.let { channel ->
                            liveGroups[channel]?.variants.orEmpty().map { variant ->
                                val host = sessionFor(variant.item.source).sourceLabel
                                val quality = variant.description.qualities.joinToString(" / ")
                                    .ifBlank { stringResource(R.string.live_quality_unknown) }
                                "$quality · $host"
                            }
                        }.orEmpty(),
                        epgPinned = pinnedProgramme,
                        onToggleEpg = ::toggleProgrammeChannel,
                        onHighlight = { programmeChannel = it },
                        playbackError = playbackError.takeIf { vodSection == null },
                        onStop = ::stopPlayback,
                        onSelect = ::playChannel
                    )
                    Destination.SETTINGS -> {
                        MenuButton(
                            label = stringResource(R.string.destination_sources),
                            modifier = Modifier.focusRequester(initialFocus),
                            onClick = { selected = Destination.SOURCES }
                        )
                        MenuButton(stringResource(R.string.category_settings), Modifier) {
                            selected = Destination.CATEGORIES
                        }
                        MenuButton(stringResource(R.string.tmdb_settings), Modifier) {
                            metadataSettingsOpen = true
                        }
                        MenuButton(stringResource(R.string.tvmaze_episode_provider), Modifier, selected = useTvmaze) {
                            useTvmaze = !useTvmaze
                            preferences?.edit()?.putBoolean("use_tvmaze", useTvmaze)?.apply()
                        }
                        if (useTvmaze) {
                            MenuButton(stringResource(R.string.tvmaze_credit), Modifier) {
                                uriHandler.openUri("https://www.tvmaze.com/")
                            }
                        }
                        MenuButton(stringResource(R.string.episode_settings), Modifier) { episodeSettingsOpen = true }
                    }
                    Destination.CATEGORIES -> {
                        Text(stringResource(R.string.category_settings_hint))
                        MenuButton(
                            stringResource(R.string.select_countries),
                            Modifier.focusRequester(initialFocus)
                        ) { choosingCountries = true }
                        MenuButton(
                            stringResource(R.string.auto_country_prompt),
                            Modifier, selected = automaticCountries
                        ) {
                            automaticCountries = !automaticCountries
                            preferences?.edit()?.putBoolean("autoCountries", automaticCountries)?.apply()
                        }
                        MenuButton(
                            stringResource(R.string.show_hidden_categories),
                            Modifier,
                            selected = showHiddenCategories
                        ) { showHiddenCategories = !showHiddenCategories }
                    }
                    Destination.SOURCES -> {
                        MenuButton(
                            label = stringResource(R.string.add_portal),
                            modifier = Modifier.focusRequester(initialFocus),
                            onClick = {
                                if (!restoring && !connecting) {
                                    address = ""
                                    mac = ""
                                    editing = true
                                }
                            }
                        )
                        Button(
                            enabled = savedSources.isNotEmpty() && !connecting && !restoring,
                            onClick = ::refreshAllSources
                        ) { Text(stringResource(R.string.refresh_all_sources)) }
                        MenuButton(
                            stringResource(R.string.merge_sources), Modifier,
                            selected = mergeSources
                        ) {
                            mergeSources = !mergeSources
                            preferences?.edit()?.putBoolean("mergeSources", mergeSources)?.apply()
                            closeGroup()
                            closeSeries()
                            details = null
                            searchedVariants = emptyMap()
                            category = null
                        }
                        Text(stringResource(
                            if (mergeSources) R.string.sources_combined else R.string.sources_separate
                        ))
                        for ((index, source) in savedSources.withIndex()) {
                            val host = remember(source.address) {
                                runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                            }
                            val isActive = source.identity == storedSource?.identity
                            MenuButton(
                                stringResource(
                                    if (!mergeSources && isActive) R.string.active_portal_label else R.string.saved_portal_label,
                                    index + 1, host
                                ),
                                Modifier.fillMaxWidth(),
                                selected = !mergeSources && isActive,
                                onClick = {
                                    if (mergeSources && !connecting) {
                                        address = source.address
                                        mac = source.mac
                                        editing = true
                                    } else switchSource(source)
                                }
                            )
                        }
                        storedSource?.takeUnless { mergeSources }?.let { saved ->
                            MenuButton(stringResource(R.string.edit_portal), Modifier) {
                                if (!connecting) {
                                    address = saved.address
                                    mac = saved.mac
                                    editing = true
                                }
                            }
                        }
                        if (editing) PortalEditor(
                            address = address,
                            mac = mac,
                            onAddressChange = { address = it },
                            onMacChange = { mac = it },
                            onDismiss = { editing = false },
                            onSubmit = ::importPortal
                        )
                        if (connecting) {
                            batchProgress?.let { (number, total) ->
                                Text(stringResource(R.string.refresh_all_progress, number, total))
                            }
                            if (importStatuses.isEmpty()) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            } else {
                                val completed = importStatuses.values.count {
                                    it != ImportStatus.Loading && it !is ImportStatus.Progress
                                }
                                LinearProgressIndicator(
                                    progress = { (completed + 1f) / (PortalSection.entries.size + 1) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(stringResource(R.string.import_steps, completed + 1, PortalSection.entries.size + 1))
                            }
                            if (importStatuses.isEmpty()) Text(stringResource(R.string.connecting))
                        }
                        sourceError?.let { message ->
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        }
                        storageError?.let { message ->
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        }
                        PortalImportStatus(importStatuses)
                        batchSummary?.let { (updated, failed) ->
                            Text(stringResource(R.string.refresh_all_summary, updated, failed))
                        }
                        if (liveVariants.isNotEmpty()) {
                            MenuButton(
                                label = stringResource(R.string.open_live_tv),
                                modifier = Modifier,
                                onClick = {
                                    selected = Destination.LIVE
                                    lastFocused = Destination.LIVE
                                }
                            )
                        }
                        MenuButton(
                            label = stringResource(R.string.back_to_settings),
                            modifier = Modifier,
                            onClick = { selected = Destination.SETTINGS }
                        )
                    }
                    Destination.GUIDE -> {
                        val guideChannels = remember(liveCategories, countries, hiddenCategories, showHiddenCategories) {
                            liveCategories.entries.filter { visibleCategory(PortalSection.LIVE, it.key, it.key) }
                                .sortedBy { categoryNames.values.indexOf(it.key) }
                                .flatMap { it.value }
                        }
                        guideState.SaveableStateProvider("programme-guide") {
                            ProgrammeTimeline(
                                channels = guideChannels,
                                pinnedIds = pinnedProgrammeKeys,
                                highlighted = programmeChannel,
                                programmes = epgCache,
                                programmeKey = ::programmeKey,
                                scroll = guideScroll,
                                onHighlight = { programmeChannel = it },
                                onPlay = ::playChannel,
                                onTogglePin = ::toggleProgrammeChannel
                            )
                        }
                    }
                    Destination.SEARCH -> PortalSearch(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        sources = sourcesForBrowsing,
                        channels = liveCategories.values.flatten(),
                        liveSources = { channel -> liveVariants[channel].orEmpty().map { it.source } },
                        liveProgramme = { channel -> epgCache[programmeKey(channel)]?.firstOrNull()?.title },
                        onHighlightChannel = { programmeChannel = it },
                        metadataClient = metadataClient,
                        portalFor = ::sessionFor,
                        countries = countries,
                        combine = combine,
                        onChannel = { channel ->
                            programmeChannel = channel
                            category = liveCategories.entries.firstOrNull { channel in it.value }?.key
                            selected = Destination.LIVE
                            lastFocused = Destination.LIVE
                            liveGroups[channel]?.let { group ->
                                reopenPlayback(liveSelection(channel, group, combine))
                            }
                        },
                        onTitle = { section, items ->
                            val title = items.first().value
                            searchedVariants = mapOf(title to items)
                            selected = if (section == PortalSection.MOVIES) Destination.MOVIES else Destination.SERIES
                            lastFocused = requireNotNull(selected)
                            showTitle(section, title)
                        }
                    )
                    Destination.MOVIES, Destination.SERIES -> {
                        val section = if (destination == Destination.MOVIES) {
                            PortalSection.MOVIES
                        } else {
                            PortalSection.SERIES
                        }
                        if (vodSection == section) {
                            PlaybackFeedback(starting, playbackError)
                            if (playing != null) PlayerFrame(player, active = true, onStop = ::stopPlayback)
                        }
                        val series = openedSeries.takeIf { section == PortalSection.SERIES }
                        if (series != null) {
                            if (resumeEpisodeMissing) Text(
                                stringResource(R.string.resume_episode_missing),
                                color = MaterialTheme.colorScheme.error
                            )
                            SeriesSeasons(
                                series = series,
                                seasons = seasons,
                                selected = openedSeason,
                                failed = episodeError,
                                unseen = trackedSeries(series)?.unseen.orEmpty(),
                                onSeason = { openedSeason = it },
                                onBack = {
                                    if (openedSeason != null) openedSeason = null else closeSeries()
                                },
                                onEpisode = { selectTitle(section, it) }
                            )
                        } else if (details?.first == section) {
                            val title = requireNotNull(details).second
                            if (section == PortalSection.SERIES) {
                                trackedSeries(title)?.let { entry ->
                                    resumeFor(entry)?.let { resume ->
                                        val position = progressPreferences?.getLong(resume.progressKey, 0L) ?: 0L
                                        Text(episodeResumeLabel(resume, position))
                                        MenuButton(stringResource(R.string.resume_playback), Modifier) {
                                            selectTitle(section, title, resume)
                                        }
                                    }
                                }
                            }
                            TitleDetails(
                                detailMetadata?.applyTo(title) ?: title, section,
                                metadataRating = detailMetadata?.rating,
                                fromTmdb = detailMetadata != null,
                                favorite = titleKey(section, title) in favorites,
                                saved = isTitleSaved(section, title),
                                newEpisodes = if (section == PortalSection.SERIES) {
                                    trackedSeries(title)?.unseen?.size ?: 0
                                } else 0,
                                onBack = { details = null },
                                onPlay = { selectTitle(section, title) },
                                onFavorite = { toggleSavedTitle(section, title, true) },
                                onWatch = { toggleSavedTitle(section, title, false) }
                            )
                        } else if (section == PortalSection.SERIES && seriesActivityView != null) {
                            val showHistory = seriesActivityView == SeriesActivityView.HISTORY
                            val entries = if (showHistory) seriesActivity.filter { it.viewed }.sortedByDescending { it.watchedAt }
                                else seriesActivity.filter { it.saved }.sortedBy { it.name }
                            MenuButton(stringResource(R.string.back_to_categories), Modifier) { seriesActivityView = null }
                            Text(stringResource(if (showHistory) R.string.series_history else R.string.watchlist))
                            if (entries.isEmpty()) Text(stringResource(R.string.no_tracked_series))
                            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                                items(entries, key = { it.key }) { entry ->
                                    val label = if (entry.unseen.isEmpty()) entry.name else
                                        "${entry.name} · ${stringResource(R.string.new_episode_count, entry.unseen.size)}"
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            MenuButton(label, Modifier.fillMaxWidth()) { openTrackedSeries(entry) }
                                            resumeFor(entry)?.let { resume ->
                                                val position = progressPreferences?.getLong(resume.progressKey, 0L) ?: 0L
                                                Text(
                                                    episodeResumeLabel(resume, position),
                                                    Modifier.padding(start = 10.dp, bottom = 6.dp),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        resumeFor(entry)?.let { resume ->
                                            MenuButton(stringResource(R.string.resume_playback), Modifier) {
                                                openTrackedSeries(entry) { title ->
                                                    closeSeries()
                                                    details = section to title
                                                    selectTitle(section, title, resume)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (section == PortalSection.MOVIES && movieHistoryOpen) {
                            MenuButton(stringResource(R.string.back_to_categories), Modifier) { movieHistoryOpen = false }
                            Text(stringResource(R.string.movie_history))
                            if (movieHistory.isEmpty()) Text(stringResource(R.string.no_watched_movies))
                            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                                items(movieHistory, key = { it.progressKey }) { entry ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            MenuButton(entry.name, Modifier.fillMaxWidth()) {
                                                openWatchedMovie(entry, resume = false)
                                            }
                                            val position = progressPreferences?.getLong(entry.progressKey, 0L) ?: 0L
                                            if (position > 0) Text(
                                                stringResource(R.string.saved_playback_position, playbackTime(position)),
                                                Modifier.padding(start = 10.dp, bottom = 6.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        MenuButton(stringResource(R.string.resume_playback), Modifier) {
                                            openWatchedMovie(entry, resume = true)
                                        }
                                    }
                                }
                            }
                        } else {
                            val group = selectedGroup?.takeIf { it.first == section }?.second
                            val baseCatalog = catalogs[section]
                            val uncategorizedSelected = !combine && group?.id == ALL_CATALOG_ITEMS
                            if (group != null) {
                                MenuButton(
                                    stringResource(R.string.back_to_categories), Modifier,
                                    onClick = ::closeGroup
                                )
                                Text(group.name, style = MaterialTheme.typography.titleMedium)
                                if (groupUsesTmdb) Text(stringResource(R.string.tmdb_catalogue_source))
                                CatalogOverview(
                                    catalog = if (uncategorizedSelected) baseCatalog else groupCatalog,
                                    loading = if (uncategorizedSelected) section in catalogLoading else groupLoading,
                                    failed = if (uncategorizedSelected) section in catalogErrors else groupError,
                                    canLoadMore = !connecting,
                                    emptyMessage = if (groupUsesTmdb) R.string.tmdb_no_portal_matches
                                    else if (section == PortalSection.MOVIES) {
                                        R.string.no_movies
                                    } else R.string.no_series,
                                    onLoadMore = {
                                        if (uncategorizedSelected) loadMore(section)
                                        else loadGroup(section, group, next = true)
                                    },
                                    isFavorite = { titleKey(section, it) in favorites },
                                    isSaved = { isTitleSaved(section, it) },
                                    preferPopularity = group.id == VodCategory.POPULAR.id,
                                    newEpisodes = { title ->
                                        if (section == PortalSection.SERIES) trackedSeries(title)?.unseen?.size ?: 0 else 0
                                    },
                                    onSelect = {
                                        searchedVariants = emptyMap()
                                        showTitle(section, it)
                                    }
                                )
                            } else {
                                if (section == PortalSection.SERIES) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MenuButton(
                                            stringResource(R.string.watchlist), Modifier,
                                            count = seriesActivity.filter { it.saved }.sumOf { it.unseen.size }
                                                .takeIf { it > 0 }
                                        ) { seriesActivityView = SeriesActivityView.WATCHLIST }
                                        MenuButton(
                                            stringResource(R.string.series_history), Modifier,
                                            count = seriesActivity.filter { it.viewed }.sumOf { it.unseen.size }
                                                .takeIf { it > 0 }
                                        ) { seriesActivityView = SeriesActivityView.HISTORY }
                                    }
                                } else {
                                    MenuButton(stringResource(R.string.movie_history), Modifier) {
                                        movieHistoryOpen = true
                                    }
                                }
                                CatalogCategories(
                                groups = browsingGroups(section)
                                    .filter { visibleCategory(section, it.id, it.name, it.country) }
                                    .sortedWith(compareBy(
                                        { if (combine) vodCategoryNames.keys.indexOf(it.id) else countryPriority(it.name, it.country) },
                                        { it.name }
                                    )),
                                loading = section in catalogLoading ||
                                    importStatuses[section] == ImportStatus.Loading,
                                failed = section in catalogErrors ||
                                    importStatuses[section] == ImportStatus.Failed,
                                isHidden = { categoryKey(section, it.id) in hiddenCategories },
                                onSelect = {
                                    if (!combine && it.id == ALL_CATALOG_ITEMS) {
                                        closeGroup()
                                        selectedGroup = section to it
                                    } else loadGroup(section, it)
                                },
                                onManage = { categoryMenu = section to it }
                                )
                            }
                        }
                    }
                }
            }
            if (maxWidth >= 560.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.weight(0.9f)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                            .verticalScroll(menuScroll)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        menu()
                    }
                    Column(
                        Modifier.weight(3.6f).verticalScroll(contentScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        content()
                    }
                }
            } else {
                Column(
                    Modifier.verticalScroll(contentScroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    menu()
                    content()
                }
            }
            LaunchedEffect(destination) {
                if (!restoreLiveFocus) initialFocus.requestFocus()
            }
        }
    }
}

@Composable
private fun PortalEditor(
    address: String,
    mac: String,
    onAddressChange: (String) -> Unit,
    onMacChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.destination_sources),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(stringResource(R.string.stalker_hint))
                    OutlinedTextField(
                        value = address,
                        onValueChange = onAddressChange,
                        label = { Text(stringResource(R.string.portal_address)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mac,
                        onValueChange = onMacChange,
                        label = { Text(stringResource(R.string.portal_mac)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        enabled = address.isNotBlank() && mac.isNotBlank(),
                        onClick = onSubmit
                    ) {
                        Text(stringResource(R.string.connect_portal))
                    }
                    MenuButton(stringResource(android.R.string.cancel), Modifier, onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun PortalImportStatus(statuses: Map<PortalSection, ImportStatus>) {
    PortalSection.entries.forEach { section ->
        val label = stringResource(section.label)
        when (val status = statuses[section]) {
            ImportStatus.Loading -> Text(stringResource(R.string.portal_section_loading, label))
            is ImportStatus.Progress -> Text(
                if (status.total != null) {
                    stringResource(R.string.live_progress_total, status.loaded, status.total)
                } else {
                    stringResource(R.string.live_progress_loaded, status.loaded)
                }
            )
            ImportStatus.Failed -> Text(
                text = stringResource(R.string.portal_section_failed, label),
                color = MaterialTheme.colorScheme.error
            )
            is ImportStatus.Count -> Text(
                stringResource(R.string.portal_section_count, label, status.value)
            )
            is ImportStatus.Partial -> Text(
                stringResource(R.string.portal_section_partial, label, status.value),
                color = MaterialTheme.colorScheme.error
            )
            is ImportStatus.Loaded -> Text(
                stringResource(R.string.portal_section_loaded, label, status.value)
            )
            is ImportStatus.Error -> Text(
                stringResource(status.message),
                color = MaterialTheme.colorScheme.error
            )
            null -> Unit
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun LiveOverview(
    player: ExoPlayer?,
    groups: Map<String, List<StalkerChannel>>,
    visibleGroups: List<String>,
    hiddenGroups: Set<String>,
    onManageGroups: (String?) -> Unit,
    category: String?,
    onCategory: (String?) -> Unit,
    categoryScroll: LazyListState,
    channelScroll: LazyListState,
    lastCategory: String?,
    restoreFocus: Boolean,
    onFocusRestored: () -> Unit,
    playing: StalkerChannel?,
    starting: StalkerChannel?,
    programmeChannel: StalkerChannel?,
    programmes: List<PortalProgramme>?,
    variants: List<String>,
    epgPinned: Boolean,
    onToggleEpg: (StalkerChannel) -> Unit,
    onHighlight: (StalkerChannel) -> Unit,
    playbackError: Int?,
    onStop: () -> Unit,
    onSelect: (StalkerChannel) -> Unit
) {
    val visibleChannels = groups[category]
    val targetIndex = if (visibleChannels == null) {
        visibleGroups.indexOfFirst { it == lastCategory }
    } else {
        visibleChannels.indexOfFirst { it === playing }.takeIf { it >= 0 }
            ?: visibleChannels.indexOfFirst { it === programmeChannel }
    }
    val activeScroll = if (visibleChannels == null) categoryScroll else channelScroll
    val restoreRequester = remember { FocusRequester() }
    LaunchedEffect(restoreFocus, targetIndex, activeScroll) {
        if (!restoreFocus) return@LaunchedEffect
        if (targetIndex < 0) {
            onFocusRestored()
            return@LaunchedEffect
        }
        val layout = snapshotFlow { activeScroll.layoutInfo }.first { it.totalItemsCount > targetIndex }
        if (layout.visibleItemsInfo.none { it.index == targetIndex }) {
            activeScroll.scrollToItem(targetIndex)
        }
    }
    val rowModifier: @Composable (Int) -> Modifier = { index ->
        if (restoreFocus && index == targetIndex) {
            LaunchedEffect(activeScroll, targetIndex) {
                withFrameNanos { }
                if (restoreRequester.requestFocus()) onFocusRestored()
            }
            Modifier.fillMaxWidth().focusRequester(restoreRequester)
        } else {
            Modifier.fillMaxWidth()
        }
    }
    val channels: @Composable () -> Unit = {
        Text(
            category ?: stringResource(R.string.channel_categories),
            style = MaterialTheme.typography.titleMedium
        )
        if (groups.isEmpty()) {
            Text(stringResource(R.string.no_channels), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (visibleChannels == null) {
            MenuButton(stringResource(R.string.manage_categories), Modifier.fillMaxWidth()) {
                onManageGroups(null)
            }
            if (visibleGroups.isEmpty()) Text(stringResource(R.string.no_visible_categories))
            LazyColumn(Modifier.heightIn(max = 320.dp), state = categoryScroll) {
                itemsIndexed(visibleGroups, key = { _, group -> group }) { index, group ->
                    MenuButton(
                        categoryEntryLabel(group, categoryKey(PortalSection.LIVE, group) in hiddenGroups),
                        rowModifier(index),
                        count = groups.getValue(group).size,
                        hiddenIndicator = categoryKey(PortalSection.LIVE, group) in hiddenGroups,
                        onLongPress = { onManageGroups(group) }
                    ) { onCategory(group) }
                }
            }
        } else {
            MenuButton(stringResource(R.string.back_to_categories), Modifier.fillMaxWidth()) {
                onCategory(null)
            }
            LazyColumn(Modifier.heightIn(max = 320.dp), state = channelScroll) {
                itemsIndexed(visibleChannels) { index, channel ->
                    MenuButton(
                        channel.name, rowModifier(index), channel == playing, channel.logo,
                        onFocused = { onHighlight(channel) }
                    ) {
                        if (programmeChannel === channel || playing === channel) onSelect(channel)
                        else onHighlight(channel)
                    }
                }
            }
        }
    }
    val preview: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaybackFeedback(starting, playbackError)
            PlayerFrame(player, active = playing != null, showEmpty = starting == null, onStop = onStop)
            if (programmeChannel != null) {
                ProgrammeDetails(programmeChannel, programmes?.take(2))
                if (variants.isNotEmpty()) {
                    Text(stringResource(R.string.live_stream_variants), style = MaterialTheme.typography.titleSmall)
                    variants.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                if (programmeChannel !== playing && programmeChannel !== starting) {
                    MenuButton(
                        stringResource(R.string.play_selected_channel), Modifier,
                        onClick = { onSelect(programmeChannel) }
                    )
                }
                if (programmeChannel.id != null) {
                    MenuButton(
                        stringResource(if (epgPinned) R.string.epg_unpin else R.string.epg_pin),
                        Modifier,
                        onClick = { onToggleEpg(programmeChannel) }
                    )
                }
            }
        }
    }
    BoxWithConstraints {
        if (maxWidth >= 380.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    Modifier.weight(1.05f).background(
                        MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium
                    ).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { channels() }
                preview(Modifier.weight(2f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                preview(Modifier.fillMaxWidth())
                channels()
            }
        }
    }
}

@Composable
private fun categoryEntryLabel(name: String, hidden: Boolean): String =
    if (hidden) stringResource(R.string.hidden_category_label, name) else name

@Composable
private fun CatalogCategories(
    groups: List<PortalGroup>,
    loading: Boolean,
    failed: Boolean,
    isHidden: (PortalGroup) -> Boolean,
    onSelect: (PortalGroup) -> Unit,
    onManage: (String?) -> Unit
) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (failed) Text(stringResource(R.string.catalog_load_failed), color = MaterialTheme.colorScheme.error)
    MenuButton(stringResource(R.string.manage_categories), Modifier) { onManage(null) }
    if (groups.isEmpty() && !loading && !failed) Text(stringResource(R.string.no_visible_categories))
    if (groups.isNotEmpty()) {
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(groups, key = { it.id }) { group ->
                MenuButton(
                    categoryEntryLabel(group.name, isHidden(group)),
                    Modifier.fillMaxWidth(),
                    hiddenIndicator = isHidden(group),
                    onLongPress = { onManage(group.id) }
                ) { onSelect(group) }
            }
        }
    }
}

@Composable
private fun PlaybackFeedback(starting: StalkerChannel?, error: Int?) {
    if (starting != null) {
        Text(stringResource(R.string.playback_starting, starting.name))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (error != null) Text(stringResource(error), color = MaterialTheme.colorScheme.error)
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerFrame(
    player: ExoPlayer?,
    active: Boolean,
    showEmpty: Boolean = true,
    fullscreen: Boolean = false,
    onStop: () -> Unit
) {
    val stop by rememberUpdatedState(onStop)
    Box(
        modifier = (if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF121C2C), Color(0xFF080C13))),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (player != null && active) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = true
                        controllerAutoShow = false
                        findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_center_controls)?.let { row ->
                            val size = (48 * context.resources.displayMetrics.density).toInt()
                            val button = ImageButton(context).apply {
                                setImageResource(R.drawable.ic_player_stop)
                                contentDescription = context.getString(R.string.stop_playback)
                                tooltipText = contentDescription
                                isFocusable = true
                                val background = TypedValue()
                                if (context.theme.resolveAttribute(
                                        android.R.attr.selectableItemBackgroundBorderless, background, true
                                    )
                                ) setBackgroundResource(background.resourceId)
                                setOnClickListener { stop() }
                            }
                            val pause = row.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
                            val index = row.indexOfChild(pause).takeIf { it >= 0 }?.plus(1) ?: row.childCount
                            row.addView(button, index, LinearLayout.LayoutParams(size, size))
                        }
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onReset = null,
                onRelease = { it.player = null },
                update = {}
            )
        } else if (showEmpty) {
            Text(stringResource(R.string.no_playback))
        }
    }
}

@Composable
private fun ProgrammeDetails(channel: StalkerChannel, programmes: List<PortalProgramme>?) {
    Text(channel.name, style = MaterialTheme.typography.titleMedium)
    when {
        programmes == null -> Text(stringResource(R.string.guide_loading))
        programmes.isEmpty() -> Text(stringResource(R.string.no_guide))
        else -> programmes.forEachIndexed { index, programme ->
            val label = when (index) {
                0 -> stringResource(R.string.guide_now)
                1 -> stringResource(R.string.guide_next)
                else -> null
            }
            val time = programme.startSeconds?.let {
                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(it))
            }
            Text(listOfNotNull(label, time, programme.title).joinToString(" · "))
        }
    }
}

@Composable
private fun episodeResumeLabel(episode: EpisodeResume, position: Long): String = listOfNotNull(
    episode.season?.let { stringResource(R.string.season_number, it) },
    episode.episode?.let { stringResource(R.string.episode_number, it) }
        ?: episode.name,
    position.takeIf { it > 0 }?.let { stringResource(R.string.saved_playback_position, playbackTime(it)) }
).joinToString(" · ")

@Composable
private fun CatalogOverview(
    catalog: PortalCatalog?,
    loading: Boolean,
    failed: Boolean,
    canLoadMore: Boolean,
    emptyMessage: Int,
    onLoadMore: () -> Unit,
    isFavorite: (PortalTitle) -> Boolean,
    isSaved: (PortalTitle) -> Boolean,
    preferPopularity: Boolean,
    newEpisodes: (PortalTitle) -> Int,
    onSelect: (PortalTitle) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (failed) Text(
            stringResource(R.string.catalog_load_failed),
            color = MaterialTheme.colorScheme.error
        )
        if (catalog == null || catalog.titles.isEmpty()) {
            if (!loading && !failed) Text(stringResource(emptyMessage))
            if (catalog?.nextPage != null && !loading && canLoadMore) {
                MenuButton(stringResource(R.string.catalog_load_more), Modifier, onClick = onLoadMore)
            }
            return@Column
        }
        catalog.total?.let { total ->
            Text(stringResource(R.string.catalog_count, catalog.titles.size, total))
        }
        TitleGrid(catalog.titles, isFavorite, isSaved, preferPopularity, newEpisodes, onSelect)
        if (catalog.nextPage != null && !loading && canLoadMore) {
            MenuButton(stringResource(R.string.catalog_load_more), Modifier, onClick = onLoadMore)
        }
    }
}

@Composable
internal fun MenuButton(
    label: String,
    modifier: Modifier,
    selected: Boolean = false,
    logo: String? = null,
    count: Int? = null,
    hiddenIndicator: Boolean = false,
    onFocused: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val container = when {
        focused -> colors.tertiaryContainer
        selected -> colors.secondaryContainer
        else -> Color.Transparent
    }
    Surface(
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused && !focused) onFocused?.invoke()
                focused = it.isFocused
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        color = container,
        contentColor = if (focused) colors.onTertiaryContainer else colors.onSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Box(Modifier.padding(end = 8.dp).width(3.dp).height(18.dp)
                    .background(if (focused) colors.onTertiaryContainer else colors.primary))
            }
            ChannelLogo(logo, Modifier.padding(end = 8.dp))
            if (hiddenIndicator) {
                HiddenCategoryIcon()
                Box(Modifier.width(8.dp))
            }
            Text(
                label,
                modifier = if (count == null) Modifier else Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Text(count.toString(), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(name = "Fernseher", widthDp = 960, heightDp = 540)
@Preview(name = "Handy im Querformat", widthDp = 852, heightDp = 393)
@Composable
private fun PlayerHomePreview() {
    MaterialTheme(colorScheme = playerColors) {
        PlayerHome()
    }
}
