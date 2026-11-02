package com.mediclog.medicationlogger.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.boxes.BoxesRoute
import com.mediclog.medicationlogger.ui.prescriptions.PrescriptionsRoute
import com.mediclog.medicationlogger.ui.refills.RefillsRoute
import com.mediclog.medicationlogger.ui.reminders.RemindersRoute
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** The app's frame, with its ViewModel: tabs and what's in them. */
@Composable
fun MainRoute(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreen(state = state, onSelectTab = viewModel::selectTab, modifier = modifier) { tab ->
        when (tab) {
            AppTab.REMINDERS -> RemindersRoute()
            AppTab.PRESCRIPTIONS -> PrescriptionsRoute(
                focusedPrescriptionId = state.focusedPrescriptionId,
                onOpenBox = viewModel::openBox,
            )
            AppTab.BOXES -> BoxesRoute(
                focusedBoxId = state.focusedBoxId,
                onOpenPrescription = viewModel::openPrescription,
            )
            AppTab.REFILLS -> RefillsRoute(
                onOpenPrescription = viewModel::openPrescription,
                onOpenBox = viewModel::openBox,
            )
        }
    }
}

/** The tab bar, with the selected tab's content above it. Boxes stands out as the main tab. */
@Composable
fun MainScreen(
    state: MainUiState,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    tabContent: @Composable (AppTab) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val isMain = tab == AppTab.BOXES
                    NavigationBarItem(
                        selected = tab == state.selectedTab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(painter = painterResource(tab.iconRes), contentDescription = null) },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                fontWeight = if (isMain) FontWeight.Bold else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = if (isMain) {
                            NavigationBarItemDefaults.colors(
                                unselectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            NavigationBarItemDefaults.colors()
                        },
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
            if (state.isSaveFailed) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.main_save_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                tabContent(state.selectedTab)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MainScreenPreview() {
    MedicationLoggerTheme {
        MainScreen(state = MainUiState(), onSelectTab = {}) { tab ->
            Text(text = stringResource(tab.labelRes), modifier = Modifier.padding(16.dp))
        }
    }
}
