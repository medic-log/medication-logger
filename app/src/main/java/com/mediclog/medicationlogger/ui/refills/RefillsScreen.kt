package com.mediclog.medicationlogger.ui.refills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.mediclog.medicationlogger.ui.components.TutorialTarget
import com.mediclog.medicationlogger.ui.components.tutorialTarget
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
            Column(
                modifier = Modifier.fillMaxWidth().tutorialTarget(TutorialTarget.DOSE_LOG),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    OutlinedButton(onClick = { onLogMiss(MedicationMiss.Kind.MISSED) }) {
                        Text(stringResource(R.string.refills_log_missed))
                    }
                    OutlinedButton(onClick = { onLogMiss(MedicationMiss.Kind.DELAYED) }) {
                        Text(stringResource(R.string.refills_log_delayed))
                    }
                }
                val message = pluralStringResource(R.plurals.refills_lot_pills, state.lotPills, state.lotPills)
                Text(
                    text = buildAnnotatedString {
                        append(message)
                        val pills = state.lotPills.toString()
                        val start = message.indexOf(pills)
                        if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + pills.length)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (state.entries.isEmpty()) {
            item(key = "empty") {
                Text(text = stringResource(R.string.refills_empty), style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(state.entries, key = { it.key }) { entry ->
            val target = if (entry == state.entries.first()) {
                Modifier.tutorialTarget(TutorialTarget.FIRST_REFILL)
            } else {
                Modifier
            }
            when (entry) {
                is RefillLogEntry.RefillEntry -> RefillCard(
                    entry = entry,
                    onOpenPrescription = onOpenPrescription,
                    onOpenBox = onOpenBox,
                    onEdit = { editingKey = entry.key },
                    modifier = target,
                )
                is RefillLogEntry.RefillGroupEntry -> RefillGroupCard(
                    group = entry,
                    onOpenPrescription = onOpenPrescription,
                    onOpenBox = onOpenBox,
                    onEdit = { editingKey = it },
                    modifier = target,
                )
                is RefillLogEntry.MissEntry -> MissCard(
                    miss = entry.miss,
                    onEdit = { editingKey = entry.key },
                    modifier = target,
                )
            }
        }
    }
    val allEntries = state.entries.flatMap { if (it is RefillLogEntry.RefillGroupEntry) it.members else listOf(it) }
    when (val entry = allEntries.find { it.key == editingKey }) {
        is RefillLogEntry.RefillEntry -> EditRefillDialog(
            refill = entry.refill,
            maxAmount = entry.maxAmount,
            leftRange = entry.leftRange,
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
        is RefillLogEntry.RefillGroupEntry, null -> Unit
    }
}

// Pasted together, sandwich style: only the outer corners of the stack are round
@Composable
private fun RefillGroupCard(
    group: RefillLogEntry.RefillGroupEntry,
    onOpenPrescription: (Long) -> Unit,
    onOpenBox: (Long) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cornerShape = MaterialTheme.shapes.medium
    Column(modifier = modifier.fillMaxWidth()) {
        val members = group.members
        members.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider()
            val shape = cornerShape.copy(
                topStart = if (index == 0) cornerShape.topStart else ZeroCornerSize,
                topEnd = if (index == 0) cornerShape.topEnd else ZeroCornerSize,
                bottomStart = if (index == members.lastIndex) cornerShape.bottomStart else ZeroCornerSize,
                bottomEnd = if (index == members.lastIndex) cornerShape.bottomEnd else ZeroCornerSize,
            )
            when (member) {
                is RefillLogEntry.RefillEntry -> RefillCard(
                    entry = member,
                    onOpenPrescription = onOpenPrescription,
                    onOpenBox = onOpenBox,
                    onEdit = { onEdit(member.key) },
                    shape = shape,
                )
                is RefillLogEntry.MissEntry -> MissCard(
                    miss = member.miss,
                    onEdit = { onEdit(member.key) },
                    shape = shape,
                    isCounted = true,
                )
                is RefillLogEntry.RefillGroupEntry -> Unit
            }
        }
    }
}

@Composable
private fun RefillCard(
    entry: RefillLogEntry.RefillEntry,
    onOpenPrescription: (Long) -> Unit,
    onOpenBox: (Long) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
) {
    val refill = entry.refill
    val openPrescription = refill.prescriptionId?.let { id -> { onOpenPrescription(id) } }
    val openBox = refill.boxId?.let { id -> { onOpenBox(id) } }
    RecordCard(modifier = modifier, onEdit = onEdit, shape = shape) {
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
                if (openBox != null) {
                    Text(
                        text = stringResource(R.string.refills_box, entry.boxName, refill.amountRemaining),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            LinkIconButton(
                iconRes = R.drawable.ic_prescription,
                descriptionRes = if (openPrescription != null) {
                    R.string.link_open_prescription
                } else {
                    R.string.link_prescription_deleted
                },
                onClick = openPrescription,
            )
            LinkIconButton(
                iconRes = R.drawable.ic_box,
                descriptionRes = if (openBox != null) R.string.link_open_box else R.string.link_box_deleted,
                onClick = openBox,
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
private fun MissCard(
    miss: MedicationMiss,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    isCounted: Boolean = false,
) {
    RecordCard(modifier = modifier, onEdit = onEdit, shape = shape) {
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
        if (isCounted) {
            Text(
                text = stringResource(R.string.refills_missed_pill),
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
                RefillLogEntry.RefillGroupEntry(
                    listOf(
                        RefillLogEntry.RefillEntry(Refill(2, 2, 1, date, 3, 27, true), "2026-11-02 #2", 2, 3, 0..30),
                        RefillLogEntry.RefillEntry(Refill(1, 1, 1, date, 4, 0, true), "2026-10-01 #1", 1, 4, 0..25),
                    ),
                    listOf(RefillLogEntry.MissEntry(MedicationMiss(2, date, MedicationMiss.Kind.MISSED))),
                ),
            ),
        ),
    )
}

@PreviewLightDark
@Composable
private fun RefillsScreenEmptyPreview() {
    RefillsPreviewContent(RefillsUiState())
}
