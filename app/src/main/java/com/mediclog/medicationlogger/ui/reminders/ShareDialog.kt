package com.mediclog.medicationlogger.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.components.QrCode
import com.mediclog.medicationlogger.ui.components.outlinedBorder
import com.mediclog.medicationlogger.ui.components.outlinedButtonColors
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

private val QR_SIZE = 200.dp

/** The download site's link as a QR code to scan, with a button to send it through another app. */
@Composable
fun ShareDialog(
    url: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_share_title)) },
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
}

@PreviewLightDark
@Composable
private fun ShareDialogPreview() {
    MedicationLoggerTheme {
        ShareDialog(url = "https://example.com/", onShare = {}, onDismiss = {})
    }
}
