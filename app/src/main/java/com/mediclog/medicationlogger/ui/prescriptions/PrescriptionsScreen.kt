package com.mediclog.medicationlogger.ui.prescriptions

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Prescription
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.components.CardButton
import com.mediclog.medicationlogger.ui.components.FilterSelector
import com.mediclog.medicationlogger.ui.components.LinkIconButton
import com.mediclog.medicationlogger.ui.components.ListFilter
import com.mediclog.medicationlogger.ui.components.RecordCard
import com.mediclog.medicationlogger.ui.components.StatusBadge
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import com.mediclog.medicationlogger.ui.theme.StatusTone
import java.time.LocalDate

/** The prescriptions tab with its ViewModel. */
@Composable
fun PrescriptionsRoute(
    focusedPrescriptionId: Long?,
    onOpenBox: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrescriptionsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(focusedPrescriptionId) {
        if (focusedPrescriptionId != null) viewModel.setFilter(ListFilter.ALL)
    }
    PrescriptionsScreen(
        state = state,
        focusedPrescriptionId = focusedPrescriptionId,
        onFilterChange = viewModel::setFilter,
        onAddPrescription = viewModel::addPrescription,
        onOrderRepetition = viewModel::orderRepetition,
        onBuyRepetition = viewModel::buyRepetition,
        onEditPrescription = viewModel::editPrescription,
        onDeletePrescription = viewModel::deletePrescription,
        onOpenBox = onOpenBox,
        modifier = modifier,
    )
}

