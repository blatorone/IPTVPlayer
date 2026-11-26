package com.iptvplayer.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * Accent colours with a colour gradient (second palette). Surfaces in the accent colour (focus
 * frames, selection bars, switches) show the gradient; text and thin lines use its middle colour.
 */
internal enum class AccentGradient(val label: String, val start: Color, val end: Color) {
    SUNSET("Sonnenuntergang", Color(0xFFFF512F), Color(0xFFDD2476)),
    FIRE("Feuer", Color(0xFFF12711), Color(0xFFF5AF19)),
    EMBER("Glut", Color(0xFFE52D27), Color(0xFFB31217)),
    PEACH("Pfirsich", Color(0xFFFF5F6D), Color(0xFFFFC371)),
    CHERRY("Kirsche", Color(0xFFEB3349), Color(0xFFF45C43)),
    CANDY("Zuckerwatte", Color(0xFFFC67FA), Color(0xFFF4C4F3)),
    MAGENTA_DREAM("Magenta-Traum", Color(0xFFEC008C), Color(0xFFFC6767)),
    NEON_PINK("Neonpink", Color(0xFFFF0080), Color(0xFF7928CA)),
    BERRY("Beere", Color(0xFF8E2DE2), Color(0xFF4A00E0)),
    PURPLE_RAIN("Lila Regen", Color(0xFF7F00FF), Color(0xFFE100FF)),
    LILAC("Flieder", Color(0xFFA18CD1), Color(0xFFFBC2EB)),
    LAVENDER_GLOW("Lavendelschein", Color(0xFF9D50BB), Color(0xFF6E48AA)),
    COSMOS("Kosmos", Color(0xFF654EA3), Color(0xFFEAAFC8)),
    ROYAL_NIGHT("Königsnacht", Color(0xFF4776E6), Color(0xFF8E54E9)),
    INDIGO_WAVE("Indigowelle", Color(0xFF5B3AA8), Color(0xFFD76D77)),
    OCEAN("Ozean", Color(0xFF2193B0), Color(0xFF6DD5ED)),
    DEEP_SEA("Tiefsee", Color(0xFF2A3BAF), Color(0xFF26D0CE)),
    SKYLINE("Skyline", Color(0xFF1488CC), Color(0xFF2B32B2)),
    GLACIER("Gletscher", Color(0xFF74EBD5), Color(0xFFACB6E5)),
    LAGOON("Lagune", Color(0xFF1C7AA8), Color(0xFF80D0C7)),
    TURQUOISE_WAVE("Türkiswelle", Color(0xFF00C9FF), Color(0xFF92FE9D)),
    MINT_FRESH("Frische Minze", Color(0xFF00B09B), Color(0xFF96C93D)),
    EMERALD_GLOW("Smaragdglanz", Color(0xFF11998E), Color(0xFF38EF7D)),
    FOREST("Wald", Color(0xFF1F6B7E), Color(0xFF71B280)),
    LIME_PUNCH("Limette", Color(0xFFA8E063), Color(0xFF56AB2F)),
    SPRING("Frühling", Color(0xFF43E97B), Color(0xFF38F9D7)),
    MEADOW("Wiese", Color(0xFF76B852), Color(0xFF8DC26F)),
    SUMMER("Sommer", Color(0xFFF7971E), Color(0xFFFFD200)),
    SUNSHINE("Sonnenschein", Color(0xFFFDC830), Color(0xFFF37335)),
    LEMONGRASS("Zitronengras", Color(0xFFFFE000), Color(0xFF799F0C)),
    GOLD_RUSH("Goldrausch", Color(0xFFFFB75E), Color(0xFFED8F03)),
    CITRUS("Zitrus", Color(0xFFF09819), Color(0xFFEDDE5D)),
    AUTUMN("Herbst", Color(0xFFD38312), Color(0xFFA83279)),
    COPPER("Kupfer", Color(0xFFB79891), Color(0xFF94716B)),
    MOCHA("Mokka", Color(0xFFA77044), Color(0xFF7A4B2A)),
    STEEL("Stahl", Color(0xFF8E9EAB), Color(0xFF5D6D7E)),
    SILVER_MOON("Silbermond", Color(0xFFBDC3C7), Color(0xFF4A6278)),
    AURORA("Polarlicht", Color(0xFF43C6AC), Color(0xFFF8FFAE)),
    DAWN("Morgenrot", Color(0xFFFF9A9E), Color(0xFFFAD0C4)),
    CARIBBEAN("Karibik", Color(0xFF1CB5E0), Color(0xFF0B8793));

    /** Middle colour: text, thin lines and everything that cannot show a gradient. */
    val color: Color get() = lerp(start, end, 0.5f)
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
    val onColor: Color get() = if (color.luminance() > 0.45f) Color(0xFF111111) else Color.White
    val container: Color get() = lerp(color, Color.Black, 0.35f)
    val hex: String get() = "#%06X".format(color.toArgb() and 0xFFFFFF)

    companion object {
        /** Saved per account next to the plain colour; a gradient wins when one is set. */
        fun key(kids: Boolean) = if (kids) "accent_gradient_kids" else "accent_gradient"
        fun parse(name: String?) = entries.firstOrNull { it.name == name }
    }
}

/** The gradient of the chosen accent, or null for a plain colour. */
internal val LocalAccentGradient = staticCompositionLocalOf<AccentGradient?> { null }

/** Brush for surfaces in the accent colour: the gradient if one is chosen, otherwise the colour. */
@Composable
internal fun accentBrush(): Brush = LocalAccentGradient.current?.brush ?: SolidColor(MaterialTheme.colorScheme.primary)

/** Colour scheme for a gradient accent (built on its middle colour). */
internal fun playerColors(gradient: AccentGradient) =
    playerColors(gradient.color, gradient.onColor, gradient.container)
