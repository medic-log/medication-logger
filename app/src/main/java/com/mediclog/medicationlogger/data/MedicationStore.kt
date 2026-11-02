package com.mediclog.medicationlogger.data

import java.io.IOException

/** Keeps the app's data between runs; tests replace it with an in-memory one. */
interface MedicationStore {
    fun load(): MedicationData

    @Throws(IOException::class)
    fun save(data: MedicationData)
}
