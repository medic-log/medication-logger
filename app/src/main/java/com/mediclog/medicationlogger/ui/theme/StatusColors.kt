package com.mediclog.medicationlogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The colors of a record's status: its soft pill and card tint, a stronger accent for outlines and buttons, and the
 * dot in its pill. The dot is the light accent in both themes, as the dark accent is the same color as the dark pill.
 */
@Immutable
data class StatusColors(
    val pill: Color,
    val onPill: Color,
    val cardTint: Color,
    val accent: Color,
    val onAccent: Color,
    val dot: Color,
)

private val OnLightAccent = Color(0xFFFFFFFF)

/** The status colors, which Material's color roles have no room for. */
enum class StatusTone(val light: StatusColors, val dark: StatusColors) {
    NOT_STARTED(
        light = StatusColors(
            pill = Color(0xFFE3E1E0),
            onPill = Color(0xFF2F2E2E),
            cardTint = Color(0xFFF4F2F1),
            accent = Color(0xFF5F5E5D),
            onAccent = OnLightAccent,
            dot = Color(0xFF5F5E5D),
        ),
        dark = StatusColors(
            pill = Color(0xFFD0CDCC),
            onPill = Color(0xFF2F2E2E),
            cardTint = Color(0xFF2B2928),
            accent = Color(0xFFD0CDCC),
            onAccent = Color(0xFF2F2E2E),
            dot = Color(0xFF5F5E5D),
        ),
    ),
    ORDERED(
        light = StatusColors(
            pill = Color(0xFFFBE7A1),
            onPill = Color(0xFF3A2E00),
            cardTint = Color(0xFFFFF8DC),
            accent = Color(0xFF7A5F00),
            onAccent = OnLightAccent,
            dot = Color(0xFF7A5F00),
        ),
        dark = StatusColors(
            pill = Color(0xFFF2DC8E),
            onPill = Color(0xFF3A2E00),
            cardTint = Color(0xFF332C14),
            accent = Color(0xFFF2DC8E),
            onAccent = Color(0xFF3A2E00),
            dot = Color(0xFF7A5F00),
        ),
    ),
    IN_PROGRESS(
        light = StatusColors(
            pill = Color(0xFFC9E2F8),
            onPill = Color(0xFF0F2F47),
            cardTint = Color(0xFFEDF5FC),
            accent = Color(0xFF2A6396),
            onAccent = OnLightAccent,
            dot = Color(0xFF2A6396),
        ),
        dark = StatusColors(
            pill = Color(0xFFA9D0F2),
            onPill = Color(0xFF0F2F47),
            cardTint = Color(0xFF1C2A36),
            accent = Color(0xFFA9D0F2),
            onAccent = Color(0xFF0F2F47),
            dot = Color(0xFF2A6396),
        ),
    ),
    EXTRA(
        light = StatusColors(
            pill = Color(0xFFC8EBCB),
            onPill = Color(0xFF10341A),
            cardTint = Color(0xFFEDF8EE),
            accent = Color(0xFF2F6B3A),
            onAccent = OnLightAccent,
            dot = Color(0xFF2F6B3A),
        ),
        dark = StatusColors(
            pill = Color(0xFFA8DBAE),
            onPill = Color(0xFF10341A),
            cardTint = Color(0xFF1D2E20),
            accent = Color(0xFFA8DBAE),
            onAccent = Color(0xFF10341A),
            dot = Color(0xFF2F6B3A),
        ),
    ),
    FINISHED(
        light = StatusColors(
            pill = Color(0xFFFFC9BF),
            onPill = Color(0xFF4A0F08),
            cardTint = Color(0xFFFFEDE9),
            accent = Color(0xFFA63F35),
            onAccent = OnLightAccent,
            dot = Color(0xFFA63F35),
        ),
        dark = StatusColors(
            pill = Color(0xFFFFB4A8),
            onPill = Color(0xFF5F1510),
            cardTint = Color(0xFF3A2320),
            accent = Color(0xFFFFB4A8),
            onAccent = Color(0xFF5F1510),
            dot = Color(0xFFA63F35),
        ),
    ),
    ;

    /** The colors for the current light or dark setting, as the theme follows it too. */
    val colors: StatusColors
        @Composable get() = if (isSystemInDarkTheme()) dark else light
}
