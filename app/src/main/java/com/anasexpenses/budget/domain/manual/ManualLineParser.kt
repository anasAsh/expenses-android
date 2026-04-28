package com.anasexpenses.budget.domain.manual

import com.anasexpenses.budget.domain.money.JodMoney

/** PRD §4.4 — `coffee 3`, `rent 500`. */
object ManualLineParser {
    private val pattern = Regex("^(.+?)\\s+([\\d.]+)$")

    fun parse(line: String): Pair<String, Long>? {
        val m = pattern.find(line.trim()) ?: return null
        val label = m.groupValues[1].trim().ifEmpty { return null }
        val milli = try {
            JodMoney.parseToMilliJod(m.groupValues[2])
        } catch (_: Exception) {
            return null
        }
        return label to milli
    }
}
