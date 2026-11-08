package com.mediclog.medicationlogger.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.AppLanguage
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.components.QrCode
import com.mediclog.medicationlogger.ui.components.outlinedBorder
import com.mediclog.medicationlogger.ui.components.outlinedButtonColors
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

private val QR_SIZE = 200.dp

/**
 * The download site's link as a QR code to scan, with a button to send it through another app and one
 * to open the site, after asking.
 */
@Composable
fun ShareDialog(
    url: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpenSiteConfirmShown by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_share_title), modifier = Modifier.weight(1f))
                IconButton(onClick = { isOpenSiteConfirmShown = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = stringResource(R.string.feedback_open_site),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QrCode(
                    content = url,
                    contentDescription = stringResource(R.string.settings_share_qr_description),
                    modifier = Modifier.size(QR_SIZE),
                )
                OutlinedButton(onClick = onShare, border = outlinedBorder(), colors = outlinedButtonColors()) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_share))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_share_close)) }
        },
    )
    if (isOpenSiteConfirmShown) {
        val uriHandler = LocalUriHandler.current
        // The site would otherwise follow the browser's language, which the app's needn't be
        val language = AppLanguage.current(LocalContext.current).tag
        AlertDialog(
            onDismissRequest = { isOpenSiteConfirmShown = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.settings_share_open_site_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isOpenSiteConfirmShown = false
                        uriHandler.openUri("$url?lang=$language")
                    },
                ) { Text(stringResource(R.string.feedback_open)) }
            },
            dismissButton = {
                TextButton(onClick = { isOpenSiteConfirmShown = false }) { Text(stringResource(R.string.edit_cancel)) }
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun ShareDialogPreview() {
    MedicationLoggerTheme {
        ShareDialog(url = "https://example.com/", onShare = {}, onDismiss = {})
    }
}
