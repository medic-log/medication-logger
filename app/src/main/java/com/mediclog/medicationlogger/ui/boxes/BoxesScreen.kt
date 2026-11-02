package com.mediclog.medicationlogger.ui.boxes

import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.Refill
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.components.FilterSelector
import com.mediclog.medicationlogger.ui.components.LinkIconButton
import com.mediclog.medicationlogger.ui.components.ListFilter
import com.mediclog.medicationlogger.ui.components.RecordCard
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.LocalDate

/** The boxes tab with its ViewModel. */
@Composable
fun BoxesRoute(
    focusedBoxId: Long?,
    onOpenPrescription: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoxesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(focusedBoxId) {
        if (focusedBoxId != null) viewModel.setFilter(ListFilter.ALL)
    }
    BoxesScreen(
        state = state,
        focusedBoxId = focusedBoxId,
        onFilterChange = viewModel::setFilter,
        onRefillWeekly = viewModel::refillWeekly,
        onRefillPartial = viewModel::refillPartial,
        onSaveAsExtra = viewModel::saveAsExtra,
        onBackToInventory = viewModel::backToInventory,
        onOpenPrescription = onOpenPrescription,
        modifier = modifier,
    )
}

/** The list of boxes with their refill actions. */
@Composable
fun BoxesScreen(
    state: BoxesUiState,
    focusedBoxId: Long?,
    onFilterChange: (ListFilter) -> Unit,
    onRefillWeekly: (Long) -> Unit,
    onRefillPartial: (Long) -> Unit,
    onSaveAsExtra: (Long) -> Unit,
    onBackToInventory: (Long) -> Unit,
    onOpenPrescription: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val focusedIndex = state.items.indexOfFirst { it.box.id == focusedBoxId }
    LaunchedEffect(focusedBoxId, focusedIndex) {
        // The first list item is the header
        if (focusedIndex >= 0) listState.animateScrollToItem(focusedIndex + 1)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterSelector(filter = state.filter, onFilterChange = onFilterChange)
                if (state.pendingRefillAmount > 0) {
                    PendingRefillNotice(state.pendingRefillAmount)
                }
                if (state.items.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (state.filter == ListFilter.ACTIVE) {
                                R.string.boxes_empty_active
                            } else {
                                R.string.boxes_empty_all
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        items(state.items, key = { it.box.id }) { item ->
            BoxCard(
                item = item,
                pendingRefillAmount = state.pendingRefillAmount,
                isHighlighted = item.box.id == focusedBoxId,
                onRefillWeekly = { onRefillWeekly(item.box.id) },
                onRefillPartial = { onRefillPartial(item.box.id) },
                onSaveAsExtra = { onSaveAsExtra(item.box.id) },
                onBackToInventory = { onBackToInventory(item.box.id) },
                onOpenPrescription = { onOpenPrescription(item.box.prescriptionId) },
            )
        }
    }
}

@Composable
private fun PendingRefillNotice(pendingRefillAmount: Int, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = pluralStringResource(R.plurals.boxes_pending_notice, pendingRefillAmount, pendingRefillAmount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun BoxCard(
    item: BoxItem,
    pendingRefillAmount: Int,
    isHighlighted: Boolean,
    onRefillWeekly: () -> Unit,
    onRefillPartial: () -> Unit,
    onSaveAsExtra: () -> Unit,
    onBackToInventory: () -> Unit,
    onOpenPrescription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val box = item.box
    RecordCard(modifier = modifier, isHighlighted = isHighlighted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = box.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            LinkIconButton(
                iconRes = R.drawable.ic_prescription,
                descriptionRes = R.string.link_open_prescription,
                onClick = onOpenPrescription,
            )
        }
        Text(
            text = stringResource(R.string.status_label, stringResource(box.status.labelRes())),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.boxes_pills_left, box.pillsLeft, box.pillsTotal),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.pharmacy_label, box.pharmacy),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (item.refills.isNotEmpty()) {
            Text(text = stringResource(R.string.boxes_refills_title), style = MaterialTheme.typography.labelLarge)
            item.refills.forEach { refill -> Text(refill.summary(), style = MaterialTheme.typography.bodySmall) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The action the user most likely needs next gets the filled button
            val isPartialHighlighted = pendingRefillAmount > 0 && box.canRefill
            if (isPartialHighlighted) {
                Button(onClick = onRefillPartial) {
                    Text(
                        pluralStringResource(
                            R.plurals.boxes_refill_partial_pending,
                            pendingRefillAmount,
                            pendingRefillAmount,
                        ),
                    )
                }
                OutlinedButton(onClick = onRefillWeekly) {
                    Text(stringResource(R.string.boxes_refill_weekly))
                }
            } else {
                Button(onClick = onRefillWeekly, enabled = box.canRefill) {
                    Text(stringResource(R.string.boxes_refill_weekly))
                }
                OutlinedButton(onClick = onRefillPartial, enabled = false) {
                    Text(stringResource(R.string.boxes_refill_partial))
                }
            }
            if (box.isExtra) {
                TextButton(onClick = onBackToInventory) {
                    Text(stringResource(R.string.boxes_back_to_inventory))
                }
            } else {
                TextButton(onClick = onSaveAsExtra, enabled = box.canSaveAsExtra) {
                    Text(stringResource(R.string.boxes_save_as_extra))
                }
            }
        }
    }
}

@Composable
private fun Refill.summary(): String {
    val amounts = pluralStringResource(R.plurals.refills_amount, amount, amount, amountRemaining)
    val line = "$date: $amounts"
    return if (isPartial) stringResource(R.string.refills_partial_suffix, line) else line
}

@StringRes
private fun Box.Status.labelRes() = when (this) {
    Box.Status.NOT_STARTED -> R.string.boxes_status_not_started
    Box.Status.IN_PROGRESS -> R.string.boxes_status_in_progress
    Box.Status.FINISHED -> R.string.boxes_status_finished
    Box.Status.EXTRA -> R.string.boxes_status_extra
}

private val previewBox = Box(
    id = 1,
    prescriptionId = 1,
    boxNumber = 1,
    pharmacy = "Corner pharmacy",
    boughtDate = LocalDate.of(2026, 11, 2),
    pillsLeft = 23,
    pillsTotal = 30,
)

@Composable
private fun BoxesPreviewContent(state: BoxesUiState) {
    MedicationLoggerTheme {
        Surface {
            BoxesScreen(
                state = state,
                focusedBoxId = null,
                onFilterChange = {},
                onRefillWeekly = {},
                onRefillPartial = {},
                onSaveAsExtra = {},
                onBackToInventory = {},
                onOpenPrescription = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BoxesScreenPreview() {
    BoxesPreviewContent(BoxesUiState(items = listOf(BoxItem(previewBox, emptyList()))))
}

@PreviewLightDark
@Composable
private fun BoxesScreenPendingPreview() {
    BoxesPreviewContent(BoxesUiState(items = listOf(BoxItem(previewBox, emptyList())), pendingRefillAmount = 3))
}

@PreviewLightDark
@Composable
private fun BoxesScreenEmptyPreview() {
    BoxesPreviewContent(BoxesUiState())
}
