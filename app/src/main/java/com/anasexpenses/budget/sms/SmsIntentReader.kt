package com.anasexpenses.budget.sms

import android.content.Intent
import android.provider.Telephony

object SmsIntentReader {
    fun bodiesFrom(intent: Intent): List<String> {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return emptyList()
        return messages.mapNotNull { sms ->
            sms?.messageBody?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
