package com.iptvplayer.app

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Size-limited local cache for opening fingerprints of series. Files are named
 * by a hash of the series identity only and live outside Android backups. The
 * data is derived from playback and is rebuilt automatically if it is removed.
 */
internal class IntroFingerprintStore(private val directory: File) {
    private val keep = 360

    fun read(series: Pair<String, String>, kind: String): ByteArray? =
        runCatching { file(series, kind).takeIf(File::isFile)?.readBytes() }.getOrNull()

    fun write(series: Pair<String, String>, kind: String, data: ByteArray) {
        runCatching {
            directory.mkdirs()
            val target = file(series, kind)
            val temporary = File(directory, target.name + ".tmp")
            temporary.writeBytes(data)
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
            trim()
        }
    }

    private fun file(series: Pair<String, String>, kind: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${series.first}\n${series.second}".encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.$kind")
    }

    private fun trim() {
        val files = directory.listFiles()?.filter(File::isFile) ?: return
        if (files.size <= keep) return
        files.sortedBy(File::lastModified).take(files.size - keep).forEach(File::delete)
    }

    companion object {
        const val VIDEO_REFERENCE = "vref"
        const val VIDEO_LEARNED = "vlearn"
        const val AUDIO_REFERENCE = "aref"
        const val AUDIO_LEARNED = "alearn"
        const val INTRO_VARIANTS = "variants"

        fun create(context: Context) = IntroFingerprintStore(File(context.noBackupFilesDir, "intro-fingerprints"))
    }
}
