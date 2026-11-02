package com.mediclog.medicationlogger.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mediclog.medicationlogger.R

/** Selects one of the list filters. */
@Composable
fun FilterSelector(filter: ListFilter, onFilterChange: (ListFilter) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ListFilter.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == filter,
                onClick = { onFilterChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, ListFilter.entries.size),
                label = {
                    Text(
                        stringResource(
                            when (option) {
                                ListFilter.ACTIVE -> R.string.filter_active
                                ListFilter.ALL -> R.string.filter_all
                            },
                        ),
                    )
                },
            )
        }
    }
}
