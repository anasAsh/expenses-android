package com.anasexpenses.budget.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Receives [Telephony.Sms.Intents.SMS_RECEIVED]. Parsing pipeline is wired in a follow-up task.
 */
class SmsTransactionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        // TODO: extract PDU bundle → enqueue ProcessSmsWorker / parser pipeline
    }
}
