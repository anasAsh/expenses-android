package com.anasexpenses.budget.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasexpenses.budget.R

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val smsRows by viewModel.smsTransactionRows.collectAsStateWithLifecycle()
    val manualRows by viewModel.manualTransactionRows.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pasteBody by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportToUri(uri) { ok ->
                status = context.getString(
                    if (ok) R.string.settings_export_done else R.string.settings_export_failed,
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        Text(
            stringResource(R.string.settings_metrics_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.settings_metrics_template, smsRows.toString(), manualRows.toString()),
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = {
                createDoc.launch("anas-budget-backup.db")
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_export_db))
        }

        Button(
            onClick = {
                viewModel.runInboxBackfill { n ->
                    status = context.getString(R.string.settings_backfill_done, n)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_sms_backfill))
        }

        Button(
            onClick = {
                viewModel.openPrivacyPolicyUrl(context.getString(R.string.privacy_policy_url))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_privacy_policy))
        }

        Text(
            stringResource(R.string.settings_cloud_planned),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_debug_paste_sms), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = pasteBody,
                    onValueChange = { pasteBody = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text(stringResource(R.string.settings_paste_sms_hint)) },
                )
                Button(
                    onClick = {
                        viewModel.ingestPastedSms(pasteBody) { ok ->
                            status = context.getString(
                                if (ok) R.string.settings_paste_ingested else R.string.settings_paste_empty,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_parse_pasted_sms))
                }
            }
        }

        status?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
