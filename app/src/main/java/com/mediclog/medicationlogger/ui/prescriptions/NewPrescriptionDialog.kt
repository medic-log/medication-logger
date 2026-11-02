package com.mediclog.medicationlogger.ui.prescriptions

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Prescription
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** Asks for a new prescription's details, starting from those of the [previous] one. */
@Composable
fun NewPrescriptionDialog(
    previous: Prescription?,
    onSave: (PrescriptionDraft) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var doctor by rememberSaveable { mutableStateOf(previous?.prescribedBy.orEmpty()) }
    var pharmacy by rememberSaveable { mutableStateOf(previous?.pharmacy.orEmpty()) }
    var pillsPerBox by rememberSaveable { mutableStateOf(previous?.pillsPerBox?.toString().orEmpty()) }
    var boxesPerRepetition by rememberSaveable { mutableStateOf(previous?.boxesPerRepetition?.toString().orEmpty()) }
    var repetitionsTotal by rememberSaveable { mutableStateOf(previous?.repetitionsTotal?.toString().orEmpty()) }

    val draft = draftOrNull(doctor, pharmacy, pillsPerBox, boxesPerRepetition, repetitionsTotal)
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prescriptions_new)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DialogField(doctor, { doctor = it }, R.string.prescriptions_field_doctor)
                DialogField(pharmacy, { pharmacy = it }, R.string.prescriptions_field_pharmacy)
                DialogField(
                    pillsPerBox,
                    { pillsPerBox = it },
                    R.string.prescriptions_field_pills_per_box,
                    isNumber = true,
                )
                DialogField(
                    boxesPerRepetition,
                    { boxesPerRepetition = it },
                    R.string.prescriptions_field_boxes_per_repetition,
                    isNumber = true,
                )
                DialogField(
                    repetitionsTotal,
                    { repetitionsTotal = it },
                    R.string.prescriptions_field_repetitions_total,
                    isNumber = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { draft?.let(onSave) }, enabled = draft != null) {
                Text(stringResource(R.string.prescriptions_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.prescriptions_cancel))
            }
        },
    )
}

@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
        modifier = modifier.fillMaxWidth(),
    )
}

private fun draftOrNull(
    doctor: String,
    pharmacy: String,
    pillsPerBox: String,
    boxesPerRepetition: String,
    repetitionsTotal: String,
): PrescriptionDraft? {
    val pills = pillsPerBox.positiveIntOrNull() ?: return null
    val boxes = boxesPerRepetition.positiveIntOrNull() ?: return null
    val repetitions = repetitionsTotal.positiveIntOrNull() ?: return null
    if (doctor.isBlank() || pharmacy.isBlank()) return null
    return PrescriptionDraft(doctor.trim(), pharmacy.trim(), pills, boxes, repetitions)
}

private fun String.positiveIntOrNull() = trim().toIntOrNull()?.takeIf { it in 1..MAX_COUNT }

// Keeps multiplied totals well within Int
private const val MAX_COUNT = 999

@PreviewLightDark
@Composable
private fun NewPrescriptionDialogPreview() {
    MedicationLoggerTheme {
        NewPrescriptionDialog(previous = null, onSave = {}, onDismiss = {})
    }
}
