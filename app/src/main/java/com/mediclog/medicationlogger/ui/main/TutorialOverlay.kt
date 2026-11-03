package com.mediclog.medicationlogger.ui.main

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp

private const val SCRIM_ALPHA = 0.6f
private const val MOVE_MILLIS = 250

/**
 * Dims the screen around the tutorial: spotlights on [highlight] and on the current [tab], each with an outline,
 * and the [card] left clear. It only draws; the main screen stops touches reaching the app behind.
 * Bounds are in window coordinates.
 */
@Composable
fun TutorialOverlay(highlight: Rect?, tab: Rect?, card: Rect?, modifier: Modifier = Modifier) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val spotlights = listOf(animateSpotlight(highlight, card, "spotlight"), animateSpotlight(tab, card, "tab"))
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val outline = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            // Offscreen, so the cleared holes show the app rather than black
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(scrim)
        card?.let { drawRect(Color.Black, it.topLeft - origin, it.size, blendMode = BlendMode.Clear) }
        val padding = 8.dp.toPx()
        val corner = CornerRadius(16.dp.toPx())
        spotlights.filter { it.width > 0f }.forEach { spotlight ->
            val hole = spotlight.translate(-origin).inflate(padding)
            drawRoundRect(Color.Black, hole.topLeft, hole.size, corner, blendMode = BlendMode.Clear)
            drawRoundRect(outline, hole.topLeft, hole.size, corner, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/** Moves a spotlight to [target]; without one, it closes to the middle of the [card], so the next grows from there. */
@Composable
private fun animateSpotlight(target: Rect?, card: Rect?, label: String): Rect {
    val bounds = target ?: card?.let { Rect(it.center, 0f) } ?: Rect.Zero
    val spotlight by animateValueAsState(bounds, Rect.VectorConverter, tween(MOVE_MILLIS), label = label)
    return spotlight
}
