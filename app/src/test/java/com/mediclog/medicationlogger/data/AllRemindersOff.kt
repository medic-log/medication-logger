package com.mediclog.medicationlogger.data

/** Reminder settings with nothing on, as every reminder starts on. */
val AllRemindersOff = ReminderSettings(
    isRefillEnabled = false,
    isPickupEnabled = false,
    isRunOutEnabled = false,
    isPillEnabled = false,
    isExpiryEnabled = false,
)
