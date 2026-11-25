package com.iptvplayer.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Decoded images, sized by the device: an eighth of the app heap (12 to 48 MB). The previous
// fixed 12 MB held only about a dozen posters, so scrolling decoded them again and again.
@Volatile private var imageCache: LruCache<String, Bitmap>? = null

private val imageCacheLock = Any()

private fun imageCache(context: Context): LruCache<String, Bitmap> = imageCache ?: synchronized(imageCacheLock) {
    imageCache ?: run {
        val heapMb = (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.memoryClass ?: 128
        val sizeKb = (heapMb * 1024 / 8).coerceIn(12 * 1024, 48 * 1024)
        object : LruCache<String, Bitmap>(sizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.byteCount / 1024)
        }.also { imageCache = it }
    }
}
private const val MAX_IMAGE_BYTES = 1024 * 1024
private const val MAX_DISK_CACHE_BYTES = 48L * 1024 * 1024
private val IMAGE_CACHE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(30)

@Composable
internal fun ChannelLogo(address: String?, modifier: Modifier = Modifier) {
    PortalImage(address, modifier.size(32.dp), ContentScale.Fit, maxPixels = 128)
}

@Composable
internal fun PortalImage(
    address: String?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxPixels: Int = 384
) {
    if (address == null) return
    val context = LocalContext.current
    val cacheKey = "$maxPixels:$address"
    val logo by produceState<Bitmap?>(null, cacheKey) {
        value = null
        val cache = imageCache(context)
        value = cache.get(cacheKey) ?: withContext(Dispatchers.IO) {
            try {
                loadLogo(context, address, maxPixels)?.also { cache.put(cacheKey, it) }
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
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun loadLogo(context: Context, address: String, maxPixels: Int): Bitmap? {
    val uri = URI(address)
    require(uri.scheme in listOf("http", "https") && uri.host != null && uri.userInfo == null)
    val cacheFile = diskCacheFile(context, address)
    val cachedBytes = if (cacheFile.isFile &&
        System.currentTimeMillis() - cacheFile.lastModified() <= IMAGE_CACHE_MAX_AGE_MS &&
        cacheFile.length() in 1..MAX_IMAGE_BYTES.toLong()
    ) {
        runCatching { cacheFile.readBytes().also { cacheFile.setLastModified(System.currentTimeMillis()) } }.getOrNull()
    } else {
        cacheFile.delete()
        null
    }
    val cachedBitmap = cachedBytes?.let { decodeImage(it, maxPixels) }
    if (cachedBytes != null && cachedBitmap == null) cacheFile.delete()
    return cachedBitmap ?: downloadImage(uri)?.also { storeImage(cacheFile, it) }?.let {
        decodeImage(it, maxPixels)
    }
}

private fun diskCacheFile(context: Context, address: String): File {
    val digest = MessageDigest.getInstance("SHA-256").digest(address.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    return File(File(context.cacheDir, "portal-artwork"), digest)
}

private fun downloadImage(uri: URI): ByteArray? {
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
                if (output.size() + count > MAX_IMAGE_BYTES) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        return bytes
    } finally {
        connection.disconnect()
    }
}

private fun decodeImage(bytes: ByteArray, maxPixels: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    while (maxOf(options.outWidth, options.outHeight) / maxOf(1, options.inSampleSize) > maxPixels) {
        options.inSampleSize = maxOf(1, options.inSampleSize) * 2
    }
    options.inJustDecodeBounds = false
    // Posters and stills have no transparency and need half the memory as RGB_565; small
    // channel logos keep their alpha channel.
    if (maxPixels > 128) options.inPreferredConfig = Bitmap.Config.RGB_565
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun storeImage(cacheFile: File, bytes: ByteArray) {
    runCatching {
        cacheFile.parentFile?.mkdirs()
        val temporary = File(cacheFile.parentFile, "${cacheFile.name}.${System.nanoTime()}.tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(cacheFile)) {
            temporary.copyTo(cacheFile, overwrite = true)
            temporary.delete()
        }
        val files = cacheFile.parentFile?.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }.orEmpty()
        var total = files.sumOf { it.length() }
        if (total > MAX_DISK_CACHE_BYTES) {
            for (file in files.sortedBy { it.lastModified() }) {
                if (file == cacheFile) continue
                total -= file.length()
                file.delete()
                if (total <= MAX_DISK_CACHE_BYTES) break
            }
        }
    }
}
