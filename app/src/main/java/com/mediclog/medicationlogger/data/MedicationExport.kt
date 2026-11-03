package com.mediclog.medicationlogger.data

import org.json.JSONException
import org.json.JSONObject
import java.time.format.DateTimeParseException

/** Writes and reads the records, without reminder settings or tutorial progress, to move them between devices. */
object MedicationExport {
    private const val INDENT = 2

    /** The prescriptions, boxes, refills and misses of [data] as JSON text. */
    fun write(data: MedicationData): String = data.recordsToJson().toString(INDENT)

    /** The records in [text], with everything else at its default, or null when it isn't an export. */
    fun read(text: String): MedicationData? = try {
        JSONObject(text).toRecords()
    } catch (_: JSONException) {
        null
    } catch (_: DateTimeParseException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
