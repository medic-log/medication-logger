package com.mediclog.medicationlogger.ui.refills

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Refill
import com.mediclog.medicationlogger.ui.components.DialogField
import com.mediclog.medicationlogger.ui.components.RecordEditDialog
import com.mediclog.medicationlogger.ui.components.dateOrNull
import com.mediclog.medicationlogger.ui.components.positiveCountOrNull
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** Edits a [refill] by hand, up to [maxAmount] pills, or deletes it. Pills left follow from the amount. */
@Composable
fun EditRefillDialog(
    refill: Refill,
    maxAmount: Int,
    onSave: (Refill) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var date by rememberSaveable { mutableStateOf(refill.date.toString()) }
    var amount by rememberSaveable { mutableStateOf(refill.amount.toString()) }
    var isPartial by rememberSaveable { mutableStateOf(refill.isPartial) }

    val parsedDate = date.dateOrNull()
    val parsedAmount = amount.positiveCountOrNull()?.takeIf { it <= maxAmount }
    val updated = if (parsedDate != null && parsedAmount != null) {
        refill.copy(date = parsedDate, amount = parsedAmount, isPartial = isPartial)
    } else {
        null
    }

    RecordEditDialog(
        title = stringResource(R.string.edit_refill_title),
        canSave = updated != null,
        deleteMessage = stringResource(
            if (refill.boxId != null) R.string.edit_refill_delete_message else R.string.edit_delete_message,
        ),
        onSave = { updated?.let(onSave) },
        onDelete = onDelete,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        DialogField(date, { date = it }, R.string.edit_field_date, isError = parsedDate == null)
        DialogField(
            amount,
            { amount = it },
            stringResource(R.string.edit_field_refill_amount, maxAmount),
            isNumber = true,
            isError = parsedAmount == null,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.edit_field_partial),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = isPartial, onCheckedChange = { isPartial = it })
        }
    }
}

@PreviewLightDark
@Composable
private fun EditRefillDialogPreview() {
    MedicationLoggerTheme {
        EditRefillDialog(
            refill = Refill(1, 1, 1, LocalDate.of(2026, 11, 2), 7, 23),
            maxAmount = 30,
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
