package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.displayText
import com.mediclog.medicationlogger.shortName
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.Month
import java.time.YearMonth

private const val MONTHS_PER_ROW = 3

/** A button showing [month] that opens a picker for it; with [canClear], the month can also be removed. */
@Composable
fun MonthSelector(
    month: YearMonth?,
    label: String,
    onMonthChange: (YearMonth?) -> Unit,
    modifier: Modifier = Modifier,
    canClear: Boolean = false,
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { isDialogOpen = true }, modifier = modifier, border = outlinedBorder(), colors = outlinedButtonColors()) {
        Text(
            if (month != null) {
                stringResource(R.string.month_selector_value, label, month.displayText())
            } else {
                stringResource(R.string.month_selector_empty, label)
            },
        )
    }
    if (isDialogOpen) {
        MonthPickerDialog(
            month = month,
            title = label,
            canClear = canClear && month != null,
            onMonthChange = {
                onMonthChange(it)
                isDialogOpen = false
            },
            onDismiss = { isDialogOpen = false },
        )
    }
}

// Material 3's date picker always asks for a day, which an expiry date doesn't have
@Composable
private fun MonthPickerDialog(
    month: YearMonth?,
    title: String,
    canClear: Boolean,
    onMonthChange: (YearMonth?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var year by rememberSaveable { mutableIntStateOf((month ?: YearMonth.now()).year) }
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year-- }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_remove),
                            contentDescription = stringResource(R.string.month_selector_previous_year),
                        )
                    }
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { year++ }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.month_selector_next_year),
                        )
                    }
                }
                Month.entries.chunked(MONTHS_PER_ROW).forEach { months ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        months.forEach { candidate ->
                            val candidateMonth = YearMonth.of(year, candidate)
                            val name = candidate.shortName(locale)
                            val buttonModifier = Modifier.weight(1f)
                            val padding = PaddingValues(0.dp)
                            if (candidateMonth == month) {
                                Button(
                                    onClick = { onMonthChange(candidateMonth) },
                                    modifier = buttonModifier,
                                    contentPadding = padding,
                                ) {
                                    Text(name)
                                }
                            } else {
                                TextButton(
                                    onClick = { onMonthChange(candidateMonth) },
                                    modifier = buttonModifier,
                                    contentPadding = padding,
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.edit_cancel))
            }
        },
        dismissButton = if (canClear) {
            {
                TextButton(onClick = { onMonthChange(null) }) {
                    Text(stringResource(R.string.month_selector_clear))
                }
            }
        } else {
            null
        },
    )
}

@PreviewLightDark
@Composable
private fun MonthPickerDialogPreview() {
    MedicationLoggerTheme {
        MonthPickerDialog(
            month = YearMonth.of(2028, 6),
            title = "Expires",
            canClear = true,
            onMonthChange = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MonthSelectorPreview() {
    MedicationLoggerTheme {
        MonthSelector(
            month = null,
            label = "Expires",
            onMonthChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
