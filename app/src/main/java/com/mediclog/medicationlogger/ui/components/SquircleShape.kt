package com.mediclog.medicationlogger.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// A superellipse of this exponent is the usual squircle: flatter sides than a circle, softer corners than a square
private const val SQUIRCLE_EXPONENT = 4.0
private const val SQUIRCLE_POINTS = 64

/** A squircle filling its bounds. */
object SquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val halfWidth = size.width / 2
        val halfHeight = size.height / 2
        val path = Path()
        for (index in 0 until SQUIRCLE_POINTS) {
            val angle = 2 * Math.PI * index / SQUIRCLE_POINTS
            val x = halfWidth + halfWidth * superellipse(cos(angle))
            val y = halfHeight + halfHeight * superellipse(sin(angle))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    private fun superellipse(value: Double) = (sign(value) * abs(value).pow(2 / SQUIRCLE_EXPONENT)).toFloat()
}
