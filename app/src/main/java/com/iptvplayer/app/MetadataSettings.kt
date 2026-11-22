package com.iptvplayer.app

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val TMDB_LOGO = "https://www.themoviedb.org/assets/2/v4/logos/v2/" +
    "blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg"

@Composable
internal fun MetadataSettings(
    connected: Boolean,
    busy: Boolean,
    error: Boolean,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.tmdb_settings)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.tmdb_token_help))
                if (connected) Text(stringResource(R.string.tmdb_connected))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.tmdb_token)) },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error) Text(stringResource(R.string.tmdb_connection_failed), color = MaterialTheme.colorScheme.error)
                if (connected) {
                    TmdbAttribution()
                    TextButton(onClick = onDisconnect, enabled = !busy) {
                        Text(stringResource(R.string.tmdb_disconnect))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConnect(token.trim()) }, enabled = token.isNotBlank() && !busy) {
                Text(stringResource(R.string.tmdb_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun TmdbAttribution() {
    AndroidView(
        modifier = Modifier.width(92.dp).height(36.dp),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isFocusable = false
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                loadDataWithBaseURL(
                    "https://www.themoviedb.org",
                    """<html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head><body style="margin:0"><img src="$TMDB_LOGO" alt="TMDB" width="84"></body></html>""",
                    "text/html", "UTF-8", null
                )
            }
        },
        onReset = null,
        onRelease = { view -> view.destroy() },
        update = {}
    )
    Text(stringResource(R.string.tmdb_attribution), style = MaterialTheme.typography.labelSmall)
    Text(stringResource(R.string.justwatch_attribution), style = MaterialTheme.typography.labelSmall)
}
