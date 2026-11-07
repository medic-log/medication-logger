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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.AppViewModelProvider
import com.mediclog.medicationlogger.ui.boxes.BoxesRoute
import com.mediclog.medicationlogger.ui.components.LocalTutorialTargets
import com.mediclog.medicationlogger.ui.components.TutorialTarget
import com.mediclog.medicationlogger.ui.components.TutorialTargets
import com.mediclog.medicationlogger.ui.components.UpdatePrompt
import com.mediclog.medicationlogger.ui.components.tutorialTarget
import com.mediclog.medicationlogger.ui.prescriptions.PrescriptionsRoute
import com.mediclog.medicationlogger.ui.refills.RefillsRoute
import com.mediclog.medicationlogger.ui.reminders.RemindersRoute
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

// From the 24 dp icon to the 64 x 32 dp indicator Material draws around it
private val INDICATOR_OUTSET = DpSize(20.dp, 4.dp)

// From the tabs, which fill the bar's 80 dp, in to the content's 16 dp margins and to 12 dp around icons and labels
private val TAB_BAR_OUTSET = DpSize((-16).dp, (-12).dp)

/** The app's frame, with its ViewModel: tabs and what's in them. */
@Composable
fun MainRoute(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tutorialTargets = remember { TutorialTargets() }
    CompositionLocalProvider(LocalTutorialTargets provides tutorialTargets) {
        MainScreen(
            state = state,
            onSelectTab = viewModel::selectTab,
            modifier = modifier,
            tutorial = {
                state.tutorialStep?.let { step ->
                    TutorialCard(
                        step = step,
                        settings = state.reminderSettings,
                        onSettingsChange = viewModel::updateReminderSettings,
                        onBack = viewModel::previousTutorialStep,
                        onNext = viewModel::nextTutorialStep,
                        onSkip = viewModel::finishTutorial,
                        modifier = Modifier.tutorialTarget(TutorialTarget.TUTORIAL_CARD),
                    )
                }
            },
            tutorialOverlay = {
                state.tutorialStep?.let { step ->
                    TutorialOverlay(
                        highlight = step.target?.let { tutorialTargets.boundsOf(it) },
                        // The tab bar already holds the tab
                        tab = step.target?.takeUnless { it == TutorialTarget.TAB_BAR }
                            ?.let { tutorialTargets.boundsOf(step.tab.tutorialTarget) },
                        card = tutorialTargets.boundsOf(TutorialTarget.TUTORIAL_CARD),
                    )
                }
            },
        ) { tab -> TabContent(tab, state, viewModel) }
        UpdatePrompt(state.availableUpdate, onDismiss = viewModel::dismissUpdate)
    }
}

@Composable
private fun TabContent(tab: AppTab, state: MainUiState, viewModel: MainViewModel) {
    when (tab) {
        AppTab.SETTINGS -> RemindersRoute(onHelp = viewModel::startTutorial)
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

/**
 * The tab bar, with the selected tab's content above it and [tutorial] between the two, and [tutorialOverlay] over
 * it all. A tutorial step that points at nothing blurs the app behind it. Boxes stands out as the main tab.
 */
@Composable
fun MainScreen(
    state: MainUiState,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    tutorial: @Composable () -> Unit = {},
    tutorialOverlay: @Composable () -> Unit = {},
    tabContent: @Composable (AppTab) -> Unit,
) {
    // Blur only works from Android 12; before that the overlay's dimming does it alone
    val blur = if (state.tutorialStep != null && state.tutorialStep.target == null) Modifier.blur(8.dp) else Modifier
    // During the tutorial only its card responds, so the app behind can't be changed by accident
    val background = if (state.tutorialStep != null) blur.ignoringTouches() else blur
    Box(modifier = modifier.fillMaxSize()) {
        MainScaffold(state, onSelectTab, background, tutorial, tabContent)
        tutorialOverlay()
    }
}

@Composable
private fun MainScaffold(
    state: MainUiState,
    onSelectTab: (AppTab) -> Unit,
    background: Modifier,
    tutorial: @Composable () -> Unit,
    tabContent: @Composable (AppTab) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(modifier = background) {
                AppTab.entries.forEach { tab ->
                    val isMain = tab == AppTab.BOXES
                    NavigationBarItem(
                        selected = tab == state.selectedTab,
                        onClick = { onSelectTab(tab) },
                        // The tabs together are spotlit as wide as the content, each tab around its indicator and label
                        modifier = Modifier.tutorialTarget(TutorialTarget.TAB_BAR, TAB_BAR_OUTSET),
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                modifier = Modifier.tutorialTarget(tab.tutorialTarget, INDICATOR_OUTSET),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                modifier = Modifier.tutorialTarget(tab.tutorialTarget),
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
            Box(modifier = Modifier.weight(1f).then(background)) {
                tabContent(state.selectedTab)
            }
            tutorial()
        }
    }
}

// Taken before the content sees them, and marked used, so its buttons and lists ignore them
private fun Modifier.ignoringTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
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
