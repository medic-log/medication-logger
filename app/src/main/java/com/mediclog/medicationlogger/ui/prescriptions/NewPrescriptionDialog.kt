package com.mediclog.medicationlogger.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Prescription
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.ui.components.DialogField
import com.mediclog.medicationlogger.ui.components.positiveCountOrNull
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** Asks for a new prescription's details, starting from those of the [previous] one. */
@Composable
fun NewPrescriptionDialog(
    previous: Prescription?,
    onSave: (PrescriptionDraft) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var medication by rememberSaveable { mutableStateOf(previous?.medication.orEmpty()) }
    var doctor by rememberSaveable { mutableStateOf(previous?.prescribedBy.orEmpty()) }
    var pharmacy by rememberSaveable { mutableStateOf(previous?.pharmacy.orEmpty()) }
    var pillsPerBox by rememberSaveable { mutableStateOf(previous?.pillsPerBox?.toString().orEmpty()) }
    var boxesPerRepetition by rememberSaveable { mutableStateOf(previous?.boxesPerRepetition?.toString().orEmpty()) }
    var repetitionsTotal by rememberSaveable { mutableStateOf(previous?.repetitionsTotal?.toString().orEmpty()) }

    val draft = draftOrNull(medication, doctor, pharmacy, pillsPerBox, boxesPerRepetition, repetitionsTotal)
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prescriptions_new)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DialogField(medication, { medication = it }, R.string.prescriptions_field_medication)
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

private fun draftOrNull(
    medication: String,
    doctor: String,
    pharmacy: String,
    pillsPerBox: String,
    boxesPerRepetition: String,
    repetitionsTotal: String,
): PrescriptionDraft? {
    val pills = pillsPerBox.positiveCountOrNull() ?: return null
    val boxes = boxesPerRepetition.positiveCountOrNull() ?: return null
    val repetitions = repetitionsTotal.positiveCountOrNull() ?: return null
    if (medication.isBlank() || doctor.isBlank() || pharmacy.isBlank()) return null
    return PrescriptionDraft(medication.trim(), doctor.trim(), pharmacy.trim(), pills, boxes, repetitions)
}

@PreviewLightDark
@Composable
private fun NewPrescriptionDialogPreview() {
    MedicationLoggerTheme {
        NewPrescriptionDialog(previous = null, onSave = {}, onDismiss = {})
    }
}
