package com.iptvplayer.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState


import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.util.TypedValue
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.key
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
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
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import android.view.SurfaceView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private val storage by lazy { PortalStorage(applicationContext) }
    private var longBackAction: (() -> Boolean)? = null
    private var screenBackAction: ((Boolean) -> Boolean)? = null
    private var playbackInputAction: (() -> Unit)? = null
    private var promptInputAction: ((android.view.KeyEvent) -> Boolean)? = null
    private var consumedLongBack = false

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        // Modal playback prompts own the remote while visible, before PlayerView or
        // Compose can route directional keys to the guide/list behind the prompt.
        if (promptInputAction?.invoke(event) == true) return true
        if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) playbackInputAction?.invoke()
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

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) playbackInputAction?.invoke()
        return super.dispatchTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        // The audio sink is wrapped so that series openings can also be recognised by their theme music.
        val renderers = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context, enableFloatOutput: Boolean, enableAudioOutputPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? =
                super.buildAudioSink(context, enableFloatOutput, enableAudioOutputPlaybackParams)
                    ?.let(::EpisodeAudioTapSink)
        }
        player = ExoPlayer.Builder(this, renderers).build().also { activePlayer ->
            activePlayer.trackSelectionParameters = activePlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage(java.util.Locale.getDefault().language)
                .build()
        }
        enableEdgeToEdge()
        setContent {
            var accentChoice by remember {
                mutableStateOf(
                    AccentChoice.entries.firstOrNull {
                        it.name == getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
                            .getString("accent_color", null)
                    } ?: AccentChoice.RED
                )
            }
            MaterialTheme(colorScheme = playerColors(accentChoice)) {
                PlayerHome(
                    player = player,
                    storage = storage,
                    accentChoice = accentChoice,
                    onAccentChoiceChange = { choice ->
                        accentChoice = choice
                        getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
                            .edit().putString("accent_color", choice.name).apply()
                    },
                    registerLongBack = { longBackAction = it },
                    registerScreenBack = { screenBackAction = it },
                    registerPlaybackInput = { playbackInputAction = it },
                    registerPromptInput = { promptInputAction = it },
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

private enum class Destination(val label: Int, val icon: Int? = null) {
    LIVE(R.string.destination_live, R.drawable.ic_menu_live),
    MOVIES(R.string.destination_movies, R.drawable.ic_menu_movies),
    SERIES(R.string.destination_series, R.drawable.ic_menu_series),
    SETTINGS(R.string.destination_settings, R.drawable.ic_menu_settings),
    SEARCH(R.string.search, R.drawable.ic_menu_search),
    CATEGORIES(R.string.category_settings),
    SOURCES(R.string.destination_sources)
}

private enum class MainMenuStyle(val label: Int) {
    BOTH(R.string.menu_style_both), TEXT(R.string.menu_style_text), ICONS(R.string.menu_style_icons)
}

private enum class ProviderCategoryStyle(val label: Int) {
    SYMBOLS(R.string.provider_style_symbols), BOTH(R.string.provider_style_both), LABELS(R.string.provider_style_labels)
}

private fun providerCategoryMark(name: String): String? = when (serviceCategory(name)) {
    VodCategory.NETFLIX -> "N"
    VodCategory.PRIME -> "prime video"
    VodCategory.APPLE -> "tv+"
    VodCategory.DISNEY -> "Disney+"
    VodCategory.MAX -> "MAX"
    VodCategory.PARAMOUNT -> "P+"
    VodCategory.SKY -> "sky"
    VodCategory.PEACOCK -> "peacock"
    else -> null
}

private fun providerCategoryIcon(name: String): Int? = when (serviceCategory(name)) {
    VodCategory.NETFLIX -> R.drawable.provider_netflix
    VodCategory.PRIME -> R.drawable.provider_prime_video
    VodCategory.APPLE -> R.drawable.provider_appletv
    VodCategory.DISNEY -> R.drawable.provider_disney_plus
    VodCategory.MAX -> R.drawable.provider_max
    VodCategory.PARAMOUNT -> R.drawable.provider_paramountplus
    VodCategory.SKY -> R.drawable.provider_sky
    VodCategory.PEACOCK -> R.drawable.provider_peacock
    else -> null
}

private enum class VodLiveView(val label: Int) {
    BACKGROUND(R.string.vod_live_background),
    CORNER(R.string.vod_live_corner),
    HIDDEN(R.string.vod_live_hidden)
}

private enum class AccentChoice(val label: Int, val color: Color, val container: Color) {
    RED(R.string.accent_red, Color(0xFFE50914), Color(0xFF9E0B12)),
    BLUE(R.string.accent_blue, Color(0xFF1689D4), Color(0xFF0B5A8A)),
    GREEN(R.string.accent_green, Color(0xFF2EAD68), Color(0xFF17663C)),
    PURPLE(R.string.accent_purple, Color(0xFF9B59D0), Color(0xFF63338A)),
    ORANGE(R.string.accent_orange, Color(0xFFFF8A24), Color(0xFF8C4A0D))
}

private val mainDestinations = Destination.entries.filter {
    it != Destination.SOURCES && it != Destination.CATEGORIES
}
private const val EXIT_CONFIRMATION_MS = 3_000L

private fun playerColors(accent: AccentChoice) = darkColorScheme(
    primary = accent.color,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = accent.container,
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFF5C451),
    onSecondary = Color(0xFF241900),
    secondaryContainer = Color(0xFF302818),
    onSecondaryContainer = Color(0xFFFFEFC4),
    tertiary = accent.color,
    surface = Color(0xFF080808),
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF292929),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF646464),
    outlineVariant = Color(0xFF383838),
    tertiaryContainer = accent.container,
    onTertiaryContainer = Color(0xFFFFFFFF)
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

private enum class SeriesActivityView { CONTINUE, HISTORY, WATCHLIST }
private data class PortalContentCounts(val tv: Int, val movies: Int, val series: Int)
private enum class SettingsSection(val label: Int) {
    GENERAL(R.string.settings_general),
    SOURCES(R.string.settings_library),
    GUIDE(R.string.destination_guide),
    APPEARANCE(R.string.settings_appearance),
    PLAYBACK(R.string.settings_playback),
    CONTROLS(R.string.settings_controls),
    METADATA(R.string.settings_metadata),
    SYNC(R.string.settings_sync),
    UPDATES(R.string.settings_updates),
    ABOUT(R.string.settings_about)
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
    accentChoice: AccentChoice = AccentChoice.RED,
    onAccentChoiceChange: (AccentChoice) -> Unit = {},
    registerLongBack: (((() -> Boolean)?) -> Unit) = {},
    registerScreenBack: ((((Boolean) -> Boolean)?) -> Unit) = {},
    registerPlaybackInput: (((() -> Unit)?) -> Unit) = {},
    registerPromptInput: ((((android.view.KeyEvent) -> Boolean)?) -> Unit) = {},
    onExit: () -> Unit = {},
    onFullscreen: (Boolean) -> Unit = {},
    canPlay: () -> Boolean = { false }
) {
    var selected by remember { mutableStateOf<Destination?>(Destination.LIVE) }
    var pendingVodCategory by rememberSaveable { mutableStateOf<Destination?>(null) }
    var lastFocused by rememberSaveable { mutableStateOf(Destination.LIVE) }
    var settingsReturnDestination by rememberSaveable { mutableStateOf(Destination.LIVE) }
    var lastSettingsSection by rememberSaveable { mutableStateOf(SettingsSection.GENERAL) }
    val initialFocus = remember { FocusRequester() }
    val settingsFocusRequesters = remember {
        SettingsSection.entries.associateWith { FocusRequester() }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val preview = LocalInspectionMode.current
    val preferences = remember(context, preview) {
        if (preview) null else context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    }
    val isTelevision = remember(context) {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    }
    var showSettingsBackButton by remember(preferences, isTelevision) {
        mutableStateOf(preferences?.getBoolean("show_settings_back_button", !isTelevision) ?: !isTelevision)
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
    var traktSession by remember { mutableStateOf<TraktSession?>(null) }
    var traktDeviceCode by remember { mutableStateOf<TraktDeviceCode?>(null) }
    var traktBusy by remember { mutableStateOf(false) }
    var sendingWatchStatus by remember { mutableStateOf(false) }
    var traktStatus by remember { mutableStateOf<String?>(null) }
    var traktPlaybackNotice by remember { mutableStateOf<String?>(null) }
    var portalRefreshNotice by remember { mutableStateOf<String?>(null) }
    var traktSyncRequested by remember { mutableStateOf(false) }
    val playbackNoticeHost = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(traktPlaybackNotice) {
        val notice = traktPlaybackNotice ?: return@LaunchedEffect
        playbackNoticeHost.showSnackbar(notice)
        traktPlaybackNotice = null
    }
    LaunchedEffect(portalRefreshNotice) {
        val notice = portalRefreshNotice ?: return@LaunchedEffect
        playbackNoticeHost.showSnackbar(notice)
        portalRefreshNotice = null
    }
    var episodeSettingsOpen by remember { mutableStateOf(false) }
    var epgSettingsOpen by remember { mutableStateOf(false) }
    var settingsSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var mainMenuStyle by remember(preferences) {
        mutableStateOf(MainMenuStyle.entries.firstOrNull {
            it.name == preferences?.getString("main_menu_style", null)
        } ?: MainMenuStyle.BOTH)
    }
    var providerCategoryStyle by remember(preferences) {
        mutableStateOf(ProviderCategoryStyle.entries.firstOrNull {
            it.name == preferences?.getString("provider_category_style", null)
        } ?: ProviderCategoryStyle.BOTH)
    }
    var guideAppearancePage by remember { mutableStateOf<GuideAppearancePage?>(null) }
    var guideAppearance by remember(preferences) { mutableStateOf(readGuideAppearance(preferences)) }
    var useXmlTv by remember(preferences) { mutableStateOf(preferences?.getBoolean("xmltv_enabled", false) ?: false) }
    var epgAtStartup by remember(preferences) { mutableStateOf(preferences?.getBoolean("epg_at_startup", true) ?: true) }
    val portalRefreshAtStartupDefault = remember(preferences) {
        val settings = preferences
        if (settings != null && settings.getInt("portal_refresh_startup_migration", 0) < 51) {
            settings.edit()
                .putBoolean("refresh_portals_at_startup", true)
                .putInt("portal_refresh_startup_migration", 51)
                .apply()
            true
        } else settings?.getBoolean("refresh_portals_at_startup", true) ?: true
    }
    var refreshPortalsAtStartup by remember(preferences) {
        mutableStateOf(portalRefreshAtStartupDefault)
    }
    var playLastChannelAtStartup by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("play_last_channel_at_startup", true) ?: true)
    }
    var epgRefreshHours by remember(preferences) { mutableStateOf(preferences?.getInt("epg_refresh_hours", 6) ?: 6) }
    var epgHorizonHours by remember(preferences) { mutableStateOf(preferences?.getInt("epg_horizon_hours", 24) ?: 24) }
    var subtitleDefaultEnabled by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("subtitle_default_enabled", false) ?: false)
    }
    var forcedSubtitlesEnabled by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("forced_subtitles_enabled", true) ?: true)
    }
    var xmlTvSources by remember { mutableStateOf(emptyList<String>()) }
    var xmlTvSourcesLoaded by remember { mutableStateOf(storage == null) }
    var epgRefreshRequest by remember { mutableIntStateOf(0) }
    var epgUpdating by remember { mutableStateOf(false) }
    var epgUpdateFailed by remember { mutableStateOf(false) }
    var epgSourceWriteFailed by remember { mutableStateOf(false) }
    var epgSourceSaving by remember { mutableStateOf(false) }
    var epgSourceDraft by remember { mutableStateOf("") }
    val activeEpisodeProvider = when {
        traktSession != null -> "Trakt"
        metadataClient != null -> "TMDB"
        else -> "TVmaze"
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
    val seriesOpeningAudio = remember { mutableMapOf<Pair<String, String>, Pair<String, List<AudioPrint>>>() }
    val introStore = remember(context, preview) { if (preview) null else IntroFingerprintStore.create(context) }
    LaunchedEffect(progressPreferences) {
        // Opening fingerprints of earlier versions used another hash and grew without limit;
        // they are rebuilt automatically in the bounded fingerprint store.
        val preferences = progressPreferences ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val obsolete = preferences.all.keys.filter { it.startsWith("opening:") || it.startsWith("opening-samples:") }
            if (obsolete.isNotEmpty()) preferences.edit().apply { obsolete.forEach(::remove) }.apply()
        }
    }
    var fullscreenVideoSurface by remember { mutableStateOf<SurfaceView?>(null) }
    var playbackVideoSurface by remember { mutableStateOf<SurfaceView?>(null) }
    var detectedTiming by remember { mutableStateOf<Pair<String, EpisodeTiming>?>(null) }
    var nextEpisodeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var previousEpisodeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var nextEpisodeName by remember { mutableStateOf("") }
    var nextEpisodeSeconds by remember { mutableStateOf<Int?>(null) }
    var checkSeriesInactivity by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("check_series_inactivity", true) ?: true)
    }
    var inactiveEpisodeLimit by remember(preferences) {
        mutableIntStateOf((preferences?.getInt("inactive_episode_limit", 3) ?: 3).coerceIn(1, 10))
    }
    var silentEpisodes by remember { mutableIntStateOf(0) }
    var interruptedEpisode by remember { mutableStateOf(false) }
    var advancingEpisode by remember { mutableStateOf(false) }
    var watchingPrompt by remember { mutableStateOf(false) }
    var skipIntroTo by remember { mutableStateOf<Long?>(null) }
    var metadataBusy by remember { mutableStateOf(false) }
    var metadataError by remember { mutableStateOf(false) }
    var pendingSeriesEpisode by remember { mutableStateOf<Pair<PortalTitle, EpisodeResume>?>(null) }
    var pendingMovie by remember { mutableStateOf<WatchedMovie?>(null) }
    var currentTraktPlayback by remember { mutableStateOf<TraktMedia?>(null) }
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
    val categoryOrders = remember { mutableStateMapOf<String, List<String>>() }
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
    var category by remember(preferences) {
        mutableStateOf(preferences?.getString("last_live_category", null))
    }
    var lastLiveCategory by remember(preferences) {
        mutableStateOf(preferences?.getString("last_live_category", null))
    }
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
    var sourceRefreshDialogOpen by remember { mutableStateOf(false) }
    var selectedRefreshSources by remember { mutableStateOf<Set<String>>(emptySet()) }
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
    var playbackSeasons by remember { mutableStateOf(emptyList<PortalSeason>()) }
    var playbackSeries by remember { mutableStateOf<PortalTitle?>(null) }
    var playbackMovieTitle by remember { mutableStateOf<PortalTitle?>(null) }
    var playbackSeason by remember { mutableStateOf<PortalSeason?>(null) }
    var playbackEpisode by remember { mutableStateOf<PortalTitle?>(null) }
    var overlaySeason by remember { mutableStateOf<PortalSeason?>(null) }
    var playbackEpisodeVariants by remember {
        mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>())
    }
    var openedSeason by remember { mutableStateOf<PortalSeason?>(null) }
    val seriesDetailScroll = rememberLazyListState()
    var catalogueReturnTitle by remember { mutableStateOf<PortalTitle?>(null) }
    var focusFirstTitle by remember { mutableStateOf(false) }
    var focusTitlePrimary by remember { mutableStateOf(false) }
    var episodeVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var episodeError by remember { mutableStateOf(false) }
    var resumeEpisodeMissing by remember { mutableStateOf(false) }
    var episodeJob by remember { mutableStateOf<Job?>(null) }
    var address by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var storedSource by remember { mutableStateOf<SavedPortal?>(null) }
    var savedSources by remember { mutableStateOf(emptyList<SavedPortal>()) }
    var sourceContentCounts by remember { mutableStateOf<Map<String, PortalContentCounts>>(emptyMap()) }
    var combinedContentCounts by remember { mutableStateOf(PortalContentCounts(0, 0, 0)) }
    val sourceContentKeys = savedSources.map { source ->
        listOf(
            source.identity, source.enabled, System.identityHashCode(source.channels),
            System.identityHashCode(source.genres), System.identityHashCode(source.catalogs),
            source.epgChannelIds.hashCode()
        )
    }
    LaunchedEffect(sourceContentKeys, selected) {
        // These merged counts are only shown on the Sources page. Building them
        // during every cold start duplicated the much more important live-group
        // work and competed with restoring the cached guide.
        if (selected != Destination.SOURCES) return@LaunchedEffect
        val snapshot = savedSources
        val counts = withContext(Dispatchers.Default) {
            val perSource = snapshot.associate { source ->
                val live = buildLiveChannelGroups(listOf(source), combine = false, mergeWithinSource = true).size
                fun uniqueCount(section: PortalSection): Int = source.catalogs[section]?.titles.orEmpty()
                    .distinctBy { title ->
                        val year = vodReleaseYear(title)
                        "${normalizedLabel(displayVodTitle(title))}:${year ?: "?"}"
                    }.size
                source.identity to PortalContentCounts(live, uniqueCount(PortalSection.MOVIES), uniqueCount(PortalSection.SERIES))
            }
            val enabled = snapshot.filter { it.enabled }
            fun mergedCount(section: PortalSection): Int = mergeCatalogueTitles(
                enabled.flatMap { source -> source.catalogs[section]?.titles.orEmpty().map { SourceItem(source, it) } },
                requireVerifiedIdentity = false
            ).size
            perSource to PortalContentCounts(
                buildLiveChannelGroups(enabled, combine = true, mergeWithinSource = true).size,
                mergedCount(PortalSection.MOVIES), mergedCount(PortalSection.SERIES)
            )
        }
        sourceContentCounts = counts.first
        combinedContentCounts = counts.second
    }
    var portalTransferStatus by remember { mutableStateOf<String?>(null) }
    var diagnosticsTree by remember(context) {
        mutableStateOf(context.getSharedPreferences("diagnostics_settings", Context.MODE_PRIVATE)
            .getString("tree_uri", null)?.let(Uri::parse))
    }
    var diagnosticsStatus by remember { mutableStateOf<String?>(null) }
    val chooseDiagnosticsFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val permission = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val persisted = runCatching { context.contentResolver.takePersistableUriPermission(uri, permission) }
            if (persisted.isSuccess) {
                context.getSharedPreferences("diagnostics_settings", Context.MODE_PRIVATE)
                    .edit().putString("tree_uri", uri.toString()).apply()
                diagnosticsTree = uri
                AppDiagnostics.record(context, "diagnostics_folder_selected")
                diagnosticsStatus = context.getString(R.string.diagnostics_folder_selected)
            } else {
                diagnosticsStatus = context.getString(R.string.diagnostics_export_failed)
            }
        }
    }
    val saveDiagnosticsFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) runCatching { AppDiagnostics.exportToDocument(context, uri) }
            .onSuccess { diagnosticsStatus = context.getString(R.string.diagnostics_export_complete) }
            .onFailure { diagnosticsStatus = context.getString(R.string.diagnostics_export_failed) }
    }
    val exportPortals = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(exportPortalCredentials(savedSources))
            } ?: error("Datei konnte nicht erstellt werden")
        }.onSuccess { portalTransferStatus = context.getString(R.string.portals_exported) }
            .onFailure { portalTransferStatus = context.getString(R.string.portal_transfer_failed) }
    }
    val importPortals = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Datei konnte nicht geöffnet werden")
            val credentials = importPortalCredentials(json)
            val added = credentials.map { credential ->
                SavedPortal(credential.address, credential.mac, emptyList(), emptyMap(), emptyMap())
            }
            val merged = (savedSources + added).distinctBy { it.identity }
            savedSources = merged
            scope.launch {
                storage?.write(SavedPortals(merged, storedSource?.identity ?: merged.first().identity))
            }
            added.size
        }.onSuccess { count ->
            portalTransferStatus = context.resources.getQuantityString(R.plurals.portals_imported, count, count)
        }.onFailure { portalTransferStatus = context.getString(R.string.portal_transfer_failed) }
    }
    val secondarySessions = remember { mutableMapOf<String, StalkerPortal>() }
    val sourcesForBrowsing = remember(sourceContentKeys, storedSource?.identity, mergeSources) {
        val enabledSources = savedSources.filter { it.enabled }
        val active = storedSource?.takeIf { it.enabled }
        if (mergeSources) enabledSources.ifEmpty { listOfNotNull(active) } else listOfNotNull(active)
    }
    val combine = mergeSources
    fun categoryOrderKey(section: PortalSection): String =
        "category_order_${if (combine) "combined" else storedSource?.identity.orEmpty()}_${section.name}"

    fun <T> withCategoryOrder(section: PortalSection, values: List<T>, id: (T) -> String): List<T> {
        val key = categoryOrderKey(section)
        val stored = categoryOrders[key] ?: runCatching {
            val data = org.json.JSONArray(preferences?.getString(key, null) ?: "[]")
            List(data.length()) { data.getString(it) }
        }.getOrDefault(emptyList())
        if (stored.isEmpty()) return values
        val positions = stored.withIndex().associate { it.value to it.index }
        return values.sortedBy { positions[id(it)] ?: Int.MAX_VALUE }
    }
    val mergePortalChannels = storedSource?.identity in mergedIndividualPortals
    var editing by remember { mutableStateOf(false) }
    var initialPortalPromptDismissed by rememberSaveable { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(storage != null) }
    var startupPortalRefresh by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var startupPortalCheck by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var startupPortalRefreshStarted by remember { mutableStateOf(false) }
    var autoDisableUnavailablePortals by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("auto_disable_unavailable_portals", true) ?: true)
    }
    var storageError by remember { mutableStateOf<Int?>(null) }
    val detailMetadata by produceState<TmdbTitle?>(null, metadataClient, details, mergeSources, traktSession?.username) {
        value = null
        if (!mergeSources || traktSession != null) return@produceState
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
    val tvmazeDetailMetadata by produceState<TvmazeTitleMetadata?>(null, traktSession?.username, metadataClient, details) {
        value = null
        if (traktSession != null || metadataClient != null) return@produceState
        val entry = details?.takeIf { it.first == PortalSection.SERIES } ?: return@produceState
        value = try {
            TvmazeClient.lookupTitleMetadata(entry.second)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(storage) {
        AppDiagnostics.record(context, "portal_library_restore_started")
        try {
            metadataClient = storage?.readMetadataToken()?.let(::TmdbClient)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            metadataError = true
        }
    }

    LaunchedEffect(storage) {
        try {
            val session = storage?.readTraktSession()
            if (session != null) {
                traktSession = session
                traktSyncRequested = true
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            traktStatus = context.getString(R.string.trakt_session_read_failed)
        }
    }

    suspend fun <T> withTraktSession(action: suspend (TraktApi, TraktSession) -> T): T {
        val clientId = TRAKT_CLIENT_ID
        val api = TraktApi(clientId)
        var session = storage?.readTraktSession() ?: traktSession
            ?: error(context.getString(R.string.trakt_not_connected))
        require(session.clientId == clientId) { context.getString(R.string.trakt_client_id_changed) }
        if (session.expiresAtMillis <= System.currentTimeMillis() + 60_000L) {
            session = api.refresh(session)
            storage?.writeTraktSession(session)
            traktSession = session
        }
        return action(api, session)
    }

    val traktDetailMetadata by produceState<TraktTitleMetadata?>(null, traktSession?.username, details) {
        value = null
        if (traktSession == null) return@produceState
        val entry = details ?: return@produceState
        value = try {
            withTraktSession { api, session -> api.lookupTitleMetadata(entry.first, entry.second, session.accessToken) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    fun connectTrakt() {
        if (traktBusy) return
        val clientId = TRAKT_CLIENT_ID
        traktBusy = true
        traktStatus = null
        scope.launch {
            try {
                val api = TraktApi(clientId)
                val code = api.beginDeviceAuthorization()
                traktDeviceCode = code
                var interval = code.intervalSeconds
                val deadline = SystemClock.elapsedRealtime() + code.expiresInSeconds * 1_000L
                while (SystemClock.elapsedRealtime() < deadline) {
                    delay(interval * 1_000L)
                    when (val result = api.pollDeviceAuthorization(code)) {
                        TraktDevicePoll.Pending -> Unit
                        TraktDevicePoll.SlowDown -> interval = (interval + 5).coerceAtMost(60)
                        is TraktDevicePoll.Authorized -> {
                            val username = runCatching { api.accountName(result.session.accessToken) }
                                .getOrDefault("Trakt")
                            val session = result.session.copy(username = username)
                            storage?.writeTraktSession(session)
                            traktSession = session
                            traktSyncRequested = true
                            traktDeviceCode = null
                            traktStatus = context.getString(R.string.trakt_connected_status, username)
                            return@launch
                        }
                    }
                }
                traktDeviceCode = null
                traktStatus = context.getString(R.string.trakt_code_expired)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                traktDeviceCode = null
                traktStatus = error.message ?: context.getString(R.string.trakt_connect_failed)
            } finally {
                traktBusy = false
            }
        }
    }

    fun disconnectTrakt() {
        if (traktBusy) return
        scope.launch {
            try {
                storage?.clearTraktSession()
                traktSession = null
                traktDeviceCode = null
                traktStatus = context.getString(R.string.trakt_disconnected_status)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                traktStatus = context.getString(R.string.trakt_disconnect_failed)
            }
        }
    }

    fun syncTrakt(forceSeriesHistory: Boolean = false) {
        if (traktBusy || sendingWatchStatus) return
        traktBusy = true
        traktStatus = context.getString(R.string.trakt_checking_changes)
        fun importedTitleKey(section: PortalSection, title: PortalTitle): String =
            "${catalogSession?.first?.sourceLabel.orEmpty()}:${section.name}:${title.id ?: title.name}"
        fun importedSeriesKey(title: PortalTitle): String =
            titleIdentity(title) ?: importedTitleKey(PortalSection.SERIES, title)
                fun traktLibraryKey(media: TraktMedia): String =
                    "trakt:${media.section.name}:${media.traktId ?: media.imdbId ?: media.tmdbId ?: normalizedLabel(media.name)}"
        scope.launch {
            try {
                val enabledSavedSources = savedSources.filter { it.enabled }
                val localSaved = enabledSavedSources.flatMap { source ->
                    listOf(PortalSection.MOVIES, PortalSection.SERIES).flatMap { section ->
                        source.catalogs[section]?.titles.orEmpty().filter { title ->
                            importedTitleKey(section, title) in watchlist
                        }.map { title ->
                            TraktMedia(section, displayVodTitle(title), title.year, title.imdbId, title.tmdbId, null)
                        }
                    }
                }.distinctBy { "${it.section}:${it.imdbId ?: it.tmdbId ?: normalizedLabel(it.name)}" }
                var pendingActivities: String? = null
                val library = withTraktSession { api, session ->
                    val activityKey = "trakt_last_activities:${session.username.lowercase()}"
                    val dirtyKey = "trakt_watchlist_dirty:${session.username.lowercase()}"
                    val activities = api.loadLastActivities(session.accessToken)
                    val cachedActivities = preferences?.getString(activityKey, null)
                    val watchlistDirty = preferences?.getBoolean(dirtyKey, true) ?: true
                    val historyReconcileKey = "trakt_series_history_reconciled:${session.username.lowercase()}"
                    val reconcileSeriesHistory = forceSeriesHistory ||
                        preferences?.getBoolean(historyReconcileKey, false) != true
                    if (cachedActivities == activities && !watchlistDirty && !reconcileSeriesHistory) {
                        withContext(Dispatchers.Main) {
                            traktStatus = context.getString(R.string.trakt_cache_current)
                        }
                        return@withTraktSession null
                    }
                    withContext(Dispatchers.Main) {
                        traktStatus = context.getString(R.string.trakt_checking_changes)
                    }
                    val remote = api.loadLibrary(
                        session.accessToken,
                        onProgress = { progress -> withContext(Dispatchers.Main) { traktStatus = progress } },
                        previousActivities = cachedActivities,
                        currentActivities = activities,
                        reconcileSeriesHistory = reconcileSeriesHistory
                    )
                    if (watchlistDirty) {
                        localSaved.forEachIndexed { index, item ->
                            if (item.imdbId != null || item.tmdbId != null) {
                                withContext(Dispatchers.Main) {
                                    traktStatus = context.getString(
                                        R.string.trakt_uploading_watchlist, index + 1, localSaved.size
                                    )
                                }
                                api.setWatchlistItem(session.accessToken, item, saved = true)
                            }
                        }
                    }
                    pendingActivities = activities
                    remote
                } ?: return@launch
                val localMatchCache = mutableMapOf<String, List<SourceItem<PortalTitle>>>()
                val portalSearchLimiter = Semaphore(3)
                suspend fun localMatches(
                    media: TraktMedia,
                    searchPortalIfMissing: Boolean = false
                ): List<SourceItem<PortalTitle>> {
                    val cacheKey = "${media.section}:${media.imdbId}:${media.tmdbId}:${normalizedLabel(media.name)}:${media.year}"
                    val cached = localMatchCache[cacheKey]
                    if (cached != null && (cached.isNotEmpty() || !searchPortalIfMissing)) return cached
                    val expected = media.asTitle()
                    var matches = cached ?: withContext(Dispatchers.Default) {
                        enabledSavedSources.flatMap { source ->
                            val titles = source.catalogs[media.section]?.titles.orEmpty()
                            equivalentCatalogueTitle(
                                expected,
                                titles,
                                // Trakt IDs often exist while portal titles omit external IDs. Exact
                                // title plus release year is still a safe local match; conflicting IDs
                                // remain rejected by equivalentCatalogueTitle.
                                requireVerifiedIdentity = false
                            )?.let { listOf(SourceItem(source, it)) }.orEmpty()
                        }
                    }
                    if (matches.isEmpty() && searchPortalIfMissing) {
                        for (source in enabledSavedSources) {
                            try {
                                val portal = if (source.identity == storedSource?.identity) {
                                    liveSession?.first ?: secondarySessions.getOrPut(source.identity) {
                                        StalkerPortal(source.address, source.mac)
                                    }
                                } else secondarySessions.getOrPut(source.identity) {
                                    StalkerPortal(source.address, source.mac)
                                }
                                val found = searchPortal(
                                    source, portal, media.section,
                                    displayVodTitle(expected), countries, portalSearchLimiter
                                ).titles
                                val candidate = equivalentCatalogueTitle(
                                    expected,
                                    found.filter { it.source.identity == source.identity }.map { it.value },
                                    requireVerifiedIdentity = false
                                )
                                if (candidate != null) matches = matches + SourceItem(source, candidate)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                // Ein nicht erreichbares Portal darf die übrigen Treffer nicht verwerfen.
                            }
                        }
                    }
                    matches = matches.distinctBy { "${it.source.identity}:${it.value.id ?: it.value.name}" }
                    localMatchCache[cacheKey] = matches
                    return matches
                }
                var matched = 0
                val updatedSeries = seriesActivity.associateByTo(linkedMapOf()) { it.key }
                fun mergeSeriesRecords(records: List<TrackedSeries>): TrackedSeries? {
                    val first = records.firstOrNull() ?: return null
                    val watchedEpisodes = records.flatMapTo(linkedSetOf()) { it.watchedEpisodes }
                    return first.copy(
                        refs = records.flatMap { it.refs }.distinct(),
                        saved = records.any { it.saved },
                        viewed = records.any { it.viewed },
                        localViewed = records.any { it.localViewed },
                        known = records.flatMapTo(linkedSetOf()) { it.known },
                        unseen = records.flatMapTo(linkedSetOf()) { it.unseen } - watchedEpisodes,
                        checkedAt = records.maxOf { it.checkedAt },
                        usedTmdb = records.any { it.usedTmdb },
                        episodeProvider = records.maxByOrNull { it.checkedAt }?.episodeProvider,
                        watchedAt = records.maxOf { it.watchedAt },
                        lastEpisode = records.maxByOrNull { it.watchedAt }?.lastEpisode,
                        nextEpisodeName = records.firstNotNullOfOrNull { it.nextEpisodeName },
                        nextAirDate = records.firstNotNullOfOrNull { it.nextAirDate },
                        watchedEpisodes = watchedEpisodes,
                        isComplete = records.mapNotNull { it.isComplete }.takeIf { it.isNotEmpty() }?.all { it }
                    )
                }
                fun trackedRecordsFor(media: TraktMedia, title: PortalTitle): List<TrackedSeries> {
                    val imdb = title.imdbId ?: media.imdbId
                    val tmdb = title.tmdbId?.toLongOrNull()?.toString() ?: media.tmdbId
                    val name = normalizedLabel(displayVodTitle(title))
                    val year = title.year ?: media.year
                    return updatedSeries.values.filter { item ->
                        val conflicts = imdb != null && item.imdb != null && imdb != item.imdb ||
                            tmdb != null && item.tmdb != null && tmdb != item.tmdb
                        if (conflicts) return@filter false
                        val sameId = imdb != null && imdb == item.imdb || tmdb != null && tmdb == item.tmdb
                        val sameName = normalizedLabel(item.name) == name &&
                            (year != null && item.year == year || year == null && item.year == null)
                        sameId || sameName
                    }
                }
                fun absorbSeries(media: TraktMedia, title: PortalTitle, canonicalKey: String): TrackedSeries? {
                    val matches = trackedRecordsFor(media, title)
                    val merged = mergeSeriesRecords(matches) ?: return null
                    matches.forEach { updatedSeries.remove(it.key) }
                    return merged.copy(
                        key = canonicalKey,
                        name = displayVodTitle(title),
                        year = title.year ?: media.year ?: merged.year,
                        imdb = title.imdbId ?: media.imdbId ?: merged.imdb,
                        tmdb = title.tmdbId ?: media.tmdbId ?: merged.tmdb
                    )
                }
                val completedEpisodes = linkedSetOf<String>()
                val updatedWatchlist = watchlist.toMutableSet()
                val updatedMovieBookmarks = movieBookmarks.associateByTo(linkedMapOf()) { it.progressKey }
                val updatedMovieHistory = movieHistory.associateByTo(linkedMapOf()) { it.progressKey }
                val importedProgress = linkedMapOf<String, Float>()
                suspend fun reportImportProgress(label: String, index: Int, total: Int) {
                    if ((index + 1) % 100 == 0 || index + 1 == total) {
                        traktStatus = "$label: ${index + 1} / $total"
                        yield()
                    }
                }
                for ((index, media) in library.watchlist.withIndex()) {
                    val entries = localMatches(media, searchPortalIfMissing = true)
                    matched++
                    val title = entries.firstOrNull()?.value ?: media.asTitle()
                    val key = entries.firstOrNull()?.let { importedTitleKey(media.section, title) } ?: traktLibraryKey(media)
                    updatedWatchlist += key
                    if (media.section == PortalSection.MOVIES) {
                        val bookmark = WatchedMovie(
                            displayVodTitle(title), title.year ?: media.year, title.imdbId ?: media.imdbId,
                            title.tmdbId ?: media.tmdbId,
                            entries.mapNotNull { item -> item.value.id?.let { SeriesPortalRef(item.source.identity, it) } }.distinct(),
                            key, 0L
                        )
                        updatedMovieBookmarks[key] = bookmark
                    } else {
                        val keyForSeries = if (entries.isNotEmpty()) importedSeriesKey(title) else traktLibraryKey(media)
                        val existing = absorbSeries(media, title, keyForSeries)
                        val refs = entries.mapNotNull { item ->
                            item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                        }.distinct()
                        val tracked = (existing ?: TrackedSeries(
                            keyForSeries, displayVodTitle(title), refs, title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                        )).copy(saved = true, refs = (existing?.refs.orEmpty() + refs).distinct())
                        updatedSeries[keyForSeries] = tracked
                    }
                    reportImportProgress("Importiere Merkliste", index, library.watchlist.size)
                }
                for ((index, media) in library.watched.withIndex()) {
                    val entries = localMatches(media, searchPortalIfMissing = true)
                    matched++
                    val title = entries.firstOrNull()?.value ?: media.asTitle()
                    if (media.section == PortalSection.MOVIES) {
                        val key = entries.firstOrNull()?.let { importedTitleKey(PortalSection.MOVIES, title) } ?: traktLibraryKey(media)
                        val watched = WatchedMovie(
                            displayVodTitle(title), title.year ?: media.year, title.imdbId ?: media.imdbId,
                            title.tmdbId ?: media.tmdbId,
                            entries.mapNotNull { it.value.id?.let { id -> SeriesPortalRef(it.source.identity, id) } }.distinct(),
                            key, media.watchedAtMillis ?: System.currentTimeMillis()
                        )
                        updatedMovieHistory[key] = watched
                    } else {
                        val key = if (entries.isNotEmpty()) importedSeriesKey(title) else traktLibraryKey(media)
                        val existing = absorbSeries(media, title, key)
                        val refs = entries.mapNotNull { it.value.id?.let { id -> SeriesPortalRef(it.source.identity, id) } }.distinct()
                        val tracked = (existing ?: TrackedSeries(
                            key, displayVodTitle(title), refs, title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                        )).copy(viewed = true, watchedAt = media.watchedAtMillis ?: System.currentTimeMillis(), refs = (existing?.refs.orEmpty() + refs).distinct())
                        updatedSeries[key] = tracked
                    }
                    reportImportProgress("Importiere gesehene Titel", index, library.watched.size)
                }
                val sortedWatchedEpisodes = library.watchedEpisodes.sortedBy { it.watchedAtMillis ?: 0L }
                if (sortedWatchedEpisodes.isNotEmpty()) {
                    traktStatus = context.getString(R.string.trakt_matching_saved_episodes)
                }
                val watchedEpisodeGroups = sortedWatchedEpisodes.groupBy { media ->
                    "${media.traktId ?: media.imdbId ?: media.tmdbId ?: normalizedLabel(media.name)}:${media.year ?: 0}"
                }.values
                var processedWatchedEpisodes = 0
                for (episodes in watchedEpisodeGroups) {
                    val media = episodes.first()
                    // A full Trakt watched snapshot repeats all episodes when even one
                    // play changes. Reuse an already linked local series record first;
                    // otherwise this loop would issue a portal search for every known
                    // Trakt show on every sync.
                    val cachedRecords = trackedRecordsFor(media, media.asTitle())
                    val cachedSeries = mergeSeriesRecords(cachedRecords)
                    val episodesToApply = episodes.filter { episode ->
                        val key = "${episode.seasonNumber}:${episode.episodeNumber}"
                        key !in cachedSeries?.watchedEpisodes.orEmpty() ||
                            (episode.watchedAtMillis ?: 0L) > (cachedSeries?.watchedAt ?: 0L)
                    }
                    if (episodesToApply.isEmpty()) {
                        processedWatchedEpisodes += episodes.size
                        reportImportProgress("Prüfe gesehene Folgen", processedWatchedEpisodes - 1, sortedWatchedEpisodes.size)
                        continue
                    }
                    val entries = if (cachedSeries == null) {
                        localMatches(media)
                    } else emptyList()
                    val title = entries.firstOrNull()?.value ?: cachedSeries?.let { record ->
                        PortalTitle(record.name, year = record.year, imdbId = record.imdb, tmdbId = record.tmdb)
                    } ?: media.asTitle()
                    val trackedKey = cachedSeries?.key ?: if (entries.isNotEmpty()) {
                        importedSeriesKey(title)
                    } else traktLibraryKey(media)
                    val old = if (cachedSeries == null) {
                        absorbSeries(media, title, trackedKey)
                    } else {
                        cachedRecords.forEach { updatedSeries.remove(it.key) }
                        cachedSeries.copy(key = trackedKey)
                    }
                    val refs = entries.mapNotNull { item ->
                        item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                    }.distinct()
                    val watchedKeys = linkedSetOf<String>()
                    var latestResume: EpisodeResume? = null
                    var latestWatchedAt = old?.watchedAt ?: 0L
                    for (episode in episodesToApply) {
                        val seasonNumber = episode.seasonNumber ?: continue
                        val episodeNumber = episode.episodeNumber ?: continue
                        val episodeKeys = buildList {
                            val canonical = titleIdentity(title)
                            if (canonical != null) add("SERIES:catalogue:$canonical:$seasonNumber:$episodeNumber")
                            entries.forEach { item ->
                                val variantCanonical = titleIdentity(item.value)
                                val scope = variantCanonical?.let { "catalogue" } ?: item.source.identity
                                val seriesId = variantCanonical ?: item.value.id ?: item.value.name
                                add("SERIES:$scope:$seriesId:$seasonNumber:$episodeNumber")
                            }
                            if (isEmpty() && title.id != null) {
                                add("SERIES:trakt:${title.id}:$seasonNumber:$episodeNumber")
                            }
                        }
                        .distinct()
                        val watchedKey = "$seasonNumber:$episodeNumber"
                        watchedKeys += watchedKey
                        val watchedAt = episode.watchedAtMillis ?: 0L
                        completedEpisodes += episodeKeys.filter { key ->
                            val localUpdated = progressPreferences?.getLong("progress_updated:$key", 0L) ?: 0L
                            if (progressPreferences?.contains("manual_watched:$key") == true) {
                                localUpdated > 0L && watchedAt > localUpdated
                            } else localUpdated == 0L || watchedAt >= localUpdated
                        }
                        if (watchedAt > latestWatchedAt || old?.lastEpisode == null && latestResume == null) {
                            latestWatchedAt = maxOf(latestWatchedAt, watchedAt)
                            latestResume = EpisodeResume(
                                "$seasonNumber:$episodeNumber", seasonNumber, episodeNumber.toString(), null,
                                episodeKeys.firstOrNull()
                                    ?: "SERIES:catalogue:${titleIdentity(title) ?: title.name}:$seasonNumber:$episodeNumber"
                            )
                        }
                    }
                    if (watchedKeys.isEmpty()) continue
                    val tracked = (old ?: TrackedSeries(
                        trackedKey, displayVodTitle(title), refs, title.year ?: media.year,
                        title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                    )).copy(
                        refs = (old?.refs.orEmpty() + refs).distinct(), viewed = true,
                        watchedAt = maxOf(old?.watchedAt ?: 0L, latestWatchedAt),
                        lastEpisode = latestResume ?: old?.lastEpisode,
                        known = old?.known.orEmpty() + watchedKeys,
                        unseen = old?.unseen.orEmpty() - watchedKeys,
                        watchedEpisodes = old?.watchedEpisodes.orEmpty() + watchedKeys
                    )
                    updatedSeries[trackedKey] = tracked
                    matched += watchedKeys.size
                    processedWatchedEpisodes += episodes.size
                    reportImportProgress("Markiere gesehene Folgen", processedWatchedEpisodes - 1, sortedWatchedEpisodes.size)
                }
                for ((index, media) in library.playback.withIndex()) {
                    val percent = media.progressPercent?.takeIf { it > 0f } ?: continue
                    val entries = localMatches(media)
                    val match = entries.firstOrNull()
                    val title = match?.value ?: media.asTitle()
                    val refs = entries.mapNotNull { item ->
                        item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                    }.distinct()
                    if (media.section == PortalSection.MOVIES) {
                        val identity = titleIdentity(title)
                        val scope = if (identity != null) "catalogue" else match?.source?.identity ?: "trakt"
                        val id = identity ?: title.id ?: media.traktId?.toString() ?: title.name
                        val key = "MOVIES:$scope::$id"
                        val localUpdated = progressPreferences?.getLong("progress_updated:$key", 0L) ?: 0L
                        if ((progressPreferences?.getLong(key, 0L) ?: 0L) <= 1L ||
                            (media.watchedAtMillis ?: 0L) >= localUpdated
                        ) importedProgress[key] = percent
                        val entry = WatchedMovie(
                            displayVodTitle(title), title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId,
                            refs, key, media.watchedAtMillis ?: System.currentTimeMillis()
                        )
                        updatedMovieHistory[key] = entry
                    } else {
                        val canonical = titleIdentity(title)
                        val scope = if (canonical != null) "catalogue" else match?.source?.identity ?: "trakt"
                        val seriesId = canonical ?: title.id ?: media.traktId?.toString() ?: title.name
                        val seasonNumber = media.seasonNumber ?: continue
                        val episodeNumber = media.episodeNumber ?: continue
                        val key = "SERIES:$scope:$seriesId:$seasonNumber:$episodeNumber"
                        val resume = EpisodeResume(
                            "$seasonNumber:$episodeNumber", seasonNumber, episodeNumber.toString(), null, key
                        )
                        val localUpdated = progressPreferences?.getLong("progress_updated:$key", 0L) ?: 0L
                        if ((progressPreferences?.getLong(key, 0L) ?: 0L) <= 1L ||
                            (media.watchedAtMillis ?: 0L) >= localUpdated
                        ) importedProgress[key] = percent
                        val trackedKey = if (match != null) importedSeriesKey(title) else traktLibraryKey(media)
                        val old = absorbSeries(media, title, trackedKey)
                        val tracked = (old ?: TrackedSeries(
                            trackedKey, displayVodTitle(title), refs, title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                        )).copy(
                            refs = (old?.refs.orEmpty() + refs).distinct(), viewed = true,
                            watchedAt = media.watchedAtMillis ?: System.currentTimeMillis(), lastEpisode = resume
                        )
                        updatedSeries[trackedKey] = tracked
                    }
                    matched++
                    reportImportProgress("Importiere Wiedergabestände", index, library.playback.size)
                }
                for ((index, progress) in library.seriesProgress.withIndex()) {
                    val media = progress.show
                    val entries = localMatches(media)
                    val title = entries.firstOrNull()?.value ?: media.asTitle()
                    val key = entries.firstOrNull()?.let { importedSeriesKey(title) } ?: traktLibraryKey(media)
                    val old = absorbSeries(media, title, key)
                    val refs = entries.mapNotNull { item ->
                        item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                    }.distinct()
                    updatedSeries[key] = (old ?: TrackedSeries(
                        key, displayVodTitle(title), refs, title.year ?: media.year,
                        title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                    )).copy(
                        refs = (old?.refs.orEmpty() + refs).distinct(),
                        viewed = true,
                        watchedAt = maxOf(old?.watchedAt ?: 0L, media.watchedAtMillis ?: 0L),
                        isComplete = progress.isComplete
                    )
                    reportImportProgress("Prüfe Serienfortschritt", index, library.seriesProgress.size)
                }
                watchlist = updatedWatchlist
                movieBookmarks = updatedMovieBookmarks.values.toList()
                movieHistory = updatedMovieHistory.values.sortedByDescending { it.watchedAt }.take(10)
                val remoteSeriesKeys = library.seriesHistorySnapshot?.let { snapshot ->
                    traktStatus = context.getString(R.string.trakt_reconcile_series_history)
                    withContext(Dispatchers.Default) {
                        snapshot.flatMapTo(hashSetOf()) { media ->
                            trackedRecordsFor(media, media.asTitle()).map { it.key }
                        }
                    }
                }
                seriesActivity = updatedSeries.values.map { record ->
                    if (remoteSeriesKeys == null || !record.viewed) record
                    else record.copy(hiddenFromHistory = !record.localViewed && record.key !in remoteSeriesKeys)
                }
                progressPreferences?.edit()?.also { editor ->
                    completedEpisodes.forEach { key ->
                        editor.putBoolean("completed:$key", true).remove(key)
                            .remove("manual_watched:$key").remove("trakt_percent:$key")
                    }
                    importedProgress.forEach { (key, percent) ->
                        if (key !in completedEpisodes && progressPreferences?.contains("manual_watched:$key") != true) {
                            editor.putFloat("trakt_percent:$key", percent).putLong(key, 1L)
                                .remove("completed:$key")
                        }
                    }
                }?.apply()
                preferences?.edit()?.putStringSet("watchlist", watchlist)?.apply()
                progressPreferences?.edit()
                    ?.putString("movie_bookmarks", encodeMovieHistory(movieBookmarks))
                    ?.putString("movie_history", encodeMovieHistory(movieHistory))
                    ?.putString("series_activity", encodeSeriesActivity(seriesActivity))
                    ?.apply()
                pendingActivities?.let { activities ->
                    traktSession?.username?.lowercase()?.let { username ->
                        preferences?.edit()
                            ?.putString("trakt_last_activities:$username", activities)
                            ?.putBoolean("trakt_watchlist_dirty:$username", false)
                            ?.also { editor ->
                                if (remoteSeriesKeys != null) editor.putBoolean("trakt_series_history_reconciled:$username", true)
                            }
                            ?.apply()
                    }
                }
                traktStatus = context.getString(R.string.trakt_sync_complete, matched)
                traktPlaybackNotice = context.getString(R.string.trakt_sync_complete, matched)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppDiagnostics.record(context, "trakt_library_sync_failed")
                traktStatus = error.message ?: context.getString(R.string.trakt_sync_failed)
            } finally {
                traktBusy = false
            }
        }
    }

    LaunchedEffect(traktSyncRequested, traktBusy, sendingWatchStatus, traktSession?.username, restoring) {
        if (traktSyncRequested && !traktBusy && !sendingWatchStatus && traktSession != null && !restoring) {
            // Let the cached guide and last channel render before remote account sync begins.
            delay(5_000)
            traktSyncRequested = false
            syncTrakt()
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
    var liveGroups: Map<StalkerChannel, LiveChannelGroup> by remember {
        mutableStateOf(emptyMap())
    }
    var liveGroupsLoading by remember { mutableStateOf(false) }
    var liveGroupsReady by remember { mutableStateOf(false) }
    LaunchedEffect(sourcesForBrowsing, combine, mergePortalChannels, countries) {
        // Large portals can contain thousands of channels. Grouping and normalizing
        // them on Compose's main dispatcher made cold start and remote navigation
        // feel frozen even though the source data had already been restored locally.
        liveGroupsLoading = true
        try {
            val groups = withContext(Dispatchers.Default) {
                buildLiveChannelGroups(sourcesForBrowsing, combine, mergePortalChannels, countries)
            }
            val originals = groups.map { it.variants.first().item.value }.toSet()
            displayChannels.keys.retainAll(originals)
            liveGroups = groups.associateBy { group ->
                val original = group.variants.first().item.value
                if (!combine && !mergePortalChannels) original else displayChannels.getOrPut(original) {
                    StalkerChannel(
                        group.description.name, original.command, original.genreId, original.genreName,
                        original.id, original.logo, original.direct, original.epgIdentity, original.number
                    )
                }
            }
        } finally {
            liveGroupsLoading = false
            liveGroupsReady = true
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
    val vodGroups by produceState(
        initialValue = emptyMap<PortalSection, Map<String, List<SourceItem<PortalGroup>>>>(),
        sourcesForBrowsing, combine, fallbackGroup, countries, metadataClient
    ) {
        value = withContext(Dispatchers.Default) {
            listOf(PortalSection.MOVIES, PortalSection.SERIES).associateWith { section ->
            val entries = sourcesForBrowsing.flatMap { source ->
                source.catalogs[section]?.availableGroups(fallbackGroup).orEmpty().map { SourceItem(source, it) }
            }.filter { item ->
                // The country selection applies to films and series as well. Merged genres
                // must not take in provider categories marked for other countries
                // (for example "NL | Serien"); unmarked categories stay available.
                !combine || countries.isEmpty() ||
                    categoryCountry(item.value.name, item.value.country).let { it == UNKNOWN_COUNTRY || it in countries }
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

    val knownLiveCountries = remember(sourcesForBrowsing) {
        sourcesForBrowsing.flatMap { source ->
            source.channels.map { channel ->
                categoryCountry(channel.name, channel.genreName ?: source.genres[channel.genreId])
            }
        }.toSet()
    }
    val knownCountries = remember(knownLiveCountries, vodGroups) {
        (knownLiveCountries +
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

    fun defaultVodCategoryOrder(section: PortalSection): List<PortalGroup> =
        browsingGroups(section).sortedWith(compareBy(
            { if (combine) vodCategoryNames.keys.indexOf(it.id) else countryPriority(it.name, it.country) },
            { it.name }
        ))

    fun titleKey(section: PortalSection, title: PortalTitle): String =
        "${catalogSession?.first?.sourceLabel.orEmpty()}:${section.name}:${title.id ?: title.name}"

    fun seriesKey(title: PortalTitle): String =
        titleIdentity(title) ?: titleKey(PortalSection.SERIES, title)
    fun trackedSeries(title: PortalTitle): TrackedSeries? {
        val titleImdb = title.imdbId?.takeIf { it.startsWith("tt") }
        val titleTmdb = title.tmdbId?.toLongOrNull()?.takeIf { it > 0 }?.toString()
        val normalizedTitle = normalizedLabel(displayVodTitle(title))
        val compatible = seriesActivity.filter { entry ->
            val conflictingIds = titleImdb != null && entry.imdb != null && titleImdb != entry.imdb ||
                titleTmdb != null && entry.tmdb != null && titleTmdb != entry.tmdb
            if (conflictingIds) return@filter false
            val sameId = titleImdb != null && entry.imdb == titleImdb ||
                titleTmdb != null && entry.tmdb == titleTmdb
            val sameNameAndYear = normalizedLabel(entry.name) == normalizedTitle &&
                (title.year == null || entry.year == null || title.year == entry.year)
            sameId || sameNameAndYear
        }
        val exact = compatible.filter { it.key == seriesKey(title) }
        val identified = compatible.filter { entry ->
            titleImdb != null && entry.imdb == titleImdb || titleTmdb != null && entry.tmdb == titleTmdb
        }
        val named = compatible.filter { entry ->
            normalizedLabel(entry.name) == normalizedTitle &&
                (title.year != null && entry.year == title.year || title.year == null && entry.year == null)
        }
        val matches = when {
            exact.isNotEmpty() -> (exact + identified).distinctBy { it.key }
            identified.isNotEmpty() -> identified
            else -> {
                if (named.size != 1 || named.mapNotNull { it.imdb }.distinct().size > 1 ||
                    named.mapNotNull { it.tmdb }.distinct().size > 1
                ) return null
                named
            }
        }
        val first = matches.firstOrNull() ?: return null
        if (matches.size == 1) return first
        val watched = matches.flatMapTo(linkedSetOf()) { it.watchedEpisodes }
        return first.copy(
            refs = matches.flatMap { it.refs }.distinct(),
            saved = matches.any { it.saved },
            viewed = matches.any { it.viewed },
            known = matches.flatMapTo(linkedSetOf()) { it.known },
            unseen = matches.flatMapTo(linkedSetOf()) { it.unseen } - watched,
            watchedAt = matches.maxOf { it.watchedAt },
            lastEpisode = matches.maxByOrNull { it.watchedAt }?.lastEpisode,
            watchedEpisodes = watched
        )
    }
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
            localViewed = initial.localViewed || watchedEpisode != null,
            hiddenFromHistory = initial.hiddenFromHistory && watchedEpisode == null,
            known = if (watchedEpisode != null && initial.checkedAt > 0) {
                initial.known + watchedEpisode.episodeKey
            } else initial.known,
            unseen = if (watchedEpisode == null) initial.unseen else initial.unseen - watchedEpisode.episodeKey,
            watchedEpisodes = if (watchedEpisode == null) initial.watchedEpisodes
                else initial.watchedEpisodes + watchedEpisode.episodeKey,
            watchedAt = if (watchedEpisode == null) initial.watchedAt else System.currentTimeMillis(),
            lastEpisode = watchedEpisode ?: initial.lastEpisode,
            isComplete = if (watchedEpisode == null) initial.isComplete else false
        )
        val remaining = seriesActivity.filterNot { it.key == key }
        saveSeriesActivity(if (updated.saved || updated.viewed) remaining + updated else remaining)
    }

    fun observeEpisodeProvider(
        key: String,
        episodes: Set<String>,
        provider: String,
        upcoming: TraktUpcomingEpisode? = null
    ) {
        saveSeriesActivity(seriesActivity.map {
            if (it.key != key) it
            else if ((it.episodeProvider ?: if (it.usedTmdb) "tmdb" else null) == provider) {
                it.observe(episodes, System.currentTimeMillis()).copy(
                    nextEpisodeName = upcoming?.name,
                    nextAirDate = upcoming?.airDate
                )
            } else it.copy(
                episodeProvider = provider, known = episodes, checkedAt = System.currentTimeMillis(),
                nextEpisodeName = upcoming?.name, nextAirDate = upcoming?.airDate
            )
        })
    }

    val trackedSources = savedSources.filter { it.enabled }.map { it.identity }
    val trackedTitles = seriesActivity.map { it.key to it.refs }
    val trackedEpisodeChecks = seriesActivity.map { it.key to it.checkedAt }
    LaunchedEffect(metadataClient, traktSession?.username, trackedSources, trackedTitles, trackedEpisodeChecks) {
        val client = metadataClient
        val provider = when {
            traktSession != null -> "trakt"
            client != null -> "tmdb"
            else -> "tvmaze"
        }
        var scanOffset = 0
        while (trackedSources.isNotEmpty() && trackedTitles.isNotEmpty()) {
            if (canPlay()) {
                val now = System.currentTimeMillis()
                val due = seriesActivity.filter {
                    (it.episodeProvider ?: if (it.usedTmdb) "tmdb" else null) != provider ||
                        now - it.checkedAt >= 30 * 60 * 1000L
                }
                // A Trakt library can contain many shows. Process one per minute
                // instead of issuing a burst of requests while the user browses.
                val tracked = due.getOrNull(scanOffset % due.size.coerceAtLeast(1))
                if (due.isNotEmpty()) scanOffset = (scanOffset + 1) % due.size
                if (tracked != null) {
                    try {
                        val title = PortalTitle(
                            tracked.name, year = tracked.year, imdbId = tracked.imdb, tmdbId = tracked.tmdb
                        )
                        val feed = if (provider == "trakt") {
                            withTraktSession { api, session -> api.episodeFeed(title, session.accessToken) }
                        } else null
                        val keys = when (provider) {
                            "trakt" -> feed?.airedKeys
                            "tmdb" -> client?.airedEpisodes(title)
                            else -> TvmazeClient.airedEpisodes(title)
                        }
                        if (keys != null && seriesActivity.any { it.key == tracked.key }) {
                            observeEpisodeProvider(tracked.key, keys, provider, feed?.upcoming)
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
            traktSession?.username?.lowercase()?.let { username ->
                preferences?.edit()?.putBoolean("trakt_watchlist_dirty:$username", true)?.apply()
            }
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
    var visibleEpgChannels by remember { mutableStateOf(emptyList<StalkerChannel>()) }
    val epgInFlight = remember { mutableSetOf<String>() }
    val epgRequestLimit = remember { Semaphore(6) }
    val xmlTvRequestLimit = remember { Semaphore(3) }
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
                    entries = epgRequestLimit.withPermit {
                        portal.loadProgramme(
                            item.value, portal.openSession(),
                            periodHours = epgHorizonHours, refreshHours = epgRefreshHours
                        )
                            .guideWindow(System.currentTimeMillis() / 1_000, epgHorizonHours)
                    }
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
        if (force) {
            channels.asSequence().flatMap { liveVariants[it].orEmpty().asSequence() }
                .distinctBy { it.source.identity }
                .forEach { item -> sessionFor(item.source).invalidateProgrammeCache() }
        }
        val results = coroutineScope {
            channels.distinct().map { channel -> async { loadProviderSchedule(channel, force) } }
                .map { it.await() }
        }
        return results.all { it }
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
                val cached = coroutineScope {
                    sources.map { uri -> async {
                        xmlTvRequestLimit.withPermit {
                            loadXmlTvGuide(context.cacheDir, uri, epgHorizonHours, false, false, maxAge)
                        }
                    } }.awaitAll()
                }
                externalGuides = cached.mapNotNull { it.guide }
                val results = coroutineScope {
                    sources.zip(cached).map { (uri, entry) -> async {
                        if (entry.guide == null || refresh && (epgRefreshRequest > 0 || entry.needsRefresh)) {
                            xmlTvRequestLimit.withPermit {
                                loadXmlTvGuide(context.cacheDir, uri, epgHorizonHours, refresh = true)
                            }
                        } else entry
                    } }.awaitAll()
                }
                externalGuides = results.mapNotNull { it.guide }
                epgUpdateFailed = results.any { it.failed }
            } else if (refresh) {
                val available = liveCategories.entries
                    .filter { visibleCategory(PortalSection.LIVE, it.key, it.key) }.flatMap { it.value }
                val selected = sequenceOf(playing, programmeChannel).filterNotNull()
                val pinned = available.filter { programmeKey(it) in pinnedProgrammeKeys }
                // Refresh only the EPG rows the user is currently looking at, plus the
                // playing/selected and explicitly pinned channels. A cache entry alone
                // must not make thousands of old channels refresh every cycle.
                val refreshTargets = (selected + visibleEpgChannels.asSequence() + pinned.asSequence())
                    .filter { it in available }.distinct().toList()
                epgUpdateFailed = !loadProviderSchedules(refreshTargets, force = epgRefreshRequest > 0)
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
                    ?.takeIf { it.enabled } ?: library.sources.firstOrNull { it.enabled }
                if (saved != null) {
                    preferences?.edit()?.putString("active_portal_cache", saved.identity)?.apply()
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
                } else {
                    storedSource = null
                    liveSession = null
                    catalogSession = null
                    channels = emptyList()
                    genres = emptyMap()
                    catalogs = emptyMap()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppDiagnostics.record(context, "portal_library_restore_failed:${error.javaClass.simpleName}")
            storageError = R.string.portal_restore_failed
        } finally {
            restoring = false
            AppDiagnostics.record(context, "portal_library_restore_completed")
        }
    }

    suspend fun save(source: SavedPortal, activate: Boolean = true) {
        if (activate || storedSource?.identity == source.identity) storedSource = source
        preferences?.edit()?.putString("active_portal_cache", storedSource?.identity)?.apply()
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        savedSources = if (index < 0) savedSources + source else {
            savedSources.toMutableList().apply { this[index] = source }
        }
        try {
            storage?.write(SavedPortals(
                savedSources, storedSource?.identity ?: savedSources.firstOrNull()?.identity ?: source.identity
            ))
            storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = R.string.portal_save_failed
        }
    }

    suspend fun persistPortalSnapshot(source: SavedPortal) {
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        val persistedSources = if (index < 0) savedSources + source else {
            savedSources.toMutableList().apply { this[index] = source }
        }
        val activeId = storedSource?.identity
            ?: persistedSources.firstOrNull { it.enabled }?.identity
            ?: source.identity
        try {
            storage?.write(SavedPortals(persistedSources, activeId))
            storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = R.string.portal_save_failed
        }
    }

    suspend fun updatePortalStatus(identity: String, reachable: Boolean? = null, enabled: Boolean? = null) {
        val current = savedSources.firstOrNull { source ->
            runCatching { source.identity == identity }.getOrDefault(false)
        } ?: return
        val nextReachable = reachable ?: current.reachable
        val userChangedEnabled = enabled != null
        val autoDisabled = when {
            userChangedEnabled -> false
            reachable == false && autoDisableUnavailablePortals && current.enabled -> true
            reachable == true && current.autoDisabled && autoDisableUnavailablePortals -> false
            else -> current.autoDisabled
        }
        val nextEnabled = when {
            userChangedEnabled -> enabled == true
            reachable == false && autoDisableUnavailablePortals && current.enabled -> false
            reachable == true && current.autoDisabled && autoDisableUnavailablePortals -> true
            else -> current.enabled
        }
        val updated = SavedPortal(
            current.address, current.mac, current.channels, current.genres, current.catalogs,
            current.epgChannelIds, current.subscriptionExpiry, nextEnabled, nextReachable, autoDisabled
        )
        if (current.enabled == nextEnabled && current.reachable == nextReachable &&
            current.autoDisabled == autoDisabled
        ) return
        if (storedSource?.identity == identity && !nextEnabled) {
            val fallback = savedSources.firstOrNull { source ->
                source.enabled && runCatching { source.identity != identity }.getOrDefault(false)
            }
            storedSource = fallback
            preferences?.edit()?.putString("active_portal_cache", fallback?.identity)?.apply()
            liveSession = fallback?.let { StalkerPortal(it.address, it.mac) to null }
            catalogSession = liveSession
            channels = fallback?.channels.orEmpty()
            genres = fallback?.genres.orEmpty()
            catalogs = fallback?.catalogs.orEmpty()
            epgChannelIds = fallback?.epgChannelIds.orEmpty()
            category = null
            programmeChannel = null
        }
        save(updated, activate = false)
    }

    fun toggleProgrammeChannel(channel: StalkerChannel) {
        val item = liveVariants[channel]?.firstOrNull() ?: return
        val id = item.value.id ?: return
        val source = item.source
        val ids = if (id in source.epgChannelIds) source.epgChannelIds - id else source.epgChannelIds + id
        if (storedSource?.identity == source.identity) epgChannelIds = ids
        val snapshot = SavedPortal(
            source.address, source.mac, source.channels, source.genres, source.catalogs, ids,
            source.subscriptionExpiry, source.enabled, source.reachable, source.autoDisabled
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
            progressPreferences?.saveInterruptedPlayback(key, position, activePlayer.duration)
        }
    }

    fun sendTraktScrobble(action: String, notifyUser: Boolean = false) {
        if (traktSession == null) return
        val media = currentTraktPlayback ?: return
        val attempt = playbackAttempt ?: return
        val activePlayer = player ?: return
        if (attempt.selection.section == PortalSection.LIVE || activePlayer.currentMediaItem?.mediaId != attempt.mediaId) return
        val duration = activePlayer.duration
        val percent = if (duration > 0L) activePlayer.currentPosition.toFloat() * 100f / duration else 0f
        scope.launch {
            try {
                withTraktSession { api, session ->
                    api.scrobble(action, media, session.accessToken, percent)
                }
                if (notifyUser) traktPlaybackNotice = context.getString(
                    if (percent >= 80f) R.string.trakt_title_watched else R.string.trakt_playback_saved
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (notifyUser) traktPlaybackNotice = context.getString(R.string.trakt_playback_sync_failed)
            }
        }
    }

    fun resumeFor(entry: TrackedSeries): EpisodeResume? =
        recoverEpisodeResume(entry, progressPreferences?.all.orEmpty())

    var preparedLiveVariant by remember { mutableStateOf<PreparedLiveVariant?>(null) }

    fun stopPlayback() {
        preparedLiveVariant = null
        savePlaybackPosition()
        if (currentTraktPlayback != null && playbackAttempt != null && (player?.currentPosition ?: 0L) > 0L) {
            sendTraktScrobble("stop", notifyUser = true)
        }
        currentTraktPlayback = null
        playbackJob?.cancel()
        playbackAttempt = null
        playbackSourceBadge = null
        pendingSeriesEpisode = null
        pendingMovie = null
        playbackMovieTitle = null
        nextEpisodeAction = null
        previousEpisodeAction = null
        nextEpisodeSeconds = null
        skipIntroTo = null
        watchingPrompt = false
        interruptedEpisode = false
        if (!advancingEpisode) silentEpisodes = 0
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

    fun advanceEpisode(confirmed: Boolean = false) {
        val next = nextEpisodeAction ?: return
        if (confirmed) silentEpisodes = 0
        watchingPrompt = false
        advancingEpisode = true
        try {
            next()
        } finally {
            advancingEpisode = false
        }
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
        if (playing == null) {
            if (starting == null) return false
            if (openFullscreen) {
                lastBack = 0
                fullscreen = true
            }
            return true
        }
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
        // VOD stays on its detail page while the stream resolves and buffers.
        // The player listener opens fullscreen only once playback actually starts.
        // Live TV still opens immediately; automatic episode transitions stay fullscreen.
        val remainFullscreen = selection.section == PortalSection.LIVE ||
            fullscreen && selection.section == PortalSection.SERIES && advancingEpisode
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
            currentTraktPlayback = null
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

    fun startupChannelScope(): String {
        val activeId = storedSource?.identity ?: preferences?.getString("active_portal_cache", null)
        val enabledScope = savedSources.filter { it.enabled }.map { it.identity }.sorted().joinToString(",")
        val sourceScope = if (mergeSources) "combined:$enabledScope" else
            "${activeId.orEmpty()}:${activeId in mergedIndividualPortals}:${activeId in enabledScope.split(',')}"
        return listOf(sourceScope, countries.joinToString(","), hiddenCategories.sorted().joinToString(",")).joinToString("|")
    }

    var cachedStartupAttempt by remember { mutableStateOf(false) }
    var startupSnapshotChecked by remember { mutableStateOf(storage == null) }
    LaunchedEffect(storage, restoring, playLastChannelAtStartup, selected) {
        if (restoring || startupSnapshotChecked) return@LaunchedEffect
        if (storage == null || !playLastChannelAtStartup || selected != Destination.LIVE) {
            startupSnapshotChecked = true
            return@LaunchedEffect
        }
        val cached = try {
            storage.readStartupChannel()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        val lastLiveKey = preferences?.getString("last_live_channel", null)
        if (cached != null && selected == Destination.LIVE && playbackSelection == null &&
            cached.scope == startupChannelScope() && cached.key == lastLiveKey
        ) {
            val enabledSources = savedSources.filter { it.enabled }.map { it.identity }.toSet()
            val validStreams = cached.streams.filter { stream ->
                SavedPortal(stream.address, stream.mac, emptyList(), emptyMap(), emptyMap()).identity in enabledSources
            }
            if (validStreams.isNotEmpty()) {
                val channel = StalkerChannel(cached.name, "", null, null)
                programmeChannel = channel
                cachedStartupAttempt = true
                startPlayback(
                    channel, PlaybackSelection(cached.selectionSource, PortalSection.LIVE, cached.selectionId),
                    validStreams.map { saved ->
                        val portal = StalkerPortal(saved.address, saved.mac)
                        val stream = StalkerChannel(cached.name, saved.command, null, null, id = saved.id, direct = saved.direct)
                        PlaybackVariant(portal.sourceLabel) {
                            portal.resolve(stream, if (saved.direct) "" else portal.openSession())
                        }
                    },
                    liveRestoreKey = cached.key
                )
            }
        }
        startupSnapshotChecked = true
    }

    LaunchedEffect(restoring, playing, playbackAttempt, liveGroups) {
        if (restoring || playing == null || playing in liveGroups || vodSection != null) return@LaunchedEffect
        val key = playbackAttempt?.liveRestoreKey ?: return@LaunchedEffect
        val restored = liveGroups.keys.firstOrNull { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
            ?: return@LaunchedEffect
        playing = restored
        programmeChannel = restored
    }

    var restoredLastLiveChannel by remember { mutableStateOf(false) }
    LaunchedEffect(category) {
        if (category != null) {
            lastLiveCategory = category
            preferences?.edit()?.putString("last_live_category", category)?.apply()
        }
    }
    LaunchedEffect(restoring, liveCategories) {
        if (restoring || liveCategories.isEmpty()) return@LaunchedEffect
        val available = liveCategories.keys
            .filter { visibleCategory(PortalSection.LIVE, it, it) }
            .sortedBy { categoryNames.values.indexOf(it) }
        if (category !in available) {
            val savedKey = preferences?.getString("last_live_channel", null)
            val inferred = savedKey?.let { key -> liveCategories.entries.firstOrNull { (_, items) ->
                items.any { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
            }?.key }
            category = inferred ?: lastLiveCategory?.takeIf { it in available } ?: available.firstOrNull()
        }
    }
    LaunchedEffect(restoring, startupSnapshotChecked, liveCategories, playbackError, playLastChannelAtStartup) {
        if (restoring || !startupSnapshotChecked || liveCategories.isEmpty() || restoredLastLiveChannel) return@LaunchedEffect
        val retryFromCatalogue = cachedStartupAttempt && playbackError != null
        if (cachedStartupAttempt && !retryFromCatalogue && playbackSelection != null) return@LaunchedEffect
        restoredLastLiveChannel = true
        if (selected != Destination.LIVE || playbackSelection != null && !retryFromCatalogue) return@LaunchedEffect
        val orderedCategories = liveCategories.keys
            .filter { visibleCategory(PortalSection.LIVE, it, it) }
            .sortedBy { categoryNames.values.indexOf(it) }
        val savedKey = preferences?.getString("last_live_channel", null)
        val categoryWithLastChannel = savedKey?.let { key ->
            liveCategories.entries.firstOrNull { (_, items) -> items.any { channel ->
                (programmeKey(channel) ?: liveGroups[channel]?.identity) == key
            } }?.key
        }
        val restoredCategory = categoryWithLastChannel
            ?: category?.takeIf { it in orderedCategories }
            ?: lastLiveCategory?.takeIf { it in orderedCategories }
            ?: orderedCategories.firstOrNull()
        category = restoredCategory
        lastLiveCategory = restoredCategory
        if (restoredCategory != null) {
            val visibleChannels = liveCategories[restoredCategory].orEmpty()
            val index = visibleChannels.indexOfFirst { channel ->
                (programmeKey(channel) ?: liveGroups[channel]?.identity) == savedKey
            }
            if (index >= 0) {
                guideScroll.requestScrollToItem(index)
                programmeChannel = visibleChannels[index]
                restoreLiveFocus = true
                if (playLastChannelAtStartup) playChannel(visibleChannels[index])
            } else {
                guideScroll.requestScrollToItem(0)
                programmeChannel = visibleChannels.firstOrNull()
            }
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
        if (!source.enabled || connecting || source.identity == storedSource?.identity) return
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

    fun vodVariantCountry(section: PortalSection, item: SourceItem<PortalTitle>): String {
        val group = item.value.categoryId?.let { id ->
            item.source.catalogs[section]?.groups?.firstOrNull { it.id == id }
        }
        return categoryCountry(group?.name ?: item.value.categoryName.orEmpty(), group?.country)
    }

    /**
     * Drops variants from provider categories of countries outside the selection (for
     * example an English or Dutch series version) and orders the rest by country priority.
     */
    fun countryVariants(section: PortalSection, variants: List<SourceItem<PortalTitle>>): List<SourceItem<PortalTitle>> {
        if (!combine || countries.isEmpty()) return variants
        return variants.map { it to vodVariantCountry(section, it) }
            .filter { (_, country) -> country == UNKNOWN_COUNTRY || country in countries }
            .sortedBy { (_, country) -> countries.indexOf(country).takeIf { it >= 0 } ?: countries.size }
            .map { it.first }
    }

    fun titleEntries(section: PortalSection, title: PortalTitle): List<SourceItem<PortalTitle>> {
        val known = searchedVariants[title] ?: groupVariants[title]
        if (known != null) return countryVariants(section, known).ifEmpty { known.take(1) }
        val source = storedSource ?: return emptyList()
        if (!combine) return listOf(SourceItem(source, title))
        val entries = listOf(SourceItem(source, title)) + sourcesForBrowsing
            .filter { it.identity != source.identity }.mapNotNull { other ->
                equivalentCatalogueTitle(
                    title, other.catalogs[section]?.titles.orEmpty(), requireVerifiedIdentity = section == PortalSection.SERIES
                )
                    ?.let { SourceItem(other, it) }
            }
        return countryVariants(section, entries).ifEmpty { entries.take(1) }
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

    fun hasPortalReference(
        section: PortalSection, reference: PortalTitle, refs: List<SeriesPortalRef>
    ): Boolean = refs.any { ref ->
        if (ref.portalTitleId.isBlank()) return@any false
        val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: return@any false
        val cached = source.catalogs[section]?.titles?.firstOrNull { it.id == ref.portalTitleId }
        if (cached == null) true else equivalentCatalogueTitle(
            reference,
            listOf(cached), requireVerifiedIdentity = section == PortalSection.SERIES
        ) != null
    }

    val historyArtworkRequests = remember { Semaphore(3) }
    // Keep multi-portal season requests bounded; a catalog can contain thousands
    // of episodes and must not open one network request per source at once.
    val episodeCatalogRequests = remember { Semaphore(2) }
    @Composable
    fun HistoryArtwork(section: PortalSection, title: PortalTitle, refs: List<SeriesPortalRef>) {
        val artwork by produceState<PortalTitle?>(null, section, title, refs, savedSources) {
            value = null
            for (ref in refs) {
                val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: continue
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
        Poster(artwork ?: title, Modifier.width(104.dp).padding(end = 10.dp))
    }

    fun titleSelection(
        section: PortalSection, title: PortalTitle, season: PortalSeason? = openedSeason
    ): PlaybackSelection {
        val root = openedSeries.takeIf { section == PortalSection.SERIES && season != null }
        val canonical = titleIdentity(root ?: title)
        val source = titleEntries(section, root ?: title).firstOrNull()?.source?.identity.orEmpty()
        val seriesId = root?.let { titleIdentity(it) ?: it.id ?: it.name }
        val id = if (root != null) {
            val seasonNumber = season?.number ?: Regex("\\d+").find(season?.title?.name.orEmpty())?.value
            val rawEpisode = title.episode.orEmpty()
            val episode = Regex("\\d+").findAll(rawEpisode).lastOrNull()?.value?.toIntOrNull()?.toString()
                ?: title.id ?: title.name
            "${seasonNumber ?: season?.title?.name}:$episode"
        } else canonical ?: title.id ?: title.name
        return PlaybackSelection(
            if (canonical != null) "catalogue" else source, section, id, title.episode, seriesId
        )
    }

    var watchStatusAction by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    fun isPlaybackCompleted(key: String, fallback: Boolean = false): Boolean {
        val prefs = progressPreferences ?: return fallback
        return prefs.isPlaybackCompleted(key, fallback)
    }
    fun setWatched(section: PortalSection, title: PortalTitle, episodes: List<Pair<PortalSeason, PortalTitle>>, watched: Boolean) {
        if (sendingWatchStatus || traktBusy) return
        val prefs = progressPreferences ?: return
        val keys = if (section == PortalSection.MOVIES) {
            setOf(titleSelection(section, title).progressKey) + movieHistory.filter { movie ->
                movie.refs.any { ref -> titleEntries(section, title).any { it.source.identity == ref.sourceId && it.value.id == ref.portalTitleId } }
            }.map { it.progressKey }
        } else episodes.mapTo(linkedSetOf()) { (season, episode) -> titleSelection(section, episode, season).progressKey }
        val previous = if (section == PortalSection.SERIES) trackedSeries(title) else null
        val editor = prefs.edit()
        (keys + listOfNotNull(previous?.lastEpisode?.progressKey?.takeIf {
            episodes.any { (season, episode) -> seriesEpisodeKey(season, episode) == previous.lastEpisode?.episodeKey }
        })).forEach { key ->
            editor.putBoolean("manual_watched:$key", watched).putBoolean("completed:$key", watched)
                .remove(key).remove("trakt_percent:$key").putLong("progress_updated:$key", System.currentTimeMillis())
        }
        editor.apply()
        if (section == PortalSection.MOVIES) {
            val remaining = movieHistory.filterNot { it.progressKey in keys }
            val added = if (watched) listOf(WatchedMovie(
                displayVodTitle(title), vodReleaseYear(title), title.imdbId, title.tmdbId,
                titleEntries(section, title).mapNotNull { item -> item.value.id?.let { SeriesPortalRef(item.source.identity, it) } },
                titleSelection(section, title).progressKey, System.currentTimeMillis()
            )) else emptyList()
            movieHistory = (added + remaining).take(10)
            prefs.edit().putString("movie_history", encodeMovieHistory(movieHistory)).apply()
        } else if (episodes.isNotEmpty()) {
            val episodeKeys = episodes.mapTo(linkedSetOf()) { (season, episode) -> seriesEpisodeKey(season, episode) }
            val last = episodes.last()
            val lastResume = EpisodeResume(
                seriesEpisodeKey(last.first, last.second), last.first.number, last.second.episode,
                last.second.name, titleSelection(section, last.second, last.first).progressKey
            )
            if (watched && previous == null) updateTrackedSeries(title, watchedEpisode = lastResume)
            val record = previous ?: trackedSeries(title)
            if (record != null) {
                val completed = if (watched) record.watchedEpisodes + episodeKeys else record.watchedEpisodes - episodeKeys
                val allEpisodes = seasons.orEmpty().flatMap { season -> season.episodes.map { seriesEpisodeKey(season, it) } }.toSet()
                val lastEpisode = if (watched) lastResume else record.lastEpisode?.takeUnless { it.episodeKey in episodeKeys }
                saveSeriesActivity(seriesActivity.map {
                    if (it.key != record.key) it else it.copy(
                        viewed = watched || completed.isNotEmpty() || lastEpisode != null,
                        localViewed = watched || it.localViewed && (completed.isNotEmpty() || lastEpisode != null),
                        hiddenFromHistory = it.hiddenFromHistory && !watched,
                        watchedEpisodes = completed,
                        unseen = it.unseen - completed,
                        watchedAt = if (watched) System.currentTimeMillis() else it.watchedAt,
                        lastEpisode = lastEpisode,
                        isComplete = allEpisodes.isNotEmpty() && allEpisodes.all { it in completed }
                    )
                })
            }
        }
        val account = traktSession?.username ?: return
        val remoteTitle = title.copy(imdbId = title.imdbId ?: previous?.imdb, tmdbId = title.tmdbId ?: previous?.tmdb)
        val numberedEpisodes = episodes.mapNotNull { (season, episode) ->
            val (seasonKey, episodeKey) = seriesEpisodeKey(season, episode).split(':', limit = 2)
            val seasonNumber = seasonKey.toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val episodeNumber = episodeKey.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            seasonNumber to episodeNumber
        }
        sendingWatchStatus = true
        scope.launch {
            try {
                val accepted = numberedEpisodes.size == episodes.size && withTraktSession { api, session ->
                    session.username == account && api.setHistoryStatus(
                        session.accessToken, section, remoteTitle, numberedEpisodes.toSet(), watched
                    )
                }
                traktPlaybackNotice = context.getString(when {
                    !accepted -> R.string.trakt_manual_history_failed
                    watched -> R.string.trakt_manual_history_added
                    else -> R.string.trakt_manual_history_removed
                })
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                traktPlaybackNotice = context.getString(R.string.trakt_manual_history_failed)
            } finally {
                sendingWatchStatus = false
            }
        }
    }

    watchStatusAction?.let { update ->
        AlertDialog(
            onDismissRequest = { watchStatusAction = null },
            title = { Text(stringResource(R.string.watched_status)) },
            text = if (traktSession != null) {
                { Text(stringResource(R.string.trakt_manual_history_description)) }
            } else null,
            confirmButton = {
                MaterialTextButton(enabled = !sendingWatchStatus && !traktBusy, onClick = { update(true); watchStatusAction = null }) {
                    Text(stringResource(R.string.mark_watched))
                }
            },
            dismissButton = {
                MaterialTextButton(enabled = !sendingWatchStatus && !traktBusy, onClick = { update(false); watchStatusAction = null }) {
                    Text(stringResource(R.string.mark_unwatched))
                }
            }
        )
    }

    fun showTitle(section: PortalSection, title: PortalTitle) {
        if (reopenPlayback(
                titleSelection(section, title),
                matchSeries = section == PortalSection.SERIES
            )
        ) return
        closeSeries()
        focusTitlePrimary = true
        if (section == PortalSection.SERIES) seriesDetailScroll.requestScrollToItem(0)
        details = section to title
        if (section == PortalSection.SERIES && isTitleSaved(section, title) && trackedSeries(title) == null) {
            updateTrackedSeries(title, saved = true)
        }
    }

    fun openTrackedSeries(entry: TrackedSeries, onOpen: ((PortalTitle) -> Unit)? = null) {
        val reference = PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
        val linkedVariants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.SERIES]?.titles?.firstOrNull { it.id == ref.portalTitleId }
                ?: reference.copy(id = ref.portalTitleId)
            SourceItem(source, title)
        }
        val discoveredVariants = if (linkedVariants.isEmpty()) sourcesForBrowsing.flatMap { source ->
            source.catalogs[PortalSection.SERIES]?.titles.orEmpty().mapNotNull { candidate ->
                equivalentCatalogueTitle(reference, listOf(candidate), requireVerifiedIdentity = false)
                    ?.let { SourceItem(source, it) }
            }
        } else emptyList()
        val allVariants = (linkedVariants + discoveredVariants).distinctBy { "${it.source.identity}:${it.value.id}" }
        // Prefer versions from the selected countries; a history entry may also link to
        // a foreign-language category of the same portal.
        val variants = countryVariants(PortalSection.SERIES, allVariants).ifEmpty { allVariants }
        val primary = variants.firstOrNull {
            equivalentCatalogueTitle(reference, listOf(it.value), requireVerifiedIdentity = true) != null
        } ?: variants.firstOrNull() ?: run {
            traktPlaybackNotice = context.getString(R.string.trakt_source_missing)
            return
        }
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
                                    episodeCatalogRequests.withPermit {
                                        val item = known[source.identity]?.value
                                            ?: findEquivalent(source, section, title) ?: return@withPermit emptyList()
                                        val portal = sessionFor(source)
                                        portal.loadSeasons(item, portal.openSession()).map { SourceItem(source, it) }
                                    }
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
                    AppDiagnostics.record(context, "series_catalog_load_failed")
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
                currentTraktPlayback = when (section) {
                    PortalSection.MOVIES -> TraktMedia(
                        PortalSection.MOVIES, displayVodTitle(title), title.year, title.imdbId, title.tmdbId, null
                    )
                    PortalSection.SERIES -> openedSeries?.let { show ->
                        TraktMedia(
                            PortalSection.SERIES, displayVodTitle(show), show.year, show.imdbId, show.tmdbId, null,
                            seasonNumber = openedSeason?.number,
                            episodeNumber = title.episode?.toIntOrNull()
                        )
                    }
                    PortalSection.LIVE -> null
                }
                if (section == PortalSection.SERIES) {
                    playbackMovieTitle = null
                    previousEpisodeAction = null
                    nextEpisodeAction = null
                    nextEpisodeSeconds = null
                    val series = openedSeries
                    val season = openedSeason
                    if (series != null && season != null) {
                        pendingSeriesEpisode = series to EpisodeResume(
                            seriesEpisodeKey(season, title), season.number, title.episode, title.name, key
                        )
                        val seasonList = seasons.orEmpty()
                        val availableVariants = episodeVariants
                        playbackSeries = series
                        playbackSeasons = seasonList
                        playbackSeason = season
                        playbackEpisode = title
                        overlaySeason = season
                        playbackEpisodeVariants = availableVariants
                        precedingEpisode(seasonList, season, title)?.let { (previousSeason, previous) ->
                            previousEpisodeAction = {
                                openedSeries = series
                                openedSeason = previousSeason
                                seasons = seasonList
                                episodeVariants = availableVariants
                                selectTitle(PortalSection.SERIES, previous)
                            }
                        } ?: run { previousEpisodeAction = null }
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
                    playbackMovieTitle = title
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
    val episodeMetadata by produceState(
        emptyMap<Pair<Int, Int>, TmdbEpisodeDetails>(), detailMetadata, seasons, metadataClient,
        details, traktSession?.username
    ) {
        value = emptyMap()
        val seriesTitle = details?.takeIf { it.first == PortalSection.SERIES }?.second
            ?: return@produceState
        if (traktSession != null) {
            val summaries = try {
                withTraktSession { api, session ->
                    api.episodeDescriptions(seriesTitle, session.accessToken)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyMap()
            }
            value = summaries.mapValues { (_, description) -> TmdbEpisodeDetails(description, null) }
        }
        val client = metadataClient ?: return@produceState
        val series = detailMetadata?.takeIf { details?.first == PortalSection.SERIES } ?: return@produceState
        // Fetch TMDB stills and localized overviews even when the portal supplied
        // a generic poster or a non-German episode description.
        val seasonNumbers = seasons.orEmpty().mapNotNull { it.number }.distinct()
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
            value = value + loaded.map { (key, episode) ->
                key to episode.copy(description = germanDescription(episode.description) ?: value[key]?.description)
            }
        }
    }

    fun openWatchedMovie(entry: WatchedMovie, resume: Boolean) {
        val reference = PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
        val linkedVariants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.MOVIES]?.titles?.firstOrNull {
                ref.portalTitleId.isNotBlank() && it.id == ref.portalTitleId
            } ?: PortalTitle(
                entry.name, id = ref.portalTitleId.ifBlank { null }, year = entry.year,
                imdbId = entry.imdb, tmdbId = entry.tmdb
            )
            SourceItem(source, title)
        }
        val discoveredVariants = if (linkedVariants.isEmpty()) sourcesForBrowsing.flatMap { source ->
            source.catalogs[PortalSection.MOVIES]?.titles.orEmpty().mapNotNull { candidate ->
                equivalentCatalogueTitle(reference, listOf(candidate), requireVerifiedIdentity = false)
                    ?.let { SourceItem(source, it) }
            }
        } else emptyList()
        val allVariants = (linkedVariants + discoveredVariants).distinctBy { "${it.source.identity}:${it.value.id}" }
        val variants = countryVariants(PortalSection.MOVIES, allVariants).ifEmpty { allVariants }
        val title = variants.firstOrNull()?.value ?: run {
            traktPlaybackNotice = context.getString(R.string.trakt_source_missing)
            return
        }
        searchedVariants = mapOf(title to variants)
        if (resume) {
            details = PortalSection.MOVIES to title
            selectTitle(PortalSection.MOVIES, title, progressOverride = entry.progressKey)
        } else showTitle(PortalSection.MOVIES, title)
    }

    fun portalRefreshKey(identity: String) = "portal_full_refresh_at_$identity"

    fun markPortalFullRefresh(identity: String) {
        preferences?.edit()?.putLong(portalRefreshKey(identity), System.currentTimeMillis())?.apply()
    }

    fun portalFullRefreshDue(source: SavedPortal): Boolean {
        val key = portalRefreshKey(source.identity)
        var lastRefresh = preferences?.getLong(key, 0L) ?: 0L
        if (lastRefresh == 0L && (source.channels.isNotEmpty() || source.catalogs.isNotEmpty())) {
            // Existing installs did not persist a refresh timestamp. Treat their saved
            // catalogue as a fresh snapshot once so upgrading does not trigger a cold scan.
            markPortalFullRefresh(source.identity)
            lastRefresh = System.currentTimeMillis()
        }
        return lastRefresh == 0L || System.currentTimeMillis() - lastRefresh >= 6 * 60 * 60 * 1_000L
    }

    suspend fun refreshPortal(enteredAddress: String, enteredMac: String, activate: Boolean): Boolean {
        AppDiagnostics.record(context, "portal_refresh_started")
        sourceError = null
        importStatuses = emptyMap()
        try {
            val source = StalkerPortal(enteredAddress, enteredMac)
            val token = source.openSession()
            val previous = savedSources.firstOrNull { candidate ->
                candidate.address == enteredAddress && runCatching {
                    normalizePortalMac(candidate.mac) == normalizePortalMac(enteredMac)
                }.getOrDefault(false)
            }
            val sourceIdentity = SavedPortal(
                enteredAddress, enteredMac, emptyList(), emptyMap(), emptyMap()
            ).identity
            var refreshedChannels = previous?.channels.orEmpty()
            var refreshedGenres = previous?.genres.orEmpty()
            var refreshedCatalogs = previous?.catalogs.orEmpty()
            // LIVE, MOVIES and SERIES load concurrently. Their results and progress
            // must be committed as one snapshot at a time so one section cannot
            // overwrite another section's newer in-memory catalogue.
            val snapshotMutex = Mutex()
            val keepEnabled = activate || previous?.enabled != false || previous?.autoDisabled == true
            val initialSnapshot = SavedPortal(
                enteredAddress, enteredMac, refreshedChannels, refreshedGenres, refreshedCatalogs,
                previous?.epgChannelIds.orEmpty(), previous?.subscriptionExpiry,
                enabled = keepEnabled, reachable = true, autoDisabled = false
            )
            // Persist the source as soon as its portal session is verified. A process exit
            // during a slow catalogue load must not make a newly added source disappear.
            persistPortalSnapshot(initialSnapshot)
            suspend fun persistProgress() {
                snapshotMutex.withLock {
                    persistPortalSnapshot(SavedPortal(
                        enteredAddress, enteredMac, refreshedChannels, refreshedGenres, refreshedCatalogs,
                        previous?.epgChannelIds.orEmpty(), previous?.subscriptionExpiry,
                        enabled = keepEnabled, reachable = true, autoDisabled = false
                    ))
                }
            }
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
                                        snapshotMutex.withLock {
                                            importStatuses = importStatuses +
                                                (section to ImportStatus.Progress(loaded, total))
                                        }
                                    }
                                }
                                imported to labels.await()
                            }
                            snapshotMutex.withLock {
                                refreshedChannels = if (imported.partial) {
                                    (imported.channels + refreshedChannels).distinctBy { it.id ?: it.command }
                                } else imported.channels
                                refreshedGenres = refreshedGenres + labels
                            }
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
                            val retainedCatalog = previous?.catalogs?.get(section)
                            val mergedCatalog = if (retainedCatalog == null) catalog else catalog.copy(
                                titles = (catalog.titles + retainedCatalog.titles).distinctBy {
                                    it.id ?: it.command ?: "${normalizedLabel(it.name)}:${it.year ?: 0}"
                                },
                                groups = catalog.groups.ifEmpty { retainedCatalog.groups }
                            )
                            snapshotMutex.withLock {
                                refreshedCatalogs = refreshedCatalogs + (section to mergedCatalog)
                            }
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
                    snapshotMutex.withLock {
                        importStatuses = importStatuses + (section to status)
                    }
                    if (status is ImportStatus.Count || status is ImportStatus.Loaded || status is ImportStatus.Partial) {
                        persistProgress()
                    }
                }
            }
            val changed = importStatuses.values.any {
                it is ImportStatus.Count || it is ImportStatus.Loaded || it is ImportStatus.Partial
            }
            if (!changed) {
                updatePortalStatus(sourceIdentity, reachable = true, enabled = if (activate) true else null)
                return false
            }
            val snapshot = SavedPortal(
                enteredAddress, enteredMac, refreshedChannels, refreshedGenres,
                refreshedCatalogs,
                savedSources.firstOrNull { it.identity == previous?.identity }?.epgChannelIds.orEmpty(),
                previous?.subscriptionExpiry ?: source.loadSubscriptionExpiry(token),
                enabled = keepEnabled,
                reachable = true,
                autoDisabled = false
            )
            val sameSource = snapshot.identity == storedSource?.identity
            val unchanged = previous != null &&
                previous.channels.map { channel ->
                    listOf(
                        channel.name, channel.command, channel.genreId, channel.genreName, channel.id,
                        channel.logo, channel.direct, channel.epgIdentity, channel.number
                    )
                } == snapshot.channels.map { channel ->
                    listOf(
                        channel.name, channel.command, channel.genreId, channel.genreName, channel.id,
                        channel.logo, channel.direct, channel.epgIdentity, channel.number
                    )
                } && previous.genres == snapshot.genres && previous.catalogs == snapshot.catalogs &&
                previous.subscriptionExpiry == snapshot.subscriptionExpiry
            if (unchanged) {
                val old = requireNotNull(previous)
                val unchangedSnapshot = SavedPortal(
                    old.address, old.mac, old.channels, old.genres, old.catalogs,
                    old.epgChannelIds, old.subscriptionExpiry,
                    enabled = activate || old.enabled || old.autoDisabled,
                    reachable = true,
                    autoDisabled = false
                )
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
                    liveSession = source to token
                    catalogSession = source to token
                    catalogLoading = emptySet()
                    catalogErrors = emptySet()
                    channels = unchangedSnapshot.channels
                    genres = unchangedSnapshot.genres
                    catalogs = unchangedSnapshot.catalogs
                    epgChannelIds = unchangedSnapshot.epgChannelIds
                    save(unchangedSnapshot, activate)
                } else {
                    updatePortalStatus(sourceIdentity, reachable = true)
                }
                AppDiagnostics.record(context, "portal_refresh_unchanged")
                val complete = importStatuses.values.all { it is ImportStatus.Count || it is ImportStatus.Loaded }
                if (complete) markPortalFullRefresh(sourceIdentity)
                return complete
            }
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
            AppDiagnostics.record(context, "portal_refresh_completed")
            val complete = importStatuses.values.all { it is ImportStatus.Count || it is ImportStatus.Loaded } &&
                storageError == null
            if (complete) markPortalFullRefresh(sourceIdentity)
            return complete
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppDiagnostics.record(context, "portal_refresh_failed:${error.javaClass.simpleName}")
            val identity = runCatching {
                SavedPortal(enteredAddress, enteredMac, emptyList(), emptyMap(), emptyMap()).identity
            }.getOrNull()
            if (identity != null) {
                try {
                    updatePortalStatus(identity, reachable = false)
                } catch (statusError: CancellationException) {
                    throw statusError
                } catch (_: Exception) {
                    AppDiagnostics.record(context, "portal_status_update_failed")
                }
            }
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

    fun openSourceRefreshDialog() {
        selectedRefreshSources = emptySet()
        sourceRefreshDialogOpen = true
    }

    fun refreshSources(sources: List<SavedPortal>) {
        if (connecting || sources.isEmpty()) return
        connecting = true
        batchSummary = null
        scope.launch {
            var completed = 0
            val unavailable = mutableListOf<String>()
            try {
                // Let the initial background check finish first. The UI stays usable and
                // the manual request is queued instead of silently discarded.
                while (startupPortalRefresh != null || startupPortalCheck != null) delay(200)
                for ((index, source) in sources.withIndex()) {
                    batchProgress = (index + 1) to sources.size
                    if (refreshPortal(source.address, source.mac, activate = false)) completed++
                    if (savedSources.firstOrNull { it.identity == source.identity }?.reachable == false) {
                        unavailable += runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                            .ifBlank { "Portal" }
                    }
                }
                batchSummary = completed to (sources.size - completed)
                val reachableCount = sources.count { source ->
                    savedSources.firstOrNull { it.identity == source.identity }?.reachable == true
                }
                portalRefreshNotice = if (unavailable.isEmpty()) {
                    context.getString(R.string.portals_refresh_success, reachableCount)
                } else context.getString(R.string.portals_refresh_with_unavailable, reachableCount, unavailable.joinToString(", "))
                if (completed > 0 && automaticCountries) askForCountries = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
            AppDiagnostics.record(context, "portal_batch_refresh_failed:${error.javaClass.simpleName}")
                portalRefreshNotice = context.getString(R.string.portals_refresh_failed)
            } finally {
                batchProgress = null
                connecting = false
            }
        }
    }

    LaunchedEffect(restoring, refreshPortalsAtStartup, liveGroupsReady) {
        if (restoring || startupPortalRefreshStarted || savedSources.isEmpty()) return@LaunchedEffect
        startupPortalRefreshStarted = true
        val sources = savedSources.filter { it.enabled || it.autoDisabled }
        val unavailable = mutableListOf<String>()
        var fullRefreshPerformed = false
        try {
            for ((index, source) in sources.withIndex()) {
                if (refreshPortalsAtStartup) {
                    if (portalFullRefreshDue(source)) {
                        fullRefreshPerformed = true
                        startupPortalRefresh = (index + 1) to sources.size
                        refreshPortal(source.address, source.mac, activate = false)
                    } else {
                        startupPortalCheck = (index + 1) to sources.size
                        try {
                            sessionFor(source).openSession()
                            updatePortalStatus(source.identity, reachable = true)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            updatePortalStatus(source.identity, reachable = false)
                        }
                    }
                } else {
                    startupPortalCheck = (index + 1) to sources.size
                    try {
                        sessionFor(source).openSession()
                        updatePortalStatus(source.identity, reachable = true)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        updatePortalStatus(source.identity, reachable = false)
                    }
                }
                if (savedSources.firstOrNull { it.identity == source.identity }?.reachable == false) {
                    unavailable += runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                        .ifBlank { "Portal" }
                }
                startupPortalRefresh = null
                startupPortalCheck = null
            }
            if (sources.isNotEmpty()) {
                val reachableCount = sources.count { source ->
                    savedSources.firstOrNull { it.identity == source.identity }?.reachable == true
                }
                if (fullRefreshPerformed) {
                    portalRefreshNotice = if (unavailable.isEmpty()) {
                        context.getString(R.string.portals_refresh_success, reachableCount)
                    } else context.getString(R.string.portals_refresh_with_unavailable, reachableCount, unavailable.joinToString(", "))
                } else if (unavailable.isNotEmpty()) {
                    portalRefreshNotice = context.getString(
                        R.string.portals_refresh_with_unavailable, reachableCount, unavailable.joinToString(", ")
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppDiagnostics.record(context, "startup_portal_refresh_failed:${error.javaClass.simpleName}")
            portalRefreshNotice = context.getString(R.string.portals_refresh_failed)
        } finally {
            startupPortalCheck = null
            startupPortalRefresh = null
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
                            saved.address, saved.mac, channels, genres, catalogs, epgChannelIds,
                            saved.subscriptionExpiry, saved.enabled, saved.reachable, saved.autoDisabled
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
                    sendTraktScrobble("start")
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
                                val key = attempt.liveRestoreKey
                                val variants = liveGroups[attempt.channel]?.variants
                                if (key != null && !variants.isNullOrEmpty() && storage != null) {
                                    val snapshot = StartupChannel(
                                        key, attempt.selection.source, attempt.selection.id, startupChannelScope(),
                                        attempt.channel.name, variants.take(8).map { (item, _) ->
                                            val stream = item.value
                                            StartupStream(item.source.address, item.source.mac, stream.id, stream.command, stream.direct)
                                        }
                                    )
                                    scope.launch {
                                        try {
                                            storage.writeStartupChannel(snapshot)
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            // The normal catalogue restore remains available.
                                        }
                                    }
                                }
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
                } else {
                    savePlaybackPosition(onFailure = true)
                    if (player?.playWhenReady == false) sendTraktScrobble("pause")
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                val attempt = playbackAttempt ?: return
                if (playbackState == Player.STATE_READY && player?.currentMediaItem?.mediaId == attempt.mediaId) {
                    val key = attempt.progressKey
                    val active = player
                    val percent = key?.let { progressPreferences?.getFloat("trakt_percent:$it", -1f) } ?: -1f
                    if (key != null && active.duration > 0 && percent in 0.1f..100f) {
                        val target = (active.duration * percent / 100f).toLong()
                            .coerceIn(0L, (active.duration - 1_000L).coerceAtLeast(0L))
                        progressPreferences?.edit()?.putLong(key, target)
                            ?.putLong("duration:$key", active.duration)
                            ?.putLong("progress_updated:$key", System.currentTimeMillis())
                            ?.remove("trakt_percent:$key")?.apply()
                        active.seekTo(target)
                    }
                }
                if (playbackState == Player.STATE_ENDED && player?.currentMediaItem?.mediaId == attempt.mediaId) {
                    if (attempt.progressKey == null) {
                        scope.launch { failPlayback(attempt, R.string.playback_error) }
                    } else {
                        progressPreferences?.edit()?.remove(attempt.progressKey)
                            ?.putBoolean("completed:${attempt.progressKey}", true)?.apply()
                        if (attempt.selection.section == PortalSection.SERIES && nextEpisodeAction == null) {
                            stopPlayback()
                        }
                    }
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
    LaunchedEffect(playbackAttempt, player) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val activePlayer = player ?: return@LaunchedEffect
        var hasPlayed = activePlayer.playbackState == Player.STATE_READY
        var bufferingSince: Long? = null
        var countedThisBuffer = false
        var prolongedBufferEpisodes = 0
        while (playbackAttempt === attempt) {
            val now = SystemClock.elapsedRealtime()
            if (activePlayer.playbackState == Player.STATE_READY) hasPlayed = true
            if (hasPlayed && canPlay() && activePlayer.playWhenReady &&
                activePlayer.playbackState == Player.STATE_BUFFERING
            ) {
                if (bufferingSince == null) {
                    bufferingSince = now
                    countedThisBuffer = false
                }
                if (!countedThisBuffer && now - bufferingSince!! >= 4_000) {
                    countedThisBuffer = true
                    prolongedBufferEpisodes++
                    if (prolongedBufferEpisodes >= 3) {
                        failPlayback(attempt, R.string.playback_timeout)
                        return@LaunchedEffect
                    }
                }
            } else if (activePlayer.playbackState != Player.STATE_BUFFERING) {
                bufferingSince = null
                countedThisBuffer = false
            }
            delay(250)
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
    var playbackControlsVisible by remember { mutableStateOf(false) }
    var dismissPlaybackControls by remember { mutableIntStateOf(0) }
    var overlayInteraction by remember { mutableIntStateOf(0) }
    var overlayScheduleFocused by remember { mutableStateOf(false) }
    var guideProgramme by remember { mutableStateOf<Pair<StalkerChannel, PortalProgramme>?>(null) }
    val guideNow by produceState(System.currentTimeMillis() / 1_000) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis() / 1_000
        }
    }
    fun guideSelection(channel: StalkerChannel): PortalProgramme? = resolveGuideProgramme(
        effectiveProgrammes(channel).orEmpty(),
        guideProgramme?.takeIf { it.first === channel }?.second,
        guideNow
    )
    val guidePanelFocus = remember { FocusRequester() }
    val guideChannelFocus = remember { FocusRequester() }
    val destinationFocusRequesters = remember {
        mainDestinations.associateWith { FocusRequester() }
    }
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
    fun guideChannelKey(channel: StalkerChannel): String {
        val description = liveGroups[channel]?.description ?: describeChannel(channel, channel.genreName)
        return channelIdentity(channel) ?: if (description.country != UNKNOWN_COUNTRY) {
            "station:${description.country}:${description.normalizedName}"
        } else {
            "entry:${channel.id ?: channel.command}:${description.normalizedName}"
        }
    }
    fun restoreGuideChannel(channel: StalkerChannel) {
        val key = guideChannelKey(channel)
        val entry = liveCategories.entries.firstNotNullOfOrNull { (group, items) ->
            items.firstOrNull { guideChannelKey(it) == key }?.let { group to it }
        }
        if (entry == null) {
            programmeChannel = channel
        } else {
            category = entry.first
            lastLiveCategory = entry.first
            programmeChannel = entry.second
            val index = liveCategories.getValue(entry.first).indexOf(entry.second)
            if (index >= 0) guideScroll.requestScrollToItem(index)
        }
        restoreLiveFocus = true
    }
    fun restoreGuideEntryFocus() {
        val savedKey = preferences?.getString("last_live_channel", null)
        val channel = playing.takeIf { vodSection == null } ?: savedKey?.let { key ->
            liveGroups.keys.firstOrNull { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
        }
        if (channel != null) restoreGuideChannel(channel) else restoreLiveFocus = true
    }
    fun navigateGuide(input: GuideInput) {
        if (input in setOf(GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT) &&
            guidePanel == GuidePanel.CATEGORIES
        ) {
            destinationFocusRequesters.getValue(Destination.LIVE).requestFocus()
            return
        }
        guidePanel = guidePanel.afterProgrammeGuide(input)
    }
    fun showFullscreenPanel(panel: GuidePanel) {
        if (panel != fullscreenGuidePanel) overlayScheduleFocused = false
        if (panel != GuidePanel.GRID) fullscreenOverlayContent = panel
        fullscreenGuidePanel = panel
    }
    LaunchedEffect(
        fullscreen, fullscreenGuidePanel, guideAppearance.closeSeconds, overlayInteraction,
        categoryMenu, vodSection, watchingPrompt, nextEpisodeSeconds, skipIntroTo
    ) {
        if (!fullscreen) showFullscreenPanel(GuidePanel.GRID)
        else if ((vodSection == null || vodSection == PortalSection.SERIES && playbackSeries != null) &&
            fullscreenGuidePanel != GuidePanel.GRID && guideAppearance.closeSeconds > 0 &&
            categoryMenu == null && !watchingPrompt && nextEpisodeSeconds == null && skipIntroTo == null
        ) {
            delay(guideAppearance.closeSeconds * 1_000L)
            if (fullscreenGuidePanel != GuidePanel.GRID && !watchingPrompt &&
                nextEpisodeSeconds == null && skipIntroTo == null
            ) showFullscreenPanel(GuidePanel.GRID)
        }
    }
    fun isMainMenuOpen() = selected == Destination.LIVE && guidePanel == GuidePanel.MENU && !fullscreen
    val mainMenuOpen = isMainMenuOpen()
    LaunchedEffect(mainMenuOpen) { if (!mainMenuOpen) lastBack = 0L }
    fun openSettings() {
        val current = if (isMainMenuOpen()) lastFocused else selected ?: lastFocused
        settingsReturnDestination = current.takeUnless {
            it == Destination.SETTINGS || it == Destination.SOURCES || it == Destination.CATEGORIES
        } ?: Destination.LIVE
        settingsSection = null
        guidePanel = GuidePanel.GRID
        lastBack = 0L
        selected = Destination.SETTINGS
    }
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
        if (vodSection == PortalSection.SERIES && playbackSeries != null && selected == Destination.SERIES) {
            openedSeries = playbackSeries
            openedSeason = playbackSeason
            seasons = playbackSeasons
            episodeVariants = playbackEpisodeVariants
        }
        restoreLiveFocus = vodSection == null && (selected == null || selected == Destination.LIVE)
        if (restoreLiveFocus && inGuide) {
            playing?.let(::restoreGuideChannel)
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
    fun requestAppExit() {
        val now = SystemClock.elapsedRealtime()
        if (lastBack != 0L && now - lastBack <= EXIT_CONFIRMATION_MS) {
            savePlaybackPosition()
            onExit()
        } else {
            lastBack = now
        }
    }
    fun navigateBack() {
        if (fullscreen) {
            if (watchingPrompt) {
                stopPlayback()
                return
            }
            if (playbackControlsVisible) {
                dismissPlaybackControls++
                return
            }
            if (fullscreenGuidePanel != GuidePanel.GRID) {
                showFullscreenPanel(
                    if (vodSection == PortalSection.SERIES) fullscreenGuidePanel.afterSeriesOverlay(GuideInput.BACK)
                    else GuidePanel.GRID
                )
                return
            }
            leaveFullscreen()
        } else if (isMainMenuOpen()) {
            requestAppExit()
        } else if (inGuide) {
            if (guidePanel == GuidePanel.GRID) requestAppExit()
            else guidePanel = GuidePanel.GRID
        } else if (selected == Destination.SETTINGS && settingsSection != null) settingsSection = null
        else if (selected == Destination.SETTINGS) {
            selected = settingsReturnDestination
            guidePanel = GuidePanel.GRID
            lastBack = 0L
            if (selected == Destination.LIVE) restoreGuideEntryFocus()
        } else if (selected == null && category != null) returnToCategories()
        else if (selected == Destination.SERIES && openedSeason != null) openedSeason = null
        else if (selected == Destination.SERIES && openedSeries != null) {
            closeSeries()
            details = null
        }
        else if (details != null && selected in listOf(Destination.SERIES, Destination.MOVIES)) {
            details = null
            movieHistoryOpen = false
            movieWatchlistOpen = false
            if (selected != Destination.SERIES) seriesActivityView = null
        }
        else if (selected == Destination.SERIES && seriesActivityView != null) seriesActivityView = null
        else if (selected == Destination.MOVIES && movieHistoryOpen) movieHistoryOpen = false
        else if (selected == Destination.MOVIES && movieWatchlistOpen) movieWatchlistOpen = false
        else if (selected == Destination.SOURCES || selected == Destination.CATEGORIES) selected = Destination.SETTINGS
        else if ((selected == Destination.MOVIES && selectedGroup?.first == PortalSection.MOVIES) ||
            (selected == Destination.SERIES && selectedGroup?.first == PortalSection.SERIES)
        ) closeGroup()
        else if (selected == Destination.MOVIES || selected == Destination.SERIES) requestAppExit()
        else openMainMenu()
    }
    var playerControlsRequest by remember { mutableIntStateOf(0) }
    fun changeLiveChannel(step: Int): Boolean {
        val groups = withCategoryOrder(PortalSection.LIVE, liveCategories.keys
            .filter { visibleCategory(PortalSection.LIVE, it, it) }
            .sortedBy { categoryNames.values.indexOf(it) }) { it }
        val currentChannel = if (vodSection == null) starting ?: playing else null
        val group = guideChannelCategory(category, groups, liveCategories, currentChannel)
        val channels = liveCategories[group].orEmpty()
        if (channels.isEmpty()) return false
        val index = channels.indexOf(currentChannel)
        val next = if (index < 0) {
            if (step > 0) channels.first() else channels.last()
        } else channels[Math.floorMod(index + step, channels.size)]
        category = group
        selected = Destination.LIVE
        playChannel(next, openFullscreenOnStart = fullscreen)
        return true
    }
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
            if (fullscreen && (vodSection == null || vodSection == PortalSection.SERIES && panel != GuidePanel.DETAILS)) {
                if (panel == GuidePanel.DETAILS) {
                    programmeChannel = playing
                    guideProgramme = null
                }
                showFullscreenPanel(panel)
                return true
            }
            if (inGuide && !fullscreen && panel != GuidePanel.CHANNELS && panel != GuidePanel.DETAILS) {
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
            ControlAction.NEXT_CHANNEL -> changeLiveChannel(1)
            ControlAction.PREVIOUS_CHANNEL -> changeLiveChannel(-1)
            else -> false
        }
    }
    val longBack by rememberUpdatedState<() -> Boolean>({
        !watchingPrompt && !playbackControlsVisible && !controlSettingsOpen && guideAppearancePage == null &&
            (runMappedControl(GuideInput.LONG_BACK) ||
                if (fullscreen && fullscreenGuidePanel != GuidePanel.GRID) {
                    showFullscreenPanel(GuidePanel.GRID)
                    true
                } else !fullscreen && openCurrentFullscreen())
    })
    val screenBack by rememberUpdatedState<(Boolean) -> Boolean>({ released ->
        val handled = fullscreen && (playbackControlsVisible || watchingPrompt || fullscreenGuidePanel != GuidePanel.GRID) || !fullscreen && (
            isMainMenuOpen() || selected == Destination.MOVIES || selected == Destination.SERIES
        )
        if (handled && released) navigateBack()
        handled
    })
    val playbackInput by rememberUpdatedState<() -> Unit>({
        if (vodSection == PortalSection.SERIES && playing != null && !watchingPrompt) {
            interruptedEpisode = true
            silentEpisodes = 0
        }
    })
    DisposableEffect(Unit) {
        registerLongBack { longBack() }
        registerScreenBack { released -> screenBack(released) }
        registerPlaybackInput { playbackInput() }
        onDispose {
            registerLongBack(null)
            registerScreenBack(null)
            registerPlaybackInput(null)
            registerPromptInput(null)
        }
    }
    BackHandler(onBack = ::navigateBack)
    categoryMenu?.let { (section, first) ->
        val defaultGroups = if (section == PortalSection.LIVE) {
            allLiveCategories.keys.sortedBy { categoryNames.values.indexOf(it) }.map { PortalGroup(it, it) }
        } else defaultVodCategoryOrder(section)
        val groups = withCategoryOrder(section, defaultGroups) { it.id }
        CategoryDialog(
            groups, first,
            isHidden = { categoryKey(section, it.id) in hiddenCategories },
            onApply = { ids, hidden ->
                setCategoryVisibility(section, ids, hidden)
                categoryMenu = null
            },
            onMove = { id, direction ->
                val order = groups.map { it.id }.moveCategory(id, direction)
                val key = categoryOrderKey(section)
                categoryOrders[key] = order
                preferences?.edit()?.putString(key, org.json.JSONArray(order).toString())?.apply()
            },
            onDismiss = { categoryMenu = null }
        )
    }
    LaunchedEffect(watchingPrompt) {
        if (watchingPrompt) {
            delay(30_000)
            if (watchingPrompt) stopPlayback()
        }
    }
    if (watchingPrompt && !fullscreen) AlertDialog(
        onDismissRequest = ::stopPlayback,
        title = { Text(stringResource(R.string.series_still_watching)) },
        text = { Text(stringResource(R.string.series_still_watching_timeout)) },
        confirmButton = { MenuButton(stringResource(R.string.play_next_episode), Modifier) { advanceEpisode(confirmed = true) } },
        dismissButton = { MenuButton(stringResource(R.string.stop_playback), Modifier, onClick = ::stopPlayback) }
    )
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
        val markerKey = "credits_marker:" + java.security.MessageDigest.getInstance("SHA-256")
            .digest(attempt.mediaId.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        val savedCreditsMarker = progressPreferences?.getLong(markerKey, 0L)?.takeIf { it > 0L }
        val scanOpening = !manualIntro && seriesKey != null && active.currentPosition < OPENING_SCAN_LIMIT_MS
        val scanCredits = !manualCredits && savedCreditsMarker == null &&
            nextEpisodeAction != null && portal.nextAt(duration) == duration
        var timing = EpisodeTiming(creditsStartMs = savedCreditsMarker)
        detectedTiming = attempt.mediaId to timing
        if (!scanOpening && !scanCredits) return@LaunchedEffect
        val uri = item.localConfiguration?.uri ?: return@LaunchedEffect
        val sampleFromSurface = uri.scheme in setOf("http", "https")
        val stepMs = if (sampleFromSurface) HLS_INTRO_FRAME_INTERVAL_MS else FRAME_INTERVAL_MS
        val store = introStore
        // References: the opening of another episode of this series (for a first
        // comparison) and a learned opening (to offer the skip while it is running).
        val previous = seriesOpenings[seriesKey]?.takeIf { it.first != selection.id }
            ?: seriesKey?.let { key ->
                withContext(Dispatchers.IO) {
                    decodeOpeningReference(store?.read(key, IntroFingerprintStore.VIDEO_REFERENCE)?.decodeToString())
                }
            }?.takeIf { it.mediaId != selection.id }?.let { it.mediaId to it.frames }
        val previousAudio = seriesOpeningAudio[seriesKey]?.takeIf { it.first != selection.id }
            ?: seriesKey?.let { key ->
                withContext(Dispatchers.IO) {
                    decodeAudioReference(store?.read(key, IntroFingerprintStore.AUDIO_REFERENCE))
                }
            }?.takeIf { it.first != selection.id }
        val learned = seriesKey?.let { key ->
            withContext(Dispatchers.IO) {
                decodeLearnedOpening(store?.read(key, IntroFingerprintStore.VIDEO_LEARNED)?.decodeToString())
            }
        }
        val learnedAudio = seriesKey?.let { key ->
            withContext(Dispatchers.IO) { decodeLearnedAudio(store?.read(key, IntroFingerprintStore.AUDIO_LEARNED)) }
        }
        if (active.currentMediaItem != item) return@LaunchedEffect
        val audioToken = if (scanOpening && sampleFromSurface) EpisodeAudioTap.start() else null
        try {
            val updates = if (sampleFromSurface) flow {
                val opening = mutableListOf<EpisodeFrame>()
                val audio = mutableListOf<AudioPrint>()
                val credits = mutableListOf<EpisodeFrame>()
                var openingComplete = !scanOpening
                var creditsComplete = !scanCredits
                var lastOpeningBucket = -HLS_INTRO_FRAME_INTERVAL_MS
                var lastCreditsBucket = -5_000L
                var emittedFrames = 0
                var emittedAudio = 0
                var emittedCredits = 0
                while (active.currentMediaItem == item && (!openingComplete || !creditsComplete)) {
                    val position = active.currentPosition
                    val inOpening = !openingComplete && position < OPENING_SCAN_LIMIT_MS
                    if (!openingComplete && position >= OPENING_SCAN_LIMIT_MS) openingComplete = true
                    if (audioToken != null) {
                        audio += EpisodeAudioTap.drain().filter { it.timeMs in 0 until OPENING_SCAN_LIMIT_MS }
                        if (openingComplete) EpisodeAudioTap.stop(audioToken)
                    }
                    // Scan the closing eight minutes so longer credit rolls and
                    // post-credit scenes are not automatically reduced to a 15s fallback.
                    val inCredits = !creditsComplete && active.duration - position <= 480_000L
                    val surface = fullscreenVideoSurface ?: playbackVideoSurface
                    if (surface != null && active.isPlaying) {
                        if (inOpening) {
                            val bucket = position / HLS_INTRO_FRAME_INTERVAL_MS * HLS_INTRO_FRAME_INTERVAL_MS
                            if (bucket > lastOpeningBucket) {
                                sampleEpisodeSurface(surface, bucket)?.let { frame ->
                                    opening += frame
                                    lastOpeningBucket = bucket
                                }
                            }
                        }
                        if (inCredits) {
                            val bucket = position / 5_000L * 5_000L
                            if (bucket > lastCreditsBucket) {
                                sampleEpisodeSurface(surface, bucket)?.let { frame ->
                                    credits += frame
                                    lastCreditsBucket = bucket
                                }
                            }
                        }
                    }
                    // Compare only when new evidence arrived instead of on every poll.
                    val audioReady = audio.size - emittedAudio >= 20 || openingComplete && audio.size > emittedAudio
                    if (opening.size != emittedFrames || audioReady || credits.size != emittedCredits) {
                        emittedFrames = opening.size
                        emittedAudio = audio.size
                        emittedCredits = credits.size
                        emit(EpisodeVideoAnalysis(opening.toList(), findCreditsStart(credits), audio.toList()))
                    }
                    if (active.playbackState == Player.STATE_ENDED ||
                        active.duration > 0 && position >= active.duration - 1_000L
                    ) {
                        openingComplete = true
                        creditsComplete = true
                    }
                    delay(if (inOpening || inCredits) 150 else 500)
                }
            } else analyzeEpisodeVideo(uri, duration, scanOpening, scanCredits)
            var savedFrames = 0
            var savedAudio = 0
            updates.collect { analysis ->
                if (active.currentMediaItem != item) return@collect
                if (seriesKey != null && (analysis.opening.isNotEmpty() || analysis.audio.isNotEmpty())) {
                    // A learned opening is confirmed early so the skip is offered while it runs.
                    // The comparison with another episode keeps running until the scan ends: an
                    // opening with fast cuts can first look shorter than it is, so every longer
                    // overlapping result extends the range and corrects the learned opening.
                    val currentIntro = timing.introStartMs?.let { start -> timing.introEndMs?.let { start until it } }
                    val (known, compared) = withContext(Dispatchers.Default) {
                        val known = if (currentIntro != null) null else
                            learned?.let { confirmKnownOpening(analysis.opening, it.frames, it.range, stepMs) }
                                ?: learnedAudio?.let { (range, prints) -> confirmKnownAudioOpening(analysis.audio, prints, range) }
                        known to mergeOpeningRanges(
                            previous?.let { findRepeatedOpening(analysis.opening, it.second, stepMs) },
                            previousAudio?.let { findRepeatedAudioOpening(analysis.audio, it.second) }
                        )
                    }
                    if (active.currentMediaItem != item) return@collect
                    val base = currentIntro ?: known
                    val extended = compared?.let { found ->
                        when {
                            base == null -> found
                            found.first <= base.last && base.first <= found.last && found.last > base.last ->
                                minOf(base.first, found.first)..found.last
                            else -> null
                        }
                    }
                    val intro = extended ?: base
                    if (intro != null && intro != currentIntro) {
                        timing = timing.copy(introStartMs = intro.first, introEndMs = intro.last + 1)
                    }
                    if (extended != null && store != null) {
                        val frames = analysis.opening.filter { it.timeMs in extended }
                        val prints = analysis.audio.filter { it.timeMs in extended.first..minOf(extended.last, extended.first + 30_000L) }
                        withContext(Dispatchers.IO) {
                            if (frames.size >= 4) store.write(
                                seriesKey, IntroFingerprintStore.VIDEO_LEARNED,
                                LearnedOpening(extended, frames).encode().encodeToByteArray()
                            )
                            if (prints.count(AudioPrint::loud) >= 40) store.write(
                                seriesKey, IntroFingerprintStore.AUDIO_LEARNED, encodeLearnedAudio(extended, prints)
                            )
                        }
                    }
                    // Keep this episode as reference for the next one, but do not replace a
                    // longer reference with a briefly watched episode.
                    val frameCoverage = analysis.opening.lastOrNull()?.timeMs ?: 0L
                    val audioCoverage = analysis.audio.lastOrNull()?.timeMs ?: 0L
                    val previousFrameCoverage = previous?.second?.lastOrNull()?.timeMs ?: 0L
                    val previousAudioCoverage = previousAudio?.second?.lastOrNull()?.timeMs ?: 0L
                    val finished = active.currentPosition >= OPENING_SCAN_LIMIT_MS - 5_000L ||
                        active.playbackState == Player.STATE_ENDED
                    if (seriesOpenings.size >= 16 && seriesKey !in seriesOpenings) {
                        seriesOpenings.remove(seriesOpenings.keys.first())
                    }
                    if (seriesOpeningAudio.size >= 16 && seriesKey !in seriesOpeningAudio) {
                        seriesOpeningAudio.remove(seriesOpeningAudio.keys.first())
                    }
                    val replaceFrames = analysis.opening.size >= 12 &&
                        frameCoverage >= minOf(previousFrameCoverage, 150_000L)
                    val replaceAudio = analysis.audio.size >= 200 &&
                        audioCoverage >= minOf(previousAudioCoverage, 150_000L)
                    if (replaceFrames) seriesOpenings[seriesKey] = selection.id to analysis.opening
                    if (replaceAudio) seriesOpeningAudio[seriesKey] = selection.id to analysis.audio
                    if (store != null) {
                        val writeFrames = replaceFrames && (analysis.opening.size - savedFrames >= 15 ||
                            finished && analysis.opening.size > savedFrames)
                        val writeAudio = replaceAudio && (analysis.audio.size - savedAudio >= 600 ||
                            finished && analysis.audio.size > savedAudio)
                        if (writeFrames || writeAudio) withContext(Dispatchers.IO) {
                            if (writeFrames) store.write(
                                seriesKey, IntroFingerprintStore.VIDEO_REFERENCE,
                                OpeningReference(selection.id, analysis.opening).encode().encodeToByteArray()
                            )
                            if (writeAudio) store.write(
                                seriesKey, IntroFingerprintStore.AUDIO_REFERENCE,
                                encodeAudioReference(selection.id, analysis.audio)
                            )
                        }
                        if (writeFrames) savedFrames = analysis.opening.size
                        if (writeAudio) savedAudio = analysis.audio.size
                    }
                }
                analysis.creditsStartMs?.let { marker ->
                    timing = timing.copy(creditsStartMs = marker)
                    progressPreferences?.edit()?.putLong(markerKey, marker)?.apply()
                }
                detectedTiming = attempt.mediaId to timing
            }
        } finally {
            audioToken?.let(EpisodeAudioTap::stop)
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
                // Never offer a stale skip target: that would seek backwards after
                // frame matching completes near the end of an intro.
                val introGrace = intro?.let { position > it.last && position - it.last <= 15_000L } == true
                val introTarget = futureIntroSkipTarget(intro, position)
                skipIntroTo = if (!introSkipped && introTarget != null && position < introTarget &&
                    (intro?.contains(position) == true || introGrace) && active.isCurrentMediaItemSeekable
                ) introTarget else null
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
                if (!watchingPrompt && autoNextEpisode && (active.isPlaying || active.playbackState == Player.STATE_ENDED) &&
                    remaining == 0L && canPlay()
                ) {
                    if (nextEpisodeAction != null) {
                        silentEpisodes = if (interruptedEpisode) 0 else silentEpisodes + 1
                        if (checkSeriesInactivity && silentEpisodes >= inactiveEpisodeLimit) {
                            active.pause()
                            watchingPrompt = true
                            nextEpisodeSeconds = null
                        } else advanceEpisode()
                        return@LaunchedEffect
                    }
                }
            }
            delay(250)
        }
    }
    if (fullscreen) {
        val episodeActionFocus = remember { FocusRequester() }
        val episodeActionSecondaryFocus = remember { FocusRequester() }
        val episodeActionTertiaryFocus = remember { FocusRequester() }
        var episodeActionSelection by remember { mutableIntStateOf(0) }
        val episodeAction = when {
            watchingPrompt -> 3
            nextEpisodeSeconds != null -> 2
            skipIntroTo != null -> 1
            else -> 0
        }
        val episodeActionCount = when {
            watchingPrompt -> 2
            else -> (if (skipIntroTo != null) 1 else 0) + (if (nextEpisodeSeconds != null) 2 else 0)
        }
        fun requestEpisodeActionFocus(index: Int) {
            episodeActionSelection = index.coerceIn(0, (episodeActionCount - 1).coerceAtLeast(0))
            when (episodeActionSelection) {
                0 -> episodeActionFocus
                1 -> episodeActionSecondaryFocus
                else -> episodeActionTertiaryFocus
            }.requestFocus()
        }
        fun activateEpisodeAction() {
            if (watchingPrompt) {
                if (episodeActionSelection == 0) advanceEpisode(confirmed = true) else stopPlayback()
                return
            }
            val introOffset = if (skipIntroTo != null) 1 else 0
            when {
                skipIntroTo != null && episodeActionSelection == 0 -> {
                    player?.seekTo(requireNotNull(skipIntroTo))
                    skipIntroTo = null
                }
                nextEpisodeSeconds != null && episodeActionSelection == introOffset ->
                    advanceEpisode(confirmed = true)
                nextEpisodeSeconds != null && episodeActionSelection == introOffset + 1 -> stopPlayback()
            }
        }
        val episodeActionKeyModifier = Modifier.onPreviewKeyEvent { event ->
            if (episodeActionCount == 0) false
            else {
                val activate = event.key == Key.DirectionCenter || event.key == Key.Enter ||
                    event.key == Key.NumPadEnter || event.key == Key.ButtonA
                if (activate) {
                    if (event.type == KeyEventType.KeyUp) activateEpisodeAction()
                    true
                } else false
            }
        }
        LaunchedEffect(episodeAction, playbackControlsVisible, fullscreenGuidePanel, skipIntroTo != null, nextEpisodeSeconds != null) {
            if (episodeAction != 0 && !playbackControlsVisible &&
                (fullscreenGuidePanel == GuidePanel.GRID || watchingPrompt || skipIntroTo != null)
            ) {
                withFrameNanos { }
                requestEpisodeActionFocus(0)
            }
        }
        val screen = LocalConfiguration.current
        val screenWidth = screen.screenWidthDp.dp
        val screenHeight = screen.screenHeightDp.dp
        val scheduleBesideList = guideAppearance.showSchedule && screenWidth >= 640.dp
        val descriptionBesideSchedule = scheduleBesideList && guideAppearance.showDescription && screenWidth >= 840.dp
        val live = vodSection == null
        val seriesOverlay = vodSection == PortalSection.SERIES && playbackSeries != null
        fun navigateFullscreen(input: GuideInput) {
            if (episodeAction != 0) {
                val selected = when (input) {
                    GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT -> (episodeActionSelection - 1).coerceAtLeast(0)
                    GuideInput.REMOTE_RIGHT, GuideInput.SWIPE_RIGHT ->
                        (episodeActionSelection + 1).coerceAtMost(episodeActionCount - 1)
                    else -> episodeActionSelection
                }
                if (selected != episodeActionSelection) requestEpisodeActionFocus(selected)
                return
            }
            if (runMappedControl(input)) return
            if (input.vertical) {
                if (live && fullscreenGuidePanel == GuidePanel.GRID) {
                    changeLiveChannel(if (input == GuideInput.REMOTE_UP || input == GuideInput.SWIPE_UP) 1 else -1)
                }
                return
            }
            if (seriesOverlay) {
                showFullscreenPanel(fullscreenGuidePanel.afterSeriesOverlay(input))
                return
            }
            if (!live) return
            if (fullscreenGuidePanel == GuidePanel.GRID && (input == GuideInput.SWIPE_RIGHT || input == GuideInput.REMOTE_RIGHT)) {
                programmeChannel = playing
                guideProgramme = null
            }
            showFullscreenPanel(fullscreenGuidePanel.after(input))
        }
        SideEffect {
            registerPromptInput(if (episodeAction != 0) { event ->
                val input = when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> GuideInput.REMOTE_UP
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GuideInput.REMOTE_DOWN
                    else -> null
                }
                if (input != null) {
                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) navigateFullscreen(input)
                    true
                } else if (event.keyCode in setOf(
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_BUTTON_A
                    )
                ) {
                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) activateEpisodeAction()
                    true
                } else false
            } else null)
        }
        DisposableEffect(Unit) {
            onDispose { registerPromptInput(null) }
        }
        val overlayFocus = remember { FocusRequester() }
        fun interceptDirection(input: GuideInput): Boolean {
            if (episodeAction != 0) return true
            if (playbackControlsVisible) return false
            if (!live && fullscreenGuidePanel == GuidePanel.GRID &&
                input in setOf(GuideInput.REMOTE_LEFT, GuideInput.REMOTE_RIGHT) &&
                player?.isCurrentMediaItemSeekable == true
            ) return false
            val custom = controlBindings[input]?.let { it != ControlAction.DEFAULT } == true
            if (input.vertical) return fullscreenGuidePanel == GuidePanel.GRID && (custom || live)
            return custom || seriesOverlay || live && (
                fullscreenGuidePanel != GuidePanel.CHANNELS || !scheduleBesideList ||
                    input == GuideInput.REMOTE_LEFT && !overlayScheduleFocused
            )
        }
        val inputModifier = Modifier.guideNavigationInput(
            interceptRemote = ::interceptDirection,
            interceptGesture = { input -> interceptDirection(input) || live && !input.vertical && !playbackControlsVisible },
            onInput = ::navigateFullscreen
        )
        Box(Modifier.fillMaxSize().background(Color.Black).then(episodeActionKeyModifier).then(inputModifier)) {
            PlayerFrame(
                player, active = true, fullscreen = true, isLivePlayback = vodSection == null, onStop = ::stopPlayback,
                onSurface = { fullscreenVideoSurface = it },
                onGuideInput = ::navigateFullscreen,
                interceptGuideKey = ::interceptDirection,
                onPreviousEpisode = { previousEpisodeAction?.invoke() },
                onNextEpisode = { advanceEpisode(confirmed = true) },
                hasPreviousEpisode = previousEpisodeAction != null,
                hasNextEpisode = nextEpisodeAction != null,
                onTouchAction = { runMappedControl(GuideInput.TAP) },
                onMediaKey = { runMappedControl(GuideInput.PLAY_PAUSE) },
                controlsRequest = playerControlsRequest,
                controlsDismissRequest = dismissPlaybackControls,
                controlsAllowed = fullscreenGuidePanel == GuidePanel.GRID && !watchingPrompt,
                actionPromptActive = episodeAction != 0 &&
                    (fullscreenGuidePanel == GuidePanel.GRID || watchingPrompt || skipIntroTo != null),
                onEpisodeActionConfirm = ::activateEpisodeAction,
                episodeActionVisible = episodeAction != 0 || fullscreenGuidePanel != GuidePanel.GRID || skipIntroTo != null,
                subtitleDefaultEnabled = subtitleDefaultEnabled,
                forcedSubtitlesEnabled = forcedSubtitlesEnabled,
                subtitleSeriesKey = if (vodSection == PortalSection.SERIES) playbackSeries?.let {
                    titleIdentity(it) ?: normalizedLabel(displayVodTitle(it))
                } else null,
                subtitlePlaybackKey = playbackAttempt?.mediaId,
                subtitlePreferences = progressPreferences,
                onControlsVisible = { playbackControlsVisible = it }
            )
            if (playbackControlsVisible && vodSection != null) {
                val episode = playbackEpisode
                val movie = playbackMovieTitle
                val show = playbackSeries
                val title = if (vodSection == PortalSection.SERIES) show else movie
                if (title != null) {
                    val episodeNumber = episode?.episode?.let { stringResource(R.string.episode_number, it) }
                    val seasonNumber = playbackSeason?.let { season ->
                        season.number?.let { stringResource(R.string.season_number, it) } ?: season.title.name
                    }
                    val detailLine = listOfNotNull(seasonNumber, episodeNumber).joinToString(" · ")
                    val episodeName = episode?.name?.takeIf { it.isNotBlank() && it != show?.name }
                    val description = if (vodSection == PortalSection.SERIES) {
                        germanDescription(episode?.description) ?: germanDescription(show?.description)
                    } else germanDescription(movie?.description)
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
                            .fillMaxWidth(0.46f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(displayVodTitle(title), style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (detailLine.isNotEmpty()) Text(detailLine,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall)
                            if (episodeName != null && episodeName != title.name) Text(episodeName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (playbackControlsVisible && live) {
                val channel = playing
                val nowSeconds = System.currentTimeMillis() / 1_000L
                val programme = channel?.let(::effectiveProgrammes).orEmpty().firstOrNull { item ->
                    val from = item.startSeconds ?: return@firstOrNull false
                    from <= nowSeconds && (item.endSeconds == null || nowSeconds < item.endSeconds)
                } ?: channel?.let(::effectiveProgrammes).orEmpty().firstOrNull()
                if (channel != null) Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp).fillMaxWidth(0.46f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(channel.name, style = MaterialTheme.typography.titleLarge, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        programme?.let { item ->
                            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            listOfNotNull(item.startSeconds, item.endSeconds).takeIf { it.isNotEmpty() }?.let { times ->
                                Text(times.joinToString(" – ", transform = ::guideTime),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                            germanDescription(item.description)?.takeIf(String::isNotBlank)?.let { description ->
                                Text(description, color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (controlBindings[GuideInput.TAP] == ControlAction.PLAY_PAUSE &&
                fullscreenGuidePanel == GuidePanel.GRID && !playbackControlsVisible
            ) {
                MenuButton(
                    stringResource(R.string.control_show_controls),
                    Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) { playerControlsRequest++ }
            }
            if ((live || seriesOverlay) && fullscreenGuidePanel != GuidePanel.GRID) {
                Box(Modifier.matchParentSize().clickable { showFullscreenPanel(GuidePanel.GRID) })
            }
            if (live || seriesOverlay) {
                AnimatedVisibility(
                    visible = fullscreenGuidePanel != GuidePanel.GRID,
                    modifier = Modifier.align(
                        if (fullscreenOverlayContent == GuidePanel.DETAILS) Alignment.TopEnd else Alignment.CenterStart
                    ).padding(12.dp),
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    val panel = fullscreenOverlayContent
                    val groups = withCategoryOrder(PortalSection.LIVE, liveCategories.keys
                        .filter { visibleCategory(PortalSection.LIVE, it, it) }
                        .sortedBy { categoryNames.values.indexOf(it) }) { it }
                    val channelCategory = guideChannelCategory(category, groups, liveCategories, playing)
                    val panelWidth = if (seriesOverlay) minOf(360.dp, screenWidth * 0.8f) else when (panel) {
                        GuidePanel.CHANNELS -> {
                            val fraction = when {
                                descriptionBesideSchedule -> 0.82f
                                scheduleBesideList -> 0.62f
                                else -> 0.38f
                            }
                            (screenWidth * fraction * guideAppearance.listWidth.factor)
                                .coerceIn(minOf(205.dp, screenWidth * 0.8f), screenWidth * 0.88f)
                        }
                        GuidePanel.DETAILS -> (screenWidth * 0.55f).coerceIn(minOf(300.dp, screenWidth * 0.85f), minOf(580.dp, screenWidth * 0.85f))
                        else -> minOf(235.dp, screenWidth * 0.85f)
                    }
                    Surface(
                        modifier = Modifier.width(panelWidth)
                            .then(if (panel == GuidePanel.DETAILS) Modifier.heightIn(max = screenHeight * 0.83f) else Modifier.fillMaxHeight()),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = guideAppearance.opacity / 100f)
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            MenuButton(
                                if (seriesOverlay && panel == GuidePanel.CHANNELS) overlaySeason?.title?.name
                                    ?.let { seasonName -> stringResource(R.string.series_overlay_season_episodes, seasonName) }
                                    ?: stringResource(R.string.series_overlay_episodes)
                                else if (seriesOverlay && panel == GuidePanel.CATEGORIES) stringResource(R.string.series_overlay_seasons)
                                else if (panel == GuidePanel.CHANNELS) channelCategory ?: stringResource(R.string.channels)
                                else stringResource(R.string.navigation_back),
                                Modifier.focusRequester(overlayFocus)
                            ) {
                                if (seriesOverlay) navigateBack()
                                else showFullscreenPanel(if (panel == GuidePanel.CHANNELS) GuidePanel.CATEGORIES else panel.after(GuideInput.BACK))
                            }
                            LaunchedEffect(panel) { if (seriesOverlay || panel != GuidePanel.CHANNELS) overlayFocus.requestFocus() }
                            when (panel) {
                                GuidePanel.CHANNELS -> {
                                    if (seriesOverlay) {
                                        val episodes = overlaySeason?.episodes.orEmpty()
                                        val current = episodes.indexOf(playbackEpisode).coerceAtLeast(0)
                                        key(overlaySeason) {
                                            LazyColumn(
                                                Modifier.weight(1f), state = rememberLazyListState(current)
                                            ) {
                                                items(episodes) { episode ->
                                                    val seasonName = overlaySeason?.title?.name
                                                    val episodeTitle = episode.name.takeUnless { name ->
                                                        name.equals(seasonName, ignoreCase = true) ||
                                                            Regex("(?i)^(season|staffel)\\s*\\d+$").matches(name.trim())
                                                    }
                                                    val episodeLabel = episode.episode?.let { number ->
                                                        if (episodeTitle.isNullOrBlank()) stringResource(R.string.episode_number, number)
                                                        else stringResource(R.string.episode_title, episodeTitle, number)
                                                    } ?: episodeTitle ?: episode.name
                                                    MenuButton(episodeLabel, Modifier.fillMaxWidth(), selected = episode == playbackEpisode) {
                                                        val series = playbackSeries ?: return@MenuButton
                                                        val season = overlaySeason ?: return@MenuButton
                                                        openedSeries = series
                                                        openedSeason = season
                                                        seasons = playbackSeasons
                                                        episodeVariants = playbackEpisodeVariants
                                                        selected = Destination.SERIES
                                                        showFullscreenPanel(GuidePanel.GRID)
                                                        selectTitle(PortalSection.SERIES, episode)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                    val channelsInPanel = liveCategories[channelCategory].orEmpty()
                                    if (channelsInPanel.isEmpty()) LaunchedEffect(Unit) { overlayFocus.requestFocus() }
                                    FullscreenChannelList(
                                        Modifier.weight(1f), channelsInPanel, playing, guideAppearance, scheduleBesideList,
                                        descriptionBesideSchedule = descriptionBesideSchedule,
                                        refreshKey = epgRefreshRequest + if (guideCacheLoaded) 1 else 0,
                                        programmes = ::effectiveProgrammes,
                                        onVisible = { visible ->
                                            visibleEpgChannels = visible
                                            if (!useXmlTv) loadProviderSchedules(visible)
                                        },
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
                                    }
                                GuidePanel.CATEGORIES -> LazyColumn(Modifier.weight(1f)) {
                                    if (seriesOverlay) {
                                        items(playbackSeasons) { season ->
                                            MenuButton(season.title.name, Modifier.fillMaxWidth(), selected = season == playbackSeason) {
                                                overlaySeason = season
                                                showFullscreenPanel(GuidePanel.CHANNELS)
                                            }
                                        }
                                    } else {
                                    items(groups) { group ->
                                        MenuButton(group, Modifier.fillMaxWidth(), selected = channelCategory == group,
                                            onLongPress = { categoryMenu = PortalSection.LIVE to group }) {
                                            category = group
                                            showFullscreenPanel(GuidePanel.CHANNELS)
                                        }
                                    }
                                }
                                    }
                                GuidePanel.MENU -> LazyColumn(Modifier.weight(1f)) {
                                    items(mainDestinations) { destination ->
                                        MenuButton(
                                            stringResource(destination.label), Modifier.fillMaxWidth(),
                                            leadingIcon = destination.icon.takeUnless { mainMenuStyle == MainMenuStyle.TEXT },
                                            iconOnly = mainMenuStyle == MainMenuStyle.ICONS
                                        ) {
                                            fullscreen = false
                                            if (destination == Destination.SETTINGS) openSettings() else {
                                                showFullscreenPanel(GuidePanel.GRID)
                                                guidePanel = GuidePanel.GRID
                                                lastFocused = destination
                                                pendingVodCategory = destination.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
                                                selected = destination
                                                if (destination == Destination.LIVE) restoreGuideEntryFocus()
                                            }
                                        }
                                    }
                                }
                                GuidePanel.DETAILS -> {
                                    val schedule = playing?.let(::effectiveProgrammes).orEmpty()
                                    ProgrammeInfoCard(
                                        playing, currentGuideProgramme(schedule, guideNow),
                                        Modifier.fillMaxWidth().weight(1f, fill = false)
                                    )
                                }
                                GuidePanel.GRID -> Unit
                            }
                        }
                    }
                }
            }
            if (episodeAction != 0 && (fullscreenGuidePanel == GuidePanel.GRID || watchingPrompt || skipIntroTo != null)) {
                Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (watchingPrompt) {
                            Text(stringResource(R.string.series_still_watching), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.series_still_watching_timeout))
                            Row(Modifier.focusGroup()) {
                                MenuButton(
                                    stringResource(R.string.play_next_episode),
                                    Modifier.focusRequester(episodeActionFocus),
                                    selected = episodeActionSelection == 0,
                                    onFocused = { episodeActionSelection = 0 }
                                ) { advanceEpisode(confirmed = true) }
                                MenuButton(
                                    stringResource(R.string.stop_playback),
                                    Modifier.focusRequester(episodeActionSecondaryFocus),
                                    selected = episodeActionSelection == 1,
                                    onFocused = { episodeActionSelection = 1 },
                                    onClick = ::stopPlayback
                                )
                            }
                        } else {
                        skipIntroTo?.let { position ->
                            MenuButton(
                                stringResource(R.string.skip_intro),
                                Modifier.focusRequester(episodeActionFocus),
                                selected = episodeActionSelection == 0,
                                onFocused = { episodeActionSelection = 0 }
                            ) {
                                player?.seekTo(position)
                                skipIntroTo = null
                            }
                        }
                        nextEpisodeSeconds?.let { seconds ->
                            Text(
                                if (autoNextEpisode) stringResource(R.string.episode_countdown, nextEpisodeName, seconds)
                                else stringResource(R.string.next_episode_ready, nextEpisodeName)
                            )
                            Row(Modifier.focusGroup()) {
                                val introOffset = if (skipIntroTo != null) 1 else 0
                                MenuButton(
                                    stringResource(R.string.play_next_episode),
                                    Modifier.focusRequester(
                                        if (introOffset == 1) episodeActionSecondaryFocus else episodeActionFocus
                                    ),
                                    selected = episodeActionSelection == introOffset,
                                    onFocused = { episodeActionSelection = introOffset }
                                ) {
                                    advanceEpisode(confirmed = true)
                                }
                                MenuButton(
                                    stringResource(R.string.stop_playback),
                                    Modifier.focusRequester(
                                        if (introOffset == 1) episodeActionTertiaryFocus else episodeActionSecondaryFocus
                                    ),
                                    selected = episodeActionSelection == introOffset + 1,
                                    onFocused = { episodeActionSelection = introOffset + 1 },
                                    onClick = ::stopPlayback
                                )
                            }
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
    if (sourceRefreshDialogOpen) {
        AlertDialog(
            onDismissRequest = { sourceRefreshDialogOpen = false },
            title = { Text(stringResource(R.string.select_sources_to_refresh_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.select_sources_to_refresh))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MenuButton(stringResource(R.string.select_all_sources), Modifier.weight(1f)) {
                            selectedRefreshSources = savedSources.map { it.identity }.toSet()
                        }
                        MenuButton(stringResource(R.string.clear_source_selection), Modifier.weight(1f)) {
                            selectedRefreshSources = emptySet()
                        }
                    }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(savedSources, key = { it.identity }) { source ->
                            val selectedForRefresh = source.identity in selectedRefreshSources
                            val host = runCatching { java.net.URI(source.address).host }.getOrNull()
                                .orEmpty().ifBlank { source.address }
                            MenuButton(
                                (if (selectedForRefresh) "✓  " else "○  ") + host,
                                Modifier.fillMaxWidth(),
                                selected = selectedForRefresh
                            ) {
                                selectedRefreshSources = if (selectedForRefresh) {
                                    selectedRefreshSources - source.identity
                                } else selectedRefreshSources + source.identity
                            }
                        }
                    }
                }
            },
            confirmButton = {
                MenuButton(
                    stringResource(R.string.refresh_selected_sources),
                    Modifier,
                    enabled = selectedRefreshSources.isNotEmpty() && !connecting
                ) {
                    val sources = savedSources.filter { it.identity in selectedRefreshSources }
                    sourceRefreshDialogOpen = false
                    refreshSources(sources)
                }
            },
            dismissButton = {
                MenuButton(stringResource(android.R.string.cancel), Modifier) {
                    sourceRefreshDialogOpen = false
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
                MenuButton(
                    stringResource(R.string.series_inactivity_check), Modifier, selected = checkSeriesInactivity
                ) {
                    checkSeriesInactivity = !checkSeriesInactivity
                    preferences?.edit()?.putBoolean("check_series_inactivity", checkSeriesInactivity)?.apply()
                }
                if (checkSeriesInactivity) {
                    Text(stringResource(R.string.series_inactive_episode_limit, inactiveEpisodeLimit))
                    Row {
                        MenuButton("−1", Modifier) {
                            inactiveEpisodeLimit = (inactiveEpisodeLimit - 1).coerceAtLeast(1)
                            preferences?.edit()?.putInt("inactive_episode_limit", inactiveEpisodeLimit)?.apply()
                        }
                        MenuButton("+1", Modifier) {
                            inactiveEpisodeLimit = (inactiveEpisodeLimit + 1).coerceAtMost(10)
                            preferences?.edit()?.putInt("inactive_episode_limit", inactiveEpisodeLimit)?.apply()
                        }
                    }
                }
                MenuButton(stringResource(R.string.skip_intro_automatically), Modifier, selected = autoSkipIntro) {
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
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { androidx.compose.material3.SnackbarHost(playbackNoticeHost) }
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val vodSectionForScreen = when (selected) {
            Destination.MOVIES -> PortalSection.MOVIES
            Destination.SERIES -> PortalSection.SERIES
            else -> null
        }
        if (vodSectionForScreen != null && cataloguePlaybackView == VodLiveView.BACKGROUND &&
            playing != null && player != null
        ) {
            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
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
                withCategoryOrder(
                    section,
                    defaultVodCategoryOrder(section).filter { visibleCategory(section, it.id, it.name, it.country) }
                ) { it.id }
            }.orEmpty()
            val recentCategoryKey = "last_vod_category_${vodSectionForScreen?.name}"
            fun destinationFocusRequester(item: Destination): FocusRequester = when {
                item == lastFocused && !inGuide && destination != Destination.SEARCH &&
                    destination != Destination.SETTINGS && destination != Destination.SOURCES &&
                    destination != Destination.CATEGORIES -> initialFocus
                inGuide && item == Destination.LIVE -> guidePanelFocus
                else -> destinationFocusRequesters.getValue(item)
            }
            fun openVodCategory(group: PortalGroup) {
                val section = vodSectionForScreen ?: return
                catalogueReturnTitle = null
                focusFirstTitle = true
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
                    val position = mainDestinations.indexOf(item)
                    val previousItem = mainDestinations.getOrNull(position - 1) ?: item
                    val nextItem = mainDestinations.getOrNull(position + 1) ?: item
                    MenuButton(
                        label = stringResource(item.label),
                        leadingIcon = item.icon.takeUnless { mainMenuStyle == MainMenuStyle.TEXT },
                        iconOnly = mainMenuStyle == MainMenuStyle.ICONS,
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(destinationFocusRequester(item))
                            .focusProperties {
                                up = destinationFocusRequester(previousItem)
                                down = destinationFocusRequester(nextItem)
                            },
                        selected = item == (destination ?: Destination.LIVE) ||
                            (item == Destination.SETTINGS &&
                                destination in listOf(Destination.SOURCES, Destination.CATEGORIES)),
                        onClick = {
                            if (item == Destination.SETTINGS) openSettings() else {
                                lastFocused = item
                                guidePanel = GuidePanel.GRID
                                if (item == Destination.LIVE) restoreGuideEntryFocus()
                                pendingVodCategory = item.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
                                selected = item
                            }
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
            val topNavigation: @Composable () -> Unit = {
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                        .focusGroup().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        Modifier.weight(1f).focusGroup().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mainDestinations.forEachIndexed { index, item ->
                            val previous = mainDestinations.getOrNull(index - 1) ?: item
                            val next = mainDestinations.getOrNull(index + 1) ?: item
                            MenuButton(
                                stringResource(item.label),
                                Modifier.focusRequester(destinationFocusRequesters.getValue(item))
                                    .focusProperties {
                                        left = destinationFocusRequesters.getValue(previous)
                                        right = destinationFocusRequesters.getValue(next)
                                    },
                                leadingIcon = item.icon.takeUnless { mainMenuStyle == MainMenuStyle.TEXT },
                                iconOnly = mainMenuStyle == MainMenuStyle.ICONS,
                                selected = item == (destination ?: Destination.LIVE) ||
                                    item == Destination.SETTINGS && destination in setOf(
                                        Destination.SOURCES, Destination.CATEGORIES
                                    ),
                                onClick = {
                                    if (item == Destination.SETTINGS) openSettings() else {
                                        lastFocused = item
                                        guidePanel = GuidePanel.GRID
                                        if (item == Destination.LIVE) restoreGuideEntryFocus()
                                        pendingVodCategory = item.takeIf {
                                            it == Destination.MOVIES || it == Destination.SERIES
                                        }
                                        selected = item
                                    }
                                }
                            )
                        }
                    }
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
                                    onSurface = { playbackVideoSurface = it },
                                    onOpenFullscreen = { if (vodSection == null) playChannel(channel) else fullscreen = true }
                                )
                            }
                        }
                    }
                }
                val portalRefreshText = startupPortalRefresh?.let { (number, total) ->
                    val source = savedSources.filter { it.enabled || it.autoDisabled }.getOrNull(number - 1)
                    val host = source?.address?.let { runCatching { java.net.URI(it).host }.getOrNull() }
                        ?.takeIf(String::isNotBlank) ?: "Portal"
                    stringResource(R.string.portal_refresh_progress, number, total, host)
                }
                val portalCheckText = startupPortalCheck?.let { (number, total) ->
                    stringResource(R.string.portal_check_progress, number, total)
                }
                if (restoring || connecting || liveGroupsLoading || traktBusy || portalRefreshText != null || portalCheckText != null ||
                    importStatuses.values.any { it == ImportStatus.Loading || it is ImportStatus.Progress }
                ) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (restoring) Text(stringResource(R.string.startup_loading))
                    if (liveGroupsLoading) Text(stringResource(R.string.live_groups_loading))
                    portalRefreshText?.let { Text(it) }
                    portalCheckText?.let { Text(it) }
                    batchProgress?.let { (number, total) -> Text("Portal-Import: $number / $total") }
                    if (traktBusy) traktStatus?.let { Text(it) }
                    importStatuses.forEach { (section, status) ->
                        when (status) {
                            ImportStatus.Loading -> Text(stringResource(section.label) + " wird geladen …")
                            is ImportStatus.Progress -> Text(
                                "${stringResource(section.label)}: ${status.loaded}" +
                                    (status.total?.let { " / $it" } ?: "")
                            )
                            else -> Unit
                        }
                    }
                }
            }
            val content: @Composable ColumnScope.() -> Unit = {
                when (destination) {
                    null -> LiveOverview(
                        player = player,
                        groups = liveCategories,
                        loading = liveGroups.isEmpty() && sourcesForBrowsing.any { it.channels.isNotEmpty() },
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
                            SettingsSection.entries.forEach { item ->
                                MenuButton(
                                    stringResource(item.label),
                                    Modifier.focusRequester(settingsFocusRequesters.getValue(item))
                                ) {
                                    lastSettingsSection = item
                                    settingsSection = item
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.padding(end = 12.dp).width(4.dp).height(34.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "EINSTELLUNGEN",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            SettingsHeading(section.label)
                                        }
                                        if (showSettingsBackButton) MenuButton(
                                            stringResource(R.string.navigation_back),
                                            Modifier.focusRequester(initialFocus)
                                        ) { settingsSection = null }
                                    }
                                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    when (section) {
                                SettingsSection.GENERAL -> {
                                    MenuButton(
                                        stringResource(R.string.show_touch_back_button), Modifier,
                                        selected = showSettingsBackButton
                                    ) {
                                        showSettingsBackButton = !showSettingsBackButton
                                        preferences?.edit()?.putBoolean(
                                            "show_settings_back_button", showSettingsBackButton
                                        )?.apply()
                                    }
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
                                        stringResource(R.string.portal_refresh_at_start), Modifier,
                                        selected = refreshPortalsAtStartup
                                    ) {
                                        refreshPortalsAtStartup = !refreshPortalsAtStartup
                                        preferences?.edit()?.putBoolean(
                                            "refresh_portals_at_startup", refreshPortalsAtStartup
                                        )?.apply()
                                    }
                                    MenuButton(
                                        label = stringResource(R.string.destination_sources),
                                        modifier = Modifier,
                                        onClick = { selected = Destination.SOURCES }
                                    )
                                    MenuButton(stringResource(R.string.category_settings), Modifier) {
                                        selected = Destination.CATEGORIES
                                    }
                                    MenuButton(
                                        stringResource(R.string.auto_disable_unavailable_portals), Modifier,
                                        selected = autoDisableUnavailablePortals
                                    ) {
                                        autoDisableUnavailablePortals = !autoDisableUnavailablePortals
                                        preferences?.edit()?.putBoolean(
                                            "auto_disable_unavailable_portals", autoDisableUnavailablePortals
                                        )?.apply()
                                    }
                                }
                                SettingsSection.GUIDE ->
                                    MenuButton(stringResource(R.string.epg_settings), Modifier) { epgSettingsOpen = true }
                                SettingsSection.APPEARANCE -> {
                                    MenuButton(
                                stringResource(R.string.accent_color_setting, stringResource(accentChoice.label)),
                                Modifier
                            ) {
                                onAccentChoiceChange(
                                    AccentChoice.entries[(accentChoice.ordinal + 1) % AccentChoice.entries.size]
                                )
                            }
                            MenuButton(
                                        stringResource(R.string.menu_style_setting, stringResource(mainMenuStyle.label)),
                                        Modifier
                                    ) {
                                        val styles = MainMenuStyle.entries
                                        mainMenuStyle = styles[(mainMenuStyle.ordinal + 1) % styles.size]
                                        preferences?.edit()?.putString("main_menu_style", mainMenuStyle.name)?.apply()
                                    }
                                    MenuButton(
                                        stringResource(R.string.provider_category_style_setting, stringResource(providerCategoryStyle.label)),
                                        Modifier
                                    ) {
                                        providerCategoryStyle = ProviderCategoryStyle.entries[
                                            (providerCategoryStyle.ordinal + 1) % ProviderCategoryStyle.entries.size
                                        ]
                                        preferences?.edit()?.putString("provider_category_style", providerCategoryStyle.name)?.apply()
                                    }
                                    MenuButton(stringResource(R.string.guide_grid_appearance), Modifier) {
                                        guideAppearancePage = GuideAppearancePage.GRID
                                    }
                                    MenuButton(stringResource(R.string.guide_channel_list_appearance), Modifier) {
                                        guideAppearancePage = GuideAppearancePage.CHANNELS
                                    }
                                }
                                SettingsSection.PLAYBACK -> {
                                    MenuButton(stringResource(R.string.episode_settings), Modifier) { episodeSettingsOpen = true }
                                    MenuButton(
                                        stringResource(
                                            if (subtitleDefaultEnabled) R.string.subtitles_default_on
                                            else R.string.subtitles_default_off
                                        ), Modifier, selected = subtitleDefaultEnabled
                                    ) {
                                        subtitleDefaultEnabled = !subtitleDefaultEnabled
                                        preferences?.edit()?.putBoolean("subtitle_default_enabled", subtitleDefaultEnabled)?.apply()
                                    }
                                    MenuButton(
                                        stringResource(R.string.subtitles_forced_auto), Modifier,
                                        selected = forcedSubtitlesEnabled
                                    ) {
                                        forcedSubtitlesEnabled = !forcedSubtitlesEnabled
                                        preferences?.edit()?.putBoolean("forced_subtitles_enabled", forcedSubtitlesEnabled)?.apply()
                                    }
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
                                SettingsSection.ABOUT -> {
                                    AboutSettings()
                                    Text(stringResource(R.string.diagnostics_description))
                                    Text(stringResource(
                                        if (diagnosticsTree == null) R.string.diagnostics_folder_missing
                                        else R.string.diagnostics_folder_selected
                                    ))
                                    MenuButton(stringResource(R.string.diagnostics_choose_folder), Modifier) {
                                        runCatching { chooseDiagnosticsFolder.launch(null) }
                                            .onFailure { diagnosticsStatus = context.getString(R.string.diagnostics_folder_open_failed) }
                                    }
                                    MenuButton(stringResource(R.string.diagnostics_save_file), Modifier) {
                                        runCatching { saveDiagnosticsFile.launch("StreamWatch-Diagnose.txt") }
                                            .onFailure { diagnosticsStatus = context.getString(R.string.diagnostics_folder_open_failed) }
                                    }
                                    MenuButton(stringResource(R.string.diagnostics_export), Modifier, enabled = diagnosticsTree != null) {
                                        diagnosticsTree?.let { tree ->
                                            runCatching { AppDiagnostics.export(context, tree) }
                                                .onSuccess { diagnosticsStatus = context.getString(R.string.diagnostics_export_complete) }
                                                .onFailure { diagnosticsStatus = context.getString(R.string.diagnostics_export_failed) }
                                        }
                                    }
                                    diagnosticsStatus?.let { Text(it) }
                                }
                                SettingsSection.METADATA -> {
                                    Text("Aktive Quelle für Folgeninfos: $activeEpisodeProvider")
                                    MenuButton(stringResource(R.string.tmdb_settings), Modifier) {
                                        metadataSettingsOpen = true
                                    }
                                    if (activeEpisodeProvider == "TVmaze") {
                                        MenuButton(stringResource(R.string.tvmaze_credit), Modifier) {
                                            uriHandler.openUri("https://www.tvmaze.com/")
                                        }
                                    }
                                }
                                SettingsSection.SYNC -> TraktSettings(
                                    accountName = traktSession?.username,
                                    deviceCode = traktDeviceCode,
                                    busy = traktBusy,
                                    status = traktStatus,
                                    onConnect = ::connectTrakt,
                                    onSync = { syncTrakt(forceSeriesHistory = true) },
                                    onDisconnect = ::disconnectTrakt
                                )
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
                        if (savedSources.isNotEmpty()) {
                            MenuButton(stringResource(R.string.export_portals), Modifier) {
                                exportPortals.launch("streamwatch-portals.json")
                            }
                        }
                        MenuButton(stringResource(R.string.import_portals), Modifier) {
                            importPortals.launch(arrayOf("application/json", "text/plain"))
                        }
                        Text(stringResource(R.string.portal_transfer_privacy))
                        portalTransferStatus?.let { Text(it) }
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
                        MenuButton(
                            stringResource(R.string.refresh_all_sources),
                            Modifier.fillMaxWidth(),
                            enabled = savedSources.isNotEmpty() && !connecting && !restoring,
                            onClick = { refreshSources(savedSources) }
                        )
                        MenuButton(
                            stringResource(R.string.refresh_sources),
                            Modifier.fillMaxWidth(),
                            enabled = savedSources.isNotEmpty() && !connecting && !restoring,
                            onClick = ::openSourceRefreshDialog
                        )
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
                            Text(stringResource(when (source.reachable) {
                                true -> R.string.portal_status_reachable
                                false -> R.string.portal_status_unreachable
                                null -> R.string.portal_status_unknown
                            }))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                Modifier.weight(1f),
                                selected = !mergeSources && isActive,
                                onClick = {
                                    if (mergeSources && !connecting) {
                                        address = source.address
                                        mac = source.mac
                                        editing = true
                                    } else switchSource(source)
                                }
                                )
                                val counts = sourceContentCounts[source.identity] ?: PortalContentCounts(0, 0, 0)
                                Text(
                                    "TV ${counts.tv}  ·  Filme ${counts.movies}  ·  Serien ${counts.series}",
                                    Modifier.padding(start = 10.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            MenuButton(
                                stringResource(if (source.enabled) R.string.disable_portal else R.string.enable_portal),
                                Modifier,
                                selected = source.enabled
                            ) {
                                scope.launch {
                                    try {
                                        updatePortalStatus(source.identity, enabled = !source.enabled)
                                        if (!source.enabled) refreshPortal(source.address, source.mac, activate = false)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        AppDiagnostics.record(context, "portal_toggle_failed")
                                        sourceError = R.string.portal_error
                                    }
                                }
                            }
                        }
                        Text(
                            "Zusammengeführt (Duplikate entfernt): TV ${combinedContentCounts.tv}  ·  Filme ${combinedContentCounts.movies}  ·  Serien ${combinedContentCounts.series}",
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
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
                            existingSources = savedSources,
                            onAddressChange = { address = it },
                            onMacChange = { mac = it },
                            onSelectExisting = { address = it.address; mac = it.mac },
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
                        val groups = withCategoryOrder(PortalSection.LIVE, liveCategories.keys
                            .filter { visibleCategory(PortalSection.LIVE, it, it) }
                            .sortedBy { categoryNames.values.indexOf(it) }) { it }
                        val guideChannels = remember(liveCategories, groups, category) {
                            if (category in groups) liveCategories[category].orEmpty()
                            else groups.flatMap { liveCategories[it].orEmpty() }
                        }
                        val livePlayback = playing.takeIf { vodSection == null }
                        val focusTarget = if (restoreLiveFocus) {
                            livePlayback ?: preferences?.getString("last_live_channel", null)?.let { key ->
                                guideChannels.firstOrNull { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
                            }
                        } else null
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(if (compactGuide) 3.dp else 6.dp)
                            ) {
                                val livePreview = playing.takeIf { vodSection == null }
                                val infoChannel = programmeChannel ?: livePreview
                                Row(
                                    Modifier.fillMaxWidth().focusGroup()
                                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                                        .padding(if (compactGuide) 4.dp else 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(if (compactGuide) 8.dp else 16.dp)
                                ) {
                                    if (guideAppearance.showPreview) Box(Modifier.width(guidePreviewWidth)) {
                                        PlayerFrame(
                                            player, active = livePreview != null, onStop = ::stopPlayback,
                                            onSurface = { playbackVideoSurface = it },
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
                                            val current = guideSelection(channel)
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
                                    if (vodSection == null && (playing != null || starting != null)) {
                                        MenuButton(
                                            stringResource(R.string.stop_playback),
                                            Modifier,
                                            onClick = ::stopPlayback
                                        )
                                    }
                                }
                            guideState.SaveableStateProvider("programme-guide") {
                                ProgrammeTimeline(
                                channels = guideChannels,
                                pinnedIds = pinnedProgrammeKeys,
                                highlighted = programmeChannel,
                                playing = playing.takeIf { vodSection == null },
                                focusTarget = focusTarget,
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
                                onVisibleChannels = { visible ->
                                    visibleEpgChannels = visible
                                    if (!useXmlTv) loadProviderSchedules(visible)
                                },
                                onHighlight = {
                                    programmeChannel = it
                                    guideProgramme = null
                                },
                                onProgrammeFocus = { channel, programme ->
                                    programmeChannel = channel
                                    guideProgramme = channel to programme
                                },
                                onPlay = { channel -> playChannel(channel, openFullscreenOnStart = false) },
                                onTogglePin = ::toggleProgrammeChannel
                                )
                            }
                            }
                            if (guidePanel == GuidePanel.DETAILS) {
                                val channel = programmeChannel ?: playing.takeIf { vodSection == null }
                                val programme = channel?.let(::guideSelection)
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
                                            restoreLiveFocus = false
                                            guideScroll.requestScrollToItem(0)
                                            guidePanel = GuidePanel.GRID
                                        }
                                        groups.forEach { group ->
                                            MenuButton(group, Modifier.fillMaxWidth(), selected = category == group,
                                                onLongPress = { categoryMenu = PortalSection.LIVE to group }) {
                                                category = group
                                                restoreLiveFocus = false
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
                            val trackedEntry = if (section == PortalSection.SERIES) {
                                trackedSeries(title) ?: seriesActivity.firstOrNull { entry -> entry.refs.any { it in refs } }
                            } else null
                            val trackedResume = trackedEntry?.let(::resumeFor)
                            val seriesResume = if (section == PortalSection.SERIES && trackedEntry?.isComplete != true) {
                                val savedResume = trackedResume
                                val savedEpisode = savedResume?.let { resumeEpisode(seasons.orEmpty(), it) }
                                if (savedResume == null || savedEpisode == null) null
                                else if (!isPlaybackCompleted(savedResume.progressKey, savedResume.episodeKey in trackedEntry?.watchedEpisodes.orEmpty())) {
                                    // An interrupted local/Trakt playback resumes at its exact episode,
                                    // including a start at position zero.
                                    savedResume
                                } else {
                                    // Trakt's watched history identifies the last completed episode.
                                    // Continue with the next episode that is not already completed.
                                    nextEpisodeAfter(seasons.orEmpty(), savedResume) { season, episode ->
                                        val key = titleSelection(section, episode, season).progressKey
                                        isPlaybackCompleted(key, seriesEpisodeKey(season, episode) in trackedEntry?.watchedEpisodes.orEmpty())
                                    }?.let { (season, episode) ->
                                        EpisodeResume(
                                            seriesEpisodeKey(season, episode), season.number, episode.episode,
                                            episode.name, titleSelection(section, episode, season).progressKey
                                        )
                                    }
                                }
                            } else null
                            val firstEpisode = if (section == PortalSection.SERIES && seriesResume == null) {
                                firstPlayableEpisode(seasons.orEmpty())
                            } else null
                            val movieResumeKey = if (section == PortalSection.MOVIES) {
                                val currentKey = titleSelection(section, title).progressKey
                                sequenceOf(currentKey).plus(movieHistory.asSequence()
                                    .filter { entry -> entry.refs.any { it in refs } }.map { it.progressKey })
                                    .firstOrNull { (progressPreferences?.getLong(it, 0L) ?: 0L) > 0L }
                            } else null
                            val resumeKey = seriesResume?.progressKey ?: movieResumeKey
                            val movieProgress = if (section == PortalSection.MOVIES) {
                                val key = movieResumeKey ?: titleSelection(section, title).progressKey
                                val position = progressPreferences?.getLong(key, 0L) ?: 0L
                                val duration = progressPreferences?.getLong("duration:$key", 0L) ?: 0L
                                EpisodeCardProgress(isPlaybackCompleted(key), position, duration)
                            } else null
                            val resumePosition = resumeKey?.let { progressPreferences?.getLong(it, 0L) ?: 0L }
                            val resumePercent = resumeKey?.let {
                                progressPreferences?.getFloat("trakt_percent:$it", -1f)?.takeIf { value -> value > 0f }
                            }
                            val resumeLabel = resumePercent?.let {
                                stringResource(R.string.trakt_resume_progress, it.toInt())
                            } ?: seriesResume?.let { episodeResumeLabel(it, resumePosition ?: 0L) }
                                ?: resumePosition?.takeIf { it > 0 }?.let(::playbackTime)
                            val baseTitle = (portalDetail ?: title).let { detail -> detailMetadata?.applyTo(detail) ?: detail }
                            val displayTitle = traktDetailMetadata?.let { metadata ->
                                baseTitle.copy(
                                    name = metadata.title,
                                    year = metadata.year ?: baseTitle.year,
                                    description = germanDescription(metadata.overview) ?: germanDescription(baseTitle.description)
                                )
                            } ?: tvmazeDetailMetadata?.let { metadata ->
                                baseTitle.copy(
                                    name = metadata.title,
                                    year = metadata.year ?: baseTitle.year,
                                    description = germanDescription(metadata.overview) ?: germanDescription(baseTitle.description)
                                )
                            } ?: baseTitle
                            val metadataProvider = when {
                                traktDetailMetadata != null -> "Trakt"
                                detailMetadata != null -> "TMDB"
                                tvmazeDetailMetadata != null -> "TVmaze"
                                else -> null
                            }
                            val titleOverview: @Composable () -> Unit = {
                            TitleDetails(
                                displayTitle, section,
                                metadataRating = traktDetailMetadata?.rating ?: detailMetadata?.rating ?: tvmazeDetailMetadata?.rating,
                                metadataProvider = metadataProvider,
                                favorite = titleKey(section, title) in favorites,
                                saved = isTitleSaved(section, title),
                                newEpisodes = if (section == PortalSection.SERIES) {
                                    trackedSeries(title)?.unseen?.size ?: 0
                                } else 0,
                                upcomingEpisode = trackedSeries(title)?.nextAirDate?.let { rawDate ->
                                    val date = runCatching {
                                        java.time.LocalDate.parse(rawDate).format(
                                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                        )
                                    }.getOrNull() ?: return@let null
                                    context.getString(
                                        R.string.next_episode_airdate,
                                        trackedSeries(title)?.nextEpisodeName ?: "",
                                        date
                                    )
                                },
                                resumeLabel = resumeLabel,
                                onResume = if (resumeKey != null && (section == PortalSection.MOVIES || seasons != null)) {
                                    { selectTitle(section, title, seriesResume, movieResumeKey) }
                                } else null,
                                firstEpisodeAction = firstEpisode?.let { (season, episode) ->
                                    {
                                        openedSeason = season
                                        selectTitle(section, episode)
                                    }
                                },
                                firstEpisodeLabel = firstEpisode?.let { (season, episode) ->
                                    val number = episode.episode
                                    if (season.number == null || number.isNullOrBlank()) stringResource(R.string.play_first_episode)
                                    else stringResource(R.string.play_numbered_episode, season.number, number)
                                },
                                focusPrimary = focusTitlePrimary,
                                onPrimaryFocused = { focusTitlePrimary = false },
                                compact = compactMedia,
                                onBack = ::navigateBack,
                                onPlay = { selectTitle(section, title) },
                                watchProgress = movieProgress,
                                onWatchStatus = if (section == PortalSection.MOVIES || seasons.orEmpty().any { it.episodes.isNotEmpty() }) {
                                    {
                                        val items = if (section == PortalSection.SERIES) {
                                            seasons.orEmpty().flatMap { season -> season.episodes.map { season to it } }
                                        } else emptyList()
                                        watchStatusAction = { watched -> setWatched(section, title, items, watched) }
                                    }
                                } else null,
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
                                    onSeasonWatchStatus = { season ->
                                        watchStatusAction = { watched ->
                                            setWatched(section, title, season.episodes.map { season to it }, watched)
                                        }
                                    },
                                    onEpisodeWatchStatus = { season, episode ->
                                        watchStatusAction = { watched -> setWatched(section, title, listOf(season to episode), watched) }
                                    },
                                    episodeProgress = { season, episode ->
                                        val key = titleSelection(section, episode, season).progressKey
                                        val savedKey = trackedResume?.takeIf { it.episodeKey == seriesEpisodeKey(season, episode) }
                                            ?.progressKey ?: key
                                        val prefs = progressPreferences
                                        val episodeKey = seriesEpisodeKey(season, episode)
                                        val completed = isPlaybackCompleted(savedKey, episodeKey in trackedEntry?.watchedEpisodes.orEmpty())
                                        val localPosition = prefs?.getLong(savedKey, 0L) ?: 0L
                                        val localDuration = prefs?.getLong("duration:$savedKey", 0L) ?: 0L
                                        val traktPercent = prefs?.getFloat("trakt_percent:$savedKey", 0f) ?: 0f
                                        val position = localPosition.takeIf { it > 0L } ?:
                                            (traktPercent.coerceIn(0f, 100f) * 10L).toLong()
                                        val duration = localDuration.takeIf { it > 0L } ?:
                                            if (traktPercent > 0f) 1000L else 0L
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
                            val showContinue = seriesActivityView == SeriesActivityView.CONTINUE
                            val entries = seriesActivity.filter { entry ->
                                (when {
                                    showHistory -> entry.viewed && !entry.hiddenFromHistory
                                    showContinue -> entry.viewed && !entry.hiddenFromHistory && entry.isComplete != true
                                    else -> entry.saved
                                }) && hasPortalReference(
                                    PortalSection.SERIES,
                                    PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                    entry.refs
                                )
                            }.let { matched ->
                                if (showHistory || showContinue) matched.sortedByDescending { it.watchedAt }.let {
                                    if (showHistory) it.take(10) else it
                                } else matched.sortedBy { it.name }
                            }
                            MenuButton(stringResource(R.string.back_to_categories), Modifier) { seriesActivityView = null }
                            Text(stringResource(when {
                                showHistory -> R.string.series_history
                                showContinue -> R.string.continue_watching
                                else -> R.string.watchlist
                            }))
                            if (entries.isEmpty()) Text(stringResource(R.string.no_tracked_series))
                            LazyColumn(Modifier.weight(1f)) {
                                items(entries, key = { it.key }) { entry ->
                                    val label = if (entry.unseen.isEmpty()) entry.name else
                                        "${entry.name} · ${stringResource(R.string.new_episode_count, entry.unseen.size)}"
                                    // A Trakt-Watched-Eintrag kann ohne Prozentwert gespeichert sein.
                                    // Die Detailseite berechnet dann aus der letzten gesehenen Folge
                                    // die nächste nicht abgeschlossene Folge.
                                    val resume = resumeFor(entry)
                                    val canContinue = entry.isComplete != true && resume != null &&
                                        (entry.isComplete == false || progressPreferences?.getBoolean("completed:${resume.progressKey}", false) != true)
                                    Row(
                                        Modifier.fillMaxWidth().background(
                                            MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)
                                        ).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HistoryArtwork(
                                            section,
                                            PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                            entry.refs
                                        )
                                        Column(Modifier.weight(1f)) {
                                            MenuButton(label, Modifier.fillMaxWidth()) { openTrackedSeries(entry) }
                                            resume?.takeIf { canContinue }?.let { episode ->
                                                val position = progressPreferences?.getLong(episode.progressKey, 0L) ?: 0L
                                                Text(
                                                    episodeResumeLabel(episode, position),
                                                    Modifier.padding(start = 10.dp, bottom = 6.dp),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        resume?.takeIf { canContinue }?.let {
                                            MenuButton(stringResource(R.string.resume_playback), Modifier) {
                                                openTrackedSeries(entry)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (section == PortalSection.MOVIES && movieWatchlistOpen) {
                            Text(stringResource(R.string.watchlist), style = MaterialTheme.typography.titleMedium)
                            val entries = movieBookmarks.filter { entry ->
                                entry.progressKey in watchlist && hasPortalReference(
                                    PortalSection.MOVIES,
                                    PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                    entry.refs
                                )
                            }
                            val legacy = (catalogs[section]?.titles.orEmpty() + groupCatalog?.titles.orEmpty())
                                .distinctBy { titleKey(section, it) }
                                .filter { title ->
                                    titleKey(section, title) in watchlist &&
                                        entries.none { it.progressKey == titleKey(section, title) }
                                }
                            if (entries.isEmpty() && legacy.isEmpty()) Text(stringResource(R.string.no_movie_bookmarks))
                            LazyColumn(Modifier.weight(1f)) {
                                items(entries, key = { it.progressKey }) { entry ->
                                    Row(
                                        Modifier.fillMaxWidth().background(
                                            MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)
                                        ).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HistoryArtwork(
                                            section, PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                            entry.refs
                                        )
                                        MenuButton(entry.name, Modifier.weight(1f)) { openWatchedMovie(entry, resume = false) }
                                    }
                                }
                                items(legacy) { title ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Poster(title, Modifier.width(104.dp).padding(end = 10.dp))
                                        MenuButton(displayVodTitle(title), Modifier.weight(1f)) { showTitle(section, title) }
                                    }
                                }
                            }
                        } else if (section == PortalSection.MOVIES && movieHistoryOpen) {
                            MenuButton(stringResource(R.string.back_to_categories), Modifier) { movieHistoryOpen = false }
                            Text(stringResource(R.string.movie_history))
                            val entries = movieHistory.filter { entry ->
                                hasPortalReference(
                                    PortalSection.MOVIES,
                                    PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                    entry.refs
                                )
                            }.sortedByDescending { it.watchedAt }.take(10)
                            if (entries.isEmpty()) Text(stringResource(R.string.no_watched_movies))
                            LazyColumn(Modifier.weight(1f)) {
                                items(entries, key = { it.progressKey }) { entry ->
                                    Row(
                                        Modifier.fillMaxWidth().background(
                                            MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)
                                        ).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HistoryArtwork(
                                            section,
                                            PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb),
                                            entry.refs
                                        )
                                        val position = progressPreferences?.getLong(entry.progressKey, 0L) ?: 0L
                                        val incomplete = progressPreferences?.getBoolean("completed:${entry.progressKey}", false) != true &&
                                            position > 0L && (progressPreferences?.getFloat("trakt_percent:${entry.progressKey}", 0f) ?: 0f) < 90f
                                        Column(Modifier.weight(1f)) {
                                            MenuButton(entry.name, Modifier.fillMaxWidth()) {
                                                openWatchedMovie(entry, resume = false)
                                            }
                                            if (incomplete) Text(
                                                stringResource(R.string.saved_playback_position, playbackTime(position)),
                                                Modifier.padding(start = 10.dp, bottom = 6.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (incomplete) MenuButton(stringResource(R.string.resume_playback), Modifier) {
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
                                        categoryTitle = group.name,
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
                                    focusFirstTitle = focusFirstTitle,
                                    onTitleFocusRestored = {
                                        catalogueReturnTitle = null
                                        focusFirstTitle = false
                                    },
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
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topNavigation()
                    header()
                    if (lastBack != 0L && !mainMenuOpen) Text(
                        stringResource(R.string.press_back_again_to_exit),
                        Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(
                        Modifier.weight(1f).fillMaxWidth().guideNavigationInput(interceptRemote = { input ->
                            !input.vertical && (guidePanel != GuidePanel.GRID ||
                                input != GuideInput.REMOTE_RIGHT && controlBindings[input]?.let {
                                    it != ControlAction.DEFAULT && it != ControlAction.CHANNELS
                                } == true)
                        }) {
                            if (!runMappedControl(it)) navigateGuide(it)
                        }
                    ) { content() }
                }
            } else if (vodSectionForScreen != null) {
                val section = vodSectionForScreen
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topNavigation()
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                            visibleVodGroups.forEach { group ->
                                MenuButton(
                                    group.name,
                                    Modifier,
                                    providerMark = providerCategoryMark(group.name).takeIf {
                                        providerCategoryStyle != ProviderCategoryStyle.LABELS && providerCategoryIcon(group.name) == null
                                    },
                                    providerIcon = providerCategoryIcon(group.name).takeIf {
                                        providerCategoryStyle != ProviderCategoryStyle.LABELS
                                    },
                                    providerMarkOnly = providerCategoryStyle == ProviderCategoryStyle.SYMBOLS,
                                    selected = selectedGroup?.first == section && selectedGroup?.second?.id == group.id &&
                                        !movieHistoryOpen && !movieWatchlistOpen && seriesActivityView == null,
                                    hiddenIndicator = categoryKey(section, group.id) in hiddenCategories,
                                    onLongPress = { categoryMenu = section to group.id }
                                ) { openVodCategory(group) }
                            }
                        MenuButton(
                            stringResource(R.string.manage_categories),
                            Modifier
                        ) {
                            categoryMenu = section to null
                        }
                        if (section == PortalSection.SERIES) MenuButton(
                            stringResource(R.string.continue_watching), Modifier,
                            selected = seriesActivityView == SeriesActivityView.CONTINUE
                        ) {
                            closeSeries()
                            details = null
                            seriesActivityView = SeriesActivityView.CONTINUE
                        }
                        MenuButton(
                            stringResource(R.string.watchlist), Modifier,
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
                            stringResource(if (section == PortalSection.MOVIES) R.string.movie_history else R.string.series_history),
                            Modifier,
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
                    if (restoring || connecting || liveGroupsLoading || traktBusy || startupPortalRefresh != null || startupPortalCheck != null ||
                        cataloguePlaybackView == VodLiveView.CORNER && playing != null
                    ) header()
                    content()
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topNavigation()
                    if (destination == Destination.SETTINGS) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SettingsSection.entries.forEach { item ->
                                    MenuButton(
                                        stringResource(item.label), Modifier,
                                        selected = settingsSection == item
                                    ) {
                                        lastSettingsSection = item
                                        settingsSection = item
                                    }
                                }
                            }
                        }
                    }
                    if (destination != Destination.SEARCH) header()
                    if (lastBack != 0L && !mainMenuOpen) Text(
                        stringResource(R.string.press_back_again_to_exit),
                        Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(
                        Modifier.weight(1f).then(
                            if (destination == Destination.SEARCH) Modifier else Modifier.verticalScroll(contentScroll)
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { content() }
                }
            }
            LaunchedEffect(destination, settingsSection) {
                if (!inGuide && !restoreLiveFocus) {
                    if (destination == Destination.SETTINGS) {
                        contentScroll.scrollTo(0)
                        withFrameNanos { }
                        if (settingsSection == null) {
                            settingsFocusRequesters[SettingsSection.entries.first()]?.requestFocus() ?: initialFocus.requestFocus()
                        } else settingsFocusRequesters[settingsSection]?.requestFocus() ?: initialFocus.requestFocus()
                    } else initialFocus.requestFocus()
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
    existingSources: List<SavedPortal>,
    onAddressChange: (String) -> Unit,
    onMacChange: (String) -> Unit,
    onSelectExisting: (SavedPortal) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var addressMode by remember { mutableStateOf(portalAddressMode(address)) }
    var selectedSavedSource by remember { mutableStateOf<SavedPortal?>(null) }
    val initialMacHex = remember {
        runCatching { normalizePortalMac(mac).filter(Char::isLetterOrDigit) }
            .getOrDefault("001A79000000")
    }
    var prefix by remember { mutableStateOf(initialMacHex.take(6)) }
    var customPrefixDigits by remember { mutableStateOf(initialMacHex.take(6).toList()) }
    var customPrefixInput by remember { mutableStateOf(initialMacHex.take(6)) }
    var customPrefix by remember { mutableStateOf(initialMacHex.take(6) !in portalMacPrefixes) }
    var deviceDigits by remember { mutableStateOf(initialMacHex.drop(6).take(6).padEnd(6, '0').toList()) }
    var prefixMenu by remember { mutableStateOf(false) }
    var savedPortalMenu by remember { mutableStateOf(false) }
    var addressModeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (mac.isBlank()) onMacChange(initialMacHex.chunked(2).joinToString(":"))
    }

    fun publishMac() {
        val prefixValue = if (customPrefix) customPrefixDigits.joinToString("") else prefix
        val hex = (prefixValue + deviceDigits.joinToString(""))
            .uppercase(java.util.Locale.ROOT).take(12).padEnd(12, '0')
        onMacChange(hex.chunked(2).joinToString(":"))
    }

    fun setMac(value: String) {
        val hex = runCatching { normalizePortalMac(value).filter(Char::isLetterOrDigit) }
            .getOrDefault("001A79000000")
        prefix = hex.take(6)
        customPrefixDigits = hex.take(6).toList()
        customPrefixInput = hex.take(6)
        customPrefix = prefix !in portalMacPrefixes
        deviceDigits = hex.drop(6).take(6).toList()
        onMacChange(hex.chunked(2).joinToString(":"))
    }

    fun chooseAddressMode(next: PortalAddressMode) {
        selectedSavedSource = null
        if (next == PortalAddressMode.SAVED) {
            addressMode = next
            onAddressChange("")
            onMacChange("")
            return
        }
        val host = portalAddressHost(address, addressMode)
        addressMode = next
        onAddressChange(if (next == PortalAddressMode.CUSTOM || host.isBlank()) "" else portalComposeAddress(next, host))
        if (customPrefix) customPrefixInput = customPrefixDigits.joinToString("")
    }

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
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
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
                    Box {
                        MenuButton(
                            stringResource(addressMode.label), Modifier.fillMaxWidth()
                        ) { addressModeMenu = true }
                        DropdownMenu(addressModeMenu, onDismissRequest = { addressModeMenu = false }) {
                            PortalAddressMode.entries.filter {
                                it != PortalAddressMode.SAVED || existingSources.isNotEmpty()
                            }.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.label)) },
                                    onClick = { chooseAddressMode(mode); addressModeMenu = false }
                                )
                            }
                        }
                    }
                    if (addressMode == PortalAddressMode.SAVED) {
                        Box {
                            MenuButton(
                                selectedSavedSource?.let { selected ->
                                    val index = existingSources.indexOfFirst { it.identity == selected.identity }
                                    val host = runCatching { java.net.URI(selected.address).host }.getOrNull()
                                        .orEmpty().ifBlank { "Portal ${index + 1}" }
                                    stringResource(R.string.saved_portal_label, index + 1, host)
                                } ?: stringResource(R.string.portal_pick_saved),
                                Modifier.fillMaxWidth(),
                                onClick = { savedPortalMenu = true }
                            )
                            DropdownMenu(savedPortalMenu, onDismissRequest = { savedPortalMenu = false }) {
                                existingSources.forEachIndexed { index, source ->
                                    val host = runCatching { java.net.URI(source.address).host }.getOrNull()
                                        .orEmpty().ifBlank { "Portal ${index + 1}" }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.saved_portal_label, index + 1, host)) },
                                        onClick = {
                                            selectedSavedSource = source
                                            addressMode = PortalAddressMode.SAVED
                                            onSelectExisting(source)
                                            setMac(source.mac)
                                            savedPortalMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (addressMode == PortalAddressMode.CUSTOM) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = onAddressChange,
                            label = { Text(stringResource(R.string.portal_address)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(addressMode.prefix)
                            OutlinedTextField(
                                value = portalAddressHost(address, addressMode),
                                onValueChange = { host ->
                                    onAddressChange(if (host.isBlank()) "" else portalComposeAddress(addressMode, host))
                                },
                                label = { Text(stringResource(R.string.portal_address_host)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text("/c")
                        }
                    }
                    if (addressMode != PortalAddressMode.SAVED) {
                        Text(stringResource(R.string.portal_mac_prefix))
                        Box {
                            MenuButton(
                                if (customPrefix) stringResource(R.string.portal_mac_prefix_custom)
                                else "${prefix.chunked(2).joinToString(":")}: ",
                                Modifier.fillMaxWidth()
                            ) { prefixMenu = true }
                            DropdownMenu(prefixMenu, onDismissRequest = { prefixMenu = false }) {
                                portalMacPrefixes.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text("${value.chunked(2).joinToString(":")}: ") },
                                        onClick = {
                                            prefix = value
                                            customPrefix = false
                                            publishMac()
                                            prefixMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.portal_mac_prefix_custom)) },
                                    onClick = {
                                        customPrefix = true
                                        customPrefixInput = customPrefixDigits.joinToString("")
                                        publishMac()
                                        prefixMenu = false
                                    }
                                )
                            }
                        }
                        if (customPrefix) {
                            Text(stringResource(R.string.portal_mac_custom_prefix_digits))
                            OutlinedTextField(
                                value = customPrefixInput,
                                onValueChange = { raw ->
                                    val clean = raw.uppercase(java.util.Locale.ROOT)
                                        .filter { it in "0123456789ABCDEF" }.take(6)
                                    customPrefixInput = clean
                                    customPrefixDigits = clean.padEnd(6, '0').toList()
                                    if (clean.length == 6) publishMac()
                                },
                                label = { Text(stringResource(R.string.portal_mac_prefix_custom)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii,
                                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters
                                )
                            )
                            MacDigitRow(customPrefixDigits) { index, value ->
                                customPrefixDigits = customPrefixDigits.toMutableList().also { it[index] = value }
                                customPrefixInput = customPrefixDigits.joinToString("")
                                publishMac()
                            }
                        }
                        Text(stringResource(R.string.portal_mac_device_digits))
                        MacDigitRow(deviceDigits) { index, value ->
                            deviceDigits = deviceDigits.toMutableList().also { it[index] = value }
                            publishMac()
                        }
                    }
                    Text(stringResource(R.string.portal_mac_preview, mac), style = MaterialTheme.typography.bodySmall)
                    val addressReady = when (addressMode) {
                        PortalAddressMode.SAVED -> selectedSavedSource != null
                        PortalAddressMode.CUSTOM -> address.isNotBlank()
                        else -> portalAddressHost(address, addressMode).isNotBlank()
                    }
                    val macReady = addressMode == PortalAddressMode.SAVED ||
                        (!customPrefix || customPrefixInput.length == 6) &&
                        runCatching { normalizePortalMac(mac) }.isSuccess
                    MenuButton(
                        stringResource(R.string.connect_portal),
                        Modifier.fillMaxWidth(),
                        enabled = addressReady && macReady,
                        onClick = onSubmit
                    )
                    MenuButton(stringResource(android.R.string.cancel), Modifier, onClick = onDismiss)
                }
            }
        }
    }
}

private enum class PortalAddressMode(val prefix: String, val label: Int) {
    HTTP("http://", R.string.portal_address_http),
    HTTPS("https://", R.string.portal_address_https),
    CUSTOM("", R.string.portal_address_manual),
    SAVED("", R.string.portal_address_saved)
}

private val portalMacPrefixes = listOf(
    "001A78", "001A79", "001B78", "001B79", "001C78", "001C79",
    "002A01", "002A78", "002A79"
)

private fun portalAddressMode(address: String): PortalAddressMode = when {
    address.isBlank() -> PortalAddressMode.HTTP
    address.startsWith("https://", ignoreCase = true) && address.endsWith("/c", ignoreCase = true) -> PortalAddressMode.HTTPS
    address.startsWith("http://", ignoreCase = true) && address.endsWith("/c", ignoreCase = true) -> PortalAddressMode.HTTP
    else -> PortalAddressMode.CUSTOM
}

private fun portalAddressHost(address: String, mode: PortalAddressMode): String {
    var value = address.trim()
    if (value.startsWith("https://", ignoreCase = true)) value = value.drop(8)
    else if (value.startsWith("http://", ignoreCase = true)) value = value.drop(7)
    value = value.trimEnd('/')
    if (value.endsWith("/c", ignoreCase = true)) value = value.dropLast(2).trimEnd('/')
    return value
}

private fun portalComposeAddress(mode: PortalAddressMode, host: String): String {
    var normalized = host.trim()
    if (normalized.startsWith("https://", ignoreCase = true)) normalized = normalized.drop(8)
    else if (normalized.startsWith("http://", ignoreCase = true)) normalized = normalized.drop(7)
    normalized = normalized.removePrefix("/").trimEnd('/')
        .let { if (it.endsWith("/c", ignoreCase = true)) it.dropLast(2).trimEnd('/') else it }
    return if (normalized.isBlank()) "" else "${mode.prefix}$normalized/c"
}

@Composable
private fun MacDigitRow(values: List<Char>, onChange: (Int, Char) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEachIndexed { index, value ->
            if (index > 0 && index % 2 == 0) Text(":", style = MaterialTheme.typography.titleMedium)
            var expanded by remember(index) { mutableStateOf(false) }
            Box {
                MenuButton(value.toString(), Modifier.width(36.dp)) { expanded = true }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    "0123456789ABCDEF".forEach { digit ->
                        DropdownMenuItem(
                            text = { Text(digit.toString()) },
                            onClick = { onChange(index, digit); expanded = false }
                        )
                    }
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
    loading: Boolean,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.padding(end = 9.dp).width(3.dp).height(22.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
            Column {
                Text(
                    category ?: stringResource(R.string.channel_categories),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (visibleChannels == null) "KATEGORIEN" else "SENDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (groups.isEmpty()) {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.startup_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.no_channels), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.padding(end = 9.dp).size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    )
                    Text(
                        if (playing != null) "JETZT LIVE" else "LIVE-TV",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    playing?.let { channel ->
                        Text(
                            "  ·  ${channel.name}",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
internal fun PlayerFrame(
    player: ExoPlayer?,
    active: Boolean,
    showEmpty: Boolean = true,
    fullscreen: Boolean = false,
    isLivePlayback: Boolean = false,
    onStop: () -> Unit,
    onSurface: ((SurfaceView?) -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
    onGuideInput: ((GuideInput) -> Unit)? = null,
    interceptGuideKey: (GuideInput) -> Boolean = { true },
    onTouchAction: (() -> Boolean)? = null,
    onMediaKey: (() -> Boolean)? = null,
    controlsRequest: Int = 0,
    controlsDismissRequest: Int = 0,
    controlsAllowed: Boolean = true,
    actionPromptActive: Boolean = false,
    subtitleDefaultEnabled: Boolean = false,
    forcedSubtitlesEnabled: Boolean = true,
    subtitleSeriesKey: String? = null,
    subtitlePlaybackKey: String? = null,
    subtitlePreferences: android.content.SharedPreferences? = null,
    onEpisodeActionConfirm: (() -> Unit)? = null,
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    episodeActionVisible: Boolean = false,
    onControlsVisible: ((Boolean) -> Unit)? = null
) {
    val accentArgb = MaterialTheme.colorScheme.primary.toArgb()
    var subtitleDialogOpen by remember(player) { mutableStateOf(false) }
    var saveSubtitleChoiceForSeries by remember(player, subtitleSeriesKey) { mutableStateOf(false) }
    var oneTimeSubtitleChoice by remember(player, subtitlePlaybackKey) { mutableStateOf(false) }
    val currentOneTimeSubtitleChoice by rememberUpdatedState(oneTimeSubtitleChoice)
    val buffering by produceState(
        initialValue = false,
        player, active, fullscreen
    ) {
        if (player == null || !active || !fullscreen) {
            value = false
        } else {
            var playbackStarted = player.isPlaying || player.playbackState == Player.STATE_READY
            var bufferingStartedAt: Long? = null
            while (true) {
                val now = SystemClock.elapsedRealtime()
                when {
                    player.isPlaying || player.playbackState == Player.STATE_READY -> {
                        playbackStarted = true
                        bufferingStartedAt = null
                        value = false
                    }
                    playbackStarted && player.playWhenReady && player.playbackState == Player.STATE_BUFFERING -> {
                        if (bufferingStartedAt == null) bufferingStartedAt = now
                        value = now - bufferingStartedAt!! >= 1_500L
                    }
                    else -> {
                        bufferingStartedAt = null
                        value = false
                    }
                }
                delay(200)
            }
        }
    }
    DisposableEffect(player, subtitleSeriesKey, subtitlePlaybackKey, subtitleDefaultEnabled, forcedSubtitlesEnabled) {
        val activePlayer = player
        if (activePlayer == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                if (currentOneTimeSubtitleChoice) return
                val choices = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMap { group ->
                        (0 until group.length).filter(group::isTrackSupported).map { index ->
                            group to index
                        }
                    }
                val preferredLanguage = java.util.Locale.getDefault().language
                val forced = choices.firstOrNull { (group, index) ->
                    val format = group.getTrackFormat(index)
                    format.selectionFlags and C.SELECTION_FLAG_FORCED != 0 &&
                        format.language?.substringBefore('-') == preferredLanguage
                } ?: choices.firstOrNull { (group, index) ->
                    val format = group.getTrackFormat(index)
                    format.selectionFlags and C.SELECTION_FLAG_FORCED != 0 && format.language.isNullOrBlank()
                }
                val languageChoice = choices.firstOrNull { (group, index) ->
                    group.getTrackFormat(index).language?.substringBefore('-') == preferredLanguage
                }
                val savedMode = subtitleSeriesKey?.let { key ->
                    subtitlePreferences?.getString("subtitle_series:$key", null)
                }
                val enabled = when (savedMode) {
                    "OFF" -> false
                    "FORCED" -> forced != null
                    "ON" -> choices.isNotEmpty()
                    else -> subtitleDefaultEnabled || (subtitleSeriesKey != null && forcedSubtitlesEnabled && forced != null)
                }
                val chosen = when {
                    !enabled -> null
                    forced != null && (savedMode == "FORCED" || savedMode == null && forcedSubtitlesEnabled) -> forced
                    else -> languageChoice ?: choices.firstOrNull()
                }
                val current = activePlayer.trackSelectionParameters
                val updated = current.buildUpon()
                if (chosen == null) {
                    updated.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                } else {
                    updated.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setOverrideForType(TrackSelectionOverride(chosen.first.mediaTrackGroup, listOf(chosen.second)))
                }
                val next = updated.build()
                if (next != current) activePlayer.trackSelectionParameters = next
            }
        }
        activePlayer.addListener(listener)
        onDispose { activePlayer.removeListener(listener) }
    }
    val stop by rememberUpdatedState(onStop)
    val openFullscreen by rememberUpdatedState(onOpenFullscreen)
    val guideInput by rememberUpdatedState(onGuideInput)
    val interceptDirection by rememberUpdatedState(interceptGuideKey)
    val touchAction by rememberUpdatedState(onTouchAction)
    val mediaKey by rememberUpdatedState(onMediaKey)
    val allowControls by rememberUpdatedState(controlsAllowed)
    val livePlayback by rememberUpdatedState(isLivePlayback)
    val controlsVisibility by rememberUpdatedState(onControlsVisible)
    val previousEpisode by rememberUpdatedState(onPreviousEpisode)
    val nextEpisode by rememberUpdatedState(onNextEpisode)
    val canPreviousEpisode by rememberUpdatedState(hasPreviousEpisode)
    val canNextEpisode by rememberUpdatedState(hasNextEpisode)
    val captureActionKeys by rememberUpdatedState(actionPromptActive)
    val confirmEpisodeAction by rememberUpdatedState(onEpisodeActionConfirm)
    fun enableRemoteFocus(view: android.view.View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        val focused = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.argb(
                85,
                android.graphics.Color.red(accentArgb),
                android.graphics.Color.green(accentArgb),
                android.graphics.Color.blue(accentArgb)
            ))
            setStroke((2 * view.resources.displayMetrics.density).toInt().coerceAtLeast(2), accentArgb)
        }
        val idle = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.TRANSPARENT)
        }
        view.foreground = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), idle)
        }
        view.nextFocusUpId = view.id
        view.nextFocusDownId = view.id
    }
    fun configureBottomFocus(view: PlayerView) {
        val progress = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress) ?: return
        val settingsButton = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)
        // Media3 disables this button when it cannot currently find selectable tracks.
        // Keep it focusable on TV so the settings entry remains in the D-pad route; its
        // own click handler still decides which settings are actually available.
        if (settingsButton?.visibility == android.view.View.VISIBLE) settingsButton.isEnabled = true
        val bottomButtons = listOf(
            view.findViewById(androidx.media3.ui.R.id.exo_settings),
            view.findViewWithTag<android.view.View>("player_subtitles_action"),
            view.findViewById(androidx.media3.ui.R.id.exo_fullscreen),
            view.findViewById(androidx.media3.ui.R.id.exo_overflow_show)
        ).filterNotNull()
            .filter { it.visibility == android.view.View.VISIBLE && it.isEnabled }
        if (bottomButtons.isEmpty()) {
            progress.nextFocusDownId = progress.id
            return
        }
        progress.nextFocusDownId = bottomButtons.first().id
        bottomButtons.forEachIndexed { index, button ->
            button.nextFocusUpId = progress.id
            button.nextFocusLeftId = bottomButtons.getOrNull(index - 1)?.id ?: button.id
            button.nextFocusRightId = bottomButtons.getOrNull(index + 1)?.id ?: button.id
        }
    }
    var shownControlsRequest by remember { mutableIntStateOf(controlsRequest) }
    var previousDismissRequest by remember { mutableIntStateOf(controlsDismissRequest) }
    var previousEpisodeAction by remember { mutableStateOf(episodeActionVisible) }
    Box(
        modifier = (if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF171717), Color(0xFF080808))),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (player != null && active) {
            AndroidView(
                factory = { context ->
                    object : PlayerView(context) {
                        private var controlsVisible = false
                        private val hideControls = Runnable { hideController() }

                        init {
                            if (fullscreen) {
                                isFocusable = true
                                controllerShowTimeoutMs = 0
                                setControllerAnimationEnabled(false)
                                setControllerVisibilityListener(ControllerVisibilityListener { visibility ->
                                    controlsVisible = visibility == android.view.View.VISIBLE
                                    controlsVisibility?.invoke(controlsVisible)
                                    removeCallbacks(hideControls)
                                    if (controlsVisible) {
                                        postDelayed(hideControls, 6_000L)
                                        post {
                                            if (controlsVisible) focusInitialControllerTarget()
                                        }
                                    } else if (allowControls) {
                                        post { if (!controlsVisible && allowControls) requestFocus() }
                                    }
                                })
                            }
                        }

                        private fun extendControlsTimeout() {
                            removeCallbacks(hideControls)
                            postDelayed(hideControls, 6_000L)
                        }

                        private fun focusInitialControllerTarget() {
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)?.requestFocus()
                        }

                        override fun onDetachedFromWindow() {
                            removeCallbacks(hideControls)
                            super.onDetachedFromWindow()
                        }

                        override fun performClick(): Boolean =
                            touchAction?.invoke() == true || super.performClick()

                        override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                            if (fullscreen && controlsVisible) extendControlsTimeout()
                            return super.dispatchTouchEvent(event)
                        }

                        override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                            if (fullscreen && captureActionKeys) {
                                val direction = when (event.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> GuideInput.REMOTE_UP
                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GuideInput.REMOTE_DOWN
                                    else -> null
                                }
                                if (direction != null) {
                                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) {
                                        guideInput?.invoke(direction)
                                    }
                                    return true
                                }
                                if (event.keyCode in setOf(
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER,
                                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                        android.view.KeyEvent.KEYCODE_BUTTON_A
                                    )
                                ) {
                                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) {
                                        confirmEpisodeAction?.invoke()
                                    }
                                    return true
                                }
                            }
                            if (event.keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                                event.action == android.view.KeyEvent.ACTION_DOWN &&
                                (event.repeatCount == 0 && mediaKey?.invoke() == true)
                            ) return true
                            if (fullscreen && allowControls) {
                                if (!livePlayback && !controlsVisible && event.keyCode in setOf(
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                                    ) && player?.isCurrentMediaItemSeekable == true
                                ) {
                                    val delta = if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) -10_000L else 10_000L
                                    if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                        val duration = player?.duration?.takeIf { it > 0L } ?: Long.MAX_VALUE
                                        player?.seekTo((player.currentPosition + delta).coerceIn(0L, duration))
                                        if (!controlsVisible) {
                                            showController()
                                            post {
                                                findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)
                                                    ?.requestFocus()
                                            }
                                        }
                                    }
                                    return true
                                }
                                if (controlsVisible) {
                                    extendControlsTimeout()
                                    if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                                        event.repeatCount == 0 && event.keyCode in setOf(
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                                            android.view.KeyEvent.KEYCODE_DPAD_UP
                                        )
                                    ) {
                                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.let { settings ->
                                            if (settings.visibility == android.view.View.VISIBLE) settings.isEnabled = true
                                        }
                                        configureBottomFocus(this)
                                        val focused = findFocus()
                                        val progress = findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)
                                        val bottomTargets = listOfNotNull(
                                            findViewWithTag<android.view.View>("player_subtitles_action"),
                                            findViewById(androidx.media3.ui.R.id.exo_settings),
                                            findViewById(androidx.media3.ui.R.id.exo_fullscreen),
                                            findViewById(androidx.media3.ui.R.id.exo_overflow_show)
                                        )
                                        val bottom = bottomTargets.firstOrNull {
                                            it.visibility == android.view.View.VISIBLE && it.isEnabled && it.isFocusable
                                        }
                                        val moved = when {
                                            event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && focused === progress ->
                                                bottom?.requestFocus() == true
                                            event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP && bottom != null &&
                                            focused in bottomTargets -> progress?.requestFocus() == true
                                            else -> false
                                        }
                                        if (moved) return true
                                    }
                                } else {
                                    val direction = when (event.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> GuideInput.REMOTE_UP
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GuideInput.REMOTE_DOWN
                                        else -> null
                                    }
                                    val onDirection = guideInput
                                    if (direction != null && onDirection != null && interceptDirection(direction)) {
                                        if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) onDirection(direction)
                                        return true
                                    }
                                    if (direction?.vertical == true && onDirection != null) return true
                                    if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                                    ) {
                                        if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) showController()
                                        return true
                                    }
                                }
                            }
                            return super.dispatchKeyEvent(event)
                        }
                    }.apply {
                        useController = true
                        controllerAutoShow = false
                        listOf(
                            androidx.media3.ui.R.id.exo_prev,
                            androidx.media3.ui.R.id.exo_next,
                            androidx.media3.ui.R.id.exo_rew,
                            androidx.media3.ui.R.id.exo_ffwd
                        ).forEach { id ->
                            findViewById<android.view.View>(id)?.visibility =
                                if (livePlayback) android.view.View.GONE else android.view.View.VISIBLE
                        }
                        findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_center_controls)?.let { row ->
                            val size = (48 * context.resources.displayMetrics.density).toInt()
                            fun actionButton(icon: Int, label: Int, action: () -> Unit) = ImageButton(context).apply {
                                setImageResource(icon)
                                contentDescription = context.getString(label)
                                tooltipText = contentDescription
                                isFocusable = true
                                val background = TypedValue()
                                if (context.theme.resolveAttribute(
                                        android.R.attr.selectableItemBackgroundBorderless, background, true
                                    )
                                ) setBackgroundResource(background.resourceId)
                                setOnClickListener { action() }
                            }
                            val previous = actionButton(android.R.drawable.ic_media_previous, R.string.previous_episode) {
                                previousEpisode?.invoke()
                            }
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
                            button.tag = "stop_playback_action"
                            val next = actionButton(android.R.drawable.ic_media_next, R.string.next_episode) {
                                nextEpisode?.invoke()
                            }
                            val pause = row.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
                            if (pause != null) {
                                previous.id = android.view.View.generateViewId()
                                button.id = android.view.View.generateViewId()
                                next.id = android.view.View.generateViewId()
                                previous.visibility = if (!livePlayback && canPreviousEpisode) android.view.View.VISIBLE else android.view.View.GONE
                                next.visibility = if (!livePlayback && canNextEpisode) android.view.View.VISIBLE else android.view.View.GONE
                                button.visibility = android.view.View.VISIBLE
                                val controls = buildList {
                                    if (previous.visibility == android.view.View.VISIBLE) add(previous)
                                    add(pause)
                                    if (next.visibility == android.view.View.VISIBLE) add(next)
                                    if (button.visibility == android.view.View.VISIBLE) add(button)
                                }
                                controls.forEachIndexed { index, control ->
                                    enableRemoteFocus(control)
                                    control.nextFocusLeftId = controls.getOrNull(index - 1)?.id ?: control.id
                                    control.nextFocusRightId = controls.getOrNull(index + 1)?.id ?: control.id
                                }
                                findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.let { progress ->
                                    controls.forEach { it.nextFocusDownId = progress.id }
                                    progress.nextFocusUpId = pause.id
                                    progress.nextFocusDownId = progress.id
                                }
                            }
                            val index = row.indexOfChild(pause).takeIf { it >= 0 } ?: 0
                            row.addView(previous, index, LinearLayout.LayoutParams(size, size))
                            row.addView(next, row.indexOfChild(pause).coerceAtLeast(0) + 1, LinearLayout.LayoutParams(size, size))
                            row.addView(button, row.indexOfChild(next) + 1, LinearLayout.LayoutParams(size, size))
                            previous.tag = "previous_episode_action"
                            next.tag = "next_episode_action"
                            previous.isEnabled = canPreviousEpisode
                            next.isEnabled = canNextEpisode
                        }
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_subtitle)?.visibility =
                            android.view.View.GONE
                        (findViewById<android.view.View>(androidx.media3.ui.R.id.exo_basic_controls) as? LinearLayout)?.let { bar ->
                            if (bar.findViewWithTag<android.view.View>("player_subtitles_action") == null) {
                                val cc = ImageButton(context).apply {
                                    id = android.view.View.generateViewId()
                                    tag = "player_subtitles_action"
                                    setImageResource(R.drawable.ic_subtitles)
                                    imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                                    contentDescription = context.getString(R.string.subtitle_tracks)
                                    tooltipText = contentDescription
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    defaultFocusHighlightEnabled = false
                                    val selectable = TypedValue()
                                    if (context.theme.resolveAttribute(
                                            android.R.attr.selectableItemBackgroundBorderless, selectable, true
                                        )
                                    ) setBackgroundResource(selectable.resourceId)
                                    setOnClickListener {
                                        saveSubtitleChoiceForSeries = false
                                        subtitleDialogOpen = true
                                    }
                                }
                                val gear = bar.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)
                                val insertAt = gear?.let { bar.indexOfChild(it) + 1 }?.takeIf { it > 0 } ?: bar.childCount
                                val size = (48 * context.resources.displayMetrics.density).toInt()
                                bar.addView(cc, insertAt, LinearLayout.LayoutParams(size, size))
                                enableRemoteFocus(cc)
                            }
                        }
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.let { progress ->
                            progress.isFocusable = true
                            progress.isFocusableInTouchMode = true
                            progress.defaultFocusHighlightEnabled = false
                            (progress as? DefaultTimeBar)?.let { timeBar ->
                                progress.setOnFocusChangeListener { _, focused ->
                                    timeBar.setScrubberColor(
                                        if (focused) accentArgb
                                        else android.graphics.Color.WHITE
                                    )
                                    timeBar.setPlayedColor(accentArgb)
                                }
                            }
                        }
                        listOf(
                            androidx.media3.ui.R.id.exo_settings,
                            androidx.media3.ui.R.id.exo_fullscreen,
                            androidx.media3.ui.R.id.exo_overflow_show
                        ).forEach { id ->
                            findViewById<android.view.View>(id)?.let(::enableRemoteFocus)
                        }
                        findViewWithTag<android.view.View>("player_subtitles_action")?.let(::enableRemoteFocus)
                        configureBottomFocus(this)
                        this.player = player
                        if (fullscreen) post { if (allowControls) requestFocus() }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onReset = null,
                onRelease = {
                    if (fullscreen) controlsVisibility?.invoke(false)
                    it.player = null
                    onSurface?.invoke(null)
                },
                update = { view ->
                    onSurface?.invoke(view.videoSurfaceView as? SurfaceView)
                    listOf(
                        androidx.media3.ui.R.id.exo_prev,
                        androidx.media3.ui.R.id.exo_next,
                        androidx.media3.ui.R.id.exo_rew,
                        androidx.media3.ui.R.id.exo_ffwd
                    ).forEach { id ->
                        view.findViewById<android.view.View>(id)?.visibility =
                            if (livePlayback) android.view.View.GONE else android.view.View.VISIBLE
                    }
                    view.findViewWithTag<android.view.View>("previous_episode_action")?.let { button ->
                        button.visibility = if (!livePlayback && canPreviousEpisode) android.view.View.VISIBLE else android.view.View.GONE
                        button.isEnabled = canPreviousEpisode
                    }
                    view.findViewWithTag<android.view.View>("next_episode_action")?.let { button ->
                        button.visibility = if (!livePlayback && canNextEpisode) android.view.View.VISIBLE else android.view.View.GONE
                        button.isEnabled = canNextEpisode
                    }
                    val pause = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
                    val stopButton = view.findViewWithTag<android.view.View>("stop_playback_action")
                    val previousButton = view.findViewWithTag<android.view.View>("previous_episode_action")
                    val nextButton = view.findViewWithTag<android.view.View>("next_episode_action")
                    val subtitlesButton = view.findViewWithTag<android.view.View>("player_subtitles_action")
                    subtitlesButton?.visibility = if (livePlayback) android.view.View.GONE else android.view.View.VISIBLE
                    val visibleControls = listOfNotNull(previousButton, pause, nextButton, stopButton)
                        .filter { it.visibility == android.view.View.VISIBLE }
                    visibleControls.forEachIndexed { index, control ->
                        control.nextFocusLeftId = visibleControls.getOrNull(index - 1)?.id ?: control.id
                        control.nextFocusRightId = visibleControls.getOrNull(index + 1)?.id ?: control.id
                        control.nextFocusDownId = androidx.media3.ui.R.id.exo_progress
                    }
                    view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.let { progress ->
                        progress.nextFocusUpId = pause?.id ?: progress.id
                        (progress as? DefaultTimeBar)?.let { timeBar ->
                            timeBar.setPlayedColor(accentArgb)
                            timeBar.setScrubberColor(
                                if (progress.isFocused) accentArgb else android.graphics.Color.WHITE
                            )
                        }
                    }
                    listOf(
                        androidx.media3.ui.R.id.exo_settings,
                        androidx.media3.ui.R.id.exo_fullscreen,
                        androidx.media3.ui.R.id.exo_overflow_show
                    ).forEach { id -> view.findViewById<android.view.View>(id)?.let(::enableRemoteFocus) }
                    subtitlesButton?.let(::enableRemoteFocus)
                    configureBottomFocus(view)
                    view.useController = openFullscreen == null && controlsAllowed
                    if (previousEpisodeAction != episodeActionVisible) {
                        previousEpisodeAction = episodeActionVisible
                        if (!episodeActionVisible && controlsAllowed && !view.isControllerFullyVisible) {
                            view.post { if (allowControls) view.requestFocus() }
                        }
                    }
                    if (previousDismissRequest != controlsDismissRequest) {
                        previousDismissRequest = controlsDismissRequest
                        view.hideController()
                    }
                    if (shownControlsRequest != controlsRequest) {
                        shownControlsRequest = controlsRequest
                        if (controlsAllowed) view.showController()
                    }
                    if (openFullscreen != null) {
                        view.isFocusable = true
                        view.setOnClickListener { openFullscreen?.invoke() }
                    }
                }
            )
            if (subtitleDialogOpen) {
                val subtitleChoices = player.currentTracks.groups
                    .filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMap { group ->
                        (0 until group.length).filter(group::isTrackSupported).map { index ->
                            val format = group.getTrackFormat(index)
                            val language = format.language?.takeUnless { it == "und" }
                                ?.let { java.util.Locale.forLanguageTag(it).getDisplayName(java.util.Locale.GERMAN) }
                            Triple(
                                group,
                                index,
                                format.label?.takeIf(String::isNotBlank) ?: language
                                    ?: format.id?.takeIf(String::isNotBlank) ?: "Spur ${index + 1}"
                            )
                        }
                    }
                val savedSeriesMode = subtitleSeriesKey?.let { key ->
                    subtitlePreferences?.getString("subtitle_series:$key", null)
                }
                val subtitlesDisabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
                fun applySubtitleChoice(selection: TrackSelectionOverride?, enabled: Boolean, savedMode: String) {
                    val persistForSeries = saveSubtitleChoiceForSeries && subtitleSeriesKey != null
                    if (persistForSeries) {
                        subtitlePreferences?.edit()?.putString("subtitle_series:$subtitleSeriesKey", savedMode)?.apply()
                        oneTimeSubtitleChoice = false
                    } else {
                        oneTimeSubtitleChoice = true
                    }
                    val parameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    if (enabled && selection != null) parameters.setOverrideForType(selection)
                    player.trackSelectionParameters = parameters.build()
                    subtitleDialogOpen = false
                }
                fun useSeriesDefault() {
                    subtitleSeriesKey?.let { key -> subtitlePreferences?.edit()?.remove("subtitle_series:$key")?.apply() }
                    oneTimeSubtitleChoice = false
                    val forced = subtitleChoices.firstOrNull { (group, index, _) ->
                        group.getTrackFormat(index).selectionFlags and C.SELECTION_FLAG_FORCED != 0
                    }
                    val language = java.util.Locale.getDefault().language
                    val preferred = subtitleChoices.firstOrNull { (group, index, _) ->
                        group.getTrackFormat(index).language?.substringBefore('-') == language
                    } ?: subtitleChoices.firstOrNull()
                    val choice = when {
                        forcedSubtitlesEnabled && forced != null -> forced
                        subtitleDefaultEnabled -> preferred
                        else -> null
                    }
                    val parameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, choice == null)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    choice?.let { (group, index, _) ->
                        parameters.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                    }
                    player.trackSelectionParameters = parameters.build()
                    subtitleDialogOpen = false
                }
                AlertDialog(
                    onDismissRequest = { subtitleDialogOpen = false },
                    title = { Text(stringResource(R.string.subtitle_tracks)) },
                    text = {
                        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            Row(Modifier.fillMaxWidth().focusGroup()) {
                                MenuButton(
                                    stringResource(R.string.subtitle_current_episode), Modifier.weight(1f),
                                    selected = !saveSubtitleChoiceForSeries
                                ) { saveSubtitleChoiceForSeries = false }
                                if (subtitleSeriesKey != null) {
                                    MenuButton(
                                        stringResource(R.string.subtitle_for_series), Modifier.weight(1f),
                                        selected = saveSubtitleChoiceForSeries
                                    ) { saveSubtitleChoiceForSeries = true }
                                }
                            }
                            MenuButton(
                                stringResource(R.string.subtitle_off), Modifier.fillMaxWidth(),
                                selected = subtitlesDisabled || savedSeriesMode == "OFF"
                            ) { applySubtitleChoice(null, false, "OFF") }
                            if (subtitleSeriesKey != null) {
                                MenuButton(
                                    stringResource(R.string.subtitles_forced_auto), Modifier.fillMaxWidth(),
                                    selected = savedSeriesMode == "FORCED"
                                ) {
                                    val forced = subtitleChoices.firstOrNull { (group, index, _) ->
                                        group.getTrackFormat(index).selectionFlags and C.SELECTION_FLAG_FORCED != 0
                                    }
                                    applySubtitleChoice(
                                        forced?.let { (group, index, _) -> TrackSelectionOverride(group.mediaTrackGroup, listOf(index)) },
                                        forced != null, "FORCED"
                                    )
                                }
                                MenuButton(stringResource(R.string.subtitle_series_default), Modifier.fillMaxWidth()) {
                                    useSeriesDefault()
                                }
                            }
                            if (subtitleChoices.isEmpty()) {
                                Text(
                                    stringResource(R.string.subtitle_none_available),
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            } else subtitleChoices.forEach { (group, index, label) ->
                                MenuButton(
                                    "${if (saveSubtitleChoiceForSeries && subtitleSeriesKey != null) "Serie" else "Diese Folge"}: $label",
                                    Modifier.fillMaxWidth(),
                                    selected = group.isTrackSelected(index) && !subtitlesDisabled
                                ) {
                                    applySubtitleChoice(
                                        TrackSelectionOverride(group.mediaTrackGroup, listOf(index)), true, "ON"
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        MaterialTextButton(onClick = { subtitleDialogOpen = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }
            if (fullscreen && buffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(42.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        } else if (showEmpty) {
            Text(stringResource(R.string.no_playback))
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun AboutSettings() {
    val context = LocalContext.current
    val app = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    val code = if (android.os.Build.VERSION.SDK_INT >= 28) app.longVersionCode else app.versionCode.toLong()
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.about_version, app.versionName.orEmpty()))
    Text(stringResource(R.string.about_version_code, code))
    Text(stringResource(R.string.about_version_date, stringResource(R.string.version_date)))
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
    categoryTitle: String,
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
    focusFirstTitle: Boolean,
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
            catalog.titles, categoryTitle, isFavorite, isSaved, preferPopularity, newEpisodes,
            returnTitle, focusFirstTitle, onTitleFocusRestored, onSelect
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
    leadingIcon: Int? = null,
    providerMark: String? = null,
    providerIcon: Int? = null,
    providerMarkOnly: Boolean = false,
    iconOnly: Boolean = false,
    enabled: Boolean = true,
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
        !enabled -> Color.Transparent
        else -> Color.Transparent
    }
    Surface(
        modifier = modifier
            .then(if (enabled) Modifier.hoverable(interactions) else Modifier)
            .onFocusChanged {
                if (it.isFocused && !focused) onFocused?.invoke()
                focused = it.isFocused
            }
            .focusProperties { canFocus = enabled }
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongPress),
        color = container,
        contentColor = when {
            !enabled -> colors.onSurface.copy(alpha = 0.38f)
            else -> colors.onSurface
        },
        shape = RoundedCornerShape(12.dp),
        border = when {
            enabled && highlighted -> BorderStroke(2.dp, colors.primary)
            else -> null
        }
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Box(Modifier.padding(end = 8.dp).width(3.dp).height(18.dp)
                    .background(colors.primary))
            }
            ChannelLogo(logo, Modifier.padding(end = 8.dp))
            providerIcon?.let { icon ->
                Icon(
                    painterResource(icon), contentDescription = null,
                    modifier = Modifier.padding(end = if (providerMarkOnly) 0.dp else 8.dp).size(24.dp),
                    tint = Color.Unspecified
                )
            }
            providerMark?.let { mark ->
                Surface(
                    color = when (mark) {
                        "N" -> Color(0xFFE50914)
                        "prime video" -> Color(0xFF1689D4)
                        "tv+" -> Color(0xFF30343B)
                        "D+" -> Color(0xFF132A75)
                        "MAX" -> Color(0xFF4A248A)
                        "P+" -> Color(0xFF0756A8)
                        "sky" -> Color(0xFF0072C6)
                        else -> colors.primaryContainer
                    },
                    contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = if (providerMarkOnly) 0.dp else 8.dp)
                ) {
                    Text(
                        mark,
                        Modifier.padding(horizontal = if (mark.length > 1) 6.dp else 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            leadingIcon?.let { icon ->
                Icon(
                    painterResource(icon),
                    contentDescription = label.takeIf { iconOnly },
                    modifier = Modifier.size(20.dp)
                )
                if (!iconOnly) Box(Modifier.width(9.dp))
            }
            if (hiddenIndicator) {
                HiddenCategoryIcon()
                Box(Modifier.width(8.dp))
            }
            if ((!iconOnly || leadingIcon == null) && !providerMarkOnly) Text(
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
        modifier = Modifier.heightIn(min = 48.dp).hoverable(interactions, enabled).onFocusChanged { focused = it.isFocused },
        interactionSource = interactions,
        shape = RoundedCornerShape(12.dp),
        border = if (highlighted) BorderStroke(2.dp, colors.primary) else null,
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
    MaterialTheme(colorScheme = playerColors(AccentChoice.RED)) {
        PlayerHome()
    }
}
