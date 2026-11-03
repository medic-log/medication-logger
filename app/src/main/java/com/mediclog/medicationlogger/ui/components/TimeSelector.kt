package com.mediclog.medicationlogger.ui.components

import android.text.format.DateFormat
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mediclog.medicationlogger.R
import java.time.LocalTime

/** A button showing [time] that opens a dialog to change it. */
@OptIn(ExperimentalMaterial3Api::class) // TimeInput has no stable alternative in Material 3
@Composable
fun TimeSelector(
    time: LocalTime,
    dialogTitle: String,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { isDialogOpen = true }, modifier = modifier) {
        Text(stringResource(R.string.reminders_choose_time, time.toString()))
    }
    if (isDialogOpen) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text(dialogTitle) },
            text = { TimeInput(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(pickerState.hour, pickerState.minute))
                        isDialogOpen = false
                    },
                ) {
                    Text(stringResource(R.string.reminders_time_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text(stringResource(R.string.reminders_time_cancel))
                }
            },
        )
    }
}
