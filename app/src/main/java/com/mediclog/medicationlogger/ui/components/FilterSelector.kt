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

/** Selects one of the [options]. */
@Composable
fun FilterSelector(
    filter: ListFilter,
    options: List<ListFilter>,
    onFilterChange: (ListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == filter,
                onClick = { onFilterChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = {
                    Text(
                        stringResource(
                            when (option) {
                                ListFilter.ACTIVE -> R.string.filter_active
                                ListFilter.ALL -> R.string.filter_all
                                ListFilter.EXTRA -> R.string.filter_extra
                            },
                        ),
                    )
                },
            )
        }
    }
}
