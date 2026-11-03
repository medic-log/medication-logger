package com.mediclog.medicationlogger.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf

/** What the tutorial can point at, across the tabs. */
enum class TutorialTarget {
    NEW_PRESCRIPTION,
    FIRST_BOX,
    DOSE_LOG,
    PILL_REMINDER,
    REFILL_REMINDER,
    TAB_SETTINGS,
    TAB_PRESCRIPTIONS,
    TAB_BOXES,
    TAB_REFILLS,
    TUTORIAL_CARD,
}

/** Where each [TutorialTarget] on screen is, in window coordinates, so the tutorial can spotlight it. */
@Stable
class TutorialTargets {
    private val bounds = mutableStateMapOf<TutorialTarget, Rect>()

    /** The target's visible bounds, or null when it isn't on screen. */
    fun boundsOf(target: TutorialTarget): Rect? = bounds[target]?.takeUnless { it.isEmpty }

    internal fun place(target: TutorialTarget, rect: Rect) {
        if (bounds[target] != rect) bounds[target] = rect
    }

    internal fun remove(target: TutorialTarget) {
        bounds.remove(target)
    }
}

/** The targets of the tutorial on screen; null where there's no tutorial, as in previews. */
val LocalTutorialTargets = staticCompositionLocalOf<TutorialTargets?> { null }

/** Lets the tutorial find this element as [target]. */
fun Modifier.tutorialTarget(target: TutorialTarget): Modifier = this then TutorialTargetElement(target)

private data class TutorialTargetElement(val target: TutorialTarget) : ModifierNodeElement<TutorialTargetNode>() {
    override fun create() = TutorialTargetNode(target)

    override fun update(node: TutorialTargetNode) {
        node.retarget(target)
    }
}

private class TutorialTargetNode(private var target: TutorialTarget) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    // The registry last placed in, so it can be cleared once this leaves the screen
    private var targets: TutorialTargets? = null

    override fun onDetach() {
        targets?.remove(target)
        targets = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // Clipped to what's visible, so a target scrolled away counts as off screen
        targets = currentValueOf(LocalTutorialTargets)
        targets?.place(target, coordinates.boundsInWindow())
    }

    fun retarget(newTarget: TutorialTarget) {
        targets?.remove(target)
        target = newTarget
    }
}
