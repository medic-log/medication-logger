package com.mediclog.medicationlogger.ui.refills

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.ui.components.DialogField
import com.mediclog.medicationlogger.ui.components.RecordEditDialog
import com.mediclog.medicationlogger.ui.components.dateOrNull
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** Edits a missed or delayed dose by hand, or deletes it. */
@Composable
fun EditMissDialog(
    miss: MedicationMiss,
    onSave: (MedicationMiss) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var date by rememberSaveable { mutableStateOf(miss.date.toString()) }
    var kind by rememberSaveable { mutableStateOf(miss.kind) }
    val parsedDate = date.dateOrNull()

    RecordEditDialog(
        title = stringResource(R.string.edit_miss_title),
        canSave = parsedDate != null,
        deleteMessage = stringResource(R.string.edit_delete_message),
        onSave = { parsedDate?.let { onSave(miss.copy(date = it, kind = kind)) } },
        onDelete = onDelete,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        DialogField(date, { date = it }, R.string.edit_field_date, isError = parsedDate == null)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MedicationMiss.Kind.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == kind,
                    onClick = { kind = option },
                    shape = SegmentedButtonDefaults.itemShape(index, MedicationMiss.Kind.entries.size),
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    MedicationMiss.Kind.MISSED -> R.string.refills_missed
                                    MedicationMiss.Kind.DELAYED -> R.string.refills_delayed
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun EditMissDialogPreview() {
    MedicationLoggerTheme {
        EditMissDialog(
            miss = MedicationMiss(1, LocalDate.of(2026, 11, 2), MedicationMiss.Kind.MISSED),
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
