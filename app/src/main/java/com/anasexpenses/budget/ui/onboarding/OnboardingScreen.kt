package com.anasexpenses.budget.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anasexpenses.budget.R

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* granted map ignored — user can still finish manually */ }

    var categoryName by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var excluded by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(R.string.sms_permission_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.sms_permission_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                smsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sms_permission_grant))
        }
        Button(
            onClick = { viewModel.markSmsSkipped() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sms_permission_skip))
        }

        Text(
            text = stringResource(R.string.onboarding_category_title),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.category_name_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = targetText,
            onValueChange = { targetText = it; targetError = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.category_target_jod_label)) },
            supportingText = if (targetError) {
                { Text(stringResource(R.string.category_target_invalid)) }
            } else null,
            isError = targetError,
            singleLine = true,
        )
        RowSwitch(
            label = stringResource(R.string.category_excluded_label),
            checked = excluded,
            onCheckedChange = { excluded = it },
        )

        Button(
            onClick = {
                viewModel.finishFirstCategory(categoryName, targetText, excluded) {
                    targetError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
