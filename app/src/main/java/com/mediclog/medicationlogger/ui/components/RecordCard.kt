package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.theme.StatusColors

// Longer than the system's long press, so that a card isn't opened for editing by accident
private const val EDIT_HOLD_MILLIS = 1000L

/**
 * A card for one record; highlighted when the user arrived by following a link to it.
 * With [statusColors], the card and everything in it take on the colors of the record's status.
 * With [onEdit], holding the card down asks whether to edit its record, and calls [onEdit] if so.
 */
@Composable
fun RecordCard(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    statusColors: StatusColors? = null,
    onEdit: (() -> Unit)? = null,
    shape: Shape = CardDefaults.shape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val systemConfiguration = LocalViewConfiguration.current
    val viewConfiguration = remember(systemConfiguration) {
        object : ViewConfiguration by systemConfiguration {
            override val longPressTimeoutMillis = EDIT_HOLD_MILLIS
        }
    }
    val haptics = LocalHapticFeedback.current
    var isConfirmingEdit by rememberSaveable { mutableStateOf(false) }
    val editLabel = stringResource(R.string.edit_action)
    CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isHighlighted) Modifier.semantics { selected = true } else Modifier)
                .then(
                    if (onEdit == null) {
                        Modifier
                    } else {
                        Modifier
                            .semantics {
                                onLongClick(label = editLabel) {
                                    isConfirmingEdit = true
                                    true
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isConfirmingEdit = true
                                    },
                                )
                            }
                    },
                ),
            shape = shape,
            colors = when {
                statusColors != null -> CardDefaults.cardColors(
                    containerColor = statusColors.cardTint,
                    contentColor = colorScheme.onSurface,
                )
                isHighlighted -> CardDefaults.cardColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                )
                else -> CardDefaults.cardColors()
            },
            border = when {
                isHighlighted -> BorderStroke(3.dp, statusColors?.accent ?: colorScheme.primary)
                statusColors != null -> BorderStroke(1.dp, statusColors.accent)
                else -> null
            },
        ) {
            // Buttons in the card read these roles, so they follow the status too
            val cardColorScheme = if (statusColors == null) {
                colorScheme
            } else {
                colorScheme.copy(
                    primary = statusColors.accent,
                    onPrimary = statusColors.onAccent,
                    outline = statusColors.accent,
                    outlineVariant = statusColors.accent,
                )
            }
            MaterialTheme(colorScheme = cardColorScheme) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        }
    }
    if (isConfirmingEdit && onEdit != null) {
        AlertDialog(
            onDismissRequest = { isConfirmingEdit = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.edit_confirm_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirmingEdit = false
                        onEdit()
                    },
                ) {
                    Text(editLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingEdit = false }) {
                    Text(stringResource(R.string.edit_cancel))
                }
            },
        )
    }
}
