package com.mediclog.medicationlogger.ui.refills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.Refill
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.components.LinkIconButton
import com.mediclog.medicationlogger.ui.components.RecordCard
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** The refills tab with its ViewModel. */
@Composable
fun RefillsRoute(
    onOpenPrescription: (Long) -> Unit,
    onOpenBox: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RefillsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RefillsScreen(
        state = state,
        onLogMiss = viewModel::logMiss,
        onEditRefill = viewModel::editRefill,
        onDeleteRefill = viewModel::deleteRefill,
        onEditMiss = viewModel::editMiss,
        onDeleteMiss = viewModel::deleteMiss,
        onOpenPrescription = onOpenPrescription,
        onOpenBox = onOpenBox,
        modifier = modifier,
    )
}

/** The log of refills, misses and delays. */
@Composable
fun RefillsScreen(
    state: RefillsUiState,
    onLogMiss: (MedicationMiss.Kind) -> Unit,
    onEditRefill: (Refill) -> Unit,
    onDeleteRefill: (Long) -> Unit,
    onEditMiss: (MedicationMiss) -> Unit,
    onDeleteMiss: (Long) -> Unit,
    onOpenPrescription: (Long) -> Unit,
    onOpenBox: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onLogMiss(MedicationMiss.Kind.MISSED) }) {
                    Text(stringResource(R.string.refills_log_missed))
                }
                OutlinedButton(onClick = { onLogMiss(MedicationMiss.Kind.DELAYED) }) {
                    Text(stringResource(R.string.refills_log_delayed))
                }
            }
        }
        if (state.entries.isEmpty()) {
            item(key = "empty") {
                Text(text = stringResource(R.string.refills_empty), style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(state.entries, key = { it.key }) { entry ->
            when (entry) {
                is RefillLogEntry.RefillEntry -> RefillCard(
                    entry = entry,
                    onOpenPrescription = entry.refill.prescriptionId?.let { id -> { onOpenPrescription(id) } },
                    onOpenBox = entry.refill.boxId?.let { id -> { onOpenBox(id) } },
                    onEdit = { editingKey = entry.key },
                )
                is RefillLogEntry.MissEntry -> MissCard(entry.miss, onEdit = { editingKey = entry.key })
            }
        }
    }
    when (val entry = state.entries.find { it.key == editingKey }) {
        is RefillLogEntry.RefillEntry -> EditRefillDialog(
            refill = entry.refill,
            maxAmount = entry.maxAmount,
            onSave = { updated ->
                onEditRefill(updated)
                editingKey = null
            },
            onDelete = {
                onDeleteRefill(entry.refill.id)
                editingKey = null
            },
            onDismiss = { editingKey = null },
        )
        is RefillLogEntry.MissEntry -> EditMissDialog(
            miss = entry.miss,
            onSave = { updated ->
                onEditMiss(updated)
                editingKey = null
            },
            onDelete = {
                onDeleteMiss(entry.miss.id)
                editingKey = null
            },
            onDismiss = { editingKey = null },
        )
        null -> Unit
    }
}

@Composable
private fun RefillCard(
    entry: RefillLogEntry.RefillEntry,
    onOpenPrescription: (() -> Unit)?,
    onOpenBox: (() -> Unit)?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refill = entry.refill
    RecordCard(modifier = modifier, onEdit = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = refill.date.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (refill.isPartial) PartialBadge(modifier = Modifier.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.refills_pills, refill.amount, refill.amount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (onOpenBox != null) {
                    Text(
                        text = stringResource(R.string.refills_box, entry.boxName, refill.amountRemaining),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            LinkIconButton(
                iconRes = R.drawable.ic_prescription,
                descriptionRes = if (onOpenPrescription != null) {
                    R.string.link_open_prescription
                } else {
                    R.string.link_prescription_deleted
                },
                onClick = onOpenPrescription,
            )
            LinkIconButton(
                iconRes = R.drawable.ic_box,
                descriptionRes = if (onOpenBox != null) R.string.link_open_box else R.string.link_box_deleted,
                onClick = onOpenBox,
                modifier = Modifier.padding(start = 4.dp),
                label = entry.boxNumber?.let { stringResource(R.string.link_box_number, it) },
            )
        }
    }
}

@Composable
private fun PartialBadge(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.refills_partial),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MissCard(miss: MedicationMiss, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    RecordCard(modifier = modifier, onEdit = onEdit) {
        Text(text = miss.date.toString(), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(
                when (miss.kind) {
                    MedicationMiss.Kind.MISSED -> R.string.refills_missed
                    MedicationMiss.Kind.DELAYED -> R.string.refills_delayed
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RefillsPreviewContent(state: RefillsUiState) {
    MedicationLoggerTheme {
        Surface {
            RefillsScreen(
                state = state,
                onLogMiss = {},
                onEditRefill = {},
                onDeleteRefill = {},
                onEditMiss = {},
                onDeleteMiss = {},
                onOpenPrescription = {},
                onOpenBox = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RefillsScreenPreview() {
    val date = LocalDate.of(2026, 11, 2)
    RefillsPreviewContent(
        RefillsUiState(
            listOf(
                RefillLogEntry.MissEntry(MedicationMiss(1, date, MedicationMiss.Kind.DELAYED)),
                RefillLogEntry.RefillEntry(Refill(1, 1, 1, date, 4, 0, isPartial = true), "2026-10-01 #1", 1, 4),
            ),
        ),
    )
}

@PreviewLightDark
@Composable
private fun RefillsScreenEmptyPreview() {
    RefillsPreviewContent(RefillsUiState())
}
