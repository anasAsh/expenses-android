package com.anasexpenses.budget.sms.parser

import com.anasexpenses.budget.domain.money.JodMoney
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Applies a single-line-first [Regex]; ignores trailing SMS clauses after the match. */
object RegexBankSmsParser {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

    /**
     * Arab Bank Arabic SMS: Click payment — amount + last 4 after `*` in account mask.
     * Several patterns: bank copy changes (balance line optional, spacing, commas).
     */
    private val arabClickCreditPatterns: List<Regex> = listOf(
        // Original PRD-style (balance clause required)
        Regex(
            """(?s)(?:JO\s*)?تم قيد دفعة كليك بمبلغ\s+([\d.,]+)\s*JOD\s+من حساب\s+\d+\*(\d{4})\s+الرصيد\s+[\d.,]+\s*JOD""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ),
        // No trailing balance line (common variant)
        Regex(
            """(?s)(?:JO\s*)?تم قيد دفعة كليك بمبلغ\s+([\d.,]+)\s*JOD\s+من حساب\s+\d+\*(\d{4})""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ),
        // Looser: كليك + بمبلغ … JOD anywhere before *NNNN (allows extra clauses / order drift)
        Regex(
            """(?s)كليك[\s\S]{0,800}?بمبلغ\s+([\d.,]+)\s*JOD[\s\S]{0,800}?\*(\d{4})""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ),
    )

    /**
     * Parses Arabic Click credit SMS. Uses [fallbackDateEpochDay] / [fallbackTimeSecondOfDay] when the SMS has no
     * timestamp (typically SMS receive time at ingest). Pass fixed values in unit tests.
     */
    fun parseArabClickCredit(
        rawSms: String,
        fallbackDateEpochDay: Long? = null,
        fallbackTimeSecondOfDay: Int? = null,
    ): ParseOutcome {
        val text = rawSms.trim()
        val match = arabClickCreditPatterns.asSequence()
            .mapNotNull { it.find(text) }
            .firstOrNull() ?: return ParseOutcome.NoMatch
        val g = match.groupValues
        if (g.size < 3) return ParseOutcome.NoMatch
        return try {
            val amountMilliJod = JodMoney.parseToMilliJod(g[1].replace(',', '.'))
            val cardLast4 = g[2]
            val dateEpochDay = fallbackDateEpochDay ?: LocalDate.now().toEpochDay()
            val timeSecondOfDay = fallbackTimeSecondOfDay ?: LocalTime.now().toSecondOfDay()
            ParseOutcome.Success(
                ParsedBankSms(
                    cardLast4 = cardLast4,
                    merchantRaw = ARAB_CLICK_MERCHANT_LABEL,
                    amountMilliJod = amountMilliJod,
                    currency = "JOD",
                    dateEpochDay = dateEpochDay,
                    timeSecondOfDay = timeSecondOfDay,
                ),
                confidence = ARAB_CLICK_CONFIDENCE,
            )
        } catch (e: Exception) {
            ParseOutcome.Failure(e.message ?: "parse error")
        }
    }

    fun parse(regexPattern: String, rawSms: String): ParseOutcome {
        val regex = Regex(regexPattern, setOf(RegexOption.DOT_MATCHES_ALL))
        val match = regex.find(rawSms.trim()) ?: return ParseOutcome.NoMatch
        val g = match.groupValues
        if (g.size < 6) return ParseOutcome.NoMatch
        return try {
            val cardLast4 = g[1]
            val merchantRaw = g[2].trim()
            val (currency, amountText, dateText, timeText) =
                if (g.size >= 7) {
                    listOf(g[3], g[4], g[5], g[6])
                } else {
                    listOf("JOD", g[3], g[4], g[5])
                }
            val amountMilliJod = JodMoney.parseToMilliJod(amountText)
            val date = LocalDate.parse(dateText, dateFormatter)
            val timeParts = timeText.split(':')
            require(timeParts.size == 2) { "time" }
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val time = LocalTime.of(hour, minute)
            ParseOutcome.Success(
                ParsedBankSms(
                    cardLast4 = cardLast4,
                    merchantRaw = merchantRaw,
                    amountMilliJod = amountMilliJod,
                    currency = currency.uppercase(Locale.ROOT),
                    dateEpochDay = date.toEpochDay(),
                    timeSecondOfDay = time.toSecondOfDay(),
                ),
                confidence = 0.95f,
            )
        } catch (e: Exception) {
            ParseOutcome.Failure(e.message ?: "parse error")
        }
    }

    private const val ARAB_CLICK_MERCHANT_LABEL = "كليك"
    /** Below [com.anasexpenses.budget.domain.PrdConstants.CONFIDENCE_AUTO_MIN] so category stays user-driven unless ruled. */
    private const val ARAB_CLICK_CONFIDENCE = 0.82f
}
