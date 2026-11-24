package com.iptvplayer.app

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class WatchStatusControlsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun selectWithRemote(label: String) {
        compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitForIdle()
    }

    @Test
    fun movieStatusAndReplayProgressAreDisplayed() {
        val progress = mutableStateOf(EpisodeCardProgress(true, 0, 100))
        var actions = 0
        compose.setContent {
            MaterialTheme {
                TitleDetails(
                    PortalTitle("Testfilm"), PortalSection.MOVIES, null, null, false, false, 0,
                    null, null, null, null, null, false, {}, true, {}, {}, progress.value,
                    { actions++ }, {}, {}
                )
            }
        }
        val activity = compose.activity
        selectWithRemote(activity.getString(R.string.watched_status))
        assertEquals(1, actions)
        compose.onNodeWithText(activity.getString(R.string.episode_watched)).assertExists()
        compose.runOnIdle {
            val preferences = activity.getSharedPreferences("watch_status_test", 0)
            val key = "test-movie"
            preferences.edit().putBoolean("manual_watched:$key", true).putBoolean("completed:$key", true).apply()
            preferences.saveInterruptedPlayback(key, 25, 100)
            val completed = preferences.isPlaybackCompleted(key, fallback = true)
            assertFalse(completed)
            progress.value = EpisodeCardProgress(completed, preferences.getLong(key, 0), preferences.getLong("duration:$key", 0))
        }
        compose.onNodeWithText(activity.getString(R.string.episode_in_progress)).assertExists()
        compose.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.25f, 0f..1f))
        ).assertExists()
    }

    @Test
    fun wholeSeriesStatusIsReachableWithTheRemote() {
        var selected = false
        compose.setContent {
            MaterialTheme {
                TitleDetails(
                    PortalTitle("Testserie"), PortalSection.SERIES, null, null, false, false, 0,
                    null, null, null, null, null, false, {}, true, {}, {}, null,
                    { selected = true }, {}, {}
                )
            }
        }
        selectWithRemote(compose.activity.getString(R.string.watched_status))
        assertEquals(true, selected)
    }

    @Test
    fun seasonAndEpisodeActionsReachTheirOwnTargets() {
        val series = PortalTitle("Testserie")
        val episode = PortalTitle("Testfolge", episode = "1")
        val season = PortalSeason(1, series, listOf(episode))
        var selectedSeason: PortalSeason? = null
        var selectedEpisode: PortalTitle? = null
        compose.setContent {
            MaterialTheme {
                SeriesSeasons(
                    modifier = Modifier.fillMaxSize(),
                    scroll = rememberLazyListState(),
                    series = series,
                    seasons = listOf(season),
                    selected = season,
                    failed = false,
                    unseen = emptySet(),
                    onSeason = {},
                    onSeasonWatchStatus = { selectedSeason = it },
                    onEpisodeWatchStatus = { _, target -> selectedEpisode = target },
                    episodeProgress = { _, _ -> EpisodeCardProgress(false, 0, 0) },
                    metadata = emptyMap(),
                    onEpisode = { _, _ -> },
                    onRetry = {},
                    onBack = {},
                    header = {}
                )
            }
        }
        selectWithRemote(compose.activity.getString(R.string.season_watched_status))
        selectWithRemote(compose.activity.getString(R.string.watched_status))
        assertEquals(season, selectedSeason)
        assertEquals(episode, selectedEpisode)
    }
}
