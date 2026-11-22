package com.iptvplayer.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState


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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton as MaterialTextButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalConfiguration
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
import android.view.SurfaceView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private val storage by lazy { PortalStorage(applicationContext) }
    private var longBackAction: (() -> Boolean)? = null
    private var screenBackAction: ((Boolean) -> Boolean)? = null
    private var consumedLongBack = false

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount > 0) {
                if (consumedLongBack || longBackAction?.invoke() == true) {
                    consumedLongBack = true
                    return true
                }
            }
            if (event.action == android.view.KeyEvent.ACTION_UP && consumedLongBack) {
                consumedLongBack = false
                return true
            }
            if (screenBackAction?.invoke(
                    event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled
                ) == true
            ) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        player = ExoPlayer.Builder(this).build()
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = playerColors) {
                PlayerHome(
                    player = player,
                    storage = storage,
                    registerLongBack = { longBackAction = it },
                    registerScreenBack = { screenBackAction = it },
                    onExit = ::finish,
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

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
    MOVIES(R.string.destination_movies),
    SERIES(R.string.destination_series),
    SETTINGS(R.string.destination_settings),
    SEARCH(R.string.search),
    CATEGORIES(R.string.category_settings),
    SOURCES(R.string.destination_sources)
}

private enum class VodLiveView(val label: Int) {
    BACKGROUND(R.string.vod_live_background),
    CORNER(R.string.vod_live_corner),
    HIDDEN(R.string.vod_live_hidden)
}

private val mainDestinations = Destination.entries.filter {
    it != Destination.SOURCES && it != Destination.CATEGORIES
}
private const val EXIT_CONFIRMATION_MS = 3_000L

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
) {
    val progressKey: String get() = "$section:$source:${seriesId.orEmpty()}:$id"
}

private data class PlaybackVariant(
    val label: String,
    val timing: EpisodeTiming = EpisodeTiming(),
    val resolve: suspend () -> String
)
private data class PreparedLiveVariant(val variant: PlaybackVariant, val url: String, val preparedAt: Long)
private data class PlaybackAttempt(
    val selection: PlaybackSelection,
    val channel: StalkerChannel,
    val variants: List<PlaybackVariant>,
    val index: Int,
    val positionMs: Long,
    val progressKey: String?,
    val mediaId: String = "",
    val openFullscreenOnStart: Boolean = true,
    val liveRestoreKey: String? = null
)

