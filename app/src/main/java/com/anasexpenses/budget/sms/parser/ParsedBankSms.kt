package com.anasexpenses.budget.sms.parser

sealed interface ParseOutcome {
    data class Success(val fields: ParsedBankSms, val confidence: Float) : ParseOutcome
    data object NoMatch : ParseOutcome
    data class Failure(val reason: String) : ParseOutcome
}

data class ParsedBankSms(
    val cardLast4: String,
    val merchantRaw: String,
    val amountMilliJod: Long,
    val currency: String,
    val dateEpochDay: Long,
    val timeSecondOfDay: Int,
)
