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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import kotlin.math.max
import kotlin.math.min

/** What the tutorial can point at, across the tabs. */
enum class TutorialTarget {
    TAB_BAR,
    FIRST_PRESCRIPTION,
    NEW_PRESCRIPTION,
    FIRST_BOX,
    FIRST_REFILL,
    DOSE_LOG,
    PILL_REMINDER,
    REFILL_REMINDER,
    TAB_SETTINGS,
    TAB_PRESCRIPTIONS,
    TAB_BOXES,
    TAB_REFILLS,
    TUTORIAL_CARD,
}

/**
 * Where each [TutorialTarget] on screen is, in window coordinates, so the tutorial can spotlight it. A target marked
 * on several elements is around all of them.
 */
@Stable
class TutorialTargets {
    private val parts = mutableStateMapOf<TutorialTarget, Map<Any, Rect>>()

    /** The target's visible bounds, or null when it isn't on screen. */
    fun boundsOf(target: TutorialTarget): Rect? = parts[target]?.values
        ?.filterNot { it.isEmpty }
        ?.reduceOrNull { a, b ->
            Rect(min(a.left, b.left), min(a.top, b.top), max(a.right, b.right), max(a.bottom, b.bottom))
        }

    internal fun place(target: TutorialTarget, part: Any, rect: Rect) {
        val current = parts[target].orEmpty()
        if (current[part] != rect) parts[target] = current + (part to rect)
    }

    internal fun remove(target: TutorialTarget, part: Any) {
        val rest = parts[target].orEmpty() - part
        if (rest.isEmpty()) parts.remove(target) else parts[target] = rest
    }
}

/** The targets of the tutorial on screen; null where there's no tutorial, as in previews. */
val LocalTutorialTargets = staticCompositionLocalOf<TutorialTargets?> { null }

/**
 * Lets the tutorial find this element as [target], grown by [outset] on each side, as for an icon whose
 * background is drawn around it.
 */
fun Modifier.tutorialTarget(target: TutorialTarget, outset: DpSize = DpSize.Zero): Modifier =
    this then TutorialTargetElement(target, outset)

private data class TutorialTargetElement(val target: TutorialTarget, val outset: DpSize) :
    ModifierNodeElement<TutorialTargetNode>() {
    override fun create() = TutorialTargetNode(target, outset)

    override fun update(node: TutorialTargetNode) {
        node.retarget(target, outset)
    }
}

private class TutorialTargetNode(private var target: TutorialTarget, private var outset: DpSize) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    // The registry last placed in, so it can be cleared once this leaves the screen
    private var targets: TutorialTargets? = null

    override fun onDetach() {
        targets?.remove(target, this)
        targets = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // Clipped to what's visible, so a target scrolled away counts as off screen
        targets = currentValueOf(LocalTutorialTargets)
        val bounds = coordinates.boundsInWindow()
        val rect = if (bounds.isEmpty) {
            bounds
        } else {
            with(currentValueOf(LocalDensity)) {
                val (x, y) = outset.width.toPx() to outset.height.toPx()
                Rect(bounds.left - x, bounds.top - y, bounds.right + x, bounds.bottom + y)
            }
        }
        targets?.place(target, this, rect)
    }

    fun retarget(newTarget: TutorialTarget, newOutset: DpSize) {
        targets?.remove(target, this)
        target = newTarget
        outset = newOutset
    }
}
