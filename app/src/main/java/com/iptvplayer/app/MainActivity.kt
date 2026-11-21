package com.iptvplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
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
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private val storage by lazy { PortalStorage(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        player = ExoPlayer.Builder(this).build()
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = playerColors) {
                PlayerHome(player, storage) { lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
            }
        }
    }

    override fun onStop() {
        player.pause()
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
    SETTINGS(R.string.destination_settings),
    SOURCES(R.string.destination_sources)
}

private val mainDestinations = Destination.entries.filter { it != Destination.SOURCES }

private val playerColors = darkColorScheme(
    primary = Color(0xFF8BB8FF),
    surface = Color(0xFF0E121B),
    surfaceContainer = Color(0xFF171E2B),
    secondaryContainer = Color(0xFF253650),
    onSecondaryContainer = Color(0xFFD7E7FF),
    tertiaryContainer = Color(0xFFB1CFFF),
    onTertiaryContainer = Color(0xFF12243D)
)

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerHome(
    player: ExoPlayer? = null,
    storage: PortalStorage? = null,
    canPlay: () -> Boolean = { false }
) {
    var selected by rememberSaveable { mutableStateOf<Destination?>(null) }
    var lastFocused by rememberSaveable { mutableStateOf(Destination.LIVE) }
    val initialFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var liveSession by remember { mutableStateOf<Pair<StalkerPortal, String?>?>(null) }
    var channels by remember { mutableStateOf(emptyList<StalkerChannel>()) }
    var genres by remember { mutableStateOf(emptyMap<String, String>()) }
    var category by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf<StalkerChannel?>(null) }
    var programmeChannel by remember { mutableStateOf<StalkerChannel?>(null) }
    var epgChannelIds by remember { mutableStateOf(emptySet<String>()) }
    var starting by remember { mutableStateOf<StalkerChannel?>(null) }
    var triedHls by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var sourceError by remember { mutableStateOf<Int?>(null) }
    var importStatuses by remember { mutableStateOf(emptyMap<PortalSection, ImportStatus>()) }
    var catalogSession by remember { mutableStateOf<Pair<StalkerPortal, String?>?>(null) }
    var catalogs by remember { mutableStateOf(emptyMap<PortalSection, PortalCatalog>()) }
    var catalogLoading by remember { mutableStateOf(emptySet<PortalSection>()) }
    var catalogErrors by remember { mutableStateOf(emptySet<PortalSection>()) }
    var playbackError by remember { mutableStateOf<Int?>(null) }
    var vodSection by remember { mutableStateOf<PortalSection?>(null) }
    var openedSeries by remember { mutableStateOf<PortalTitle?>(null) }
    var episodes by remember { mutableStateOf<List<PortalTitle>?>(null) }
    var episodeError by remember { mutableStateOf(false) }
    var episodeJob by remember { mutableStateOf<Job?>(null) }
    var address by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var storedSource by remember { mutableStateOf<SavedPortal?>(null) }
    var editing by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(storage != null) }
    var storageError by remember { mutableStateOf<Int?>(null) }
    var epgCache by remember(liveSession) {
        mutableStateOf(emptyMap<String, List<PortalProgramme>>())
    }
    val pinnedProgramme = programmeChannel?.id in epgChannelIds
    val focusedProgrammes by produceState<List<PortalProgramme>?>(
        null, programmeChannel, liveSession, pinnedProgramme
    ) {
        value = null
        val channel = programmeChannel ?: return@produceState
        val session = liveSession ?: return@produceState
        if (pinnedProgramme) return@produceState
        delay(250)
        value = try {
            session.first.loadProgramme(channel, session.second ?: session.first.openSession())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
    }
    val programmes = if (pinnedProgramme) {
        epgCache[programmeChannel?.id]
    } else focusedProgrammes

    LaunchedEffect(liveSession, epgChannelIds, channels) {
        val session = liveSession ?: return@LaunchedEffect
        if (epgChannelIds.isEmpty()) return@LaunchedEffect
        while (true) {
            for (channel in channels.filter { it.id in epgChannelIds }.distinctBy { it.id }) {
                val id = channel.id ?: continue
                try {
                    val token = session.second ?: session.first.openSession()
                    epgCache = epgCache + (id to session.first.loadProgramme(channel, token))
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
            storage?.read()?.let { saved ->
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
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = R.string.portal_restore_failed
        } finally {
            restoring = false
        }
    }

    suspend fun save(source: SavedPortal) {
        storedSource = source
        try {
            storage?.write(source)
            storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = R.string.portal_save_failed
        }
    }

    fun toggleProgrammeChannel(channel: StalkerChannel) {
        val id = channel.id ?: return
        val saved = storedSource ?: return
        epgChannelIds = if (id in epgChannelIds) epgChannelIds - id else epgChannelIds + id
        val snapshot = SavedPortal(saved.address, saved.mac, channels, genres, catalogs, epgChannelIds)
        scope.launch { save(snapshot) }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        player?.stop()
        player?.clearMediaItems()
        playing = null
        starting = null
        playbackError = null
        vodSection = null
    }

    fun startPlayback(
        channel: StalkerChannel,
        section: PortalSection? = null,
        resolve: suspend () -> String
    ) {
        val activePlayer = player ?: return
        stopPlayback()
        vodSection = section
        triedHls = false
        starting = channel
        playbackJob = scope.launch {
            try {
                val stream = resolve()
                playing = channel
                activePlayer.setMediaItem(MediaItem.fromUri(stream))
                activePlayer.prepare()
                activePlayer.playWhenReady = canPlay()
                if (!canPlay()) starting = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                starting = null
                playbackError = if (error is PortalRequestException &&
                    error.status in listOf(401, 403)
                ) R.string.playback_portal_denied else R.string.playback_link_error
            }
        }
    }

    fun closeSeries() {
        episodeJob?.cancel()
        openedSeries = null
        episodes = null
        episodeError = false
    }

    fun selectTitle(section: PortalSection, title: PortalTitle) {
        val session = catalogSession ?: return
        if (section == PortalSection.SERIES && openedSeries == null) {
            openedSeries = title
            episodeError = false
            episodes = null
            episodeJob = scope.launch {
                try {
                    val token = session.second ?: session.first.openSession()
                    episodes = session.first.loadEpisodes(title, token)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    episodeError = true
                }
            }
        } else if (title.command != null) {
            startPlayback(StalkerChannel(title.name, title.command, null, null), section) {
                val token = session.second ?: session.first.openSession()
                session.first.resolveVod(title, token)
            }
        } else {
            vodSection = section
            playbackError = R.string.vod_missing_link
        }
    }

    fun importPortal() {
        editing = false
        val enteredAddress = address.trim()
        val enteredMac = mac.trim()
        connecting = true
        sourceError = null
        importStatuses = emptyMap()
        scope.launch {
            try {
                val source = StalkerPortal(enteredAddress, enteredMac)
                val token = source.openSession()
                val sameSource = storedSource?.let {
                    it.address == enteredAddress &&
                        normalizePortalMac(it.mac) == normalizePortalMac(enteredMac)
                } == true
                val session = source to token
                var activated = false
                fun activateSource() {
                    if (activated) return
                    if (!sameSource) {
                        closeSeries()
                        channels = emptyList()
                        genres = emptyMap()
                        catalogs = emptyMap()
                        category = null
                        programmeChannel = null
                        epgChannelIds = emptySet()
                    }
                    liveSession = session
                    catalogSession = session
                    catalogLoading = emptySet()
                    catalogErrors = emptySet()
                    activated = true
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
                                            importStatuses = importStatuses +
                                                (section to ImportStatus.Progress(loaded, total))
                                        }
                                    }
                                    imported to labels.await()
                                }
                                activateSource()
                                channels = imported.channels
                                genres = labels
                                category = null
                                if (imported.partial) ImportStatus.Partial(channels.size)
                                else ImportStatus.Count(channels.size)
                            } else {
                                val catalog = source.loadCatalog(section, token)
                                activateSource()
                                catalogs = catalogs + (section to catalog)
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
                        if (status is ImportStatus.Count || status is ImportStatus.Loaded ||
                            status is ImportStatus.Partial
                        ) {
                            save(SavedPortal(enteredAddress, enteredMac, channels, genres, catalogs, epgChannelIds))
                        }
                    }
                }
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
            } finally {
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
                if (isPlaying) starting = null
            }

            override fun onPlayerError(error: PlaybackException) {
                val item = player?.currentMediaItem
                val startingChannel = starting
                val uri = item?.localConfiguration?.uri
                val formatError = error.errorCode in setOf(
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                )
                if (player != null && uri != null && startingChannel != null && formatError &&
                    !triedHls && !uri.path.orEmpty().endsWith(".m3u8", ignoreCase = true)
                ) {
                    triedHls = true
                    scope.launch {
                        if (starting === startingChannel && player.currentMediaItem == item) {
                            player.setMediaItem(
                                MediaItem.Builder().setUri(uri)
                                    .setMimeType(MimeTypes.APPLICATION_M3U8).build()
                            )
                            player.prepare()
                            player.playWhenReady = canPlay()
                        }
                    }
                    return
                }
                starting = null
                val httpStatus = (error.cause as? HttpDataSource.InvalidResponseCodeException)
                    ?.responseCode
                playbackError = when {
                    httpStatus == 401 || httpStatus == 403 -> R.string.playback_stream_denied
                    formatError -> R.string.playback_format_error
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        R.string.playback_stream_http_error
                    else -> R.string.playback_error
                }
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(starting) {
        if (starting != null) {
            delay(25_000)
            playbackJob?.cancel()
            player?.stop()
            starting = null
            playbackError = R.string.playback_timeout
        }
    }

    val insideCategory = (selected == null || selected == Destination.LIVE) && category != null
    BackHandler(enabled = selected != null || insideCategory) {
        if (insideCategory) category = null
        else if (selected == Destination.SERIES && openedSeries != null) closeSeries()
        else selected = if (selected == Destination.SOURCES) Destination.SETTINGS else null
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                            destination != Destination.SOURCES
                        ) {
                            Modifier.fillMaxWidth().focusRequester(initialFocus)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        selected = item == (destination ?: Destination.LIVE) ||
                            (item == Destination.SETTINGS && destination == Destination.SOURCES),
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
                        channels = channels,
                        genres = genres,
                        category = category,
                        onCategory = { category = it },
                        playing = playing.takeIf { vodSection == null },
                        starting = starting.takeIf { vodSection == null },
                        programmeChannel = programmeChannel,
                        programmes = programmes,
                        epgPinned = programmeChannel?.id in epgChannelIds,
                        onToggleEpg = ::toggleProgrammeChannel,
                        onHighlight = { programmeChannel = it },
                        playbackError = playbackError.takeIf { vodSection == null },
                        onStop = ::stopPlayback,
                        onSelect = { channel ->
                            val session = liveSession
                            if (session != null) {
                                programmeChannel = channel
                                startPlayback(channel) {
                                    val token = session.second ?: session.first.openSession()
                                    session.first.resolve(channel, token)
                                }
                            }
                        }
                    )
                    Destination.SETTINGS -> MenuButton(
                        label = stringResource(R.string.destination_sources),
                        modifier = Modifier.focusRequester(initialFocus),
                        onClick = { selected = Destination.SOURCES }
                    )
                    Destination.SOURCES -> {
                        MenuButton(
                            label = stringResource(
                                if (storedSource == null) R.string.add_portal else R.string.edit_portal
                            ),
                            modifier = Modifier.focusRequester(initialFocus),
                            onClick = { if (!restoring && !connecting) editing = true }
                        )
                        if (storedSource != null) Text(stringResource(R.string.portal_saved))
                        if (editing) PortalEditor(
                            address = address,
                            mac = mac,
                            onAddressChange = { address = it },
                            onMacChange = { mac = it },
                            onDismiss = { editing = false },
                            onSubmit = ::importPortal
                        )
                        if (connecting) {
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
                        if (channels.isNotEmpty()) {
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
                    Destination.GUIDE -> GuideOverview(
                        programmeChannel,
                        programmes,
                        remember(channels, epgChannelIds) {
                            channels.filter { it.id in epgChannelIds }.distinctBy { it.id }
                        },
                        onSelect = { programmeChannel = it }
                    )
                    Destination.MOVIES, Destination.SERIES -> {
                        val section = if (destination == Destination.MOVIES) {
                            PortalSection.MOVIES
                        } else {
                            PortalSection.SERIES
                        }
                        if (vodSection == section) {
                            PlaybackFeedback(starting, playbackError)
                            if (playing != null) PlayerFrame(player, active = true, controls = true)
                            if (playing != null || starting != null) {
                                MenuButton(stringResource(R.string.stop_playback), Modifier, onClick = ::stopPlayback)
                            }
                        }
                        val series = openedSeries.takeIf { section == PortalSection.SERIES }
                        if (series != null) {
                            Text(series.name, style = MaterialTheme.typography.titleMedium)
                            MenuButton(stringResource(R.string.back_to_series), Modifier, onClick = ::closeSeries)
                            if (episodes == null && !episodeError) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (episodeError || episodes?.isEmpty() == true) Text(stringResource(R.string.no_episodes))
                            LazyColumn(Modifier.heightIn(max = 240.dp)) {
                                items(episodes.orEmpty()) { episode ->
                                    val label = episode.episode?.let {
                                        stringResource(R.string.episode_title, episode.name, it)
                                    } ?: episode.name
                                    MenuButton(label, Modifier.fillMaxWidth()) { selectTitle(section, episode) }
                                }
                            }
                        } else CatalogOverview(
                            catalog = catalogs[section],
                            loading = section in catalogLoading ||
                                importStatuses[section] == ImportStatus.Loading,
                            failed = section in catalogErrors ||
                                importStatuses[section] == ImportStatus.Failed,
                            canLoadMore = !connecting,
                            emptyMessage = if (section == PortalSection.MOVIES) {
                                R.string.no_movies
                            } else {
                                R.string.no_series
                            },
                            onLoadMore = { loadMore(section) },
                            onSelect = { selectTitle(section, it) }
                        )
                    }
                }
            }
            if (maxWidth >= 560.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.weight(0.9f)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        menu()
                    }
                    Column(
                        Modifier.weight(3.6f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        content()
                    }
                }
            } else {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    menu()
                    content()
                }
            }
            LaunchedEffect(destination) { initialFocus.requestFocus() }
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
    channels: List<StalkerChannel>,
    genres: Map<String, String>,
    category: String?,
    onCategory: (String?) -> Unit,
    playing: StalkerChannel?,
    starting: StalkerChannel?,
    programmeChannel: StalkerChannel?,
    programmes: List<PortalProgramme>?,
    epgPinned: Boolean,
    onToggleEpg: (StalkerChannel) -> Unit,
    onHighlight: (StalkerChannel) -> Unit,
    playbackError: Int?,
    onStop: () -> Unit,
    onSelect: (StalkerChannel) -> Unit
) {
    val uncategorized = stringResource(R.string.uncategorized_channels)
    val categoryLabel = stringResource(R.string.channel_category_number)
    val groups = remember(channels, genres, uncategorized, categoryLabel) {
        channels.groupBy { channel ->
            channel.genreName ?: genres[channel.genreId]
                ?: channel.genreId?.takeUnless { it == "0" }?.let { categoryLabel.format(it) }
                ?: uncategorized
        }
    }
    val visibleChannels = groups[category]
    val channels: @Composable () -> Unit = {
        Text(
            category ?: stringResource(R.string.channel_categories),
            style = MaterialTheme.typography.titleMedium
        )
        if (channels.isEmpty()) {
            Text(stringResource(R.string.no_channels), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (visibleChannels == null) {
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(groups.keys.sorted()) { group ->
                    MenuButton(
                        group, Modifier.fillMaxWidth(),
                        count = groups.getValue(group).size
                    ) { onCategory(group) }
                }
            }
        } else {
            MenuButton(stringResource(R.string.back_to_categories), Modifier.fillMaxWidth()) {
                onCategory(null)
            }
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(visibleChannels) { channel ->
                    MenuButton(
                        channel.name, Modifier.fillMaxWidth(), channel == playing, channel.logo,
                        onFocused = { onHighlight(channel) }
                    ) {
                        if (programmeChannel === channel) onSelect(channel) else onHighlight(channel)
                    }
                }
            }
        }
    }
    val preview: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaybackFeedback(starting, playbackError)
            PlayerFrame(player, active = playing != null, showEmpty = starting == null)
            if (playing != null || starting != null) {
                MenuButton(stringResource(R.string.stop_playback), Modifier, onClick = onStop)
            }
            if (programmeChannel != null) {
                ProgrammeDetails(programmeChannel, programmes?.take(2))
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
    controls: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
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
                        useController = controls
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
private fun GuideOverview(
    channel: StalkerChannel?,
    programmes: List<PortalProgramme>?,
    pinnedChannels: List<StalkerChannel>,
    onSelect: (StalkerChannel) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pinnedChannels.isNotEmpty()) {
            LazyColumn(Modifier.heightIn(max = 136.dp)) {
                items(pinnedChannels) { item ->
                    MenuButton(item.name, Modifier.fillMaxWidth(), item == channel) { onSelect(item) }
                }
            }
        }
        if (channel == null) Text(stringResource(R.string.select_channel_for_guide))
        else ProgrammeDetails(channel, programmes)
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
private fun CatalogOverview(
    catalog: PortalCatalog?,
    loading: Boolean,
    failed: Boolean,
    canLoadMore: Boolean,
    emptyMessage: Int,
    onLoadMore: () -> Unit,
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
            return@Column
        }
        catalog.total?.let { total ->
            Text(stringResource(R.string.catalog_count, catalog.titles.size, total))
        }
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(catalog.titles) { title ->
                MenuButton(title.name, Modifier.fillMaxWidth()) { onSelect(title) }
            }
        }
        if (catalog.nextPage != null && !loading && canLoadMore) {
            MenuButton(stringResource(R.string.catalog_load_more), Modifier, onClick = onLoadMore)
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    modifier: Modifier,
    selected: Boolean = false,
    logo: String? = null,
    count: Int? = null,
    onFocused: (() -> Unit)? = null,
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
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !focused) onFocused?.invoke()
            focused = it.isFocused
        },
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
