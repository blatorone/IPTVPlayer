package com.iptvplayer.app

import java.util.Locale

private val descriptionWords = Regex("[\\p{L}]+")
private val englishWords = setOf(
    "the", "and", "with", "his", "her", "their", "they", "she", "when", "who", "while",
    "from", "into", "after", "before", "that", "this", "has", "have", "must", "was", "were", "of", "to"
)
private val germanWords = setOf(
    "der", "die", "das", "und", "mit", "sich", "sein", "seine", "ihr", "ihre", "ihren", "einem",
    "einer", "eine", "den", "dem", "des", "wird", "werden", "muss", "dass", "als", "aus", "für",
    "von", "nach", "vor", "auf", "über", "bei", "einen", "zum", "zur"
)
private val dutchWords = setOf(
    "de", "het", "een", "en", "van", "voor", "met", "zijn", "haar", "hun", "wordt", "worden",
    "maar", "naar", "niet", "bij", "door", "heeft", "hebben", "waren"
)

internal fun germanDescription(text: String?): String? {
    val description = text?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val words = descriptionWords.findAll(description.lowercase(Locale.ROOT)).map { it.value }.toSet()
    val english = words.count { it in englishWords }
    val german = words.count { it in germanWords }
    val dutch = words.count { it in dutchWords }
    return description.takeUnless {
        english >= 2 && english > german * 2 || dutch >= 3 && dutch > german * 2
    }
}
