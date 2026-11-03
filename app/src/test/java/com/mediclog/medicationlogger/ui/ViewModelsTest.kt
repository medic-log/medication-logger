package com.mediclog.medicationlogger.ui

import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.FakeMedicationStore
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.data.Refill
import com.mediclog.medicationlogger.data.ReminderSettings
import com.mediclog.medicationlogger.ui.boxes.BoxesViewModel
import com.mediclog.medicationlogger.ui.components.ListFilter
import com.mediclog.medicationlogger.ui.main.AppTab
import com.mediclog.medicationlogger.ui.main.MainViewModel
import com.mediclog.medicationlogger.ui.prescriptions.PrescriptionsViewModel
import com.mediclog.medicationlogger.ui.refills.RefillLogEntry
import com.mediclog.medicationlogger.ui.refills.RefillsViewModel
import com.mediclog.medicationlogger.ui.reminders.RemindersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val store = FakeMedicationStore()
    private val draft = PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 1, 1)
    private lateinit var repository: MedicationRepository

    @Before
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = MedicationRepository(store, clock, CoroutineScope(dispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app opens on boxes, and links open the record's tab`() {
        val viewModel = MainViewModel(repository)
        assertEquals(AppTab.BOXES, viewModel.uiState.value.selectedTab)

        viewModel.openPrescription(4)
        assertEquals(AppTab.PRESCRIPTIONS, viewModel.uiState.value.selectedTab)
        assertEquals(4L, viewModel.uiState.value.focusedPrescriptionId)

        viewModel.openBox(7)
        assertEquals(AppTab.BOXES, viewModel.uiState.value.selectedTab)
        assertEquals(7L, viewModel.uiState.value.focusedBoxId)
        assertNull(viewModel.uiState.value.focusedPrescriptionId)

        viewModel.selectTab(AppTab.REFILLS)
        assertNull(viewModel.uiState.value.focusedBoxId)
    }

    @Test
    fun `save failures reach the main state`() {
        val viewModel = MainViewModel(repository)

        store.isFailing = true
        repository.addPrescription(draft)

        assertTrue(viewModel.uiState.value.isSaveFailed)
    }

    @Test
    fun `prescriptions filter hides finished ones, and the newest comes first`() {
        val viewModel = PrescriptionsViewModel(repository)
        viewModel.addPrescription(draft)
        viewModel.addPrescription(draft.copy(prescribedBy = "Dr Second"))
        viewModel.buyRepetition(1, emptyList())

        assertEquals(listOf(2L), viewModel.uiState.value.items.map { it.prescription.id })

        viewModel.setFilter(ListFilter.ALL)
        assertEquals(listOf(2L, 1L), viewModel.uiState.value.items.map { it.prescription.id })
        assertEquals("Dr Second", viewModel.uiState.value.previousPrescription?.prescribedBy)
        assertEquals(1, viewModel.uiState.value.items.last().boxes.size)
    }

    @Test
    fun `boxes filter shows only those in use, and all lists finished ones last`() {
        val viewModel = BoxesViewModel(repository, clock)
        repository.addPrescription(draft.copy(pillsPerBox = 7, boxesPerRepetition = 3))
        repository.buyRepetition(1)
        viewModel.refillWeekly(1)
        viewModel.saveAsExtra(2)

        assertEquals(listOf(3L), viewModel.uiState.value.items.map { it.box.id })

        viewModel.setFilter(ListFilter.ALL)
        assertEquals(listOf(2L, 3L, 1L), viewModel.uiState.value.items.map { it.box.id })

        viewModel.backToInventory(2)
        viewModel.setFilter(ListFilter.ACTIVE)
        assertEquals(listOf(2L, 3L), viewModel.uiState.value.items.map { it.box.id })
    }

    @Test
    fun `prescriptions sort by status, then newest first`() {
        val viewModel = PrescriptionsViewModel(repository)
        viewModel.addPrescription(draft)
        viewModel.addPrescription(draft.copy(repetitionsTotal = 2))
        viewModel.addPrescription(draft)
        viewModel.addPrescription(draft)
        viewModel.addPrescription(draft)
        viewModel.buyRepetition(1, emptyList())
        viewModel.buyRepetition(2, emptyList())
        viewModel.orderRepetition(3)

        viewModel.setFilter(ListFilter.ALL)

        assertEquals(listOf(3L, 2L, 5L, 4L, 1L), viewModel.uiState.value.items.map { it.prescription.id })
    }

    @Test
    fun `boxes sort by status, then oldest date, then number`() {
        val twoBoxes = draft.copy(boxesPerRepetition = 2)
        val data = MedicationData()
            .addPrescription(twoBoxes, today)
            .addPrescription(twoBoxes.copy(pillsPerBox = 7), today)
            .addPrescription(twoBoxes, today)
            .buyRepetition(1, today)
            .buyRepetition(2, today)
            .buyRepetition(3, today.minusDays(1))
            .refillWeekly(1, today.atTime(21, 0))
            .saveAsExtra(2)
            .refillWeekly(3, today.atTime(21, 0))
        val dispatcher = UnconfinedTestDispatcher()
        val viewModel = BoxesViewModel(
            MedicationRepository(FakeMedicationStore(data), clock, CoroutineScope(dispatcher)),
            clock,
        )

        viewModel.setFilter(ListFilter.ALL)

        val boxes = viewModel.uiState.value.items.map { it.box }
        assertEquals(
            listOf(
                Box.Status.IN_PROGRESS,
                Box.Status.EXTRA,
                Box.Status.NOT_STARTED,
                Box.Status.NOT_STARTED,
                Box.Status.NOT_STARTED,
                Box.Status.FINISHED,
            ),
            boxes.map { it.status },
        )
        assertEquals(listOf(5L, 6L, 4L), boxes.subList(2, 5).map { it.id })
    }

    @Test
    fun `boxes extra filter shows only extra boxes`() {
        val viewModel = BoxesViewModel(repository, clock)
        repository.addPrescription(draft.copy(pillsPerBox = 7, boxesPerRepetition = 3))
        repository.buyRepetition(1)
        viewModel.saveAsExtra(2)

        viewModel.setFilter(ListFilter.EXTRA)

        assertEquals(listOf(2L), viewModel.uiState.value.items.map { it.box.id })
    }

    @Test
    fun `box items hold their refills, and the pending amount lasts the lot until the next refill`() {
        val viewModel = BoxesViewModel(repository, clock)
        repository.addPrescription(draft.copy(pillsPerBox = 4, boxesPerRepetition = 2))
        repository.buyRepetition(1)

        // Monday noon, so six doses are left before Sunday's refill
        viewModel.refillWeekly(1)
        assertEquals(4, viewModel.uiState.value.lotPills)
        assertEquals(2, viewModel.uiState.value.pendingRefillAmount)

        viewModel.refillPartial(2)
        val state = viewModel.uiState.value
        assertEquals(6, state.lotPills)
        assertEquals(0, state.pendingRefillAmount)
        assertEquals(listOf(2), state.items.single().refills.map { it.amountRemaining })
    }

    @Test
    fun `refills log lists entries newest first, with box names and misses`() {
        val viewModel = RefillsViewModel(repository)
        repository.addPrescription(draft)
        repository.buyRepetition(1)
        repository.refillWeekly(1)
        repository.refillWeekly(1)
        viewModel.logMiss(MedicationMiss.Kind.DELAYED)

        val entries = viewModel.uiState.value.entries
        assertTrue(entries.first() is RefillLogEntry.MissEntry)
        val refills = entries.filterIsInstance<RefillLogEntry.RefillEntry>()
        assertEquals(listOf(16, 23), refills.map { it.refill.amountRemaining })
        assertEquals("$today #1", refills.first().boxName)
    }

    private fun partial(id: Long, amount: Int, date: LocalDate = today) =
        Refill(id, 1, 1, date, amount, 0, isPartial = true)

    private fun logOf(vararg refills: Refill, misses: List<MedicationMiss> = emptyList()): List<RefillLogEntry> {
        val data = MedicationData(refills = refills.toList(), misses = misses)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        return RefillsViewModel(MedicationRepository(FakeMedicationStore(data), clock, scope)).uiState.value.entries
    }

    private fun List<RefillLogEntry>.ids() = map { entry ->
        when (entry) {
            is RefillLogEntry.RefillEntry -> listOf(entry.refill.id)
            is RefillLogEntry.RefillGroupEntry -> entry.refills.map { it.refill.id }
            is RefillLogEntry.MissEntry -> emptyList()
        }
    }

    @Test
    fun `partial refills on one day that add up to a week are grouped, newest first`() {
        assertEquals(listOf(listOf(3L, 2L, 1L)), logOf(partial(1, 2), partial(2, 2), partial(3, 3)).ids())
    }

    @Test
    fun `partial refills that don't add up to a week aren't grouped`() {
        assertEquals(listOf(listOf(2L), listOf(1L)), logOf(partial(1, 3), partial(2, 3)).ids())
    }

    @Test
    fun `partial refills on different days aren't grouped`() {
        val log = logOf(partial(1, 4, today.minusDays(1)), partial(2, 3))

        assertEquals(listOf(listOf(2L), listOf(1L)), log.ids())
    }

    @Test
    fun `a full refill between partial ones keeps them apart`() {
        val full = Refill(2, 1, 1, today, 7, 0)

        assertEquals(listOf(listOf(3L), listOf(2L), listOf(1L)), logOf(partial(1, 4), full, partial(3, 3)).ids())
    }

    @Test
    fun `a week is found after a partial refill that doesn't fit in it`() {
        assertEquals(listOf(listOf(3L, 2L), listOf(1L)), logOf(partial(1, 5), partial(2, 3), partial(3, 4)).ids())
    }

    private fun missed(id: Long, date: LocalDate = today) = MedicationMiss(id, date, MedicationMiss.Kind.MISSED)

    private fun List<RefillLogEntry>.keys() = map { entry ->
        if (entry is RefillLogEntry.RefillGroupEntry) entry.members.map { it.key } else listOf(entry.key)
    }

    @Test
    fun `a missed dose joins the refill before it, on top`() {
        val log = logOf(partial(1, 7), misses = listOf(missed(1, today.plusDays(2))))

        assertEquals(listOf(listOf("miss-1", "refill-1")), log.keys())
    }

    @Test
    fun `a missed dose joins a group of refills`() {
        val log = logOf(partial(1, 4), partial(2, 3), misses = listOf(missed(1)))

        assertEquals(listOf(listOf("miss-1", "refill-2", "refill-1")), log.keys())
    }

    @Test
    fun `a missed dose joins the last refill made by its date, not a later one`() {
        val log = logOf(
            partial(1, 7, today.minusDays(7)),
            partial(2, 7),
            misses = listOf(missed(1, today.minusDays(3))),
        )

        assertEquals(listOf(listOf("refill-2"), listOf("miss-1", "refill-1")), log.keys())
    }

    @Test
    fun `a missed dose before any refill, and a delayed dose, stand alone`() {
        val log = logOf(
            partial(1, 7),
            misses = listOf(missed(1, today.minusDays(1)), MedicationMiss(2, today, MedicationMiss.Kind.DELAYED)),
        )

        assertEquals(listOf(listOf("miss-2"), listOf("refill-1"), listOf("miss-1")), log.keys())
    }

    @Test
    fun `reminders state follows settings and data`() {
        val viewModel = RemindersViewModel(repository, clock)
        assertNull(viewModel.uiState.value.runOutDate)

        repository.addPrescription(draft)
        viewModel.updateSettings(ReminderSettings(isPickupEnabled = true, pickupDaysAfterOrder = 2))
        repository.orderRepetition(1)

        val state = viewModel.uiState.value
        assertTrue(state.settings.isPickupEnabled)
        assertEquals(today.plusDays(2), state.pickupDate)
        // Noon is after today's pill time, so the pills start tomorrow
        assertEquals(today.plusDays(31), state.runOutDate)
        assertEquals(30, state.pillsRemaining)
    }
}
