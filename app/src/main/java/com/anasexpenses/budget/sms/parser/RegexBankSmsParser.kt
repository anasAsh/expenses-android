package com.anasexpenses.budget.sms.parser

import com.anasexpenses.budget.domain.money.JodMoney
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Applies a single-line-first [Regex]; ignores trailing SMS clauses after the match. */
object RegexBankSmsParser {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

    fun parse(regexPattern: String, rawSms: String): ParseOutcome {
        val regex = Regex(regexPattern, setOf(RegexOption.DOT_MATCHES_ALL))
        val match = regex.find(rawSms.trim()) ?: return ParseOutcome.NoMatch
        val g = match.groupValues
        if (g.size < 6) return ParseOutcome.NoMatch
        return try {
            val cardLast4 = g[1]
            val merchantRaw = g[2].trim()
            val amountMilliJod = JodMoney.parseToMilliJod(g[3])
            val date = LocalDate.parse(g[4], dateFormatter)
            val timeParts = g[5].split(':')
            require(timeParts.size == 2) { "time" }
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val time = LocalTime.of(hour, minute)
            ParseOutcome.Success(
                ParsedBankSms(
                    cardLast4 = cardLast4,
                    merchantRaw = merchantRaw,
                    amountMilliJod = amountMilliJod,
                    currency = "JOD",
                    dateEpochDay = date.toEpochDay(),
                    timeSecondOfDay = time.toSecondOfDay(),
                ),
                confidence = 0.95f,
            )
        } catch (e: Exception) {
            ParseOutcome.Failure(e.message ?: "parse error")
        }
    }
}
