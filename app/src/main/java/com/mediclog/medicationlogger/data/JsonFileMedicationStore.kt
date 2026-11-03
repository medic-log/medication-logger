package com.mediclog.medicationlogger.data

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeParseException

/** Stores everything as one JSON file, written atomically so a crash can't leave half a file. */
class JsonFileMedicationStore(private val file: File) : MedicationStore {
    private val atomicFile = AtomicFile(file)

    override fun load(): MedicationData {
        if (!atomicFile.baseFile.exists()) return MedicationData()
        return try {
            atomicFile.readFully().decodeToString().let(::JSONObject).toMedicationData()
        } catch (_: IOException) {
            setAsideUnreadableFile()
        } catch (_: JSONException) {
            setAsideUnreadableFile()
        } catch (_: DateTimeParseException) {
            setAsideUnreadableFile()
        } catch (_: IllegalArgumentException) {
            setAsideUnreadableFile()
        }
    }

    override fun save(data: MedicationData) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(data.toJson().toString().encodeToByteArray())
            atomicFile.finishWrite(stream)
        } catch (exception: IOException) {
            atomicFile.failWrite(stream)
            throw exception
        }
    }

    // Starting empty would overwrite the file on the next save, so it's kept for recovery
    private fun setAsideUnreadableFile(): MedicationData {
        file.renameTo(File(file.path + ".unreadable"))
        return MedicationData()
    }
}

private fun MedicationData.toJson() = JSONObject()
    .put("prescriptions", prescriptions.toJsonArray { it.toJson() })
    .put("boxes", boxes.toJsonArray { it.toJson() })
    .put("refills", refills.toJsonArray { it.toJson() })
    .put("misses", misses.toJsonArray { it.toJson() })
    .put("reminderSettings", reminderSettings.toJson())
    .put("weeklyLot", weeklyLot.toJson())

private fun JSONObject.toMedicationData(): MedicationData {
    val refills = getJSONArray("refills").map { it.toRefill() }
    return MedicationData(
        prescriptions = getJSONArray("prescriptions").map { it.toPrescription() },
        boxes = getJSONArray("boxes").map { it.toBox() },
        refills = refills,
        misses = getJSONArray("misses").map { it.toMiss() },
        reminderSettings = getJSONObject("reminderSettings").toReminderSettings(),
        weeklyLot = optJSONObject("weeklyLot")?.toWeeklyLot() ?: estimatedWeeklyLot(refills),
    )
}

private fun WeeklyLot.toJson() = JSONObject()
    .put("pills", pills)
    .put("countedAt", countedAt?.toString())

private fun JSONObject.toWeeklyLot() = WeeklyLot(
    pills = getInt("pills"),
    countedAt = if (has("countedAt")) LocalDateTime.parse(getString("countedAt")) else null,
)

// Files saved before the lot was counted start from their last refills, counted at the end of that day
private fun estimatedWeeklyLot(refills: List<Refill>): WeeklyLot {
    val last = refills.lastOrNull() ?: return WeeklyLot()
    val partialPills = refills.asReversed().takeWhile { it.isPartial }.sumOf { it.amount } % PILLS_PER_WEEK
    return WeeklyLot(
        pills = if (partialPills == 0) PILLS_PER_WEEK else partialPills,
        countedAt = last.date.atTime(LocalTime.MAX),
    )
}

private const val YEAR_MONTH_LENGTH = 7

private fun Prescription.toJson() = JSONObject()
    .put("id", id)
    .put("prescribedDate", prescribedDate.toString())
    .put("prescribedBy", prescribedBy)
    .put("pharmacy", pharmacy)
    .put("pillsPerBox", pillsPerBox)
    .put("boxesPerRepetition", boxesPerRepetition)
    .put("repetitionsTotal", repetitionsTotal)
    .put("status", status.name)
    .put("orderedDate", orderedDate?.toString())
    .put("boughtFirstDate", boughtFirstDate?.toString())
    .put("boughtLastDate", boughtLastDate?.toString())
    .put("repetitionsBought", repetitionsBought)

private fun JSONObject.toPrescription() = Prescription(
    id = getLong("id"),
    prescribedDate = LocalDate.parse(getString("prescribedDate")),
    prescribedBy = getString("prescribedBy"),
    pharmacy = getString("pharmacy"),
    pillsPerBox = getInt("pillsPerBox"),
    boxesPerRepetition = getInt("boxesPerRepetition"),
    repetitionsTotal = getInt("repetitionsTotal"),
    status = Prescription.Status.valueOf(getString("status")),
    orderedDate = dateOrNull("orderedDate"),
    boughtFirstDate = dateOrNull("boughtFirstDate"),
    boughtLastDate = dateOrNull("boughtLastDate"),
    repetitionsBought = getInt("repetitionsBought"),
)

