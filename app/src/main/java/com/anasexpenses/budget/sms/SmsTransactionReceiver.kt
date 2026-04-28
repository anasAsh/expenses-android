package com.anasexpenses.budget.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.di.ApplicationScope
import com.anasexpenses.budget.di.IoDispatcher
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt does not support `@AndroidEntryPoint` field injection on [BroadcastReceiver];
 * use [SmsReceiverEntryPoint] via [EntryPointAccessors].
 */
class SmsTransactionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val appContext = context.applicationContext
        val entry = EntryPointAccessors.fromApplication(appContext, SmsReceiverEntryPoint::class.java)
        val repository = entry.transactionRepository()
        val scope = entry.applicationScope()
        val io = entry.ioDispatcher()

        val pendingResult = goAsync()
        val bodies = SmsIntentReader.bodiesFrom(intent)
        scope.launch(io) {
            try {
                repository.ingestSmsBodies(bodies)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    fun transactionRepository(): TransactionRepository

    @ApplicationScope
    fun applicationScope(): CoroutineScope

    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher
}