private enum class SeriesActivityView { HISTORY, WATCHLIST }
private enum class SettingsSection(val label: Int) {
    GENERAL(R.string.settings_general),
    SOURCES(R.string.settings_library),
    GUIDE(R.string.destination_guide),
    APPEARANCE(R.string.settings_appearance),
    PLAYBACK(R.string.settings_playback),
    CONTROLS(R.string.settings_controls),
    METADATA(R.string.settings_metadata),
    UPDATES(R.string.settings_updates)
}

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
private fun EpisodeTimingAdjustment(
    label: String, seconds: Int, minimum: Int, maximum: Int, onChange: (Int) -> Unit
) {
    Text(label)
    Row {
        MenuButton("−5 s", Modifier) { onChange((seconds - 5).coerceAtLeast(minimum)) }
        MenuButton("+5 s", Modifier) { onChange((seconds + 5).coerceAtMost(maximum)) }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerHome(
    player: ExoPlayer? = null,
    storage: PortalStorage? = null,
    registerLongBack: (((() -> Boolean)?) -> Unit) = {},
    registerScreenBack: ((((Boolean) -> Boolean)?) -> Unit) = {},
    onExit: () -> Unit = {},
    onFullscreen: (Boolean) -> Unit = {},
    canPlay: () -> Boolean = { false }
) {
    var selected by rememberSaveable { mutableStateOf<Destination?>(Destination.LIVE) }
    var pendingVodCategory by rememberSaveable { mutableStateOf<Destination?>(null) }
    var lastFocused by rememberSaveable { mutableStateOf(Destination.LIVE) }
    val initialFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val preview = LocalInspectionMode.current
    val preferences = remember(context, preview) {
        if (preview) null else context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    }
    var vodLiveView by remember(preferences) {
        val saved = preferences?.getString("vod_live_view", null)
        mutableStateOf(VodLiveView.entries.firstOrNull { it.name == saved } ?: VodLiveView.BACKGROUND)
    }
    val progressPreferences = remember(context, preview) {
        if (preview) null else context.getSharedPreferences("vod_progress", Context.MODE_PRIVATE)
    }
    var seriesActivity by remember(progressPreferences) {
        mutableStateOf(decodeSeriesActivity(progressPreferences?.getString("series_activity", null)))
    }
    var seriesActivityView by rememberSaveable { mutableStateOf<SeriesActivityView?>(null) }
    var movieHistoryOpen by rememberSaveable { mutableStateOf(false) }
    var movieWatchlistOpen by rememberSaveable { mutableStateOf(false) }
    var movieHistory by remember(progressPreferences) {
        mutableStateOf(decodeMovieHistory(progressPreferences?.getString("movie_history", null)))
    }
    var movieBookmarks by remember(progressPreferences) {
        mutableStateOf(decodeMovieHistory(progressPreferences?.getString("movie_bookmarks", null)))
    }
    var metadataClient by remember { mutableStateOf<TmdbClient?>(null) }
    var metadataSettingsOpen by remember { mutableStateOf(false) }
    var episodeSettingsOpen by remember { mutableStateOf(false) }
    var epgSettingsOpen by remember { mutableStateOf(false) }
    var settingsSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var guideAppearancePage by remember { mutableStateOf<GuideAppearancePage?>(null) }
    var guideAppearance by remember(preferences) { mutableStateOf(readGuideAppearance(preferences)) }
    var useXmlTv by remember(preferences) { mutableStateOf(preferences?.getBoolean("xmltv_enabled", false) ?: false) }
    var epgAtStartup by remember(preferences) { mutableStateOf(preferences?.getBoolean("epg_at_startup", true) ?: true) }
    var playLastChannelAtStartup by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("play_last_channel_at_startup", true) ?: true)
    }
    var epgRefreshHours by remember(preferences) { mutableStateOf(preferences?.getInt("epg_refresh_hours", 6) ?: 6) }
    var epgHorizonHours by remember(preferences) { mutableStateOf(preferences?.getInt("epg_horizon_hours", 24) ?: 24) }
    var xmlTvSources by remember { mutableStateOf(emptyList<String>()) }
    var xmlTvSourcesLoaded by remember { mutableStateOf(storage == null) }
    var epgRefreshRequest by remember { mutableIntStateOf(0) }
    var epgUpdating by remember { mutableStateOf(false) }
    var epgUpdateFailed by remember { mutableStateOf(false) }
    var epgSourceWriteFailed by remember { mutableStateOf(false) }
    var epgSourceSaving by remember { mutableStateOf(false) }
    var epgSourceDraft by remember { mutableStateOf("") }
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
    var manualIntro by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("manual_intro", false) ?: false)
    }
    var manualCredits by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("manual_credits", false) ?: false)
    }
    var introStartSeconds by remember(preferences) {
        mutableStateOf(preferences?.getInt("intro_start_seconds", 0) ?: 0)
    }
    var introEndSeconds by remember(preferences) {
        mutableStateOf(preferences?.getInt("intro_end_seconds", 60) ?: 60)
    }
    var creditsBeforeEndSeconds by remember(preferences) {
        mutableStateOf(preferences?.getInt("credits_before_end_seconds", 60) ?: 60)
    }
    val seriesOpenings = remember { mutableMapOf<Pair<String, String>, Pair<String, List<EpisodeFrame>>>() }
    val learnedIntros = remember { mutableMapOf<Pair<String, String>, Pair<String, LongRange>>() }
    var fullscreenVideoSurface by remember { mutableStateOf<SurfaceView?>(null) }
    var detectedTiming by remember { mutableStateOf<Pair<String, EpisodeTiming>?>(null) }
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
    var mergedIndividualPortals by remember(preferences) {
        mutableStateOf(preferences?.getStringSet("merged_individual_portals", emptySet()).orEmpty().toSet())
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
    val updateSettings = rememberAppUpdateSettings(selected == Destination.SETTINGS && !fullscreen)
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
    val vodPlaybackViews = remember(preferences) {
        mutableStateMapOf(*listOf(PortalSection.MOVIES, PortalSection.SERIES).map { section ->
            section to (VodLiveView.entries.firstOrNull {
                it.name == preferences?.getString("vod_view_${section.name}", null)
            } ?: VodLiveView.CORNER)
        }.toTypedArray())
    }
    val cataloguePlaybackView = vodPlaybackViews[vodSection] ?: vodLiveView
    val controlBindings = remember(preferences) {
        mutableStateMapOf(*GuideInput.entries.mapNotNull { input ->
            ControlAction.entries.firstOrNull {
                it.name == preferences?.getString("control_${input.name}", null)
            }?.let { input to it }
        }.toTypedArray())
    }
    var controlSettingsOpen by remember { mutableStateOf(false) }
    var openedSeries by remember { mutableStateOf<PortalTitle?>(null) }
    var seasons by remember { mutableStateOf<List<PortalSeason>?>(null) }
    var openedSeason by remember { mutableStateOf<PortalSeason?>(null) }
    val seriesDetailScroll = rememberLazyListState()
    var catalogueReturnTitle by remember { mutableStateOf<PortalTitle?>(null) }
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
    val mergePortalChannels = storedSource?.identity in mergedIndividualPortals
    var editing by remember { mutableStateOf(false) }
    var initialPortalPromptDismissed by rememberSaveable { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(storage != null) }
    var storageError by remember { mutableStateOf<Int?>(null) }
    val detailMetadata by produceState<TmdbTitle?>(null, metadataClient, details, mergeSources) {
        value = null
        if (!mergeSources) return@produceState
        val client = metadataClient ?: return@produceState
        val entry = details ?: return@produceState
        if (entry.first == PortalSection.SERIES && titleIdentity(entry.second) == null) return@produceState
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

    fun saveEpgSources(sources: List<String>) {
        if (epgSourceSaving || storage == null) return
        epgSourceSaving = true
        epgSourceWriteFailed = false
        scope.launch {
            try {
                storage.writeEpgSources(sources)
                xmlTvSources = sources
                epgSourceDraft = ""
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                epgSourceWriteFailed = true
            } finally {
                epgSourceSaving = false
            }
        }
    }

    LaunchedEffect(storage) {
        try {
            xmlTvSources = storage?.readEpgSources().orEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            epgSourceWriteFailed = true
        } finally {
            xmlTvSourcesLoaded = true
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
    val displayChannels = remember(combine, mergePortalChannels) { mutableMapOf<StalkerChannel, StalkerChannel>() }
    val liveGroups = remember(sourcesForBrowsing, combine, mergePortalChannels) {
        val groups = buildLiveChannelGroups(sourcesForBrowsing, combine, mergePortalChannels)
        val originals = groups.map { it.variants.first().item.value }.toSet()
        displayChannels.keys.retainAll(originals)
        groups.associateBy { group ->
            val original = group.variants.first().item.value
            if (!combine && !mergePortalChannels) original else displayChannels.getOrPut(original) {
                StalkerChannel(
                    group.description.name, original.command, original.genreId, original.genreName,
                    original.id, original.logo, original.direct, original.epgIdentity, original.number
                )
            }
        }
    }
    val liveVariants = remember(liveGroups) {
        liveGroups.mapValues { (_, group) -> group.variants.map { it.item } }
    }
    val allLiveCategories = remember(liveGroups, categoryNames, combine, uncategorized) {
        liveGroups.keys.groupBy { channel ->
            val group = liveGroups.getValue(channel)
            if (combine) categoryNames.getValue(group.description.category) else {
                val first = group.variants.first().item
                first.value.genreName?.takeIf(String::isNotBlank)
                    ?: first.source.genres[first.value.genreId] ?: uncategorized
            }
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
            resources.getString(R.string.saved_portal_label, index + 1, portal.sourceLabel)
    }

    fun programmeKey(channel: StalkerChannel): String? {
        val item = liveVariants[channel]?.firstOrNull { it.value.id != null } ?: return null
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
            else if (section == PortalSection.MOVIES) {
                val others = movieBookmarks.filterNot { it.progressKey == key }
                val ownerEntries = searchedVariants[title] ?: groupVariants[title] ?: listOfNotNull(
                    storedSource?.let { SourceItem(it, title) }
                )
                movieBookmarks = if (saved) {
                    listOf(WatchedMovie(
                        displayVodTitle(title), vodReleaseYear(title), title.imdbId, title.tmdbId,
                        ownerEntries.map { SeriesPortalRef(it.source.identity, it.value.id.orEmpty()) }.distinct(),
                        key, System.currentTimeMillis()
                    )) + others
                } else others
                progressPreferences?.edit()?.putString("movie_bookmarks", encodeMovieHistory(movieBookmarks))?.apply()
            }
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
                val variants = if (combine) {
                    mergeCatalogueTitles(entries.distinct(), requireVerifiedIdentity = section == PortalSection.SERIES)
                } else entries.map(::listOf)
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
    var epgCache by remember {
        mutableStateOf(emptyMap<String, List<PortalProgramme>>())
    }
    var epgFetchedAt by remember { mutableStateOf(emptyMap<String, Long>()) }
    val epgInFlight = remember { mutableSetOf<String>() }
    var guideCacheLoaded by remember(storage) { mutableStateOf(storage == null) }
    var externalGuides by remember { mutableStateOf(emptyList<XmlTvGuide>()) }
    LaunchedEffect(storage) {
        if (storage == null) return@LaunchedEffect
        val saved = try {
            storage.readGuideCache()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyMap()
        }
        val now = System.currentTimeMillis()
        val recent = saved.filterValues { now - it.fetchedAt in 0..(7 * 86_400_000L) }
        epgCache = recent.mapValues { it.value.programmes } + epgCache
        epgFetchedAt = recent.mapValues { it.value.fetchedAt } + epgFetchedAt
        guideCacheLoaded = true
    }
    LaunchedEffect(storage, guideCacheLoaded) {
        if (storage == null || !guideCacheLoaded) return@LaunchedEffect
        snapshotFlow { epgFetchedAt to epgCache }.collectLatest { (times, entries) ->
            if (times.isEmpty()) return@collectLatest
            delay(900)
            try {
                storage.writeGuideCache(times.mapNotNull { (key, time) ->
                    entries[key]?.let { key to CachedProgramme(time, it) }
                }.toMap())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A cache write is optional. The in-memory guide remains available.
            }
        }
    }

    fun effectiveProgrammes(channel: StalkerChannel): List<PortalProgramme>? =
        if (useXmlTv) {
            externalGuides.firstNotNullOfOrNull { guide ->
                liveVariants[channel].orEmpty().firstNotNullOfOrNull { item ->
                    guide.programmes(item.value, channel.name).takeIf { it.isNotEmpty() }
                }
            } ?: emptyList<PortalProgramme>().takeUnless { epgUpdating }
        } else {
            val key = programmeKey(channel)
            if (key == null) emptyList() else epgCache[key]
        }

    suspend fun loadProviderSchedule(channel: StalkerChannel, force: Boolean = false): Boolean {
        val key = programmeKey(channel) ?: return true
        val variants = liveVariants[channel].orEmpty().filter { it.value.id != null }.distinctBy { it.source.identity to it.value.id }
        if (variants.isEmpty()) return true
        val last = epgFetchedAt[key] ?: -1
        val maxAge = if (epgRefreshHours > 0) epgRefreshHours * 3_600_000L else Long.MAX_VALUE
        if (!guideCacheLoaded) return true
        if (!force && last >= 0 && System.currentTimeMillis() - last in 0 until maxAge) return true
        if (!epgInFlight.add(key)) return true
        return try {
            var entries = emptyList<PortalProgramme>()
            var answered = false
            for (item in variants) {
                val portal = sessionFor(item.source)
                try {
                    entries = portal.loadProgramme(item.value, portal.openSession())
                        .guideWindow(System.currentTimeMillis() / 1_000, epgHorizonHours)
                    answered = true
                    if (entries.isNotEmpty()) break
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    continue
                }
            }
            check(answered)
            epgCache = epgCache + (key to entries)
            epgFetchedAt = epgFetchedAt + (key to System.currentTimeMillis())
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (key !in epgCache) epgCache = epgCache + (key to emptyList())
            false
        } finally {
            epgInFlight.remove(key)
        }
    }

    suspend fun loadProviderSchedules(channels: List<StalkerChannel>, force: Boolean = false): Boolean {
        var success = true
        for (batch in channels.distinct().chunked(4)) {
            val results = coroutineScope { batch.map { async { loadProviderSchedule(it, force) } }.map { it.await() } }
            success = success && results.all { it }
        }
        return success
    }

    val pinnedProgramme = programmeChannel?.let(::programmeKey) in pinnedProgrammeKeys
    val programmes = programmeChannel?.let(::effectiveProgrammes)
    LaunchedEffect(programmeChannel, sourcesForBrowsing, useXmlTv, epgHorizonHours, epgRefreshRequest, guideCacheLoaded) {
        val channel = programmeChannel ?: return@LaunchedEffect
        if (useXmlTv) return@LaunchedEffect
        delay(250)
        loadProviderSchedule(channel)
    }
    val hasPortal = storedSource != null
    LaunchedEffect(epgRefreshHours, hasPortal) {
        if (epgRefreshHours <= 0 || !hasPortal) return@LaunchedEffect
        while (true) {
            delay(epgRefreshHours * 3_600_000L)
            if (canPlay()) epgRefreshRequest++
        }
    }
    LaunchedEffect(restoring, hasPortal, liveSession, useXmlTv, xmlTvSources, xmlTvSourcesLoaded, guideCacheLoaded,
        epgHorizonHours, epgRefreshRequest, epgRefreshHours) {
        if (restoring || !hasPortal || !xmlTvSourcesLoaded || !guideCacheLoaded) return@LaunchedEffect
        val refresh = epgAtStartup || epgRefreshRequest > 0
        epgUpdateFailed = false
        epgUpdating = true
        try {
            if (useXmlTv) {
                val sources = xmlTvSources.mapNotNull(::guideSourceUri)
                val maxAge = if (epgRefreshHours > 0) epgRefreshHours * 3_600_000L else Long.MAX_VALUE
                val cached = sources.map {
                    loadXmlTvGuide(context.cacheDir, it, epgHorizonHours, false, false, maxAge)
                }
                externalGuides = cached.mapNotNull { it.guide }
                val results = sources.zip(cached).map { (uri, entry) ->
                    if (entry.guide == null || refresh && (epgRefreshRequest > 0 || entry.needsRefresh)) {
                        loadXmlTvGuide(context.cacheDir, uri, epgHorizonHours, refresh = true)
                    } else entry
                }
                externalGuides = results.mapNotNull { it.guide }
                epgUpdateFailed = results.any { it.failed }
            } else if (refresh) {
                val available = liveCategories.entries
                    .filter { visibleCategory(PortalSection.LIVE, it.key, it.key) }.flatMap { it.value }
                val known = available.filter {
                    val key = programmeKey(it)
                    key in pinnedProgrammeKeys || key in epgCache
                }
                if (epgRefreshRequest == 0) delay(700)
                epgUpdateFailed = !loadProviderSchedules(available.take(12) + known, force = epgRefreshRequest > 0)
            }
        } finally {
            epgUpdating = false
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
        val snapshot = SavedPortal(
            source.address, source.mac, source.channels, source.genres, source.catalogs, ids, source.subscriptionExpiry
        )
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
            progressPreferences?.edit()?.putLong(key, position)?.apply {
                if (activePlayer.duration > 0) putLong("duration:$key", activePlayer.duration)
            }?.apply()
        }
    }

    fun resumeFor(entry: TrackedSeries): EpisodeResume? =
        recoverEpisodeResume(entry, progressPreferences?.all.orEmpty())

    var preparedLiveVariant by remember { mutableStateOf<PreparedLiveVariant?>(null) }

    fun stopPlayback() {
        preparedLiveVariant = null
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

    fun reopenPlayback(
        selection: PlaybackSelection, matchSeries: Boolean = false, openFullscreen: Boolean = true
    ): Boolean {
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
        if (openFullscreen) {
            lastBack = 0
            fullscreen = true
        }
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
                    val variant = attempt.variants[index]
                    val prepared = preparedLiveVariant?.takeIf {
                        it.variant === variant && SystemClock.elapsedRealtime() - it.preparedAt < 15_000
                    }
                    preparedLiveVariant = null
                    val stream = prepared?.url ?: variant.resolve()
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
        progressKey: String? = null,
        openFullscreenOnStart: Boolean = true,
        liveRestoreKey: String? = null
    ) {
        if (player == null || variants.isEmpty() || reopenPlayback(selection, openFullscreen = openFullscreenOnStart)) return
        val remainFullscreen = fullscreen && selection.section == PortalSection.LIVE
        stopPlayback()
        fullscreen = remainFullscreen
        playbackSelection = selection
        vodSection = selection.section.takeUnless { it == PortalSection.LIVE }
        val resume = progressKey?.let { progressPreferences?.getLong(it, 0L) } ?: 0L
        runPlayback(PlaybackAttempt(
            selection, channel, variants, 0, resume, progressKey,
            openFullscreenOnStart = openFullscreenOnStart, liveRestoreKey = liveRestoreKey
        ))
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

    fun playChannel(channel: StalkerChannel, openFullscreenOnStart: Boolean = true) {
        val group = liveGroups[channel] ?: return
        programmeChannel = channel
        val selection = liveSelection(channel, group, combine)
        if (!openFullscreenOnStart && playing != null && reopenPlayback(selection)) return
        startPlayback(
            channel,
            selection,
            group.variants.map { variant ->
                val item = variant.item
                val portal = sessionFor(item.source)
                val quality = variant.description.qualities.joinToString(" / ")
                val sourceLabel = playbackSourceLabel(item.source, portal)
                val label = if (quality.isBlank()) sourceLabel else "$quality · $sourceLabel"
                PlaybackVariant(label) { portal.resolve(item.value, portal.openSession()) }
            },
            openFullscreenOnStart = openFullscreenOnStart,
            liveRestoreKey = programmeKey(channel) ?: group.identity
        )
    }

    var restoredLastLiveChannel by remember { mutableStateOf(false) }
    LaunchedEffect(restoring, liveCategories) {
        if (restoring || restoredLastLiveChannel) return@LaunchedEffect
        restoredLastLiveChannel = true
        if (!playLastChannelAtStartup || selected != Destination.LIVE || playbackSelection != null) return@LaunchedEffect
        val savedKey = preferences?.getString("last_live_channel", null) ?: return@LaunchedEffect
        val visibleChannels = liveCategories.entries
            .filter { visibleCategory(PortalSection.LIVE, it.key, it.key) }
            .sortedBy { categoryNames.values.indexOf(it.key) }
            .flatMap { it.value }
        val index = visibleChannels.indexOfFirst { channel ->
            (programmeKey(channel) ?: liveGroups[channel]?.identity) == savedKey
        }
        if (index >= 0) {
            guideScroll.requestScrollToItem(index)
            playChannel(visibleChannels[index])
        }
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
                equivalentCatalogueTitle(
                    title, other.catalogs[section]?.titles.orEmpty(), requireVerifiedIdentity = section == PortalSection.SERIES
                )
                    ?.let { SourceItem(other, it) }
            }
    }

    suspend fun findEquivalent(
        source: SavedPortal, section: PortalSection, title: PortalTitle, sameSource: Boolean = false
    ): PortalTitle? {
        val portal = sessionFor(source)
        val search = displayVodTitle(title)
        if (search.isBlank()) return null
        val candidates = portal.loadCatalog(
            section, portal.openSession(), search = search
        ).titles
        if (sameSource && title.id != null) candidates.firstOrNull { it.id == title.id }?.let { return it }
        return equivalentCatalogueTitle(title, candidates, requireVerifiedIdentity = section == PortalSection.SERIES)
    }

    val historyArtworkRequests = remember { Semaphore(3) }
    @Composable
    fun HistoryArtwork(section: PortalSection, title: PortalTitle, refs: List<SeriesPortalRef>) {
        val artwork by produceState<PortalTitle?>(null, section, title, refs, savedSources) {
            value = null
            for (ref in refs) {
                val source = savedSources.firstOrNull { it.identity == ref.sourceId } ?: continue
                val cached = source.catalogs[section]?.titles?.firstOrNull {
                    ref.portalTitleId.isNotBlank() && it.id == ref.portalTitleId
                }
                val candidate = cached?.takeIf { it.poster != null } ?: try {
                    historyArtworkRequests.withPermit {
                        findEquivalent(source, section, cached ?: title.copy(id = ref.portalTitleId), sameSource = true)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                if (candidate?.poster != null) {
                    value = candidate
                    break
                }
            }
        }
        Poster(artwork ?: title, Modifier.width(52.dp).padding(end = 6.dp))
    }

    fun titleSelection(
        section: PortalSection, title: PortalTitle, season: PortalSeason? = openedSeason
    ): PlaybackSelection {
        val root = openedSeries.takeIf { section == PortalSection.SERIES && season != null }
        val canonical = titleIdentity(root ?: title)
        val source = titleEntries(section, root ?: title).firstOrNull()?.source?.identity.orEmpty()
        val seriesId = root?.let { titleIdentity(it) ?: it.id ?: it.name }
        val id = if (root != null) {
            val episode = title.episode?.toIntOrNull()?.toString() ?: title.episode ?: title.id ?: title.name
            "${season?.number ?: season?.title?.name}:$episode"
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
        if (section == PortalSection.SERIES) seriesDetailScroll.requestScrollToItem(0)
        details = section to title
        if (section == PortalSection.SERIES && isTitleSaved(section, title) && trackedSeries(title) == null) {
            updateTrackedSeries(title, saved = true)
        }
    }

    fun openTrackedSeries(entry: TrackedSeries, onOpen: ((PortalTitle) -> Unit)? = null) {
        val reference = PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
        val variants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.SERIES]?.titles?.firstOrNull { it.id == ref.portalTitleId }
                ?: reference.copy(id = ref.portalTitleId)
            SourceItem(source, title)
        }
        val primary = variants.firstOrNull {
            equivalentCatalogueTitle(reference, listOf(it.value), requireVerifiedIdentity = true) != null
        } ?: variants.firstOrNull() ?: return
        val title = primary.value
        searchedVariants = mapOf(title to (listOf(primary) + variants.filter {
            it != primary && equivalentCatalogueTitle(title, listOf(it.value), requireVerifiedIdentity = true) != null
        }))
        if (onOpen == null) showTitle(PortalSection.SERIES, title) else onOpen(title)
    }

    fun selectTitle(
        section: PortalSection,
        title: PortalTitle,
        resume: EpisodeResume? = null,
        progressOverride: String? = null,
        loadOnly: Boolean = false
    ) {
        if (storedSource == null) return
        val selection = titleSelection(section, title)
        if (!loadOnly && reopenPlayback(selection, matchSeries = section == PortalSection.SERIES && openedSeries == null)) {
            return
        }
        if (section == PortalSection.SERIES && openedSeries == title) {
            if (resume != null) {
                val item = resumeEpisode(seasons.orEmpty(), resume)
                if (item == null) resumeEpisodeMissing = true
                else {
                    openedSeason = item.first
                    selectTitle(section, item.second, progressOverride = resume.progressKey)
                }
            }
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
                val key = progressOverride ?: selection.progressKey
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
                                resources.getString(R.string.episode_number, it)
                            } ?: following.name
                            nextEpisodeAction = {
                                openedSeries = series
                                openedSeason = followingSeason
                                seasons = seasonList
                                episodeVariants = availableVariants
                                selectTitle(PortalSection.SERIES, following)
                                progressPreferences?.edit()?.remove(key)?.putBoolean("completed:$key", true)?.apply()
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

    LaunchedEffect(details) {
        val (section, title) = details ?: return@LaunchedEffect
        if (section == PortalSection.SERIES && openedSeries == null) selectTitle(section, title, loadOnly = true)
    }
    val portalDetail by produceState<PortalTitle?>(null, details) {
        value = null
        val (section, title) = details ?: return@produceState
        if (title.poster != null && germanDescription(title.description) != null) return@produceState
        for (item in titleEntries(section, title)) {
            val found = try {
                findEquivalent(item.source, section, item.value, sameSource = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            value = title.copy(
                poster = value?.poster ?: title.poster ?: item.value.poster ?: found?.poster,
                description = value?.description ?: germanDescription(title.description)
                    ?: germanDescription(item.value.description) ?: germanDescription(found?.description)
            )
            if (value?.poster != null && !value?.description.isNullOrBlank()) break
        }
    }
    val episodeMetadata by produceState(emptyMap<Pair<Int, Int>, TmdbEpisodeDetails>(), detailMetadata, seasons, metadataClient) {
        value = emptyMap()
        val client = metadataClient ?: return@produceState
        val series = detailMetadata?.takeIf { details?.first == PortalSection.SERIES } ?: return@produceState
        val seasonNumbers = seasons.orEmpty().filter { season ->
            season.episodes.any { it.poster == null || germanDescription(it.description) == null }
        }.mapNotNull { it.number }.distinct()
        for (batch in seasonNumbers.chunked(3)) {
            val loaded = coroutineScope {
                batch.map { number ->
                    async {
                        try {
                            client.episodeDetails(series.id, number).mapKeys { number to it.key }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            emptyMap()
                        }
                    }
                }.flatMap { it.await().entries }
            }
            value = value + loaded.map { it.key to it.value }
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
                savedSources.firstOrNull { it.identity == previous?.identity }?.epgChannelIds.orEmpty(),
                source.subscriptionExpiry
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
        val firstPortal = storedSource == null
        val enteredAddress = address.trim()
        val enteredMac = mac.trim()
        connecting = true
        batchSummary = null
        scope.launch {
            try {
                val imported = refreshPortal(enteredAddress, enteredMac, activate = true)
                if (automaticCountries && importStatuses.values.any {
                    it is ImportStatus.Count || it is ImportStatus.Loaded || it is ImportStatus.Partial
                }) askForCountries = true
                if (firstPortal && imported && selected == Destination.SOURCES) selected = Destination.LIVE
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
                        save(SavedPortal(
                            saved.address, saved.mac, channels, genres, catalogs, epgChannelIds, saved.subscriptionExpiry
                        ))
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
                            if (attempt.selection.section == PortalSection.LIVE) {
                                preferences?.edit()?.putString("last_live_channel", attempt.liveRestoreKey)?.apply()
                            }
                            lastBadgedAttempt = attempt.mediaId
                            playbackSourceBadge = attempt.mediaId to attempt.variants[attempt.index].label
                        }
                    }
                    val openFullscreen = when (vodSection) {
                        null -> selected == null || selected == Destination.LIVE
                        PortalSection.MOVIES -> selected == Destination.MOVIES
                        PortalSection.SERIES -> selected == Destination.SERIES
                        PortalSection.LIVE -> false
                    }
                    if (starting != null && openFullscreen && playbackAttempt?.openFullscreenOnStart == true) fullscreen = true
                    starting = null
                } else savePlaybackPosition(onFailure = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                val attempt = playbackAttempt ?: return
                if (playbackState == Player.STATE_ENDED && player?.currentMediaItem?.mediaId == attempt.mediaId) {
                    if (attempt.progressKey == null) {
                        scope.launch { failPlayback(attempt, R.string.playback_error) }
                    } else progressPreferences?.edit()?.remove(attempt.progressKey)
                        ?.putBoolean("completed:${attempt.progressKey}", true)?.apply()
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
            delay(if (attempt.selection.section == PortalSection.LIVE) 12_000 else 25_000)
            if (canPlay() && (starting != null || player?.playWhenReady == true)) {
                failPlayback(attempt, R.string.playback_timeout)
            }
        }
    }
    LaunchedEffect(playbackAttempt, starting) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        if (starting != null || attempt.selection.section != PortalSection.LIVE) return@LaunchedEffect
        val next = attempt.variants.getOrNull(attempt.index + 1) ?: return@LaunchedEffect
        delay(750)
        if (player?.isPlaying != true) return@LaunchedEffect
        try {
            val stream = next.resolve()
            if (playbackAttempt === attempt) {
                preparedLiveVariant = PreparedLiveVariant(next, stream, SystemClock.elapsedRealtime())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Resolve again on demand if the running stream fails.
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

    val inGuide = selected == Destination.LIVE
    var guidePanel by remember { mutableStateOf(GuidePanel.GRID) }
    var fullscreenGuidePanel by remember { mutableStateOf(GuidePanel.GRID) }
    var fullscreenOverlayContent by remember { mutableStateOf(GuidePanel.CHANNELS) }
    var overlayInteraction by remember { mutableIntStateOf(0) }
    var overlayScheduleFocused by remember { mutableStateOf(false) }
    var guideProgramme by remember { mutableStateOf<PortalProgramme?>(null) }
    val guidePanelFocus = remember { FocusRequester() }
    val guideChannelFocus = remember { FocusRequester() }
    fun openMainMenu() {
        if (selected == Destination.MOVIES || selected == Destination.SERIES) {
            catalogueReturnTitle = null
            closeGroup()
            closeSeries()
            details = null
            movieHistoryOpen = false
            movieWatchlistOpen = false
            seriesActivityView = null
        }
        pendingVodCategory = null
        lastBack = 0L
        guidePanel = GuidePanel.MENU
        selected = Destination.LIVE
    }
    fun navigateGuide(input: GuideInput) {
        guidePanel = guidePanel.afterProgrammeGuide(input)
    }
    fun showFullscreenPanel(panel: GuidePanel) {
        if (panel != fullscreenGuidePanel) overlayScheduleFocused = false
        if (panel != GuidePanel.GRID) fullscreenOverlayContent = panel
        fullscreenGuidePanel = panel
    }
    LaunchedEffect(fullscreen, fullscreenGuidePanel, guideAppearance.closeSeconds, overlayInteraction) {
        if (!fullscreen) showFullscreenPanel(GuidePanel.GRID)
        else if (fullscreenGuidePanel != GuidePanel.GRID && guideAppearance.closeSeconds > 0) {
            delay(guideAppearance.closeSeconds * 1_000L)
            showFullscreenPanel(GuidePanel.GRID)
        }
    }
    val mediaDestination = inGuide || selected == Destination.MOVIES || selected == Destination.SERIES
    fun isMainMenuOpen() = selected == Destination.LIVE && guidePanel == GuidePanel.MENU && !fullscreen
    val mainMenuOpen = isMainMenuOpen()
    LaunchedEffect(mainMenuOpen) { if (!mainMenuOpen) lastBack = 0L }
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
            delay(EXIT_CONFIRMATION_MS)
            lastBack = 0
        }
    }
    fun leaveFullscreen() {
        savePlaybackPosition()
        restoreLiveFocus = vodSection == null && (selected == null || selected == Destination.LIVE)
        if (restoreLiveFocus && inGuide) {
            if (category != null && playing !in liveCategories[category].orEmpty()) category = null
            programmeChannel = playing
        }
        if (vodSection != null && cataloguePlaybackView == VodLiveView.HIDDEN) player?.pause()
        fullscreen = false
        lastBack = 0L
    }
    fun openCurrentFullscreen(): Boolean {
        if (playing == null || playbackError != null) return false
        guidePanel = GuidePanel.GRID
        lastBack = 0L
        fullscreen = true
        return true
    }
    fun navigateBack() {
        if (fullscreen) {
            if (fullscreenGuidePanel != GuidePanel.GRID) {
                showFullscreenPanel(GuidePanel.GRID)
                return
            }
            leaveFullscreen()
        } else if (isMainMenuOpen()) {
            val now = SystemClock.elapsedRealtime()
            if (lastBack != 0L && now - lastBack <= EXIT_CONFIRMATION_MS) {
                savePlaybackPosition()
                onExit()
            } else {
                lastBack = now
            }
        } else if (inGuide) {
            if (guidePanel == GuidePanel.GRID) openMainMenu()
            else guidePanel = GuidePanel.GRID
        } else if (selected == null && category != null) returnToCategories()
        else if (selected == Destination.SERIES && openedSeason != null) openedSeason = null
        else if (selected == Destination.SERIES && openedSeries != null) {
            closeSeries()
            details = null
            seriesActivityView = null
        }
        else if (details != null && selected in listOf(Destination.SERIES, Destination.MOVIES)) {
            details = null
            movieHistoryOpen = false
            movieWatchlistOpen = false
            seriesActivityView = null
        }
        else if (selected == Destination.SERIES && seriesActivityView != null) seriesActivityView = null
        else if (selected == Destination.MOVIES && movieHistoryOpen) movieHistoryOpen = false
        else if (selected == Destination.MOVIES && movieWatchlistOpen) movieWatchlistOpen = false
        else if (selected == Destination.SOURCES || selected == Destination.CATEGORIES) selected = Destination.SETTINGS
        else if (selected == Destination.SETTINGS && settingsSection != null) settingsSection = null
        else openMainMenu()
    }
    var playerControlsRequest by remember { mutableIntStateOf(0) }
    fun runMappedControl(input: GuideInput): Boolean {
        val action = controlBindings[input] ?: ControlAction.DEFAULT
        val panel = when (action) {
            ControlAction.CHANNELS -> GuidePanel.CHANNELS
            ControlAction.CATEGORIES -> GuidePanel.CATEGORIES
            ControlAction.MENU -> GuidePanel.MENU
            ControlAction.DETAILS -> GuidePanel.DETAILS
            else -> null
        }
        if (panel != null) {
            if (fullscreen && vodSection == null) {
                if (panel == GuidePanel.DETAILS) {
                    programmeChannel = playing
                    guideProgramme = null
                }
                showFullscreenPanel(panel)
                return true
            }
            if (inGuide && !fullscreen && panel != GuidePanel.CHANNELS) {
                guidePanel = panel
                return true
            }
            return false
        }
        return when (action) {
            ControlAction.BACK -> { navigateBack(); true }
            ControlAction.FULLSCREEN -> if (fullscreen) { leaveFullscreen(); true } else openCurrentFullscreen()
            ControlAction.PLAY_PAUSE -> player?.takeIf { playing != null }?.let {
                if (it.playWhenReady) it.pause() else it.play()
                true
            } ?: false
            ControlAction.STOP -> if (playing != null || starting != null) { stopPlayback(); true } else false
            ControlAction.CONTROLS -> if (fullscreen) { playerControlsRequest++; true } else false
            else -> false
        }
    }
    val longBack by rememberUpdatedState<() -> Boolean>({
        !controlSettingsOpen && guideAppearancePage == null &&
            (runMappedControl(GuideInput.LONG_BACK) || (!fullscreen && openCurrentFullscreen()))
    })
    val screenBack by rememberUpdatedState<(Boolean) -> Boolean>({ released ->
        val handled = !fullscreen && (
            isMainMenuOpen() || selected == Destination.MOVIES || selected == Destination.SERIES
        )
        if (handled && released) navigateBack()
        handled
    })
    DisposableEffect(Unit) {
        registerLongBack { longBack() }
        registerScreenBack { released -> screenBack(released) }
        onDispose {
            registerLongBack(null)
            registerScreenBack(null)
        }
    }
    BackHandler(onBack = ::navigateBack)
    guideAppearancePage?.let { page ->
        GuideAppearanceDialog(page, guideAppearance, onChange = {
            guideAppearance = it
            it.save(preferences)
        }, onDismiss = { guideAppearancePage = null })
    }
    if (controlSettingsOpen) ControlMappingDialog(
        bindings = controlBindings,
        onChange = { input, action ->
            controlBindings[input] = action
            preferences?.edit()?.putString("control_${input.name}", action.name)?.apply()
        },
        onReset = {
            controlBindings.clear()
            preferences?.edit()?.also { editor ->
                GuideInput.entries.forEach { editor.remove("control_${it.name}") }
            }?.apply()
        },
        onDismiss = { controlSettingsOpen = false }
    )
    LaunchedEffect(playbackAttempt, manualIntro, manualCredits) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val active = player ?: return@LaunchedEffect
        val selection = attempt.selection
        if (selection.section != PortalSection.SERIES || (manualIntro && manualCredits)) return@LaunchedEffect
        while (active.currentMediaItem?.mediaId != attempt.mediaId || active.duration <= 0 ||
            active.playbackState != Player.STATE_READY) delay(250)
        val item = active.currentMediaItem ?: return@LaunchedEffect
        if (!active.isCurrentMediaItemSeekable) return@LaunchedEffect
        val duration = active.duration
        val portal = attempt.variants[attempt.index].timing
        val seriesKey = selection.seriesId?.let { selection.source to it }
        val scanOpening = !manualIntro && seriesKey != null && active.currentPosition < 180_000
        val scanCredits = !manualCredits && nextEpisodeAction != null && portal.nextAt(duration) == duration
        if (!scanOpening && !scanCredits) return@LaunchedEffect
        val uri = item.localConfiguration?.uri ?: return@LaunchedEffect
        val hls = item.localConfiguration?.mimeType == MimeTypes.APPLICATION_M3U8 ||
            uri.lastPathSegment.orEmpty().endsWith(".m3u8", ignoreCase = true)
        if (hls && !scanOpening) return@LaunchedEffect
        val previous = seriesOpenings[seriesKey]
        val learned = learnedIntros[seriesKey]?.takeIf { it.first == previous?.first }?.second
        var timing = EpisodeTiming()
        val updates = if (hls) flow {
            val opening = mutableListOf<EpisodeFrame>()
            while (active.currentMediaItem == item && active.currentPosition < 180_000) {
                val position = active.currentPosition
                val bucket = position / HLS_INTRO_FRAME_INTERVAL_MS * HLS_INTRO_FRAME_INTERVAL_MS
                val surface = fullscreenVideoSurface
                if (surface != null && active.isPlaying && position - bucket < 500 &&
                    (opening.lastOrNull()?.timeMs ?: -1) < bucket
                ) {
                    sampleEpisodeSurface(surface, bucket)?.let {
                        opening += it
                        emit(EpisodeVideoAnalysis(opening.toList()))
                    }
                }
                delay(150)
            }
        } else analyzeEpisodeVideo(uri, duration, scanOpening, scanCredits)
        updates.collect { analysis ->
            if (active.currentMediaItem != item) return@collect
            if (seriesKey != null && analysis.opening.isNotEmpty()) {
                if (previous != null && previous.first != selection.id && timing.introEndMs == null) {
                    val intro = withContext(Dispatchers.Default) {
                        if (hls) {
                            learned?.let { confirmKnownOpening(analysis.opening, previous.second, it) }
                                ?: findRepeatedOpening(analysis.opening, previous.second, HLS_INTRO_FRAME_INTERVAL_MS)
                        } else findRepeatedOpening(analysis.opening, previous.second)
                    }
                    if (active.currentMediaItem != item) return@collect
                    intro?.let {
                        timing = timing.copy(introStartMs = intro.first, introEndMs = intro.last + 1)
                        if (hls) learnedIntros[seriesKey] = selection.id to intro
                    }
                }
                if (seriesOpenings.size >= 16 && seriesKey !in seriesOpenings) {
                    val oldest = seriesOpenings.keys.first()
                    seriesOpenings.remove(oldest)
                    learnedIntros.remove(oldest)
                }
                if (previous?.first != selection.id || analysis.opening.size > previous.second.size) {
                    seriesOpenings[seriesKey] = selection.id to analysis.opening
                }
            }
            analysis.creditsStartMs?.takeIf { it > active.currentPosition }?.let {
                timing = timing.copy(creditsStartMs = it)
            }
            detectedTiming = attempt.mediaId to timing
        }
    }
    LaunchedEffect(
        playbackAttempt, autoNextEpisode, autoSkipIntro, episodeNoticeSeconds,
        manualIntro, manualCredits, introStartSeconds, introEndSeconds, creditsBeforeEndSeconds
    ) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val active = player ?: return@LaunchedEffect
        if (attempt.selection.section != PortalSection.SERIES) return@LaunchedEffect
        var introSkipped = false
        while (playbackAttempt === attempt) {
            val duration = active.duration
            if (active.currentMediaItem?.mediaId == attempt.mediaId && duration > 0) {
                val portal = attempt.variants[attempt.index].timing
                val detected = detectedTiming?.takeIf { it.first == attempt.mediaId }?.second
                val position = if (active.playbackState == Player.STATE_ENDED) duration else active.currentPosition
                val intro = if (manualIntro) {
                    EpisodeTiming(introStartSeconds * 1_000L, introEndSeconds * 1_000L).intro(duration)
                } else portal.intro(duration) ?: detected?.intro(duration)
                skipIntroTo = if (!introSkipped && intro != null && position in intro &&
                    active.isCurrentMediaItemSeekable) intro.last + 1 else null
                if (autoSkipIntro && active.isPlaying) skipIntroTo?.let {
                    active.seekTo(it)
                    skipIntroTo = null
                    introSkipped = true
                }
                val nextAt = if (manualCredits) {
                    (duration - creditsBeforeEndSeconds * 1_000L).takeIf { it > 0 } ?: duration
                } else minOf(portal.nextAt(duration), detected?.nextAt(duration) ?: duration)
                val remaining = (nextAt - position).coerceAtLeast(0)
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
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val scheduleBesideList = guideAppearance.showSchedule && screenWidth >= 640.dp
        fun navigateFromLiveFullscreen(input: GuideInput) {
            if (runMappedControl(input)) return
            if (fullscreenGuidePanel == GuidePanel.GRID && (input == GuideInput.SWIPE_RIGHT || input == GuideInput.REMOTE_RIGHT)) {
                programmeChannel = playing
                guideProgramme = null
            }
            showFullscreenPanel(fullscreenGuidePanel.after(input))
        }
        val live = vodSection == null
        val overlayFocus = remember { FocusRequester() }
        val inputModifier = if (live) Modifier.guideNavigationInput(interceptRemote = { input ->
            val mapped = controlBindings[input]
            val custom = mapped != null && mapped != ControlAction.DEFAULT
            custom || fullscreenGuidePanel != GuidePanel.CHANNELS || !scheduleBesideList ||
                (input == GuideInput.REMOTE_LEFT && !overlayScheduleFocused)
        }, onInput = ::navigateFromLiveFullscreen) else Modifier
        Box(Modifier.fillMaxSize().background(Color.Black).then(inputModifier)) {
            PlayerFrame(
                player, active = true, fullscreen = true, onStop = ::stopPlayback,
                onSurface = { fullscreenVideoSurface = it },
                onGuideInput = if (live) ::navigateFromLiveFullscreen else null,
                onTouchAction = { runMappedControl(GuideInput.TAP) },
                onMediaKey = { runMappedControl(GuideInput.PLAY_PAUSE) },
                controlsRequest = playerControlsRequest
            )
            if (controlBindings[GuideInput.TAP] == ControlAction.PLAY_PAUSE &&
                fullscreenGuidePanel == GuidePanel.GRID
            ) {
                MenuButton(
                    stringResource(R.string.control_show_controls),
                    Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) { playerControlsRequest++ }
            }
            if (live && fullscreenGuidePanel != GuidePanel.GRID) {
                Box(Modifier.matchParentSize().clickable { showFullscreenPanel(GuidePanel.GRID) })
            }
            if (live) {
                AnimatedVisibility(
                    visible = fullscreenGuidePanel != GuidePanel.GRID,
                    modifier = Modifier.align(
                        if (fullscreenOverlayContent == GuidePanel.DETAILS) Alignment.TopEnd else Alignment.CenterStart
                    ).padding(12.dp),
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    val panel = fullscreenOverlayContent
                    val groups = liveCategories.keys.filter { visibleCategory(PortalSection.LIVE, it, it) }
                        .sortedBy { categoryNames.values.indexOf(it) }
                    val channelCategory = guideChannelCategory(category, groups, liveCategories, playing)
                    val panelWidth = when (panel) {
                        GuidePanel.CHANNELS -> {
                            val fraction = if (scheduleBesideList) 0.62f else 0.38f
                            (screenWidth * fraction * guideAppearance.listWidth.factor)
                                .coerceIn(minOf(205.dp, screenWidth * 0.8f), screenWidth * 0.88f)
                        }
                        GuidePanel.DETAILS -> minOf(300.dp, screenWidth * 0.85f)
                        else -> minOf(235.dp, screenWidth * 0.85f)
                    }
                    Surface(
                        modifier = Modifier.width(panelWidth)
                            .then(if (panel == GuidePanel.DETAILS) Modifier.heightIn(max = 230.dp) else Modifier.fillMaxHeight()),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = guideAppearance.opacity / 100f)
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            MenuButton(
                                if (panel == GuidePanel.CHANNELS) channelCategory ?: stringResource(R.string.channels)
                                else stringResource(R.string.navigation_back),
                                Modifier.focusRequester(overlayFocus)
                            ) {
                                showFullscreenPanel(if (panel == GuidePanel.CHANNELS) GuidePanel.CATEGORIES else panel.after(GuideInput.BACK))
                            }
                            LaunchedEffect(panel) { if (panel != GuidePanel.CHANNELS) overlayFocus.requestFocus() }
                            when (panel) {
                                GuidePanel.CHANNELS -> {
                                    val channelsInPanel = liveCategories[channelCategory].orEmpty()
                                    if (channelsInPanel.isEmpty()) LaunchedEffect(Unit) { overlayFocus.requestFocus() }
                                    FullscreenChannelList(
                                        Modifier.weight(1f), channelsInPanel, playing, guideAppearance, scheduleBesideList,
                                        refreshKey = epgRefreshRequest + if (guideCacheLoaded) 1 else 0,
                                        programmes = ::effectiveProgrammes,
                                        onVisible = { visible -> if (!useXmlTv) loadProviderSchedules(visible) },
                                        onInteraction = { overlayInteraction++ },
                                        onScheduleFocused = { overlayScheduleFocused = it },
                                        onSelect = { channel ->
                                            overlayInteraction++
                                            if (!guideAppearance.keepChannelList) showFullscreenPanel(GuidePanel.GRID)
                                            selected = Destination.LIVE
                                            playChannel(channel)
                                        }
                                    )
                                }
                                GuidePanel.CATEGORIES -> LazyColumn(Modifier.weight(1f)) {
                                    items(groups) { group ->
                                        MenuButton(group, Modifier.fillMaxWidth(), selected = channelCategory == group) {
                                            category = group
                                            showFullscreenPanel(GuidePanel.CHANNELS)
                                        }
                                    }
                                }
                                GuidePanel.MENU -> LazyColumn(Modifier.weight(1f)) {
                                    items(mainDestinations) { destination ->
                                        MenuButton(stringResource(destination.label), Modifier.fillMaxWidth()) {
                                            showFullscreenPanel(GuidePanel.GRID)
                                            guidePanel = GuidePanel.GRID
                                            lastFocused = destination
                                            pendingVodCategory = destination.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
                                            selected = destination
                                            fullscreen = false
                                        }
                                    }
                                }
                                GuidePanel.DETAILS -> {
                                    val now = System.currentTimeMillis() / 1_000
                                    val schedule = playing?.let(::effectiveProgrammes).orEmpty()
                                    val programme = schedule.firstOrNull {
                                        val from = it.startSeconds
                                        val to = it.endSeconds
                                        from != null && to != null && now in from until to
                                    } ?: schedule.firstOrNull { it.startSeconds == null }
                                    playing?.let { Text(it.name, style = MaterialTheme.typography.labelMedium) }
                                    Text(programme?.title ?: stringResource(R.string.no_guide), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        programme?.description ?: stringResource(R.string.no_title_description),
                                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                GuidePanel.GRID -> Unit
                            }
                        }
                    }
                }
            }
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
            playbackSourceBadge?.takeUnless { fullscreenGuidePanel == GuidePanel.DETAILS }?.let { (_, label) ->
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
    if (storage != null && !restoring && storageError == null && savedSources.isEmpty() &&
        !hasPortal && !initialPortalPromptDismissed && !editing && !connecting
    ) AlertDialog(
        onDismissRequest = { initialPortalPromptDismissed = true },
        title = { Text(stringResource(R.string.first_portal_title)) },
        text = { Text(stringResource(R.string.first_portal_message)) },
        confirmButton = {
            TextButton(onClick = {
                initialPortalPromptDismissed = true
                selected = Destination.SOURCES
                address = ""
                mac = ""
                editing = true
            }) { Text(stringResource(R.string.add_portal)) }
        },
        dismissButton = {
            TextButton(onClick = { initialPortalPromptDismissed = true }) { Text(stringResource(R.string.later)) }
        }
    )
    if (epgSettingsOpen) AlertDialog(
        onDismissRequest = { epgSettingsOpen = false },
        title = { Text(stringResource(R.string.epg_settings)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MenuButton(stringResource(R.string.epg_provider), Modifier, selected = !useXmlTv) {
                    useXmlTv = false
                    preferences?.edit()?.putBoolean("xmltv_enabled", false)?.apply()
                }
                MenuButton(stringResource(R.string.epg_custom), Modifier, selected = useXmlTv) {
                    useXmlTv = true
                    preferences?.edit()?.putBoolean("xmltv_enabled", true)?.apply()
                }
                if (useXmlTv) {
                    xmlTvSources.forEachIndexed { index, url ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${guideSourceUri(url)?.host.orEmpty()}", Modifier.weight(1f))
                            TextButton(
                                enabled = !epgSourceSaving,
                                onClick = { saveEpgSources(xmlTvSources.filterNot { it == url }) }
                            ) { Text(stringResource(R.string.epg_remove_source)) }
                        }
                    }
                    OutlinedTextField(
                        value = epgSourceDraft, onValueChange = { if (it.length <= 2_048) epgSourceDraft = it },
                        label = { Text(stringResource(R.string.epg_source_url)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    val source = guideSourceUri(epgSourceDraft)?.toASCIIString()
                    TextButton(
                        enabled = source != null && source !in xmlTvSources && !epgSourceSaving && xmlTvSourcesLoaded,
                        onClick = { source?.let { saveEpgSources(xmlTvSources + it) } }
                    ) { Text(stringResource(R.string.epg_add_source)) }
                }
                MenuButton(stringResource(R.string.epg_refresh_at_start), Modifier, selected = epgAtStartup) {
                    epgAtStartup = !epgAtStartup
                    preferences?.edit()?.putBoolean("epg_at_startup", epgAtStartup)?.apply()
                }
                val refreshLabel = if (epgRefreshHours == 0) stringResource(R.string.epg_refresh_off)
                    else stringResource(R.string.epg_refresh_hours, epgRefreshHours)
                MenuButton(refreshLabel, Modifier) {
                    val intervals = listOf(0, 1, 3, 6, 12, 24)
                    epgRefreshHours = intervals[(intervals.indexOf(epgRefreshHours) + 1) % intervals.size]
                    preferences?.edit()?.putInt("epg_refresh_hours", epgRefreshHours)?.apply()
                }
                val horizon = if (epgHorizonHours < 24) stringResource(R.string.epg_schedule_hours, epgHorizonHours)
                    else if (epgHorizonHours == 24) stringResource(R.string.epg_schedule_day)
                    else stringResource(R.string.epg_schedule_days, epgHorizonHours / 24)
                MenuButton(horizon, Modifier) {
                    val windows = listOf(6, 12, 24, 48, 72, 168)
                    epgHorizonHours = windows[(windows.indexOf(epgHorizonHours) + 1) % windows.size]
                    preferences?.edit()?.putInt("epg_horizon_hours", epgHorizonHours)?.apply()
                    epgRefreshRequest++
                }
                TextButton(
                    enabled = !epgUpdating && (!useXmlTv || xmlTvSources.isNotEmpty()),
                    onClick = { epgRefreshRequest++ }
                ) { Text(stringResource(R.string.epg_refresh_now)) }
                if (epgUpdating) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (epgUpdateFailed || epgSourceWriteFailed) {
                    Text(stringResource(R.string.epg_refresh_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { epgSettingsOpen = false }) { Text(stringResource(android.R.string.ok)) }
        }
    )
    if (episodeSettingsOpen) AlertDialog(
        onDismissRequest = { episodeSettingsOpen = false },
        title = { Text(stringResource(R.string.episode_settings)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MenuButton(stringResource(R.string.auto_next_episode), Modifier, selected = autoNextEpisode) {
                    autoNextEpisode = !autoNextEpisode
                    preferences?.edit()?.putBoolean("auto_next_episode", autoNextEpisode)?.apply()
                }
                MenuButton(stringResource(R.string.auto_skip_intro), Modifier, selected = autoSkipIntro) {
                    autoSkipIntro = !autoSkipIntro
                    preferences?.edit()?.putBoolean("auto_skip_intro", autoSkipIntro)?.apply()
                }
                MenuButton(
                    stringResource(if (manualIntro) R.string.intro_manual else R.string.intro_automatic),
                    Modifier
                ) {
                    manualIntro = !manualIntro
                    preferences?.edit()?.putBoolean("manual_intro", manualIntro)?.apply()
                }
                if (manualIntro) {
                    EpisodeTimingAdjustment(
                        stringResource(R.string.intro_start_time, playbackTime(introStartSeconds * 1_000L)),
                        introStartSeconds, 0, introEndSeconds - 5
                    ) {
                        introStartSeconds = it
                        preferences?.edit()?.putInt("intro_start_seconds", it)?.apply()
                    }
                    EpisodeTimingAdjustment(
                        stringResource(R.string.intro_end_time, playbackTime(introEndSeconds * 1_000L)),
                        introEndSeconds, introStartSeconds + 5, 600
                    ) {
                        introEndSeconds = it
                        preferences?.edit()?.putInt("intro_end_seconds", it)?.apply()
                    }
                }
                MenuButton(
                    stringResource(if (manualCredits) R.string.credits_manual else R.string.credits_automatic),
                    Modifier
                ) {
                    manualCredits = !manualCredits
                    preferences?.edit()?.putBoolean("manual_credits", manualCredits)?.apply()
                }
                if (manualCredits) {
                    EpisodeTimingAdjustment(
                        stringResource(R.string.credits_before_end, playbackTime(creditsBeforeEndSeconds * 1_000L)),
                        creditsBeforeEndSeconds, 0, 600
                    ) {
                        creditsBeforeEndSeconds = it
                        preferences?.edit()?.putInt("credits_before_end_seconds", it)?.apply()
                    }
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val vodSidebarWidth = (maxWidth * 0.23f).coerceIn(118.dp, 230.dp)
        val vodSectionForScreen = when (selected) {
            Destination.MOVIES -> PortalSection.MOVIES
            Destination.SERIES -> PortalSection.SERIES
            else -> null
        }
        if (vodSectionForScreen != null && cataloguePlaybackView == VodLiveView.BACKGROUND &&
            playing != null && player != null
        ) {
            Box(Modifier.fillMaxSize().padding(start = vodSidebarWidth + 36.dp)) {
                AndroidView(
                    factory = { viewContext ->
                        (android.view.LayoutInflater.from(viewContext)
                            .inflate(R.layout.vod_live_backdrop, null) as PlayerView).apply { this.player = player }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onReset = null,
                    onRelease = { it.player = null },
                    update = {}
                )
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)))
            }
        }
        val compactGuideWindow = inGuide && LocalConfiguration.current.screenHeightDp < 440
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = if (compactGuideWindow) 10.dp else 24.dp,
                    vertical = if (compactGuideWindow) 4.dp else 16.dp
                )
        ) {
            val destination = selected
            val compactMedia = maxHeight < 400.dp
            val cornerPreviewWidth = (maxWidth * 0.18f).coerceIn(90.dp, 164.dp)
            val visibleVodGroups = vodSectionForScreen?.let { section ->
                browsingGroups(section)
                    .filter { visibleCategory(section, it.id, it.name, it.country) }
                    .sortedWith(compareBy(
                        { if (combine) vodCategoryNames.keys.indexOf(it.id) else countryPriority(it.name, it.country) },
                        { it.name }
                    ))
            }.orEmpty()
            val recentCategoryKey = "last_vod_category_${vodSectionForScreen?.name}"
            fun openVodCategory(group: PortalGroup) {
                val section = vodSectionForScreen ?: return
                catalogueReturnTitle = null
                preferences?.edit()?.putString(recentCategoryKey, group.id)?.apply()
                closeSeries()
                details = null
                movieHistoryOpen = false
                movieWatchlistOpen = false
                seriesActivityView = null
                if (!combine && group.id == ALL_CATALOG_ITEMS) {
                    closeGroup()
                    selectedGroup = section to group
                } else loadGroup(section, group)
            }
            val compactGuide = inGuide && (maxHeight < 400.dp || maxWidth < 680.dp)
            val basePreviewWidth = if (compactGuide) {
                minOf(maxWidth * 0.24f, maxHeight * 0.43f).coerceAtLeast(76.dp)
            } else (maxWidth * 0.29f).coerceIn(128.dp, 290.dp)
            val guidePreviewWidth = (basePreviewWidth * guideAppearance.previewSize.factor)
                .coerceAtMost(minOf(maxWidth * 0.45f, maxHeight * 0.7f))
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
                            item == lastFocused && !inGuide && destination != Destination.SEARCH &&
                            destination != Destination.SETTINGS &&
                            destination != Destination.SOURCES && destination != Destination.CATEGORIES
                        ) {
                            Modifier.fillMaxWidth().focusRequester(initialFocus)
                        } else if (inGuide && item == Destination.LIVE) {
                            Modifier.fillMaxWidth().focusRequester(guidePanelFocus)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        selected = item == (destination ?: Destination.LIVE) ||
                            (item == Destination.SETTINGS &&
                                destination in listOf(Destination.SOURCES, Destination.CATEGORIES)),
                        onClick = {
                            lastFocused = item
                            guidePanel = GuidePanel.GRID
                            pendingVodCategory = item.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
                            selected = item
                        }
                    )
                }
                if (mainMenuOpen && lastBack != 0L) {
                    Text(
                        stringResource(R.string.press_back_again_to_exit),
                        Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            val header: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (vodSectionForScreen != null) Box(Modifier.weight(1f))
                    else if (destination != Destination.SEARCH) Text(
                        text = stringResource(if (inGuide) R.string.destination_guide else destination?.label ?: R.string.destination_live),
                        style = if (compactGuide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall
                    )
                    if (inGuide) {
                        MenuButton(category ?: stringResource(R.string.guide_all_channels),
                            Modifier.focusRequester(initialFocus).focusProperties {
                                canFocus = guidePanel == GuidePanel.GRID
                                down = guideChannelFocus
                            }
                        ) {
                            guidePanel = GuidePanel.CATEGORIES
                        }
                    }
                    if (vodSectionForScreen != null && cataloguePlaybackView == VodLiveView.CORNER) {
                        playing?.let { channel ->
                            Box(Modifier.width(cornerPreviewWidth)) {
                                PlayerFrame(
                                    player, active = true, onStop = ::stopPlayback,
                                    onOpenFullscreen = { if (vodSection == null) playChannel(channel) else fullscreen = true }
                                )
                            }
                        }
                    }
                }
                if (restoring || connecting) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (restoring) Text(stringResource(R.string.startup_loading))
                }
            }
            val content: @Composable ColumnScope.() -> Unit = {
                when (destination) {
                    null -> LiveOverview(
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
                        val section = settingsSection
                        if (section == null) {
                            SettingsSection.entries.forEachIndexed { index, item ->
                                MenuButton(
                                    stringResource(item.label),
                                    if (index == 0) Modifier.focusRequester(initialFocus) else Modifier
                                ) { settingsSection = item }
                            }
                        } else {
                            MenuButton(stringResource(R.string.navigation_back), Modifier.focusRequester(initialFocus)) {
                                settingsSection = null
                            }
                            SettingsHeading(section.label)
                            when (section) {
                                SettingsSection.GENERAL -> {
                                    MenuButton(
                                        stringResource(R.string.start_last_channel),
                                        Modifier,
                                        selected = playLastChannelAtStartup
                                    ) {
                                        playLastChannelAtStartup = !playLastChannelAtStartup
                                        preferences?.edit()?.putBoolean("play_last_channel_at_startup", playLastChannelAtStartup)?.apply()
                                    }
                                }
                                SettingsSection.SOURCES -> {
                                    MenuButton(
                                        label = stringResource(R.string.destination_sources),
                                        modifier = Modifier,
                                        onClick = { selected = Destination.SOURCES }
                                    )
                                    MenuButton(stringResource(R.string.category_settings), Modifier) {
                                        selected = Destination.CATEGORIES
                                    }
                                }
                                SettingsSection.GUIDE ->
                                    MenuButton(stringResource(R.string.epg_settings), Modifier) { epgSettingsOpen = true }
                                SettingsSection.APPEARANCE -> {
                                    MenuButton(stringResource(R.string.guide_grid_appearance), Modifier) {
                                        guideAppearancePage = GuideAppearancePage.GRID
                                    }
                                    MenuButton(stringResource(R.string.guide_channel_list_appearance), Modifier) {
                                        guideAppearancePage = GuideAppearancePage.CHANNELS
                                    }
                                }
                                SettingsSection.PLAYBACK -> {
                                    MenuButton(stringResource(R.string.episode_settings), Modifier) { episodeSettingsOpen = true }
                                    MenuButton(stringResource(R.string.live_preview_in_vod, stringResource(vodLiveView.label)), Modifier) {
                                        vodLiveView = VodLiveView.entries[(vodLiveView.ordinal + 1) % VodLiveView.entries.size]
                                        preferences?.edit()?.putString("vod_live_view", vodLiveView.name)?.apply()
                                    }
                                    for ((section, label) in listOf(
                                        PortalSection.MOVIES to R.string.vod_movies_playback,
                                        PortalSection.SERIES to R.string.vod_series_playback
                                    )) {
                                        val view = vodPlaybackViews.getValue(section)
                                        MenuButton(stringResource(label, stringResource(
                                            if (view == VodLiveView.HIDDEN) R.string.vod_playback_off else view.label
                                        )), Modifier) {
                                            val next = VodLiveView.entries[(view.ordinal + 1) % VodLiveView.entries.size]
                                            vodPlaybackViews[section] = next
                                            preferences?.edit()?.putString("vod_view_${section.name}", next.name)?.apply()
                                            if (next == VodLiveView.HIDDEN && vodSection == section) player?.pause()
                                        }
                                    }
                                }
                                SettingsSection.CONTROLS ->
                                    MenuButton(stringResource(R.string.control_settings), Modifier) { controlSettingsOpen = true }
                                SettingsSection.UPDATES -> updateSettings()
                                SettingsSection.METADATA -> {
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
                                }
                            }
                        }
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
                        val activePortalId = storedSource?.identity
                        if (!mergeSources && activePortalId != null) {
                            MenuButton(stringResource(R.string.merge_portal_channels), Modifier, selected = mergePortalChannels) {
                                mergedIndividualPortals = if (mergePortalChannels) mergedIndividualPortals - activePortalId
                                    else mergedIndividualPortals + activePortalId
                                preferences?.edit()?.putStringSet("merged_individual_portals", mergedIndividualPortals)?.apply()
                                category = null
                            }
                        }
                        for ((index, source) in savedSources.withIndex()) {
                            val host = remember(source.address) {
                                runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                            }
                            val isActive = source.identity == storedSource?.identity
                            MenuButton(
                                listOf(
                                    stringResource(
                                        if (!mergeSources && isActive) R.string.active_portal_label else R.string.saved_portal_label,
                                        index + 1, host
                                    ),
                                    source.subscriptionExpiry?.let { date ->
                                        stringResource(R.string.portal_expiry, date.format(
                                            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                                        ))
                                    } ?: stringResource(R.string.portal_expiry_unknown)
                                ).joinToString(" · "),
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
                    Destination.LIVE -> {
                        val groups = liveCategories.keys
                            .filter { visibleCategory(PortalSection.LIVE, it, it) }
                            .sortedBy { categoryNames.values.indexOf(it) }
                        val guideChannels = remember(liveCategories, groups, category) {
                            if (category in groups) liveCategories[category].orEmpty()
                            else groups.flatMap { liveCategories[it].orEmpty() }
                        }
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(if (compactGuide) 3.dp else 6.dp)
                            ) {
                                val livePreview = playing.takeIf { vodSection == null }
                                val infoChannel = programmeChannel ?: livePreview
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                                        .padding(if (compactGuide) 4.dp else 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(if (compactGuide) 8.dp else 16.dp)
                                ) {
                                    if (guideAppearance.showPreview) Box(Modifier.width(guidePreviewWidth)) {
                                        PlayerFrame(
                                            player, active = livePreview != null, onStop = ::stopPlayback,
                                            onOpenFullscreen = (livePreview ?: infoChannel)?.let { channel ->
                                                { playChannel(channel) }
                                            }
                                        )
                                    }
                                    Column(
                                        Modifier.weight(1f).heightIn(max = guidePreviewWidth * (9f / 16f)),
                                        verticalArrangement = Arrangement.spacedBy(if (compactGuide) 1.dp else 5.dp)
                                    ) {
                                        infoChannel?.let { channel ->
                                            val now = System.currentTimeMillis() / 1000
                                            val schedule = effectiveProgrammes(channel).orEmpty()
                                            val current = guideProgramme ?: schedule.firstOrNull { entry ->
                                                entry.startSeconds?.let { now >= it } == true &&
                                                    entry.endSeconds?.let { now < it } == true
                                            } ?: schedule.firstOrNull { it.startSeconds == null }
                                            val time = listOfNotNull(current?.startSeconds, current?.endSeconds).joinToString(" – ") {
                                                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                                                    .format(Instant.ofEpochSecond(it))
                                            }
                                            Text(
                                                if (time.isEmpty()) channel.name else "${channel.name} · $time",
                                                style = if (compactGuide) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                current?.title ?: stringResource(R.string.no_guide),
                                                style = if (compactGuide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            current?.description?.let {
                                                Text(
                                                    it, style = MaterialTheme.typography.bodySmall,
                                                    maxLines = if (compactGuide) 1 else 3, overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            guideState.SaveableStateProvider("programme-guide") {
                                ProgrammeTimeline(
                                channels = guideChannels,
                                pinnedIds = pinnedProgrammeKeys,
                                highlighted = programmeChannel,
                                playing = playing.takeIf { vodSection == null },
                                programmes = ::effectiveProgrammes,
                                programmeKey = ::programmeKey,
                                scroll = guideScroll,
                                compact = compactGuide,
                                appearance = guideAppearance,
                                refreshKey = epgRefreshRequest + if (guideCacheLoaded) 1 else 0,
                                horizonHours = epgHorizonHours,
                                active = guidePanel == GuidePanel.GRID,
                                channelColumnFocus = guideChannelFocus,
                                restorePlayingFocus = restoreLiveFocus,
                                onPlayingFocusRestored = { restoreLiveFocus = false },
                                onChannelLeft = { guidePanel = GuidePanel.CATEGORIES },
                                onVisibleChannels = { visible -> if (!useXmlTv) loadProviderSchedules(visible) },
                                onHighlight = {
                                    programmeChannel = it
                                    guideProgramme = null
                                },
                                onProgrammeFocus = { channel, programme ->
                                    programmeChannel = channel
                                    guideProgramme = programme
                                },
                                onPlay = { channel -> playChannel(channel, openFullscreenOnStart = false) },
                                onTogglePin = ::toggleProgrammeChannel
                                )
                            }
                            }
                            if (guidePanel == GuidePanel.DETAILS) {
                                val channel = programmeChannel ?: playing.takeIf { vodSection == null }
                                val now = System.currentTimeMillis() / 1_000
                                val schedule = channel?.let(::effectiveProgrammes).orEmpty()
                                val programme = guideProgramme ?: schedule.firstOrNull {
                                    val start = it.startSeconds
                                    val end = it.endSeconds
                                    start != null && end != null && now in start until end
                                } ?: schedule.firstOrNull { it.startSeconds == null }
                                Column(
                                    Modifier.align(Alignment.TopEnd).fillMaxHeight()
                                        .width((guidePreviewWidth * 1.65f).coerceIn(250.dp, 420.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                        .verticalScroll(rememberScrollState()).padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MenuButton(stringResource(R.string.navigation_back), Modifier.focusRequester(guidePanelFocus)) {
                                        navigateGuide(GuideInput.BACK)
                                    }
                                    channel?.let { Text(it.name, style = MaterialTheme.typography.labelMedium) }
                                    Text(
                                        programme?.title ?: stringResource(R.string.no_guide),
                                        style = if (compactGuide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        programme?.description ?: stringResource(R.string.no_title_description),
                                        style = if (compactGuide) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else if (guidePanel != GuidePanel.GRID) {
                                Column(
                                    Modifier.width(240.dp).fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                        .verticalScroll(rememberScrollState()).padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (guidePanel == GuidePanel.MENU) menu()
                                    else {
                                        MenuButton(
                                            stringResource(R.string.guide_all_channels),
                                            Modifier.fillMaxWidth().focusRequester(guidePanelFocus), selected = category == null
                                        ) {
                                            category = null
                                            guideScroll.requestScrollToItem(0)
                                            guidePanel = GuidePanel.GRID
                                        }
                                        groups.forEach { group ->
                                            MenuButton(group, Modifier.fillMaxWidth(), selected = category == group) {
                                                category = group
                                                guideScroll.requestScrollToItem(0)
                                                guidePanel = GuidePanel.GRID
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Destination.SEARCH -> PortalSearch(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        focusRequester = initialFocus,
                        sources = sourcesForBrowsing,
                        channels = liveCategories.values.flatten(),
                        liveSources = { channel -> liveVariants[channel].orEmpty().map { it.source } },
                        liveProgramme = { channel -> effectiveProgrammes(channel)?.firstOrNull()?.title },
                        onHighlightChannel = { programmeChannel = it },
                        metadataClient = metadataClient.takeIf { combine },
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
                        LaunchedEffect(pendingVodCategory, destination, visibleVodGroups, restoring) {
                            if (restoring || pendingVodCategory != destination || visibleVodGroups.isEmpty()) return@LaunchedEffect
                            val previous = preferences?.getString(recentCategoryKey, null)
                            pendingVodCategory = null
                            val group = visibleVodGroups.firstOrNull { it.id == previous } ?: visibleVodGroups.first()
                            if (selectedGroup?.first != section || selectedGroup?.second?.id != group.id) openVodCategory(group)
                        }
                        if (vodSection == section) {
                            PlaybackFeedback(starting, playbackError)
                        }
                        val series = openedSeries.takeIf { section == PortalSection.SERIES }
                        if (details?.first == section) {
                            val title = requireNotNull(details).second
                            val refs = titleEntries(section, title).mapNotNull { item ->
                                item.value.id?.takeIf(String::isNotBlank)?.let { SeriesPortalRef(item.source.identity, it) }
                            }.toSet()
                            val seriesResume = if (section == PortalSection.SERIES) {
                                (trackedSeries(title) ?: seriesActivity.firstOrNull { entry -> entry.refs.any { it in refs } })
                                    ?.let(::resumeFor)
                            } else null
                            val movieResumeKey = if (section == PortalSection.MOVIES) {
                                val currentKey = titleSelection(section, title).progressKey
                                sequenceOf(currentKey).plus(movieHistory.asSequence()
                                    .filter { entry -> entry.refs.any { it in refs } }.map { it.progressKey })
                                    .firstOrNull { (progressPreferences?.getLong(it, 0L) ?: 0L) > 0L }
                            } else null
                            val resumeKey = seriesResume?.progressKey ?: movieResumeKey
                            val resumePosition = resumeKey?.let { progressPreferences?.getLong(it, 0L) ?: 0L }
                            val displayTitle = (portalDetail ?: title).let { detail -> detailMetadata?.applyTo(detail) ?: detail }
                            val titleOverview: @Composable () -> Unit = {
                            TitleDetails(
                                displayTitle, section,
                                metadataRating = detailMetadata?.rating,
                                fromTmdb = detailMetadata != null,
                                favorite = titleKey(section, title) in favorites,
                                saved = isTitleSaved(section, title),
                                newEpisodes = if (section == PortalSection.SERIES) {
                                    trackedSeries(title)?.unseen?.size ?: 0
                                } else 0,
                                resumeLabel = seriesResume?.let { episodeResumeLabel(it, resumePosition ?: 0L) }
                                    ?: resumePosition?.let(::playbackTime),
                                onResume = if (resumeKey != null && (section == PortalSection.MOVIES || seasons != null)) {
                                    { selectTitle(section, title, seriesResume, movieResumeKey) }
                                } else null,
                                compact = compactMedia,
                                onBack = ::navigateBack,
                                onPlay = { selectTitle(section, title) },
                                onFavorite = { toggleSavedTitle(section, title, true) },
                                onWatch = { toggleSavedTitle(section, title, false) }
                            )
                            }
                            if (section == PortalSection.SERIES) {
                                SeriesSeasons(
                                    modifier = Modifier.weight(1f),
                                    scroll = seriesDetailScroll,
                                    series = displayTitle, seasons = seasons, selected = openedSeason,
                                    failed = episodeError, unseen = trackedSeries(title)?.unseen.orEmpty(),
                                    metadata = episodeMetadata,
                                    onSeason = { openedSeason = it.takeUnless { season -> season == openedSeason } },
                                    episodeProgress = { season, episode ->
                                        val key = titleSelection(section, episode, season).progressKey
                                        val savedKey = seriesResume?.takeIf { it.episodeKey == seriesEpisodeKey(season, episode) }
                                            ?.progressKey ?: key
                                        val prefs = progressPreferences
                                        val completed = prefs?.getBoolean("completed:$savedKey", false) == true
                                        val position = prefs?.getLong(savedKey, 0L) ?: 0L
                                        val duration = prefs?.getLong("duration:$savedKey", 0L) ?: 0L
                                        EpisodeCardProgress(completed, position, duration)
                                    },
                                    onEpisode = { season, episode ->
                                        openedSeason = season
                                        selectTitle(section, episode)
                                    },
                                    onRetry = {
                                        closeSeries()
                                        selectTitle(section, title, loadOnly = true)
                                    },
                                    onBack = ::navigateBack,
                                    header = {
                                        titleOverview()
                                        if (resumeEpisodeMissing) Text(
                                            stringResource(R.string.resume_episode_missing),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            } else titleOverview()
                        } else if (section == PortalSection.SERIES && seriesActivityView != null) {
                            val showHistory = seriesActivityView == SeriesActivityView.HISTORY
                            val entries = if (showHistory) seriesActivity.filter { it.viewed }.sortedByDescending { it.watchedAt }
                                else seriesActivity.filter { it.saved }.sortedBy { it.name }
                            MenuButton(stringResource(R.string.back_to_categories), Modifier) { seriesActivityView = null }
                            Text(stringResource(if (showHistory) R.string.series_history else R.string.watchlist))
                            if (entries.isEmpty()) Text(stringResource(R.string.no_tracked_series))
                            LazyColumn(Modifier.weight(1f)) {
                                items(entries, key = { it.key }) { entry ->
                                    val label = if (entry.unseen.isEmpty()) entry.name else
                                        "${entry.name} · ${stringResource(R.string.new_episode_count, entry.unseen.size)}"
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        HistoryArtwork(
                                            section,
                                            PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                            entry.refs
                                        )
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
                        } else if (section == PortalSection.MOVIES && movieWatchlistOpen) {
                            Text(stringResource(R.string.watchlist), style = MaterialTheme.typography.titleMedium)
                            val entries = movieBookmarks.filter { it.progressKey in watchlist }
                            val legacy = (catalogs[section]?.titles.orEmpty() + groupCatalog?.titles.orEmpty())
                                .distinctBy { titleKey(section, it) }
                                .filter { title ->
                                    titleKey(section, title) in watchlist &&
                                        entries.none { it.progressKey == titleKey(section, title) }
                                }
                            if (entries.isEmpty() && legacy.isEmpty()) Text(stringResource(R.string.no_movie_bookmarks))
                            LazyColumn(Modifier.weight(1f)) {
                                items(entries, key = { it.progressKey }) { entry ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        HistoryArtwork(
                                            section, PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                            entry.refs
                                        )
                                        MenuButton(entry.name, Modifier.weight(1f)) { openWatchedMovie(entry, resume = false) }
                                    }
                                }
                                items(legacy) { title ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Poster(title, Modifier.width(52.dp))
                                        MenuButton(displayVodTitle(title), Modifier.weight(1f)) { showTitle(section, title) }
                                    }
                                }
                            }
                        } else if (section == PortalSection.MOVIES && movieHistoryOpen) {
                            MenuButton(stringResource(R.string.back_to_categories), Modifier) { movieHistoryOpen = false }
                            Text(stringResource(R.string.movie_history))
                            if (movieHistory.isEmpty()) Text(stringResource(R.string.no_watched_movies))
                            LazyColumn(Modifier.weight(1f)) {
                                items(movieHistory, key = { it.progressKey }) { entry ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        HistoryArtwork(
                                            section,
                                            PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                            entry.refs
                                        )
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
                                Text(group.name, style = MaterialTheme.typography.titleSmall)
                                if (groupUsesTmdb) Text(stringResource(R.string.tmdb_catalogue_source))
                                guideState.SaveableStateProvider("vod-${section.name}-${group.id}") {
                                CatalogOverview(
                                    modifier = Modifier.weight(1f),
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
                                    returnTitle = catalogueReturnTitle,
                                    onTitleFocusRestored = { catalogueReturnTitle = null },
                                    newEpisodes = { title ->
                                        if (section == PortalSection.SERIES) trackedSeries(title)?.unseen?.size ?: 0 else 0
                                    },
                                    onSelect = {
                                        catalogueReturnTitle = it
                                        searchedVariants = emptyMap()
                                        showTitle(section, it)
                                    }
                                )
                                }
                            } else {
                                if (visibleVodGroups.isEmpty()) CatalogCategories(
                                groups = visibleVodGroups,
                                loading = section in catalogLoading ||
                                    importStatuses[section] == ImportStatus.Loading,
                                failed = section in catalogErrors ||
                                    importStatuses[section] == ImportStatus.Failed,
                                isHidden = { categoryKey(section, it.id) in hiddenCategories },
                                onSelect = ::openVodCategory,
                                onManage = { categoryMenu = section to it }
                                )
                            }
                        }
                    }
                }
            }
            if (inGuide) {
                Column(
                    Modifier.fillMaxSize().guideNavigationInput(interceptRemote = { input ->
                        guidePanel != GuidePanel.GRID || controlBindings[input]?.let {
                            it != ControlAction.DEFAULT && it != ControlAction.CHANNELS
                        } == true
                    }) {
                        if (!runMappedControl(it)) navigateGuide(it)
                    },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    header()
                    content()
                }
            } else if (mediaDestination) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val section = requireNotNull(vodSectionForScreen)
                    Column(
                        Modifier.width(vodSidebarWidth).fillMaxHeight()
                            .onHorizontalRemote(allowRight = false) { navigateBack() }
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        MenuButton(
                            stringResource(R.string.manage_categories),
                            Modifier.fillMaxWidth().focusRequester(initialFocus)
                        ) { categoryMenu = section to null }
                        LazyColumn(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(visibleVodGroups, key = { it.id }) { group ->
                                MenuButton(
                                    group.name, Modifier.fillMaxWidth(),
                                    selected = selectedGroup?.first == section && selectedGroup?.second?.id == group.id &&
                                        !movieHistoryOpen && !movieWatchlistOpen && seriesActivityView == null,
                                    hiddenIndicator = categoryKey(section, group.id) in hiddenCategories,
                                    onLongPress = { categoryMenu = section to group.id }
                                ) { openVodCategory(group) }
                            }
                        }
                            MenuButton(
                                stringResource(R.string.watchlist), Modifier.fillMaxWidth(),
                                selected = if (section == PortalSection.MOVIES) movieWatchlistOpen else
                                    seriesActivityView == SeriesActivityView.WATCHLIST,
                                count = if (section == PortalSection.SERIES) {
                                    seriesActivity.filter { it.saved }.sumOf { it.unseen.size }.takeIf { it > 0 }
                                } else null
                            ) {
                                closeSeries()
                                details = null
                                if (section == PortalSection.MOVIES) {
                                    movieHistoryOpen = false
                                    movieWatchlistOpen = true
                                } else seriesActivityView = SeriesActivityView.WATCHLIST
                            }
                            MenuButton(
                                stringResource(
                                    if (section == PortalSection.MOVIES) R.string.movie_history else R.string.series_history
                                ),
                                Modifier.fillMaxWidth(),
                                selected = if (section == PortalSection.MOVIES) movieHistoryOpen else
                                    seriesActivityView == SeriesActivityView.HISTORY,
                                count = if (section == PortalSection.SERIES) {
                                    seriesActivity.filter { it.viewed }.sumOf { it.unseen.size }.takeIf { it > 0 }
                                } else null
                            ) {
                                closeSeries()
                                details = null
                                if (section == PortalSection.MOVIES) {
                                    movieWatchlistOpen = false
                                    movieHistoryOpen = true
                                } else seriesActivityView = SeriesActivityView.HISTORY
                            }
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (restoring || connecting || cataloguePlaybackView == VodLiveView.CORNER && playing != null) header()
                        content()
                    }
                }
            } else if (maxWidth >= 560.dp) {
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
                    Column(Modifier.weight(3.6f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (destination != Destination.SEARCH || restoring || connecting) header()
                        Column(
                            Modifier.weight(1f).then(if (destination == Destination.SEARCH) Modifier else Modifier.verticalScroll(contentScroll)),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) { content() }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.heightIn(max = 110.dp).verticalScroll(menuScroll)) { menu() }
                    header()
                    Column(
                        Modifier.weight(1f).then(if (destination == Destination.SEARCH) Modifier else Modifier.verticalScroll(contentScroll)),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { content() }
                }
            }
            LaunchedEffect(destination, settingsSection) {
                if (!inGuide && !restoreLiveFocus) {
                    if (destination == Destination.SETTINGS) contentScroll.scrollTo(0)
                    initialFocus.requestFocus()
                }
            }
            LaunchedEffect(inGuide, guidePanel) {
                if (inGuide) {
                    if (guidePanel != GuidePanel.GRID) guidePanelFocus.requestFocus()
                    else initialFocus.requestFocus()
                }
            }
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
    onStop: () -> Unit,
    onSurface: ((SurfaceView?) -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
    onGuideInput: ((GuideInput) -> Unit)? = null,
    onTouchAction: (() -> Boolean)? = null,
    onMediaKey: (() -> Boolean)? = null,
    controlsRequest: Int = 0
) {
    val stop by rememberUpdatedState(onStop)
    val openFullscreen by rememberUpdatedState(onOpenFullscreen)
    val guideInput by rememberUpdatedState(onGuideInput)
    val touchAction by rememberUpdatedState(onTouchAction)
    val mediaKey by rememberUpdatedState(onMediaKey)
    var shownControlsRequest by remember { mutableIntStateOf(controlsRequest) }
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
                    object : PlayerView(context) {
                        override fun performClick(): Boolean =
                            touchAction?.invoke() == true || super.performClick()

                        override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                            if (event.keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                                event.action == android.view.KeyEvent.ACTION_DOWN &&
                                (event.repeatCount == 0 && mediaKey?.invoke() == true)
                            ) return true
                            return super.dispatchKeyEvent(event)
                        }
                    }.apply {
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
                onRelease = {
                    it.player = null
                    onSurface?.invoke(null)
                },
                update = { view ->
                    onSurface?.invoke(view.videoSurfaceView as? SurfaceView)
                    view.useController = openFullscreen == null
                    if (shownControlsRequest != controlsRequest) {
                        shownControlsRequest = controlsRequest
                        view.showController()
                    }
                    if (openFullscreen != null) {
                        view.isFocusable = true
                        view.setOnClickListener { openFullscreen?.invoke() }
                    }
                    if (guideInput != null) {
                        view.setOnKeyListener { _, keyCode, event ->
                            val direction = when (keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                                else -> null
                            }
                            if (direction == null) false else {
                                if (event.action == android.view.KeyEvent.ACTION_UP) guideInput?.invoke(direction)
                                true
                            }
                        }
                    }
                }
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
    modifier: Modifier,
    catalog: PortalCatalog?,
    loading: Boolean,
    failed: Boolean,
    canLoadMore: Boolean,
    emptyMessage: Int,
    onLoadMore: () -> Unit,
    isFavorite: (PortalTitle) -> Boolean,
    isSaved: (PortalTitle) -> Boolean,
    preferPopularity: Boolean,
    returnTitle: PortalTitle?,
    onTitleFocusRestored: () -> Unit,
    newEpisodes: (PortalTitle) -> Int,
    onSelect: (PortalTitle) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
        TitleGrid(
            catalog.titles, isFavorite, isSaved, preferPopularity, newEpisodes,
            returnTitle, onTitleFocusRestored, onSelect
        )
        if (catalog.nextPage != null && !loading && canLoadMore) {
            MenuButton(stringResource(R.string.catalog_load_more), Modifier, onClick = onLoadMore)
        }
    }
}

@Composable
private fun SettingsHeading(label: Int) {
    Text(
        stringResource(label),
        Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ControlMappingDialog(
    bindings: Map<GuideInput, ControlAction>,
    onChange: (GuideInput, ControlAction) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.control_settings)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (input in GuideInput.entries.filter { it != GuideInput.BACK }) {
                    var expanded by remember(input) { mutableStateOf(false) }
                    Text(stringResource(input.label), style = MaterialTheme.typography.labelLarge)
                    Box {
                        MenuButton(
                            stringResource((bindings[input] ?: ControlAction.DEFAULT).label),
                            Modifier.fillMaxWidth()
                        ) { expanded = true }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            for (action in ControlAction.entries) {
                                DropdownMenuItem(text = { Text(stringResource(action.label)) }, onClick = {
                                    onChange(input, action)
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onReset) { Text(stringResource(R.string.control_reset)) } }
    )
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
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val highlighted = focused || hovered
    val colors = MaterialTheme.colorScheme
    val container = when {
        highlighted -> colors.tertiaryContainer
        selected -> colors.secondaryContainer
        else -> Color.Transparent
    }
    Surface(
        modifier = modifier
            .hoverable(interactions)
            .onFocusChanged {
                if (it.isFocused && !focused) onFocused?.invoke()
                focused = it.isFocused
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        color = container,
        contentColor = if (highlighted) colors.onTertiaryContainer else colors.onSurface,
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

@Composable
internal fun TextButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val highlighted = enabled && (focused || hovered)
    MaterialTextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.hoverable(interactions, enabled).onFocusChanged { focused = it.isFocused },
        interactionSource = interactions,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (highlighted) colors.tertiaryContainer else Color.Transparent,
            contentColor = if (highlighted) colors.onTertiaryContainer else colors.primary
        ),
        content = content
    )
}

@Preview(name = "Fernseher", widthDp = 960, heightDp = 540)
@Preview(name = "Handy im Querformat", widthDp = 852, heightDp = 393)
@Composable
private fun PlayerHomePreview() {
    MaterialTheme(colorScheme = playerColors) {
        PlayerHome()
    }
}
