package com.iptvplayer.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val LogoBlue = Color(0xFF3B82F6)
private val LogoPurple = Color(0xFFA855F7)
private val LogoPink = Color(0xFFFF4D8D)

/**
 * The StreamWatch mark (striped play symbol) and word mark on a transparent background,
 * drawn after Extern/streamwatch-modern-tv-banner.svg so it scales sharply to any height.
 */
@Composable
internal fun StreamWatchLogo(height: Dp = 30.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(height * 0.28f)) {
        StreamWatchMark(Modifier.size(height))
        val textSize = (height.value * 0.78f).sp
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("Stream") }
                withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(LogoPurple, LogoPink)))) { append("Watch") }
            },
            style = TextStyle(fontSize = textSize, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em),
            maxLines = 1
        )
    }
}

/** Play triangle with rounded corners and two cut-out stripes (SVG viewBox 150..410 × 124..388). */
@Composable
internal fun StreamWatchMark(modifier: Modifier) {
    Canvas(modifier) {
        // Source geometry: triangle (196,146) (196,366) (376,256), stroke 44, inside 150..398 × 124..388.
        val minX = 160f
        val minY = 124f
        val span = 264f
        val scale = size.minDimension / span
        fun x(value: Float) = (value - minX) * scale
        fun y(value: Float) = (value - minY) * scale
        val triangle = Path().apply {
            moveTo(x(196f), y(146f))
            lineTo(x(196f), y(366f))
            lineTo(x(376f), y(256f))
            close()
        }
        val brush = Brush.linearGradient(listOf(LogoBlue, LogoPurple, LogoPink), Offset.Zero, Offset(size.width, size.height))
        val stripe = 16f * scale
        // The stripes are transparent gaps, so the mark works on any background.
        clipRect(0f, y(226f), size.width, y(226f) + stripe, ClipOp.Difference) {
            clipRect(0f, y(272f), size.width, y(272f) + stripe, ClipOp.Difference) {
                drawPath(triangle, brush, style = Fill)
                drawPath(triangle, brush, style = Stroke(width = 44f * scale, join = StrokeJoin.Round))
            }
        }
    }
}
