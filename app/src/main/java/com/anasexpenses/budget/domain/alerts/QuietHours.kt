package com.anasexpenses.budget.domain.alerts

import java.time.LocalTime
import java.time.ZoneId

/** PRD §4.7 — no push 22:00–08:00 local. */
object QuietHours {
    private val START = LocalTime.of(22, 0)
    private val END = LocalTime.of(8, 0)

    fun isQuietNow(zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val now = LocalTime.now(zone)
        return now >= START || now < END
    }
}
