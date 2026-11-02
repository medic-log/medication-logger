package com.mediclog.medicationlogger.data

import java.io.IOException

/** Keeps data in memory, and can pretend that storage is failing. */
class FakeMedicationStore(var saved: MedicationData = MedicationData()) : MedicationStore {
    var isFailing = false

    override fun load(): MedicationData = saved

    override fun save(data: MedicationData) {
        if (isFailing) throw IOException("storage is full")
        saved = data
    }
}
