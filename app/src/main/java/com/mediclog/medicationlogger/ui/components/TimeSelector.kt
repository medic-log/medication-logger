package com.mediclog.medicationlogger.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.R
import java.time.LocalTime

/** A button showing [time] that opens a dialog to change it. */
@Composable
fun TimeSelector(
    time: LocalTime,
    dialogTitle: String,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { isDialogOpen = true }, modifier = modifier, border = outlinedBorder(), colors = outlinedButtonColors()) {
        Text(stringResource(R.string.reminders_choose_time, time.toString()))
    }
    if (isDialogOpen) {
        TimePickerDialog(
            time = time,
            title = dialogTitle,
            onConfirm = {
                onTimeChange(it)
                isDialogOpen = false
            },
            onDismiss = { isDialogOpen = false },
        )
    }
}

/** A dialog to pick a time, starting at [time]. */
@OptIn(ExperimentalMaterial3Api::class) // TimeInput has no stable alternative in Material 3
@Composable
fun TimePickerDialog(
    time: LocalTime,
    title: String,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        // Always 24-hour, like the times the app shows: in 12-hour input, typing 7 into an evening time gives 19:00
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(title) },
        text = { TimeInput(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.reminders_time_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reminders_time_cancel))
            }
        },
    )
}
