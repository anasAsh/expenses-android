package com.anasexpenses.budget.domain.category

import com.anasexpenses.budget.domain.money.JodMoney

/** Parses lines like `Gas: 100` or `Eating Out: 200.50` (JOD amounts). */
object CategoryBulkImport {
    data class ParsedLine(val name: String, val milliJod: Long)

    data class ParseResult(
        val lines: List<ParsedLine>,
        val lineErrors: List<String>,
    )

    fun parseLines(text: String): ParseResult {
        val lines = mutableListOf<ParsedLine>()
        val errors = mutableListOf<String>()
        text.lines().forEachIndexed { index, raw ->
            val lineNum = index + 1
            val t = raw.trim()
            if (t.isEmpty()) return@forEachIndexed
            val colon = t.lastIndexOf(':')
            if (colon <= 0 || colon >= t.lastIndex) {
                errors.add("Line $lineNum: expected \"Name: amount\"")
                return@forEachIndexed
            }
            val name = t.substring(0, colon).trim()
            val amountPart = t.substring(colon + 1).trim()
            if (name.isEmpty()) {
                errors.add("Line $lineNum: empty name")
                return@forEachIndexed
            }
            val milli = try {
                val m = JodMoney.parseToMilliJod(amountPart)
                if (m <= 0L) {
                    errors.add("Line $lineNum: amount must be positive")
                    return@forEachIndexed
                }
                m
            } catch (_: Exception) {
                errors.add("Line $lineNum: invalid amount \"$amountPart\"")
                return@forEachIndexed
            }
            lines.add(ParsedLine(name, milli))
        }
        return ParseResult(lines, errors)
    }
}
