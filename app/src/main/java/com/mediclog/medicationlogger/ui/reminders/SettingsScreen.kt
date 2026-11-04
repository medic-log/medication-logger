package com.mediclog.medicationlogger.ui.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.AppLanguage
import com.mediclog.medicationlogger.BuildConfig
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.LatestRelease
import com.mediclog.medicationlogger.data.ReminderSettings
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.components.RecordCard
import com.mediclog.medicationlogger.ui.components.SquircleShape
import com.mediclog.medicationlogger.ui.components.TimeSelector
import com.mediclog.medicationlogger.ui.components.TutorialTarget
import com.mediclog.medicationlogger.ui.components.WeekdaySelector
import com.mediclog.medicationlogger.ui.components.displayName
import com.mediclog.medicationlogger.ui.components.outlinedBorder
import com.mediclog.medicationlogger.ui.components.tutorialTarget
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import com.mediclog.medicationlogger.update.ApkInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

private const val MIN_DAYS = 1
private const val MAX_DAYS = 365
private const val EXPORT_MIME_TYPE = "application/json"

/** The settings tab with its ViewModel, and the notification permission reminders depend on. */
@Composable
fun RemindersRoute(
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareText = stringResource(R.string.settings_share_text, BuildConfig.SITE_URL)
    var areNotificationsAllowed by remember { mutableStateOf(true) }
    val checkNotifications = {
        areNotificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    // Also catches the user changing the setting in system settings and coming back
    LifecycleResumeEffect(Unit) {
        checkNotifications()
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checkNotifications()
    }
    val scope = rememberCoroutineScope()
    val showMessage = { message: Int -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    val exportContract = ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)
    val exportLauncher = rememberLauncherForActivityResult(exportContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = viewModel.exportRecords()
        scope.launch {
            val isWritten = withContext(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                    stream?.use { it.write(text.encodeToByteArray()) } != null
                } catch (_: IOException) {
                    false
                }
            }
            showMessage(if (isWritten) R.string.settings_export_done else R.string.settings_export_failed)
        }
    }
    // The picked file, waiting for the second confirmation before it replaces the records
    var importUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri
    }
    val importFile = { uri: Uri ->
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } catch (_: IOException) {
                    null
                }
            }
            val isImported = text != null && viewModel.importRecords(text)
            showMessage(if (isImported) R.string.settings_import_done else R.string.settings_import_failed)
        }
    }
    var isImportConfirmShown by rememberSaveable { mutableStateOf(false) }
    var isLanguageShown by rememberSaveable { mutableStateOf(false) }
    var isFeedbackShown by rememberSaveable { mutableStateOf(false) }
    if (isFeedbackShown) FeedbackDialog(onDismiss = { isFeedbackShown = false })
    var isShareShown by rememberSaveable { mutableStateOf(false) }
    if (isShareShown) {
        ShareDialog(
            url = BuildConfig.SITE_URL,
            onShare = {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, shareText),
                        null,
                    ),
                )
            },
            onDismiss = { isShareShown = false },
        )
    }
    if (isLanguageShown) {
        val activity = LocalActivity.current
        LanguageDialog(
            selected = AppLanguage.current(context),
            onConfirm = { language ->
                isLanguageShown = false
                if (language != AppLanguage.current(context)) activity?.let { AppLanguage.choose(it, language) }
            },
            onDismiss = { isLanguageShown = false },
        )
    }
    // The version being downloaded, while the update is downloading
    var downloadingVersion by remember { mutableStateOf<String?>(null) }
    downloadingVersion?.let { UpdateDownloadDialog(it) }
    // A newer version found, waiting for the user to confirm downloading it
    var availableUpdate by remember { mutableStateOf<LatestRelease?>(null) }
    availableUpdate?.let { release ->
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = { Text(stringResource(R.string.settings_update_confirm_title, release.versionName)) },
            text = { Text(stringResource(R.string.settings_update_confirm_text, BuildConfig.VERSION_NAME)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        availableUpdate = null
                        scope.launch {
                            downloadingVersion = release.versionName
                            val isStarted = try {
                                ApkInstaller.install(context, release)
                            } finally {
                                downloadingVersion = null
                            }
                            if (!isStarted) showMessage(R.string.settings_update_download_failed)
                        }
                    },
                ) { Text(stringResource(R.string.settings_update)) }
            },
            dismissButton = {
                TextButton(onClick = { availableUpdate = null }) { Text(stringResource(R.string.edit_cancel)) }
            },
        )
    }
    if (isImportConfirmShown) {
        AlertDialog(
            onDismissRequest = { isImportConfirmShown = false },
            title = { Text(stringResource(R.string.settings_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_import_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isImportConfirmShown = false
                        // Some file managers don't know .json as application/json, so any file can be picked
                        importLauncher.launch(arrayOf(EXPORT_MIME_TYPE, "text/plain", "application/octet-stream"))
                    },
                ) { Text(stringResource(R.string.settings_import_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { isImportConfirmShown = false }) { Text(stringResource(R.string.edit_cancel)) }
            },
        )
    }
    importUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text(stringResource(R.string.settings_import_replace_title)) },
            text = { Text(stringResource(R.string.settings_import_replace_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        importUri = null
                        importFile(uri)
                    },
                ) { Text(stringResource(R.string.settings_import_replace)) }
            },
            dismissButton = {
                TextButton(onClick = { importUri = null }) { Text(stringResource(R.string.edit_cancel)) }
            },
        )
    }
    SettingsScreen(
        state = state,
        areNotificationsAllowed = areNotificationsAllowed,
        onSettingsChange = { settings ->
            val isTurningOn = settings.hasAnyEnabled && !state.settings.hasAnyEnabled
            viewModel.updateSettings(settings)
            if (isTurningOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        onSettingsAction = {
            when (it) {
                SettingsAction.HELP -> onHelp()
                SettingsAction.UPDATE -> scope.launch {
                    val latest = withContext(Dispatchers.IO) { LatestRelease.fetch(BuildConfig.SITE_URL) }
                    when {
                        latest == null -> showMessage(R.string.settings_update_failed)
                        latest.versionCode <= BuildConfig.VERSION_CODE -> showMessage(R.string.settings_update_latest)
                        else -> availableUpdate = latest
                    }
                }
                SettingsAction.LANGUAGE -> isLanguageShown = true
                SettingsAction.FEEDBACK -> isFeedbackShown = true
                SettingsAction.EXPORT -> exportLauncher.launch("medication-logger-${LocalDate.now()}.json")
                SettingsAction.IMPORT -> isImportConfirmShown = true
                SettingsAction.SHARE -> isShareShown = true
            }
        },
        modifier = modifier,
    )
}

/** The settings tab: the configuration of the five reminders, then the app settings, none yet. */
@Composable
fun SettingsScreen(
    state: RemindersUiState,
    areNotificationsAllowed: Boolean,
    onSettingsChange: (ReminderSettings) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (settings.hasAnyEnabled && !areNotificationsAllowed) {
            NotificationsOffNotice(onOpenNotificationSettings)
        }
        SectionTitle(stringResource(R.string.settings_section_reminders))
        ReminderCard(
            title = stringResource(R.string.reminders_pill_title),
            summary = stringResource(R.string.reminders_pill_summary),
            isEnabled = settings.isPillEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(isPillEnabled = it)) },
            modifier = Modifier.tutorialTarget(TutorialTarget.PILL_REMINDER),
        ) {
            TimeSelector(
                time = settings.pillTime,
                dialogTitle = stringResource(R.string.reminders_pill_time_dialog_title),
                onTimeChange = { onSettingsChange(settings.copy(pillTime = it)) },
            )
            if (settings.isPillEnabled) {
                Text(
                    text = stringResource(
                        R.string.reminders_pill_next,
                        state.nextPill.toLocalDate().toString(),
                        state.nextPill.toLocalTime().toString(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        ReminderCard(
            title = stringResource(R.string.reminders_refill_title),
            summary = stringResource(R.string.reminders_refill_summary),
            isEnabled = settings.isRefillEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(isRefillEnabled = it)) },
            modifier = Modifier.tutorialTarget(TutorialTarget.REFILL_REMINDER),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeekdaySelector(settings.refillDay, onDayChange = { onSettingsChange(settings.copy(refillDay = it)) })
                TimeSelector(
                    time = settings.refillTime,
                    dialogTitle = stringResource(R.string.reminders_time_dialog_title),
                    onTimeChange = { onSettingsChange(settings.copy(refillTime = it)) },
                )
            }
            if (settings.isRefillEnabled) {
                Text(
                    text = stringResource(
                        R.string.reminders_refill_next,
                        state.nextRefill.dayOfWeek.displayName(),
                        state.nextRefill.toLocalDate().toString(),
                        state.nextRefill.toLocalTime().toString(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        ReminderCard(
            title = stringResource(R.string.reminders_run_out_title),
            summary = stringResource(R.string.reminders_run_out_summary),
            isEnabled = settings.isRunOutEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(isRunOutEnabled = it)) },
        ) {
            val title = stringResource(R.string.reminders_run_out_title)
            DaysStepper(
                reminderName = title,
                days = settings.runOutDaysBefore,
                onDaysChange = { onSettingsChange(settings.copy(runOutDaysBefore = it)) },
            )
            Text(
                text = state.runOutDate?.let { stringResource(R.string.reminders_run_out_date, it.toString()) }
                    ?: stringResource(R.string.reminders_run_out_none),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.runOutDate != null) {
                Text(
                    text = pluralStringResource(
                        R.plurals.reminders_pills_remaining,
                        state.pillsRemaining,
                        state.pillsRemaining,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        ReminderCard(
            title = stringResource(R.string.reminders_pickup_title),
            summary = stringResource(R.string.reminders_pickup_summary),
            isEnabled = settings.isPickupEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(isPickupEnabled = it)) },
        ) {
            val title = stringResource(R.string.reminders_pickup_title)
            DaysStepper(
                reminderName = title,
                days = settings.pickupDaysAfterOrder,
                onDaysChange = { onSettingsChange(settings.copy(pickupDaysAfterOrder = it)) },
            )
            Text(
                text = state.pickupDate?.let { stringResource(R.string.reminders_pickup_next, it.toString()) }
                    ?: stringResource(R.string.reminders_pickup_none),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ReminderCard(
            title = stringResource(R.string.reminders_expiry_title),
            summary = stringResource(R.string.reminders_expiry_summary),
            isEnabled = settings.isExpiryEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(isExpiryEnabled = it)) },
        ) {
            DaysStepper(
                reminderName = stringResource(R.string.reminders_expiry_title),
                days = settings.expiryDaysBefore,
                onDaysChange = { onSettingsChange(settings.copy(expiryDaysBefore = it)) },
            )
            Text(
                text = state.expiringBox?.let {
                    stringResource(R.string.reminders_expiry_next, it.name, it.expiryDate.toString())
                } ?: stringResource(R.string.reminders_expiry_none),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SectionTitle(stringResource(R.string.settings_section_settings))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsAction.rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    row.forEach { action ->
                        SettingsActionButton(action = action, onClick = { onSettingsAction(action) })
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
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

@Composable
private fun SettingsActionButton(action: SettingsAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            // The label names the button, so the icon is decorative
            Icon(painter = painterResource(action.iconRes), contentDescription = null)
            Text(
                text = stringResource(action.labelRes),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun NotificationsOffNotice(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.reminders_notifications_off),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenSettings, border = outlinedBorder()) {
                Text(stringResource(R.string.reminders_open_settings))
            }
        }
    }
}

@Composable
private fun ReminderCard(
    title: String,
    summary: String,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    RecordCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = summary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
        }
        content()
    }
}

@Composable
private fun DaysStepper(
    reminderName: String,
    days: Int,
    onDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            iconRes = R.drawable.ic_remove,
            description = stringResource(R.string.reminders_fewer_days, reminderName),
            isEnabled = days > MIN_DAYS,
            onClick = { onDaysChange(days - 1) },
        )
        Text(
            text = pluralStringResource(R.plurals.reminders_days, days, days),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        StepperButton(
            iconRes = R.drawable.ic_add,
            description = stringResource(R.string.reminders_more_days, reminderName),
            isEnabled = days < MAX_DAYS,
            onClick = { onDaysChange(days + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    @DrawableRes iconRes: Int,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, enabled = isEnabled, modifier = modifier) {
        Icon(painter = painterResource(iconRes), contentDescription = description)
    }
}

@Composable
private fun SettingsPreviewContent(state: RemindersUiState, areNotificationsAllowed: Boolean = true) {
    MedicationLoggerTheme {
        Surface {
            SettingsScreen(
                state = state,
                areNotificationsAllowed = areNotificationsAllowed,
                onSettingsChange = {},
                onOpenNotificationSettings = {},
                onSettingsAction = {},
            )
        }
    }
}

private val previewState = RemindersUiState(
    settings = ReminderSettings(isRefillEnabled = true, isRunOutEnabled = true, isPillEnabled = true),
    nextRefill = LocalDateTime.of(2026, 11, 8, 9, 0),
    nextPill = LocalDateTime.of(2026, 11, 3, 8, 0),
    runOutDate = LocalDate.of(2027, 2, 1),
    pillsRemaining = 90,
)

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    SettingsPreviewContent(previewState)
}

@PreviewLightDark
@Composable
private fun SettingsScreenNotificationsOffPreview() {
    SettingsPreviewContent(previewState, areNotificationsAllowed = false)
}