/** The list of prescriptions, with the button for a new one. */
@Composable
fun PrescriptionsScreen(
    state: PrescriptionsUiState,
    focusedPrescriptionId: Long?,
    onFilterChange: (ListFilter) -> Unit,
    onAddPrescription: (PrescriptionDraft) -> Unit,
    onOrderRepetition: (Long) -> Unit,
    onBuyRepetition: (Long) -> Unit,
    onEditPrescription: (Prescription) -> Unit,
    onDeletePrescription: (Long) -> Unit,
    onOpenBox: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val focusedIndex = state.items.indexOfFirst { it.prescription.id == focusedPrescriptionId }
    LaunchedEffect(focusedPrescriptionId, focusedIndex) {
        // The first list item is the header
        if (focusedIndex >= 0) listState.animateScrollToItem(focusedIndex + 1)
    }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Room at the bottom so the floating button doesn't cover the last card
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                FilterSelector(
                    filter = state.filter,
                    options = listOf(ListFilter.ACTIVE, ListFilter.ALL),
                    onFilterChange = onFilterChange,
                )
            }
            if (state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(
                            if (state.filter == ListFilter.ACTIVE) {
                                R.string.prescriptions_empty_active
                            } else {
                                R.string.prescriptions_empty_all
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(state.items, key = { it.prescription.id }) { item ->
                PrescriptionCard(
                    item = item,
                    isHighlighted = item.prescription.id == focusedPrescriptionId,
                    onOrderRepetition = { onOrderRepetition(item.prescription.id) },
                    onBuyRepetition = { onBuyRepetition(item.prescription.id) },
                    onEdit = { editingId = item.prescription.id },
                    onOpenBox = onOpenBox,
                )
            }
        }
        val newPrescription = stringResource(R.string.prescriptions_new)
        ExtendedFloatingActionButton(
            text = { Text(newPrescription) },
            icon = { Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null) },
            onClick = { isDialogOpen = true },
            // The button's text doesn't reach its merged semantics, so it's spelled out for accessibility services
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .semantics { contentDescription = newPrescription },
        )
    }
    if (isDialogOpen) {
        NewPrescriptionDialog(
            previous = state.previousPrescription,
            onSave = { draft ->
                onAddPrescription(draft)
                isDialogOpen = false
            },
            onDismiss = { isDialogOpen = false },
        )
    }
    state.items.find { it.prescription.id == editingId }?.let { item ->
        EditPrescriptionDialog(
            prescription = item.prescription,
            onSave = { updated ->
                onEditPrescription(updated)
                editingId = null
            },
            onDelete = {
                onDeletePrescription(item.prescription.id)
                editingId = null
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun PrescriptionCard(
    item: PrescriptionItem,
    isHighlighted: Boolean,
    onOrderRepetition: () -> Unit,
    onBuyRepetition: () -> Unit,
    onEdit: () -> Unit,
    onOpenBox: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prescription = item.prescription
    val statusColors = prescription.status.tone().colors
    RecordCard(modifier = modifier, isHighlighted = isHighlighted, statusColors = statusColors, onEdit = onEdit) {
        Row {
            Text(
                text = stringResource(R.string.prescriptions_prescribed_date, prescription.prescribedDate.toString()),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(
                status = stringResource(prescription.status.labelRes()),
                amount = stringResource(
                    R.string.prescriptions_repetitions,
                    prescription.repetitionsBought,
                    prescription.repetitionsTotal,
                ),
                currentAmount = prescription.repetitionsBought,
                colors = statusColors,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Detail(stringResource(R.string.prescriptions_prescribed_by, prescription.prescribedBy))
        Detail(stringResource(R.string.pharmacy_label, prescription.pharmacy))
        Detail(stringResource(R.string.prescriptions_per_box, prescription.pillsPerBox))
        Detail(
            stringResource(
                R.string.prescriptions_per_repetition,
                prescription.boxesPerRepetition,
                prescription.pillsPerRepetition,
            ),
        )
        Detail(stringResource(R.string.prescriptions_total, prescription.boxesTotal, prescription.pillsTotal))
        prescription.orderedDate?.let {
            Detail(stringResource(R.string.prescriptions_ordered_date, it.toString()))
        }
        prescription.boughtFirstDate?.let {
            Detail(stringResource(R.string.prescriptions_bought_first, it.toString()))
        }
        prescription.boughtLastDate?.let {
            Detail(stringResource(R.string.prescriptions_bought_last, it.toString()))
        }
        if (item.boxes.isNotEmpty()) {
            Text(
                text = stringResource(R.string.prescriptions_linked_boxes),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.boxes.forEach { box ->
                    LinkIconButton(
                        iconRes = R.drawable.ic_box,
                        descriptionRes = R.string.link_open_box,
                        onClick = { onOpenBox(box.id) },
                        label = stringResource(R.string.link_box_number, box.boxNumber),
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardButton(onClick = onOrderRepetition, enabled = prescription.canOrder) {
                Text(stringResource(R.string.prescriptions_order))
            }
            CardButton(onClick = onBuyRepetition, enabled = prescription.canBuy, isFilled = true) {
                Text(stringResource(R.string.prescriptions_buy))
            }
        }
    }
}

@Composable
private fun Detail(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

@StringRes
private fun Prescription.Status.labelRes() = when (this) {
    Prescription.Status.NOT_STARTED -> R.string.prescriptions_status_not_started
    Prescription.Status.ORDERED -> R.string.prescriptions_status_ordered
    Prescription.Status.IN_PROGRESS -> R.string.prescriptions_status_in_progress
    Prescription.Status.FINISHED -> R.string.prescriptions_status_finished
}

private fun Prescription.Status.tone() = when (this) {
    Prescription.Status.NOT_STARTED -> StatusTone.NOT_STARTED
    Prescription.Status.ORDERED -> StatusTone.ORDERED
    Prescription.Status.IN_PROGRESS -> StatusTone.IN_PROGRESS
    Prescription.Status.FINISHED -> StatusTone.FINISHED
}

@Composable
private fun PrescriptionsPreviewContent(state: PrescriptionsUiState) {
    MedicationLoggerTheme {
        Surface {
            PrescriptionsScreen(
                state = state,
                focusedPrescriptionId = null,
                onFilterChange = {},
                onAddPrescription = {},
                onOrderRepetition = {},
                onBuyRepetition = {},
                onEditPrescription = {},
                onDeletePrescription = {},
                onOpenBox = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PrescriptionsScreenPreview() {
    val prescription = Prescription(
        id = 1,
        prescribedDate = LocalDate.of(2026, 11, 2),
        prescribedBy = "A doctor",
        pharmacy = "Corner pharmacy",
        pillsPerBox = 30,
        boxesPerRepetition = 3,
        repetitionsTotal = 2,
    )
    PrescriptionsPreviewContent(PrescriptionsUiState(items = listOf(PrescriptionItem(prescription, emptyList()))))
}

@PreviewLightDark
@Composable
private fun PrescriptionsScreenEmptyPreview() {
    PrescriptionsPreviewContent(PrescriptionsUiState())
}
