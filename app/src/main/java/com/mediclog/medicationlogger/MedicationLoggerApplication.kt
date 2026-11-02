package com.mediclog.medicationlogger

import android.app.Application
import com.mediclog.medicationlogger.data.JsonFileMedicationStore
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock

/** Holds the objects shared across the app, and keeps reminder alarms in step with the data. */
class MedicationLoggerApplication : Application() {
    val clock: Clock = Clock.systemDefaultZone()

    // Loaded on first use, on the main thread: it's one small file, and every screen needs it to draw
    val repository: MedicationRepository by lazy {
        MedicationRepository(
            store = JsonFileMedicationStore(File(filesDir, "medication-data.json")),
            clock = clock,
            saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this, clock) }

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            repository.data.collect(reminderScheduler::reschedule)
        }
    }
}
