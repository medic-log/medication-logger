package com.mediclog.medicationlogger.ui.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.DateStyle
import com.mediclog.medicationlogger.displayText
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** Picks how dates are shown, applied on Confirm; the language option shows [today] in it, as it varies. */
@Composable
fun DateFormatDialog(
    selected: DateStyle,
    today: LocalDate,
    onConfirm: (DateStyle) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var chosen by rememberSaveable { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_date_format_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                DateStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = style == chosen, onClick = { chosen = style }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = style == chosen, onClick = null)
                        Text(
                            text = when (style) {
                                DateStyle.YEAR_MONTH_DAY -> stringResource(R.string.settings_date_format_year_month_day)
                                DateStyle.DAY_MONTH_YEAR -> stringResource(R.string.settings_date_format_day_month_year)
                                DateStyle.MONTH_DAY_YEAR -> stringResource(R.string.settings_date_format_month_day_year)
                                DateStyle.LANGUAGE ->
                                    stringResource(R.string.settings_date_format_language, today.displayText(style))
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen) }) { Text(stringResource(R.string.settings_language_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_cancel)) }
        },
    )
}

@PreviewLightDark
@Composable
private fun DateFormatDialogPreview() {
    MedicationLoggerTheme {
        DateFormatDialog(
            selected = DateStyle.YEAR_MONTH_DAY,
            today = LocalDate.of(2026, 11, 14),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
