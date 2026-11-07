package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.R

/**
 * Edits a record's [fields], with a delete button that asks for confirmation, saying [deleteMessage].
 * With [keepChildrenLabel], the confirmation also offers deleting only the record.
 */
@Composable
fun RecordEditDialog(
    title: String,
    canSave: Boolean,
    deleteMessage: String,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    keepChildrenLabel: String? = null,
    onDeleteKeepingChildren: () -> Unit = {},
    fields: @Composable ColumnScope.() -> Unit,
) {
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }
    val deleteColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
    if (isConfirmingDelete) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = { isConfirmingDelete = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.edit_delete_title)) },
            text = { Text(deleteMessage) },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.End) {
                    if (keepChildrenLabel != null) {
                        TextButton(onClick = onDeleteKeepingChildren) {
                            Text(keepChildrenLabel)
                        }
                    }
                    TextButton(onClick = onDelete, colors = deleteColors) {
                        Text(
                            stringResource(
                                if (keepChildrenLabel != null) R.string.edit_delete_all else R.string.edit_delete,
                            ),
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingDelete = false }) {
                    Text(stringResource(R.string.edit_cancel))
                }
            },
        )
    } else {
        FormDialog(
            title = { Text(title) },
            modifier = modifier,
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onSave, enabled = canSave) {
                    Text(stringResource(R.string.edit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.edit_cancel))
                }
            },
        ) {
            fields()
            TextButton(onClick = { isConfirmingDelete = true }, colors = deleteColors) {
                Text(stringResource(R.string.edit_delete))
            }
        }
    }
}
