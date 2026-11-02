package com.mediclog.medicationlogger.ui.prescriptions

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.Prescription
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PrescriptionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val date = LocalDate.of(2026, 11, 2)

    private fun rowTop(boxNumber: Int) =
        composeRule.onNodeWithText("#$boxNumber").getUnclippedBoundsInRoot().top

    @Test
    @Config(qualifiers = "w412dp-h1280dp")
    fun `six box links fit in a row on a typical phone`() {
        val prescription = Prescription(
            id = 1,
            prescribedDate = date,
            prescribedBy = "Dr Test",
            pharmacy = "Test Pharmacy",
            pillsPerBox = 30,
            boxesPerRepetition = 6,
            repetitionsTotal = 2,
        )
        val boxes = List(12) { index ->
            Box(
                id = index + 1L,
                prescriptionId = 1,
                boxNumber = index + 1,
                pharmacy = "Test Pharmacy",
                boughtDate = date,
                pillsLeft = 30,
                pillsTotal = 30,
            )
        }

        composeRule.setContent {
            MedicationLoggerTheme {
                PrescriptionsScreen(
                    state = PrescriptionsUiState(items = listOf(PrescriptionItem(prescription, boxes))),
                    focusedPrescriptionId = null,
                    onFilterChange = {},
                    onAddPrescription = {},
                    onOrderRepetition = {},
                    onBuyRepetition = {},
                    onEditPrescription = {},
                    onDeletePrescription = {},
                    onOpenBox = {},
                )
            }
        }

        assertEquals(rowTop(1), rowTop(6))
        assertTrue(rowTop(7) > rowTop(6))
        assertEquals(rowTop(7), rowTop(12))
    }
}
