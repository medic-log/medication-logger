package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** A compact button on a record card, outlined unless [isFilled] marks the likely next action. */
@Composable
fun CardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFilled: Boolean = false,
    shape: Shape = CircleShape,
    isIconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    // Material still keeps a 48 dp touch target around the smaller button
    val sizedModifier = modifier.widthIn(min = if (isIconOnly) 32.dp else 40.dp).heightIn(min = 32.dp)
    val contentPadding = PaddingValues(horizontal = if (isIconOnly) 0.dp else 12.dp)
    val styledContent: @Composable RowScope.() -> Unit = {
        ProvideTextStyle(MaterialTheme.typography.labelMedium) { content() }
    }
    if (isFilled) {
        Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            content = styledContent,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            contentPadding = contentPadding,
            content = styledContent,
        )
    }
}
