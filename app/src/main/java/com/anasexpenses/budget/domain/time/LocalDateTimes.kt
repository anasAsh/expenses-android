package com.anasexpenses.budget.domain.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

fun epochMillisFrom(dateEpochDay: Long, timeSecondOfDay: Int, zone: ZoneId): Long {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val time = LocalTime.ofSecondOfDay(timeSecondOfDay.toLong().coerceIn(0L, 86399L))
    return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
}
