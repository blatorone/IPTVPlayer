package com.iptvplayer.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/*
 * "Neuigkeiten" under "Über StreamWatch": the changes of the installed version and the ones
 * before it, from CHANGELOG.md (copied into the app's assets at build time; German only).
 */

/** One "## x.y.z" section of the changelog: the version and its lines. */
internal data class ChangelogSection(val version: String, val text: String)

/** The newest [limit] sections of a changelog in the "## version" format, in file order. */
internal fun changelogSections(changelog: String, limit: Int = 6): List<ChangelogSection> {
    val sections = mutableListOf<ChangelogSection>()
    var version: String? = null
    val lines = StringBuilder()
    fun close() {
        version?.let { sections += ChangelogSection(it, lines.toString().trim()) }
        lines.clear()
    }
    for (line in changelog.lineSequence()) {
        if (line.startsWith("## ")) {
            close()
            if (sections.size >= limit) return sections
            version = line.removePrefix("## ").trim()
        } else if (version != null) {
            lines.appendLine(line)
        }
    }
    close()
    return sections.take(limit)
}

/** The text shown on the page: each version as a heading, its lines below. */
internal fun whatsNewText(sections: List<ChangelogSection>): String =
    sections.joinToString("\n\n") { "Version ${it.version}\n\n${it.text}" }

private fun readChangelog(context: Context): String =
    runCatching { context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() } }.getOrDefault("")

@Composable
internal fun WhatsNewText(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val text = remember { whatsNewText(changelogSections(readChangelog(context))) }
    RemoteScrollText(text, modifier)
}
