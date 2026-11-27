package com.iptvplayer.app

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * A hidden input (password, token, secret) with the usual value/onValueChange pair. Wraps the
 * secure text field, which replaced the text field with a password transformation.
 */
@Composable
internal fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null
) {
    val state = rememberTextFieldState(value)
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)
    // Changes from outside (cleared after saving, shortened or trimmed by the caller).
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (text != currentValue) currentOnChange(text)
        }
    }
    OutlinedSecureTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        label = { Text(label) },
        textObfuscationMode = TextObfuscationMode.RevealLastTyped,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        onKeyboardAction = onDone?.let { done -> KeyboardActionHandler { done() } }
    )
}
