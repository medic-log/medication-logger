package com.mediclog.medicationlogger.ui.boxes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.ui.components.DialogField
import com.mediclog.medicationlogger.ui.components.RecordEditDialog
import com.mediclog.medicationlogger.ui.components.countOrNull
import com.mediclog.medicationlogger.ui.components.dateOrNull
import com.mediclog.medicationlogger.ui.components.positiveCountOrNull
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** Edits the fields of a [box] by hand, or deletes it. */
@Composable
fun EditBoxDialog(
    box: Box,
    onSave: (Box) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var boxNumber by rememberSaveable { mutableStateOf(box.boxNumber.toString()) }
    var pharmacy by rememberSaveable { mutableStateOf(box.pharmacy) }
    var boughtDate by rememberSaveable { mutableStateOf(box.boughtDate.toString()) }
    var pillsLeft by rememberSaveable { mutableStateOf(box.pillsLeft.toString()) }
    var pillsTotal by rememberSaveable { mutableStateOf(box.pillsTotal.toString()) }

    val number = boxNumber.positiveCountOrNull()
    val date = boughtDate.dateOrNull()
    val total = pillsTotal.positiveCountOrNull()
    val left = pillsLeft.countOrNull()?.takeIf { total != null && it <= total }
    val updated = if (number != null && date != null && total != null && left != null && pharmacy.isNotBlank()) {
        box.copy(
            boxNumber = number,
            pharmacy = pharmacy.trim(),
            boughtDate = date,
            pillsLeft = left,
            pillsTotal = total,
        )
    } else {
        null
    }

    RecordEditDialog(
        title = stringResource(R.string.edit_box_title),
        canSave = updated != null,
        deleteMessage = stringResource(R.string.edit_box_delete_message),
        onSave = { updated?.let(onSave) },
        onDelete = onDelete,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        DialogField(
            boxNumber,
            { boxNumber = it },
            R.string.edit_field_box_number,
            isNumber = true,
            isError = number == null,
        )
        DialogField(pharmacy, { pharmacy = it }, R.string.prescriptions_field_pharmacy, isError = pharmacy.isBlank())
        DialogField(boughtDate, { boughtDate = it }, R.string.edit_field_bought_date, isError = date == null)
        DialogField(
            pillsLeft,
            { pillsLeft = it },
            R.string.edit_field_pills_left,
            isNumber = true,
            isError = left == null,
        )
        DialogField(
            pillsTotal,
            { pillsTotal = it },
            R.string.edit_field_pills_total,
            isNumber = true,
            isError = total == null,
        )
    }
}

@PreviewLightDark
@Composable
private fun EditBoxDialogPreview() {
    MedicationLoggerTheme {
        EditBoxDialog(
            box = Box(
                id = 1,
                prescriptionId = 1,
                boxNumber = 1,
                pharmacy = "Corner pharmacy",
                boughtDate = LocalDate.of(2026, 11, 2),
                pillsLeft = 23,
                pillsTotal = 30,
            ),
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
