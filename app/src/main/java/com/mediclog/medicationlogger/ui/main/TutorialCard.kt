package com.mediclog.medicationlogger.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.ReminderSettings
import com.mediclog.medicationlogger.ui.components.TimeSelector
import com.mediclog.medicationlogger.ui.components.WeekdaySelector
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** A tutorial step, docked above the tab bar, with the settings it asks for and the way to the next one. */
@Composable
fun TutorialCard(
    step: TutorialStep,
    settings: ReminderSettings,
    onSettingsChange: (ReminderSettings) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.tutorial_progress, step.ordinal + 1, TutorialStep.entries.size),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(step.textRes), style = MaterialTheme.typography.bodyMedium)
            when (step) {
                TutorialStep.PILL_TIME -> TimeSelector(
                    time = settings.pillTime,
                    dialogTitle = stringResource(R.string.reminders_pill_time_dialog_title),
                    onTimeChange = { onSettingsChange(settings.copy(pillTime = it)) },
                )
                TutorialStep.REFILL_TIME -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekdaySelector(
                        day = settings.refillDay,
                        onDayChange = { onSettingsChange(settings.copy(refillDay = it)) },
                    )
                    TimeSelector(
                        time = settings.refillTime,
                        dialogTitle = stringResource(R.string.reminders_time_dialog_title),
                        onTimeChange = { onSettingsChange(settings.copy(refillTime = it)) },
                    )
                }
                else -> Unit
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (step.next != null) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.tutorial_skip)) }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (step.previous != null) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.tutorial_back)) }
                }
                Button(onClick = onNext) {
                    Text(stringResource(if (step.next != null) R.string.tutorial_next else R.string.tutorial_done))
                }
            }
        }
    }
}

@Composable
private fun TutorialCardPreviewContent(step: TutorialStep) {
    MedicationLoggerTheme {
        TutorialCard(
            step = step,
            settings = ReminderSettings(),
            onSettingsChange = {},
            onBack = {},
            onNext = {},
            onSkip = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun TutorialCardPreview() {
    TutorialCardPreviewContent(TutorialStep.WELCOME)
}

@PreviewLightDark
@Composable
private fun TutorialCardRefillTimePreview() {
    TutorialCardPreviewContent(TutorialStep.REFILL_TIME)
}
