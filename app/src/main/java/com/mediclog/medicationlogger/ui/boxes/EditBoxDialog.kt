package com.mediclog.medicationlogger.ui.boxes

import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mediclog.medicationlogger.ui.components.MonthSelector
import com.mediclog.medicationlogger.ui.components.RecordEditDialog
import com.mediclog.medicationlogger.ui.components.countOrNull
import com.mediclog.medicationlogger.ui.components.dateOrNull
import com.mediclog.medicationlogger.ui.components.isOptionalDate
import com.mediclog.medicationlogger.ui.components.positiveCountOrNull
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** Edits the fields of a [box] by hand, or deletes it. */
@Composable
fun EditBoxDialog(
    box: Box,
    hasRefills: Boolean,
    onSave: (Box) -> Unit,
    onDelete: (keepRefills: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var boxNumber by rememberSaveable { mutableStateOf(box.boxNumber.toString()) }
    var boughtDate by rememberSaveable { mutableStateOf(box.boughtDate.toString()) }
    var openDate by rememberSaveable { mutableStateOf(box.openDate?.toString().orEmpty()) }
    var expiryDate by rememberSaveable { mutableStateOf(box.expiryDate) }
    var pillsLeft by rememberSaveable { mutableStateOf(box.pillsLeft.toString()) }
    var pillsTotal by rememberSaveable { mutableStateOf(box.pillsTotal.toString()) }

    val number = boxNumber.positiveCountOrNull()
    val date = boughtDate.dateOrNull()
    val total = pillsTotal.positiveCountOrNull()
    val left = pillsLeft.countOrNull()?.takeIf { total != null && it <= total }
    val updated = if (
        number != null && date != null && total != null && left != null && openDate.isOptionalDate()
    ) {
        box.copy(
            boxNumber = number,
            boughtDate = date,
            openDate = openDate.dateOrNull(),
            expiryDate = expiryDate,
            pillsLeft = left,
            pillsTotal = total,
        )
    } else {
        null
    }

    RecordEditDialog(
        title = stringResource(R.string.edit_box_title),
        canSave = updated != null,
        deleteMessage = stringResource(
            if (hasRefills) R.string.edit_box_delete_message else R.string.edit_delete_message,
        ),
        onSave = { updated?.let(onSave) },
        onDelete = { onDelete(false) },
        onDismiss = onDismiss,
        modifier = modifier,
        keepChildrenLabel = if (hasRefills) stringResource(R.string.edit_delete_keep_refills) else null,
        onDeleteKeepingChildren = { onDelete(true) },
    ) {
        DialogField(
            boxNumber,
            { boxNumber = it },
            R.string.edit_field_box_number,
            isNumber = true,
            isError = number == null,
        )
        DialogField(boughtDate, { boughtDate = it }, R.string.edit_field_bought_date, isError = date == null)
        DialogField(
            openDate,
            { openDate = it },
            R.string.edit_field_open_date,
            isError = !openDate.isOptionalDate(),
        )
        MonthSelector(
            month = expiryDate,
            label = stringResource(R.string.edit_field_expiry_date),
            onMonthChange = { expiryDate = it },
            modifier = Modifier.fillMaxWidth(),
            canClear = true,
        )
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
                boughtDate = LocalDate.of(2026, 11, 2),
                pillsLeft = 23,
                pillsTotal = 30,
            ),
            hasRefills = true,
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
