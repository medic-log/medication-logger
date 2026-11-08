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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.Refill
import com.mediclog.medicationlogger.displayName
import com.mediclog.medicationlogger.displayText
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.components.CardButton
import com.mediclog.medicationlogger.ui.components.FilterSelector
import com.mediclog.medicationlogger.ui.components.LinkIconButton
import com.mediclog.medicationlogger.ui.components.ListFilter
import com.mediclog.medicationlogger.ui.components.RecordCard
import com.mediclog.medicationlogger.ui.components.StatusBadge
import com.mediclog.medicationlogger.ui.components.TutorialTarget
import com.mediclog.medicationlogger.ui.components.tutorialTarget
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import com.mediclog.medicationlogger.ui.theme.StatusTone
import java.time.LocalDate
import java.time.YearMonth

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
        onNotesChange = viewModel::setBoxNotes,
        onEditBox = viewModel::editBox,
        onDeleteBox = viewModel::deleteBox,
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
    onNotesChange: (Long, String) -> Unit,
    onEditBox: (Box) -> Unit,
    onDeleteBox: (Long, Boolean) -> Unit,
    onOpenPrescription: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
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
                FilterSelector(
                    filter = state.filter,
                    options = ListFilter.entries,
                    onFilterChange = onFilterChange,
                )
                if (state.pendingRefillAmount > 0) {
                    PendingRefillNotice(state.pendingRefillAmount)
                }
                if (state.items.isEmpty()) {
                    Text(
                        text = stringResource(
                            when (state.filter) {
                                ListFilter.ACTIVE -> R.string.boxes_empty_active
                                ListFilter.ALL -> R.string.boxes_empty_all
                                ListFilter.EXTRA -> R.string.boxes_empty_extra
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
                onNotesChange = { onNotesChange(item.box.id, it) },
                onEdit = { editingId = item.box.id },
                onOpenPrescription = item.box.prescriptionId?.let { id -> { onOpenPrescription(id) } },
                modifier = if (item == state.items.first()) {
                    Modifier.tutorialTarget(TutorialTarget.FIRST_BOX)
                } else {
                    Modifier
                },
            )
        }
    }
    state.items.find { it.box.id == editingId }?.let { item ->
        EditBoxDialog(
            box = item.box,
            hasRefills = item.refills.isNotEmpty(),
            onSave = { updated ->
                onEditBox(updated)
                editingId = null
            },
            onDelete = { keepRefills ->
                onDeleteBox(item.box.id, keepRefills)
                editingId = null
            },
            onDismiss = { editingId = null },
        )
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
    onNotesChange: (String) -> Unit,
    onEdit: () -> Unit,
    onOpenPrescription: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val box = item.box
    val statusColors = box.status.tone().colors
    RecordCard(modifier = modifier, isHighlighted = isHighlighted, statusColors = statusColors, onEdit = onEdit) {
        Row {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = box.displayName(), style = MaterialTheme.typography.titleMedium)
                box.openDate?.let {
                    Text(
                        text = stringResource(R.string.boxes_opened, it.displayText()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                box.expiryDate?.let {
                    Text(
                        text = stringResource(R.string.boxes_expires, it.displayText()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            StatusBadge(
                status = stringResource(box.status.labelRes()),
                amount = stringResource(R.string.boxes_pills_left, box.pillsLeft, box.pillsTotal),
                currentAmount = box.pillsLeft,
                colors = statusColors,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (box.isExtra) {
            BoxNotesField(notes = box.notes, onNotesChange = onNotesChange)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.refills.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.boxes_refills_title),
                        style = MaterialTheme.typography.labelLarge,
                        // Sets the title apart from the notes field above it
                        modifier = if (box.isExtra) Modifier.padding(top = 4.dp) else Modifier,
                    )
                    item.refills.forEach { refill ->
                        Text(refill.summary(), style = MaterialTheme.typography.bodySmall)
                    }
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
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The action the user most likely needs next gets the filled button
            val isPartialHighlighted = pendingRefillAmount > 0 && box.canRefill
            if (isPartialHighlighted) {
                CardButton(onClick = onRefillPartial, isFilled = true) {
                    Text(
                        pluralStringResource(
                            R.plurals.boxes_refill_partial_pending,
                            pendingRefillAmount,
                            pendingRefillAmount,
                        ),
                    )
                }
                CardButton(onClick = onRefillWeekly) {
                    Text(stringResource(R.string.boxes_refill_weekly))
                }
            } else if (box.canRefill) {
                CardButton(onClick = onRefillWeekly, isFilled = true) {
                    Text(stringResource(R.string.boxes_refill_weekly))
                }
            }
            if (box.isExtra) {
                CardButton(onClick = onBackToInventory) {
                    Text(stringResource(R.string.boxes_back_to_inventory))
                }
            } else {
                CardButton(onClick = onSaveAsExtra, enabled = box.canSaveAsExtra) {
                    Text(stringResource(R.string.boxes_save_as_extra))
                }
            }
        }
    }
}

/** Notes typed on the card, saved when the field loses focus or the keyboard's done key is pressed. */
@Composable
private fun BoxNotesField(notes: String, onNotesChange: (String) -> Unit, modifier: Modifier = Modifier) {
    // Keyed by the saved notes, so that changes from elsewhere, such as an import, replace what's shown
    var text by rememberSaveable(notes) { mutableStateOf(notes) }
    val focusManager = LocalFocusManager.current
    val contentColor = LocalContentColor.current
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.boxes_notes_label)) },
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        // The card takes the box's status colors, so the field follows them
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            focusedBorderColor = contentColor,
            unfocusedBorderColor = contentColor,
            focusedLabelColor = contentColor,
            unfocusedLabelColor = contentColor,
            cursorColor = contentColor,
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused && text.trim() != notes) onNotesChange(text) },
    )
}

@Composable
private fun Refill.summary(): String {
    val amounts = pluralStringResource(R.plurals.refills_amount, amount, amount, amountRemaining)
    val line = "${date.displayText()}: $amounts"
    return if (isPartial) stringResource(R.string.refills_partial_suffix, line) else line
}

@StringRes
private fun Box.Status.labelRes() = when (this) {
    Box.Status.NOT_STARTED -> R.string.boxes_status_not_started
    Box.Status.IN_PROGRESS -> R.string.boxes_status_in_progress
    Box.Status.FINISHED -> R.string.boxes_status_finished
    Box.Status.EXTRA -> R.string.boxes_status_extra
}

private fun Box.Status.tone() = when (this) {
    Box.Status.NOT_STARTED -> StatusTone.NOT_STARTED
    Box.Status.IN_PROGRESS -> StatusTone.IN_PROGRESS
    Box.Status.FINISHED -> StatusTone.FINISHED
    Box.Status.EXTRA -> StatusTone.EXTRA
}

private val previewBox = Box(
    id = 1,
    prescriptionId = 1,
    boxNumber = 1,
    boughtDate = LocalDate.of(2026, 11, 2),
    pillsLeft = 23,
    pillsTotal = 30,
    openDate = LocalDate.of(2026, 11, 3),
    expiryDate = YearMonth.of(2028, 6),
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
                onNotesChange = { _, _ -> },
                onEditBox = {},
                onDeleteBox = { _, _ -> },
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

@PreviewLightDark
@Composable
private fun BoxesScreenExtraPreview() {
    BoxesPreviewContent(
        BoxesUiState(items = listOf(
            BoxItem(previewBox.copy(isExtra = true), emptyList())
        )))
}
