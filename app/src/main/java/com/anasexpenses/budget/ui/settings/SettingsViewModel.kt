package com.anasexpenses.budget.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.time.BudgetCycle
import com.anasexpenses.budget.data.metrics.AppMetricsRepository
import com.anasexpenses.budget.data.export.DatabaseExportHelper
import com.anasexpenses.budget.sms.SmsInboxBackfill
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseExportHelper: DatabaseExportHelper,
    private val transactionRepository: TransactionRepository,
    private val smsInboxBackfill: SmsInboxBackfill,
    private val userPreferencesRepository: UserPreferencesRepository,
    appMetricsRepository: AppMetricsRepository,
) : ViewModel() {

    val budgetCycleStartDay: StateFlow<Int> =
        userPreferencesRepository.budgetCycleStartDay.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BudgetCycle.MIN_START_DAY,
        )

    fun setBudgetCycleStartDay(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetCycleStartDay(day)
        }
    }

    val smsTransactionRows: StateFlow<Long> =
        appMetricsRepository.smsTransactionRows.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0L,
        )
    val manualTransactionRows: StateFlow<Long> =
        appMetricsRepository.manualTransactionRows.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0L,
        )

    val dailyBackupTreeUri: StateFlow<String?> =
        userPreferencesRepository.dailyBackupTreeUri.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    fun exportToUri(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    databaseExportHelper.checkpointAndCopyTo(out)
                } ?: error("no stream")
            }.isSuccess
            onDone(ok)
        }
    }

    fun setDailyBackupFolder(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                userPreferencesRepository.setDailyBackupTreeUri(uri.toString())
            }.isSuccess
            onDone(ok)
        }
    }

    fun disableDailyBackup(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val current = userPreferencesRepository.dailyBackupTreeUri.first()
                if (!current.isNullOrBlank()) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(current),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                }
                userPreferencesRepository.setDailyBackupTreeUri(null)
            }.isSuccess
            onDone(ok)
        }
    }

    fun runInboxBackfill(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val bodies = smsInboxBackfill.collectRecentBankBodies()
            transactionRepository.ingestSmsBodies(bodies)
            onDone(bodies.size)
        }
    }

    fun ingestPastedSms(body: String, onDone: (Boolean) -> Unit) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            transactionRepository.ingestSmsBodies(listOf(trimmed))
            onDone(true)
        }
    }

    fun openPrivacyPolicyUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
