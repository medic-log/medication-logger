package com.mediclog.medicationlogger.ui.reminders

import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mediclog.medicationlogger.BuildConfig
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.Feedback
import com.mediclog.medicationlogger.ui.components.DialogField
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What a piece of feedback is about; the same list as the feedback page on the download site. [isAnswerExpected]
 * topics ask for an email to answer to.
 */
enum class FeedbackTopic(@param:StringRes val labelRes: Int, val isAnswerExpected: Boolean = false) {
    TRANSLATION(R.string.feedback_topic_translation),
    BUG(R.string.feedback_topic_bug),
    SUGGESTION(R.string.feedback_topic_suggestion),
    HELP(R.string.feedback_topic_help, isAnswerExpected = true),
    QUESTION(R.string.feedback_topic_question, isAnswerExpected = true),
    OTHER(R.string.feedback_topic_other),
}

/**
 * A feedback form with the app's version filled in, not editable. Send posts it to the feedback Worker, which opens
 * an issue, and closes the dialog once it's sent. The button by the title offers to open the same form on the
 * download site, with the version passed along.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var title by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf(FeedbackTopic.SUGGESTION) }
    var message by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Optional, so only a filled-in one can be wrong
    val isEmailInvalid = topic.isAnswerExpected && email.isNotBlank() && !Feedback.isValidEmail(email.trim())
    var isTopicMenuOpen by rememberSaveable { mutableStateOf(false) }
    var isOpenSiteConfirmShown by rememberSaveable { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_feedback), modifier = Modifier.weight(1f))
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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.feedback_description))
                DialogField(value = title, onValueChange = { title = it.take(Feedback.MAX_TITLE_LENGTH) }, labelRes = R.string.feedback_title)
                ExposedDropdownMenuBox(expanded = isTopicMenuOpen, onExpandedChange = { isTopicMenuOpen = it }) {
                    OutlinedTextField(
                        value = stringResource(topic.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.feedback_topic)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTopicMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = isTopicMenuOpen, onDismissRequest = { isTopicMenuOpen = false }) {
                        FeedbackTopic.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = {
                                    topic = option
                                    isTopicMenuOpen = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                if (topic.isAnswerExpected) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.take(Feedback.MAX_EMAIL_LENGTH) },
                        label = { Text(stringResource(R.string.feedback_email)) },
                        singleLine = true,
                        isError = isEmailInvalid,
                        supportingText = if (isEmailInvalid) {
                            { Text(stringResource(R.string.feedback_email_invalid)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = BuildConfig.VERSION_NAME,
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    label = { Text(stringResource(R.string.feedback_version)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(Feedback.MAX_MESSAGE_LENGTH) },
                    label = { Text(stringResource(R.string.feedback_message)) },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
                Text(
                    text = stringResource(R.string.feedback_sent_contents),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isSending = true
                    val feedback = Feedback(
                        title = title.trim(),
                        topic = topic.name.lowercase(),
                        version = BuildConfig.VERSION_NAME,
                        message = message.trim(),
                        email = if (topic.isAnswerExpected) email.trim() else "",
                    )
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { feedback.send(BuildConfig.FEEDBACK_URL) }
                        isSending = false
                        val text = when (result) {
                            Feedback.SendResult.SENT -> R.string.feedback_sent
                            Feedback.SendResult.OFFLINE -> R.string.feedback_send_offline
                            Feedback.SendResult.UNAVAILABLE -> R.string.feedback_send_unavailable
                            Feedback.SendResult.REJECTED -> R.string.feedback_send_rejected
                        }
                        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                        if (result == Feedback.SendResult.SENT) onDismiss()
                    }
                },
                enabled = title.isNotBlank() && message.isNotBlank() && !isEmailInvalid && !isSending,
            ) { Text(stringResource(R.string.feedback_send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_cancel)) }
        },
    )
    if (isOpenSiteConfirmShown) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { isOpenSiteConfirmShown = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.feedback_open_site_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isOpenSiteConfirmShown = false
                        uriHandler.openUri(BuildConfig.SITE_URL + "feedback/?version=" + Uri.encode(BuildConfig.VERSION_NAME))
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
private fun FeedbackDialogPreview() {
    MedicationLoggerTheme {
        FeedbackDialog(onDismiss = {})
    }
}
