package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private const val DISABLED_ALPHA = 0.12f

/**
 * The border of an outlined button, in `outline` like the filter buttons: Material's default `outlineVariant` barely
 * shows in the dark theme.
 */
@Composable
fun outlinedBorder(isEnabled: Boolean = true): BorderStroke = BorderStroke(
    1.dp,
    if (isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface.copy(DISABLED_ALPHA),
)
