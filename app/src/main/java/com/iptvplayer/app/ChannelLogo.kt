package com.iptvplayer.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logos = LruCache<String, Bitmap>(64)

@Composable
internal fun ChannelLogo(address: String?, modifier: Modifier = Modifier) {
    if (address == null) return
    val logo by produceState<Bitmap?>(null, address) {
        value = null
        value = logos.get(address) ?: withContext(Dispatchers.IO) {
            try {
                loadLogo(address)?.also { logos.put(address, it) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        }
    }
    logo?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(32.dp)
        )
    }
}

private fun loadLogo(address: String): Bitmap? {
    val uri = URI(address)
    require(uri.scheme in listOf("http", "https") && uri.host != null && uri.userInfo == null)
    val connection = uri.toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        val bytes = connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > 1024 * 1024) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        while (maxOf(options.outWidth, options.outHeight) / maxOf(1, options.inSampleSize) > 128) {
            options.inSampleSize = maxOf(1, options.inSampleSize) * 2
        }
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } finally {
        connection.disconnect()
    }
}
