package com.iptvplayer.app

import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.SilenceMediaSource
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FullscreenControlsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun remoteFocusBackAndTimeoutWorkWhilePaused() {
        var visible by mutableStateOf(false)
        compose.setContent {
            val player = remember {
                ExoPlayer.Builder(compose.activity).build().apply {
                    setMediaSource(SilenceMediaSource.Factory().setDurationUs(30_000_000L).createMediaSource())
                    prepare()
                }
            }
            DisposableEffect(player) { onDispose { player.release() } }
            var dismissRequest by remember { mutableIntStateOf(0) }
            BackHandler(enabled = visible) { dismissRequest++ }
            PlayerFrame(
                player = player,
                active = true,
                fullscreen = true,
                onStop = {},
                controlsDismissRequest = dismissRequest,
                onControlsVisible = { visible = it }
            )
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val playPause = androidx.media3.ui.R.id.exo_play_pause
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(3_000) {
            compose.runOnIdle { visible && compose.activity.findViewById<View>(playPause).hasFocus() }
        }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.runOnIdle {
            val button = compose.activity.findViewById<View>(playPause)
            assertTrue(compose.activity.findViewById<View>(button.nextFocusRightId).hasFocus())
        }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(3_000) { !visible }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(3_000) { visible }
        compose.waitUntil(8_000) { !visible }
        assertTrue(!compose.activity.isFinishing)
    }
}
