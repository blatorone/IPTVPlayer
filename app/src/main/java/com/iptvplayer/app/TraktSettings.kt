package com.iptvplayer.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.nio.charset.StandardCharsets

@Composable
internal fun TraktSettings(
    accountName: String?,
    deviceCode: TraktDeviceCode?,
    busy: Boolean,
    status: String?,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.trakt_intro), style = MaterialTheme.typography.bodyMedium)
        if (accountName == null) {
            SettingRow(
                stringResource(if (busy) R.string.trakt_connecting else R.string.trakt_connect),
                                enabled = !busy && deviceCode == null,
                onClick = onConnect
            )
        } else {
            Text(stringResource(R.string.trakt_connected_as, accountName), fontWeight = FontWeight.SemiBold)
            SettingRow(
                stringResource(if (busy) R.string.trakt_syncing else R.string.trakt_sync_now),
                enabled = !busy, onClick = onSync
            )
            SettingRow(
                stringResource(R.string.trakt_disconnect),
                enabled = !busy, onClick = onDisconnect
            )
        }
        if (deviceCode != null) {
            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QrCode(deviceCode.qrUrl, Modifier.size(148.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.trakt_enter_code, deviceCode.verificationUrl), style = MaterialTheme.typography.bodyMedium)
                    Text(deviceCode.userCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(deviceCode.verificationUrl, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.trakt_waiting), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (busy && deviceCode == null) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Text(stringResource(R.string.trakt_disclaimer), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QrCode(value: String, modifier: Modifier = Modifier) {
    val bitmap = remember(value) {
        runCatching {
            val matrix = QRCodeWriter().encode(
                value, BarcodeFormat.QR_CODE, 360, 360,
                mapOf(EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name())
            )
            Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565).apply {
                for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                    setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }.asImageBitmap()
        }.getOrNull()
    }
    bitmap?.let {
        Image(it, contentDescription = stringResource(R.string.trakt_qr_description), modifier = modifier.background(androidx.compose.ui.graphics.Color.White))
    } ?: Text(
        stringResource(R.string.trakt_qr_unavailable), modifier,
        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall
    )
}
