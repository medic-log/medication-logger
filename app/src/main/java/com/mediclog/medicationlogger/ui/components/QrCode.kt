package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlin.math.floor
import kotlin.math.min

/**
 * The blank border around the code, counted in modules. The standard asks for four; the extra
 * two only give the code a little more room on its ground.
 */
private const val MARGIN_MODULES = 5

/**
 * Enough correction for a code read off a screen, without spending so much of it on redundancy
 * that the modules turn small and hard to read.
 */
private val ERROR_CORRECTION = ErrorCorrectionLevel.M

/** How much of a module the rounding takes off its corners. */
private const val MODULE_CORNER_FRACTION = 0.3f

/** The side of a finder pattern — the three big marks in the corners — counted in modules. */
private const val FINDER_MODULES = 7

/** The filled square inside a finder pattern: three modules across, two in from its edge. */
private const val FINDER_EYE_MODULES = 3
private const val FINDER_EYE_INSET_MODULES = 2

/** Corner rounding of a finder pattern's ring and eye, in modules. */
private const val FINDER_CORNER_MODULES = 2f
private const val FINDER_EYE_CORNER_MODULES = 1f

/**
 * A scannable QR code for [content], filling the space the caller gives it. Modules snap to whole
 * pixels and the code is centred in what is left, so the squares stay crisp at any size. The
 * ground is a squircle and the marks on it are rounded, the finders as a ring around an eye.
 *
 * [contentDescription] is what a reader announces; null where the link is written out beside it.
 *
 * The colours are dark on light whatever the theme is: many readers refuse a code drawn the other
 * way round, so this is what it has to be rather than a matter of how it looks.
 */
@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color.Black,
    background: Color = Color.White,
    contentDescription: String? = null
) {
    // Encoding is pure and only depends on the text, so it is kept rather than redone on every
    // recomposition.
    val matrix = remember(content) { Encoder.encode(content, ERROR_CORRECTION).matrix }
    val semanticsModifier = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    }
    val finders = remember(matrix) { finderOrigins(matrix.width, matrix.height) }
    // The geometry only changes when the box does, so it is worked out once per size
    // rather than on every frame the code is drawn on.
    Spacer(
        modifier = modifier
            .then(semanticsModifier)
            .background(background, SquircleShape)
            .drawWithCache {
                val span = matrix.width + 2 * MARGIN_MODULES
                val module = floor(min(size.width, size.height) / span)
                val extent = module * span
                // The code sits centred on the ground, whatever the snapping to whole pixels
                // left over.
                val origin = Offset(
                    (size.width - extent) / 2f + MARGIN_MODULES * module,
                    (size.height - extent) / 2f + MARGIN_MODULES * module
                )
                val moduleSize = Size(module, module)
                val moduleCorner = CornerRadius(module * MODULE_CORNER_FRACTION)

                onDrawBehind {
                    if (module < 1f) return@onDrawBehind
                    for (row in 0 until matrix.height) {
                        for (column in 0 until matrix.width) {
                            if (matrix[column, row].toInt() != 1) continue
                            // The finder patterns are drawn whole, below, so their modules are
                            // left out here.
                            if (finders.any { (x, y) ->
                                    column - x in 0 until FINDER_MODULES &&
                                        row - y in 0 until FINDER_MODULES
                                }
                            ) {
                                continue
                            }
                            drawRoundRect(
                                color = foreground,
                                topLeft = Offset(
                                    origin.x + column * module,
                                    origin.y + row * module
                                ),
                                size = moduleSize,
                                cornerRadius = moduleCorner
                            )
                        }
                    }
                    finders.forEach { (column, row) ->
                        drawFinder(
                            topLeft = Offset(
                                origin.x + column * module,
                                origin.y + row * module
                            ),
                            module = module,
                            color = foreground
                        )
                    }
                }
            }
    )
}

/** Where the three finder patterns start: top-left, top-right and bottom-left of the matrix. */
private fun finderOrigins(width: Int, height: Int): List<Pair<Int, Int>> = listOf(
    0 to 0,
    width - FINDER_MODULES to 0,
    0 to height - FINDER_MODULES
)

/**
 * One finder pattern: a ring one module thick around an eye of three, both rounded. Drawn from
 * the geometry rather than from the matrix, so the corners come out even.
 */
private fun DrawScope.drawFinder(topLeft: Offset, module: Float, color: Color) {
    // A stroke straddles the line it is drawn on, so the ring is inset by half a module to keep
    // it inside the seven the pattern is allowed.
    val ringInset = module / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(topLeft.x + ringInset, topLeft.y + ringInset),
        size = Size(
            (FINDER_MODULES - 1) * module,
            (FINDER_MODULES - 1) * module
        ),
        cornerRadius = CornerRadius(FINDER_CORNER_MODULES * module),
        style = Stroke(width = module)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(
            topLeft.x + FINDER_EYE_INSET_MODULES * module,
            topLeft.y + FINDER_EYE_INSET_MODULES * module
        ),
        size = Size(FINDER_EYE_MODULES * module, FINDER_EYE_MODULES * module),
        cornerRadius = CornerRadius(FINDER_EYE_CORNER_MODULES * module)
    )
}
