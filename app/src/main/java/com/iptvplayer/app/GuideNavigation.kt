package com.iptvplayer.app

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal enum class GuidePanel { GRID, CHANNELS, CATEGORIES, MENU, DETAILS }

internal enum class GuideInput(val label: Int) {
    REMOTE_LEFT(R.string.control_left), REMOTE_RIGHT(R.string.control_right),
    SWIPE_LEFT(R.string.control_swipe_left), SWIPE_RIGHT(R.string.control_swipe_right),
    LONG_BACK(R.string.control_long_back), TAP(R.string.control_tap),
    PLAY_PAUSE(R.string.control_media_key), BACK(R.string.navigation_back)
}

internal enum class ControlAction(val label: Int) {
    DEFAULT(R.string.control_default),
    BACK(R.string.navigation_back),
    CHANNELS(R.string.channels),
    CATEGORIES(R.string.back_to_categories),
    MENU(R.string.guide_main_menu),
    DETAILS(R.string.control_programme_details),
    FULLSCREEN(R.string.control_fullscreen),
    PLAY_PAUSE(R.string.control_play_pause),
    STOP(R.string.stop_playback),
    CONTROLS(R.string.control_show_controls)
}

@Composable
internal fun Modifier.guideNavigationInput(
    interceptRemote: (GuideInput) -> Boolean = { true },
    onInput: (GuideInput) -> Unit
): Modifier {
    val current = rememberUpdatedState(onInput)
    return onPreviewKeyEvent { event ->
        val input = when (event.key) {
            Key.DirectionLeft -> GuideInput.REMOTE_LEFT
            Key.DirectionRight -> GuideInput.REMOTE_RIGHT
            else -> null
        }
        if (input == null || !interceptRemote(input)) false else {
            if (event.type == KeyEventType.KeyUp) current.value(input)
            true
        }
    }.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var horizontal = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) return@awaitEachGesture
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                val delta = change.position - down.position
                if (!horizontal) {
                    if (abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) >= abs(delta.x)) {
                        return@awaitEachGesture
                    }
                    horizontal = abs(delta.x) > viewConfiguration.touchSlop && abs(delta.x) > abs(delta.y)
                }
                if (horizontal) change.consume()
                if (!change.pressed && horizontal && abs(delta.x) >= 30.dp.toPx()) {
                    current.value(if (delta.x < 0) GuideInput.SWIPE_LEFT else GuideInput.SWIPE_RIGHT)
                }
            } while (change.pressed)
        }
    }
}

internal fun Modifier.onHorizontalRemote(
    allowRight: Boolean = true,
    onDirection: (right: Boolean) -> Unit
): Modifier =
    onPreviewKeyEvent { event ->
        val right = event.key == Key.DirectionRight
        if (event.key != Key.DirectionLeft && !(right && allowRight)) false else {
            if (event.type == KeyEventType.KeyUp) onDirection(right)
            true
        }
    }

internal fun guideChannelCategory(
    selected: String?,
    visible: List<String>,
    categories: Map<String, List<StalkerChannel>>,
    reference: StalkerChannel?
): String? = selected?.takeIf { it in visible }
    ?: visible.firstOrNull { reference != null && reference in categories[it].orEmpty() }
    ?: visible.firstOrNull()

internal fun GuidePanel.after(input: GuideInput): GuidePanel = when (input) {
    GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT -> when (this) {
        GuidePanel.DETAILS -> GuidePanel.GRID
        GuidePanel.GRID -> GuidePanel.CHANNELS
        GuidePanel.CHANNELS -> GuidePanel.CATEGORIES
        GuidePanel.CATEGORIES, GuidePanel.MENU -> GuidePanel.MENU
    }
    GuideInput.REMOTE_RIGHT -> when (this) {
        GuidePanel.GRID, GuidePanel.DETAILS -> GuidePanel.DETAILS
        else -> after(GuideInput.BACK)
    }
    GuideInput.SWIPE_RIGHT -> when (this) {
        GuidePanel.GRID, GuidePanel.DETAILS -> GuidePanel.DETAILS
        GuidePanel.CHANNELS -> GuidePanel.CATEGORIES
        GuidePanel.CATEGORIES, GuidePanel.MENU -> GuidePanel.MENU
    }
    GuideInput.BACK -> when (this) {
        GuidePanel.MENU -> GuidePanel.CATEGORIES
        GuidePanel.CATEGORIES -> GuidePanel.CHANNELS
        else -> GuidePanel.GRID
    }
    GuideInput.LONG_BACK, GuideInput.TAP, GuideInput.PLAY_PAUSE -> this
}

internal fun GuidePanel.afterProgrammeGuide(input: GuideInput): GuidePanel = when (input) {
    GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT -> when (this) {
        GuidePanel.DETAILS -> GuidePanel.GRID
        else -> GuidePanel.MENU
    }
    GuideInput.REMOTE_RIGHT, GuideInput.SWIPE_RIGHT -> when (this) {
        GuidePanel.GRID, GuidePanel.DETAILS -> GuidePanel.DETAILS
        else -> GuidePanel.GRID
    }
    GuideInput.BACK -> GuidePanel.GRID
    else -> this
}
