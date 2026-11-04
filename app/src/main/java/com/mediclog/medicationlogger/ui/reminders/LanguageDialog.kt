package com.mediclog.medicationlogger.ui.reminders

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.mediclog.medicationlogger.AppLanguage
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.util.Locale

/** Picks the app's language, applied on Confirm; each is named in itself, to be found from any language. */
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
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val resources = remember(chosen, configuration) {
        val localized = Configuration(configuration).apply { setLocale(Locale.forLanguageTag(chosen.tag)) }
        context.createConfigurationContext(localized).resources
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(resources.getString(R.string.settings_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = language == chosen,
                                onClick = { chosen = language },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == chosen, onClick = null)
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen) }) { Text(resources.getString(R.string.settings_language_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(resources.getString(R.string.edit_cancel)) }
        },
    )
}

@PreviewLightDark
@Composable
private fun LanguageDialogPreview() {
    MedicationLoggerTheme {
        LanguageDialog(selected = AppLanguage.ENGLISH, onConfirm = {}, onDismiss = {})
    }
}
