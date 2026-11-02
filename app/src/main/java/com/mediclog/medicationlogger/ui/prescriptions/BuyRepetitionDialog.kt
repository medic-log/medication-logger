package com.mediclog.medicationlogger.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mediclog.medicationlogger.ui.components.MonthSelector
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.YearMonth

/** Asks the expiry date of each of a repetition's [boxCount] boxes, numbered from [firstBoxNumber], before buying. */
@Composable
fun BuyRepetitionDialog(
    boxCount: Int,
    firstBoxNumber: Int,
    onSave: (List<YearMonth>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dates by rememberSaveable { mutableStateOf(List<YearMonth?>(boxCount) { null }) }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prescriptions_expiry_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dates.forEachIndexed { index, date ->
                    MonthSelector(
                        month = date,
                        label = stringResource(R.string.prescriptions_expiry_field, firstBoxNumber + index),
                        onMonthChange = { dates = dates.withDate(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(dates.filterNotNull()) },
                enabled = dates.none { it == null },
            ) {
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

// Boxes bought together mostly expire together, so the first date chosen starts them all
private fun List<YearMonth?>.withDate(index: Int, date: YearMonth?): List<YearMonth?> {
    val isFirstDate = all { it == null }
    return mapIndexed { other, current -> if (isFirstDate || other == index) date else current }
}

@PreviewLightDark
@Composable
private fun BuyRepetitionDialogPreview() {
    MedicationLoggerTheme {
        BuyRepetitionDialog(boxCount = 3, firstBoxNumber = 1, onSave = {}, onDismiss = {})
    }
}
