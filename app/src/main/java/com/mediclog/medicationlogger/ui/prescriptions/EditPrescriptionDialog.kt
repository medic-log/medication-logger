package com.mediclog.medicationlogger.ui.prescriptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Prescription
import com.mediclog.medicationlogger.ui.components.DialogField
import com.mediclog.medicationlogger.ui.components.RecordEditDialog
import com.mediclog.medicationlogger.ui.components.countOrNull
import com.mediclog.medicationlogger.ui.components.dateOrNull
import com.mediclog.medicationlogger.ui.components.isOptionalDate
import com.mediclog.medicationlogger.ui.components.positiveCountOrNull
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** Edits every field of a [prescription] by hand, or deletes it. */
@Composable
fun EditPrescriptionDialog(
    prescription: Prescription,
    hasBoxes: Boolean,
    onSave: (Prescription) -> Unit,
    onDelete: (keepBoxes: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var prescribedDate by rememberSaveable { mutableStateOf(prescription.prescribedDate.toString()) }
    var medication by rememberSaveable { mutableStateOf(prescription.medication) }
    var doctor by rememberSaveable { mutableStateOf(prescription.prescribedBy) }
    var pharmacy by rememberSaveable { mutableStateOf(prescription.pharmacy) }
    var pillsPerBox by rememberSaveable { mutableStateOf(prescription.pillsPerBox.toString()) }
    var boxesPerRepetition by rememberSaveable { mutableStateOf(prescription.boxesPerRepetition.toString()) }
    var repetitionsTotal by rememberSaveable { mutableStateOf(prescription.repetitionsTotal.toString()) }
    var repetitionsBought by rememberSaveable { mutableStateOf(prescription.repetitionsBought.toString()) }
    var orderedDate by rememberSaveable { mutableStateOf(prescription.orderedDate?.toString().orEmpty()) }
    var boughtFirstDate by rememberSaveable { mutableStateOf(prescription.boughtFirstDate?.toString().orEmpty()) }
    var boughtLastDate by rememberSaveable { mutableStateOf(prescription.boughtLastDate?.toString().orEmpty()) }

    val total = repetitionsTotal.positiveCountOrNull()
    val bought = repetitionsBought.countOrNull()?.takeIf { total != null && it <= total }
    val date = prescribedDate.dateOrNull()
    val pills = pillsPerBox.positiveCountOrNull()
    val boxes = boxesPerRepetition.positiveCountOrNull()
    val areOptionalDatesValid = listOf(orderedDate, boughtFirstDate, boughtLastDate).all { it.isOptionalDate() }
    val updated = if (
        date != null && pills != null && boxes != null && total != null && bought != null &&
        medication.isNotBlank() && doctor.isNotBlank() && pharmacy.isNotBlank() && areOptionalDatesValid
    ) {
        prescription.copy(
            prescribedDate = date,
            medication = medication.trim(),
            prescribedBy = doctor.trim(),
            pharmacy = pharmacy.trim(),
            pillsPerBox = pills,
            boxesPerRepetition = boxes,
            repetitionsTotal = total,
            repetitionsBought = bought,
            orderedDate = orderedDate.dateOrNull(),
            boughtFirstDate = boughtFirstDate.dateOrNull(),
            boughtLastDate = boughtLastDate.dateOrNull(),
        )
    } else {
        null
    }

    RecordEditDialog(
        title = stringResource(R.string.edit_prescription_title),
        canSave = updated != null,
        deleteMessage = stringResource(
            if (hasBoxes) R.string.edit_prescription_delete_message else R.string.edit_delete_message,
        ),
        onSave = { updated?.let(onSave) },
        onDelete = { onDelete(false) },
        onDismiss = onDismiss,
        modifier = modifier,
        keepChildrenLabel = if (hasBoxes) stringResource(R.string.edit_delete_keep_boxes) else null,
        onDeleteKeepingChildren = { onDelete(true) },
    ) {
        DialogField(
            prescribedDate,
            { prescribedDate = it },
            R.string.edit_field_prescribed_date,
            isError = date == null,
        )
        DialogField(
            medication,
            { medication = it },
            R.string.prescriptions_field_medication,
            isError = medication.isBlank(),
        )
        DialogField(doctor, { doctor = it }, R.string.prescriptions_field_doctor, isError = doctor.isBlank())
        DialogField(pharmacy, { pharmacy = it }, R.string.prescriptions_field_pharmacy, isError = pharmacy.isBlank())
        DialogField(
            pillsPerBox,
            { pillsPerBox = it },
            R.string.prescriptions_field_pills_per_box,
            isNumber = true,
            isError = pills == null,
        )
        DialogField(
            boxesPerRepetition,
            { boxesPerRepetition = it },
            R.string.prescriptions_field_boxes_per_repetition,
            isNumber = true,
            isError = boxes == null,
        )
        DialogField(
            repetitionsTotal,
            { repetitionsTotal = it },
            R.string.prescriptions_field_repetitions_total,
            isNumber = true,
            isError = total == null,
        )
        DialogField(
            repetitionsBought,
            { repetitionsBought = it },
            R.string.edit_field_repetitions_bought,
            isNumber = true,
            isError = bought == null,
        )
        DialogField(
            orderedDate,
            { orderedDate = it },
            R.string.edit_field_ordered_date,
            isError = !orderedDate.isOptionalDate(),
        )
        DialogField(
            boughtFirstDate,
            { boughtFirstDate = it },
            R.string.edit_field_bought_first_date,
            isError = !boughtFirstDate.isOptionalDate(),
        )
        DialogField(
            boughtLastDate,
            { boughtLastDate = it },
            R.string.edit_field_bought_last_date,
            isError = !boughtLastDate.isOptionalDate(),
        )
    }
}

@PreviewLightDark
@Composable
private fun EditPrescriptionDialogPreview() {
    MedicationLoggerTheme {
        EditPrescriptionDialog(
            prescription = Prescription(
                id = 1,
                medication = "Ibuprofen 600 mg",
                prescribedDate = LocalDate.of(2026, 11, 2),
                prescribedBy = "A doctor",
                pharmacy = "Corner pharmacy",
                pillsPerBox = 30,
                boxesPerRepetition = 3,
                repetitionsTotal = 2,
            ),
            hasBoxes = true,
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
