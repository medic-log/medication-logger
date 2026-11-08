package com.mediclog.medicationlogger.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.ReminderSettings
import com.mediclog.medicationlogger.ui.components.displayName
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Tells that the device's time zone changed, and the times the pill and refill reminders moved to. */
@Composable
fun TimeZoneChangeDialog(change: TimeZoneChange, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val settings = change.settings
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = modifier,
        title = { Text(stringResource(R.string.time_zone_changed_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val to = settings.utcOffset?.let { "${change.zone.id} (${it.label()})" }.orEmpty()
                Text(stringResource(R.string.time_zone_changed_text, change.from.label(), to))
                if (settings.isPillEnabled) {
                    Text(stringResource(R.string.time_zone_changed_pill, settings.pillTime.toString()))
                }
                if (settings.isRefillEnabled) {
                    Text(
                        stringResource(
                            R.string.time_zone_changed_refill,
                            settings.refillDay.displayName(),
                            settings.refillTime.toString(),
                        ),
                    )
                }
                Text(stringResource(R.string.time_zone_changed_hint))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminders_time_confirm)) }
        },
    )
}

/** The offset as `UTC+01:00`. */
// UTC's own offset is written "Z"
private fun ZoneOffset.label() = "UTC${if (totalSeconds == 0) "+00:00" else id}"

@PreviewLightDark
@Composable
private fun TimeZoneChangeDialogPreview() {
    MedicationLoggerTheme {
        TimeZoneChangeDialog(
            change = TimeZoneChange(
                from = ZoneOffset.ofHours(1),
                zone = ZoneId.of("America/New_York"),
                settings = ReminderSettings(
                    refillDay = DayOfWeek.SUNDAY,
                    refillTime = LocalTime.of(14, 0),
                    pillTime = LocalTime.of(1, 30),
                    utcOffset = ZoneOffset.ofHours(-5),
                ),
            ),
            onDismiss = {},
        )
    }
}
