package com.anasexpenses.budget.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.export.DatabaseExportHelper
import com.anasexpenses.budget.sms.SmsInboxBackfill
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseExportHelper: DatabaseExportHelper,
    private val transactionRepository: TransactionRepository,
    private val smsInboxBackfill: SmsInboxBackfill,
) : ViewModel() {

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
