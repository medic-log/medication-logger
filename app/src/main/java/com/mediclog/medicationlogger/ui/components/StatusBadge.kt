package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediclog.medicationlogger.ui.theme.StatusColors

/**
 * A record's status in a pill, with how much of it is left or done below, [currentAmount] in bold.
 * For the top right corner of a card.
 */
@Composable
fun StatusBadge(
    status: String,
    amount: String,
    currentAmount: Int,
    colors: StatusColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = colors.pill,
            contentColor = colors.onPill,
            border = BorderStroke(1.dp, colors.accent),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.size(8.dp).background(colors.dot, CircleShape))
                Text(text = status, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            text = buildAnnotatedString {
                append(amount)
                val current = currentAmount.toString()
                val start = amount.indexOf(current)
                if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + current.length)
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
