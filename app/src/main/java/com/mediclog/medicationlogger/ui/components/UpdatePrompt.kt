package com.mediclog.medicationlogger.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.BuildConfig
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.LatestRelease
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import com.mediclog.medicationlogger.update.ApkInstaller
import kotlinx.coroutines.launch

/**
 * Offers [release], a newer version, and downloads it if the user chooses to update; [onDismiss] runs either way,
 * when the offer closes. The download keeps going after that while this stays on screen.
 */
@Composable
fun UpdatePrompt(release: LatestRelease?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The version being downloaded, while the update is downloading
    var downloadingVersion by remember { mutableStateOf<String?>(null) }
    downloadingVersion?.let { UpdateDownloadDialog(it) }
    release?.let {
        UpdateAvailableDialog(
            versionName = it.versionName,
            onUpdate = {
                onDismiss()
                scope.launch {
                    downloadingVersion = it.versionName
                    val isStarted = try {
                        ApkInstaller.install(context, it)
                    } finally {
                        downloadingVersion = null
                    }
                    if (!isStarted) {
                        Toast.makeText(context, R.string.settings_update_download_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    versionName: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_update_confirm_title)) },
        text = { Text(stringResource(R.string.settings_update_confirm_text, versionName, BuildConfig.VERSION_NAME)) },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.settings_update)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_update_later)) } },
    )
}

/** Shown while an update downloads, before the system asks to install it; it can't be dismissed. */
@Composable
private fun UpdateDownloadDialog(versionName: String, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_update_downloading, versionName)) },
        text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
    )
}

@PreviewLightDark
@Composable
private fun UpdateAvailableDialogPreview() {
    MedicationLoggerTheme {
        UpdateAvailableDialog(versionName = "1.2.0", onUpdate = {}, onDismiss = {})
    }
}
