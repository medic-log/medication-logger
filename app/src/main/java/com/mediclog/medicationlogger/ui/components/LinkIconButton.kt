package com.mediclog.medicationlogger.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** An icon that opens a linked record, with a short [label] when the icon alone doesn't say which one. */
@Composable
fun LinkIconButton(
    @DrawableRes iconRes: Int,
    @StringRes descriptionRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    CardButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            modifier = Modifier.size(if (label == null) 16.dp else 12.dp),
        )
        if (label != null) Text(text = label, modifier = Modifier.padding(start = 4.dp))
    }
}
