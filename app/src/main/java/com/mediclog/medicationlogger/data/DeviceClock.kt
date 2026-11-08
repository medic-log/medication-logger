package com.mediclog.medicationlogger.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The system clock in the device's current time zone. Clock.systemDefaultZone() keeps the zone it was made in, so
 * after a time zone change it would give, and set alarms at, times in the old zone until the app's process restarts.
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