private fun Box.toJson() = JSONObject()
    .put("id", id)
    .put("prescriptionId", prescriptionId)
    .put("boxNumber", boxNumber)
    .put("boughtDate", boughtDate.toString())
    .put("pillsLeft", pillsLeft)
    .put("pillsTotal", pillsTotal)
    .put("isExtra", isExtra)
    .put("openDate", openDate?.toString())
    .put("expiryDate", expiryDate?.toString())

private fun JSONObject.toBox() = Box(
    id = getLong("id"),
    prescriptionId = longOrNull("prescriptionId"),
    boxNumber = getInt("boxNumber"),
    boughtDate = LocalDate.parse(getString("boughtDate")),
    pillsLeft = getInt("pillsLeft"),
    pillsTotal = getInt("pillsTotal"),
    isExtra = getBoolean("isExtra"),
    openDate = dateOrNull("openDate"),
    expiryDate = yearMonthOrNull("expiryDate"),
)

private fun Refill.toJson() = JSONObject()
    .put("id", id)
    .put("boxId", boxId)
    .put("prescriptionId", prescriptionId)
    .put("date", date.toString())
    .put("amount", amount)
    .put("amountRemaining", amountRemaining)
    .put("isPartial", isPartial)

private fun JSONObject.toRefill() = Refill(
    id = getLong("id"),
    boxId = longOrNull("boxId"),
    prescriptionId = longOrNull("prescriptionId"),
    date = LocalDate.parse(getString("date")),
    amount = getInt("amount"),
    amountRemaining = getInt("amountRemaining"),
    isPartial = getBoolean("isPartial"),
)

private fun JSONObject.longOrNull(name: String) = if (isNull(name)) null else getLong(name)

private fun MedicationMiss.toJson() = JSONObject()
    .put("id", id)
    .put("date", date.toString())
    .put("kind", kind.name)

private fun JSONObject.toMiss() = MedicationMiss(
    id = getLong("id"),
    date = LocalDate.parse(getString("date")),
    kind = MedicationMiss.Kind.valueOf(getString("kind")),
)

private fun ReminderSettings.toJson() = JSONObject()
    .put("isRefillEnabled", isRefillEnabled)
    .put("refillDay", refillDay.name)
    .put("refillTime", refillTime.toString())
    .put("isPickupEnabled", isPickupEnabled)
    .put("pickupDaysAfterOrder", pickupDaysAfterOrder)
    .put("isRunOutEnabled", isRunOutEnabled)
    .put("runOutDaysBefore", runOutDaysBefore)
    .put("isPillEnabled", isPillEnabled)
    .put("pillTime", pillTime.toString())
    .put("isExpiryEnabled", isExpiryEnabled)
    .put("expiryDaysBefore", expiryDaysBefore)

private fun JSONObject.toReminderSettings() = ReminderSettings(
    isRefillEnabled = getBoolean("isRefillEnabled"),
    refillDay = DayOfWeek.valueOf(getString("refillDay")),
    refillTime = LocalTime.parse(getString("refillTime")),
    isPickupEnabled = getBoolean("isPickupEnabled"),
    pickupDaysAfterOrder = getInt("pickupDaysAfterOrder"),
    isRunOutEnabled = getBoolean("isRunOutEnabled"),
    runOutDaysBefore = getInt("runOutDaysBefore"),
    // Files saved before the pill reminder existed don't have these
    isPillEnabled = optBoolean("isPillEnabled", ReminderSettings().isPillEnabled),
    pillTime = if (has("pillTime")) LocalTime.parse(getString("pillTime")) else ReminderSettings().pillTime,
    // And files saved before the expiry reminder existed don't have these
    isExpiryEnabled = optBoolean("isExpiryEnabled", ReminderSettings().isExpiryEnabled),
    expiryDaysBefore = optInt("expiryDaysBefore", ReminderSettings().expiryDaysBefore),
)

private fun <T> List<T>.toJsonArray(toJson: (T) -> JSONObject) = JSONArray().also { array ->
    forEach { array.put(toJson(it)) }
}

private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
    List(length()) { transform(getJSONObject(it)) }

// put() leaves null values out, so an absent key means null
private fun JSONObject.dateOrNull(key: String): LocalDate? =
    if (has(key)) LocalDate.parse(getString(key)) else null

// Taking only the year and month also reads the full dates that expiry dates were first saved as
private fun JSONObject.yearMonthOrNull(key: String): YearMonth? =
    if (has(key)) YearMonth.parse(getString(key).take(YEAR_MONTH_LENGTH)) else null
