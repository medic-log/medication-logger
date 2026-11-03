package com.mediclog.medicationlogger.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * The single source of the app's data. Changes apply in memory at once and are written in [saveScope],
 * which should run off the main thread.
 */
class MedicationRepository(
    private val store: MedicationStore,
    private val clock: Clock,
    private val saveScope: CoroutineScope,
) {
    private val saveLock = Mutex()
    private val mutableData = MutableStateFlow(store.load())
    private val mutableIsSaveFailed = MutableStateFlow(false)

    private val sample = MutableStateFlow<MedicationData?>(null)
    private val mutableShownData = MutableStateFlow(mutableData.value)

    /** The saved data, which reminders follow. */
    val data: StateFlow<MedicationData> = mutableData.asStateFlow()

    /** What the screens show: the saved data, or sample records in its place, with the saved reminder settings. */
    val shownData: StateFlow<MedicationData> = mutableShownData.asStateFlow()

    /** Whether the latest change couldn't be written to storage. */
    val isSaveFailed: StateFlow<Boolean> = mutableIsSaveFailed.asStateFlow()

    fun addPrescription(draft: PrescriptionDraft) = change { it.addPrescription(draft, today()) }

    fun orderRepetition(prescriptionId: Long) = change { it.orderRepetition(prescriptionId, today()) }

    fun buyRepetition(prescriptionId: Long, expiryDates: List<YearMonth> = emptyList()) =
        change { it.buyRepetition(prescriptionId, today(), expiryDates) }

    fun refillWeekly(boxId: Long) = change { it.refillWeekly(boxId, now()) }

    fun refillPartial(boxId: Long) = change { it.refillPartial(boxId, now()) }

    fun saveAsExtra(boxId: Long) = change { it.saveAsExtra(boxId) }

    fun backToInventory(boxId: Long) = change { it.backToInventory(boxId) }

    fun logMiss(kind: MedicationMiss.Kind, time: LocalTime? = null) = change { it.logMiss(kind, now(), time) }

    fun editPrescription(updated: Prescription) = change { it.editPrescription(updated) }

    fun deletePrescription(prescriptionId: Long, keepBoxes: Boolean) =
        change { it.deletePrescription(prescriptionId, keepBoxes) }

    fun editBox(updated: Box) = change { it.editBox(updated) }

    fun deleteBox(boxId: Long, keepRefills: Boolean) = change { it.deleteBox(boxId, keepRefills) }

    fun editRefill(updated: Refill) = change { it.editRefill(updated) }

    fun deleteRefill(refillId: Long) = change { it.deleteRefill(refillId) }

    fun editMiss(updated: MedicationMiss) = change { it.editMiss(updated) }

    fun deleteMiss(missId: Long) = change { it.deleteMiss(missId) }

    fun updateReminderSettings(settings: ReminderSettings) = change { it.copy(reminderSettings = settings) }

    fun finishTutorial() = change { it.finishTutorial() }

    /** Shows [sample] on the screens instead of the saved data, which stays as it is, or the saved data again. */
    fun showSample(sample: MedicationData?) {
        this.sample.value = sample
        refreshShownData()
    }

    private fun refreshShownData() {
        mutableShownData.value = sample.value?.copy(reminderSettings = mutableData.value.reminderSettings)
            ?: mutableData.value
    }

    private fun today() = LocalDate.now(clock)

    private fun now() = LocalDateTime.now(clock)

    private fun change(action: (MedicationData) -> MedicationData) {
        mutableData.update(action)
        refreshShownData()
        saveScope.launch {
            // Saves run one at a time and each writes the latest data, so the last one always wins
            saveLock.withLock {
                try {
                    store.save(mutableData.value)
                    mutableIsSaveFailed.value = false
                } catch (_: IOException) {
                    mutableIsSaveFailed.value = true
                }
            }
        }
    }
}
