package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * An alert dialog for forms: it scrolls as a whole, title and buttons included, within the space the
 * system bars and the keyboard leave, so a tall form stays reachable while typing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDialog(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
                    shape = AlertDialogDefaults.shape,
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                ) {
                    Column(Modifier.padding(24.dp)) {
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                                Box(Modifier.padding(bottom = 16.dp)) { title() }
                            }
                        }
                        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        ) {
                            dismissButton()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
