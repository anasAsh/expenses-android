package com.anasexpenses.budget.ui.home

import com.anasexpenses.budget.domain.money.formatJodFromMilli

/** Plain-text budget summary for ACTION_SEND (messages, email, etc.). */
object HomeShareTextBuilder {

    fun build(
        appName: String,
        monthLabel: String,
        rows: List<CategorySpendRow>,
        unassignedMilliJod: Long = 0L,
    ): String = buildString {
        appendLine("$appName — $monthLabel")
        appendLine()
        appendLine(
            "Unassigned (no category): ${formatJodFromMilli(unassignedMilliJod.coerceAtLeast(0L))} JOD",
        )
        appendLine()

        if (rows.isEmpty()) {
            appendLine("(No categories this month)")
            return@buildString
        }

        var includedSpent = 0L
        var includedTarget = 0L

        for (row in rows) {
            val c = row.category
            val spent = row.spentMilliJod
            val target = c.monthlyTargetMilliJod
            appendLine(c.name)
            append(
                "${formatJodFromMilli(spent.coerceAtLeast(0L))} / ${formatJodFromMilli(target)} JOD",
            )
            if (!c.excludedFromSpend && target > 0L) {
                includedSpent += spent
                includedTarget += target
                val remaining = target - spent
                when {
                    remaining > 0L -> append(" · Left ${formatJodFromMilli(remaining)}")
                    remaining < 0L -> append(" · Over ${formatJodFromMilli(-remaining)}")
                    else -> append(" · On target")
                }
            }
            if (c.excludedFromSpend) {
                append(" [Excluded]")
            }
            appendLine()
            appendLine()
        }

        if (includedTarget > 0L) {
            appendLine("---")
            appendLine(
                "Included totals: ${formatJodFromMilli(includedSpent.coerceAtLeast(0L))} / " +
                    "${formatJodFromMilli(includedTarget)} JOD",
            )
            val left = includedTarget - includedSpent
            if (left >= 0L) {
                appendLine("Remaining (included): ${formatJodFromMilli(left)} JOD")
            } else {
                appendLine("Over included budget by: ${formatJodFromMilli(-left)} JOD")
            }
        }
    }
}
