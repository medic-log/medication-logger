package com.mediclog.medicationlogger.ui.reminders

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.ReminderSettings
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.components.RecordCard
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val MIN_DAYS = 1
private const val MAX_DAYS = 365

/** The reminders tab with its ViewModel, and the notification permission it depends on. */
@Composable
fun RemindersRoute(
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
    RemindersScreen(
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
        modifier = modifier,
    )
}

/** The configuration of the four reminders. */
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    areNotificationsAllowed: Boolean,
    onSettingsChange: (ReminderSettings) -> Unit,
    onOpenNotificationSettings: () -> Unit,
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
        ReminderCard(
            title = stringResource(R.string.reminders_pill_title),
            summary = stringResource(R.string.reminders_pill_summary),
            isEnabled = settings.isPillEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(isPillEnabled = it)) },
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
    }
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
            OutlinedButton(onClick = onOpenSettings) {
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
private fun WeekdaySelector(day: DayOfWeek, onDayChange: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { isExpanded = true }) {
            Text(day.displayName())
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DayOfWeek.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName()) },
                    onClick = {
                        onDayChange(option)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // TimeInput has no stable alternative in Material 3
@Composable
private fun TimeSelector(
    time: LocalTime,
    dialogTitle: String,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { isDialogOpen = true }, modifier = modifier) {
        Text(stringResource(R.string.reminders_choose_time, time.toString()))
    }
    if (isDialogOpen) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text(dialogTitle) },
            text = { TimeInput(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(pickerState.hour, pickerState.minute))
                        isDialogOpen = false
                    },
                ) {
                    Text(stringResource(R.string.reminders_time_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text(stringResource(R.string.reminders_time_cancel))
                }
            },
        )
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

private fun DayOfWeek.displayName() = getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
private fun RemindersPreviewContent(state: RemindersUiState, areNotificationsAllowed: Boolean = true) {
    MedicationLoggerTheme {
        Surface {
            RemindersScreen(
                state = state,
                areNotificationsAllowed = areNotificationsAllowed,
                onSettingsChange = {},
                onOpenNotificationSettings = {},
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
private fun RemindersScreenPreview() {
    RemindersPreviewContent(previewState)
}

@PreviewLightDark
@Composable
private fun RemindersScreenNotificationsOffPreview() {
    RemindersPreviewContent(previewState, areNotificationsAllowed = false)
}
