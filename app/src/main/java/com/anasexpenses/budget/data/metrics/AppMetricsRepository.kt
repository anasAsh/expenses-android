package com.anasexpenses.budget.data.metrics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.metricsDataStore by preferencesDataStore(name = "app_metrics")

/** Simple on-device counters (PRD §9 style — no network). */
@Singleton
class AppMetricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.metricsDataStore

    private object Keys {
        val smsRows = longPreferencesKey("sms_transaction_rows")
        val manualRows = longPreferencesKey("manual_transaction_rows")
    }

    val smsTransactionRows: Flow<Long> = store.data.map { it[Keys.smsRows] ?: 0L }
    val manualTransactionRows: Flow<Long> = store.data.map { it[Keys.manualRows] ?: 0L }

    suspend fun recordSmsTransactionInserted() {
        store.edit { it[Keys.smsRows] = (it[Keys.smsRows] ?: 0L) + 1L }
    }

    suspend fun recordManualTransactionInserted() {
        store.edit { it[Keys.manualRows] = (it[Keys.manualRows] ?: 0L) + 1L }
    }
}
