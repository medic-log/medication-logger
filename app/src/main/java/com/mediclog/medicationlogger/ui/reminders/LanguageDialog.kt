package com.mediclog.medicationlogger.ui.reminders

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
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.AppLanguage
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.components.InLanguage
import com.mediclog.medicationlogger.ui.components.LanguageOptions
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** Picks the app's language, applied on Confirm. */
@Composable
fun LanguageDialog(
    selected: AppLanguage,
    onConfirm: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Switching redraws the app, so a tap only marks the language until it's confirmed
    var chosen by rememberSaveable { mutableStateOf(selected) }
    // The dialog's own words preview the marked language, so they can be read before confirming
    InLanguage(chosen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnClickOutside = false),
            modifier = modifier,
            title = { Text(stringResource(R.string.settings_language)) },
            text = { LanguageOptions(selected = chosen, onSelect = { chosen = it }) },
            confirmButton = {
                TextButton(onClick = { onConfirm(chosen) }) { Text(stringResource(R.string.settings_language_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_cancel)) }
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun LanguageDialogPreview() {
    MedicationLoggerTheme {
        LanguageDialog(selected = AppLanguage.ENGLISH, onConfirm = {}, onDismiss = {})
    }
}
