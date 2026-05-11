package com.anasexpenses.budget.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anasexpenses.budget.R
import com.anasexpenses.budget.domain.category.DefaultCategorySeeds

private enum class SmsStepOutcome {
    None,
    Granted,
    DeniedOrPartial,
    Skipped,
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var smsOutcome by remember { mutableStateOf(SmsStepOutcome.None) }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        smsOutcome =
            if (result.isNotEmpty() && result.values.all { granted -> granted }) {
                SmsStepOutcome.Granted
            } else {
                SmsStepOutcome.DeniedOrPartial
            }
    }

    LaunchedEffect(Unit) {
        viewModel.ensureStarterCategoriesIfNeeded()
    }

    val excludedFromSpendSuffix = stringResource(R.string.onboarding_category_excluded_suffix)
    val defaultCategoryBullets = DefaultCategorySeeds.rows.joinToString("\n") { row ->
        val suffix = if (row.excludedFromSpend) " ($excludedFromSpendSuffix)" else ""
        "• ${row.name}$suffix"
    }

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
            text = stringResource(R.string.onboarding_default_categories_kickstart, defaultCategoryBullets),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            onClick = {
                viewModel.markSmsSkipped()
                smsOutcome = SmsStepOutcome.Skipped
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sms_permission_skip))
        }

        when (smsOutcome) {
            SmsStepOutcome.Granted -> {
                Text(
                    text = stringResource(R.string.onboarding_sms_granted_proceed),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.onboarding_tap_start_budgeting_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SmsStepOutcome.DeniedOrPartial, SmsStepOutcome.Skipped -> {
                Text(
                    text = stringResource(R.string.onboarding_sms_continue_without_sms_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.onboarding_tap_start_budgeting_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            SmsStepOutcome.None -> {
                Text(
                    text = stringResource(R.string.onboarding_sms_choose_then_proceed_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FilledTonalButton(
            onClick = { viewModel.finishOnboarding() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}
