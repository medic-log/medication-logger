package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** A button showing [day] that opens a menu of weekdays. */
@Composable
fun WeekdaySelector(day: DayOfWeek, onDayChange: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
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

/** The weekday's full name in the user's language. */
fun DayOfWeek.displayName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())
