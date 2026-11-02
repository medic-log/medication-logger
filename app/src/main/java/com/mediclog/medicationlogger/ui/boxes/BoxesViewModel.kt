package com.mediclog.medicationlogger.ui.boxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.Refill
import com.mediclog.medicationlogger.ui.components.ListFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A box with its refills, newest first. */
data class BoxItem(val box: Box, val refills: List<Refill>)

/** The boxes to show, and how many pills the last refill still needs. */
data class BoxesUiState(
    val filter: ListFilter = ListFilter.ACTIVE,
    val items: List<BoxItem> = emptyList(),
    val pendingRefillAmount: Int = 0,
)

/** Lists boxes and refills from them. */
class BoxesViewModel(private val repository: MedicationRepository) : ViewModel() {
    private val filter = MutableStateFlow(ListFilter.ACTIVE)

    val uiState: StateFlow<BoxesUiState> = combine(repository.data, filter, ::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(repository.data.value, filter.value))

    fun setFilter(newFilter: ListFilter) {
        filter.value = newFilter
    }

    fun refillWeekly(boxId: Long) = repository.refillWeekly(boxId)

    fun refillPartial(boxId: Long) = repository.refillPartial(boxId)

    fun saveAsExtra(boxId: Long) = repository.saveAsExtra(boxId)

    fun backToInventory(boxId: Long) = repository.backToInventory(boxId)

    fun editBox(updated: Box) = repository.editBox(updated)

    fun deleteBox(boxId: Long, keepRefills: Boolean) = repository.deleteBox(boxId, keepRefills)

    private fun toUiState(data: MedicationData, filter: ListFilter) = BoxesUiState(
        filter = filter,
        items = data.boxes
            .filter { box ->
                when (filter) {
                    ListFilter.ACTIVE -> box.isActive
                    ListFilter.ALL -> true
                    ListFilter.EXTRA -> box.isExtra
                }
            }
            // Oldest first within a status, as that's the order boxes get used in
            .sortedWith(compareBy({ it.status.sortRank() }, { it.boughtDate }, { it.boxNumber }, { it.id }))
            .map { box -> BoxItem(box, data.refills.filter { it.boxId == box.id }.asReversed()) },
        pendingRefillAmount = data.pendingRefillAmount,
    )

    private fun Box.Status.sortRank() = when (this) {
        Box.Status.IN_PROGRESS -> 0
        Box.Status.EXTRA -> 1
        Box.Status.NOT_STARTED -> 2
        Box.Status.FINISHED -> 3
    }
}
