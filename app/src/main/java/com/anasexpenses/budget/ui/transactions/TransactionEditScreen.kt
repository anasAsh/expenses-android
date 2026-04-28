package com.anasexpenses.budget.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasexpenses.budget.R
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.domain.money.formatJodFromMilli
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionEditViewModel = hiltViewModel(),
) {
    val entity by viewModel.transaction.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    if (entity == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val initial = entity!!
    val iso = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    var amountText by remember(initial.id) { mutableStateOf(formatJodFromMilli(initial.amountMilliJod)) }
    var dateText by remember(initial.id) {
        mutableStateOf(LocalDate.ofEpochDay(initial.dateEpochDay).format(iso))
    }
    var isRefund by remember(initial.id) { mutableStateOf(initial.isRefund) }
    var dismissed by remember(initial.id) { mutableStateOf(initial.status == TxStatus.DISMISSED) }
    var categoryId by remember(initial.id) { mutableLongStateOf(initial.categoryId ?: -1L) }
    var fieldError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.transaction_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(initial.merchant, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; fieldError = false },
                label = { Text(stringResource(R.string.transaction_edit_amount_jod)) },
                isError = fieldError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it; fieldError = false },
                label = { Text(stringResource(R.string.transaction_edit_date)) },
                supportingText = { Text(stringResource(R.string.transaction_edit_date_hint)) },
                isError = fieldError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.transaction_edit_refund), modifier = Modifier.weight(1f))
                Switch(checked = isRefund, onCheckedChange = { isRefund = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.transaction_edit_dismissed), modifier = Modifier.weight(1f))
                Switch(checked = dismissed, onCheckedChange = { dismissed = it })
            }
            Text(stringResource(R.string.assign_category_title), style = MaterialTheme.typography.labelMedium)
            categories.forEach { c ->
                CategoryRow(
                    c = c,
                    selected = categoryId == c.id,
                    onSelect = { categoryId = c.id },
                )
            }
            if (fieldError) {
                Text(
                    stringResource(R.string.category_target_invalid),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    viewModel.save(
                        amountText,
                        dateText,
                        isRefund,
                        dismissed,
                        categoryId.takeIf { it > 0L },
                    ) { ok ->
                        if (ok) onClose() else fieldError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.transaction_edit_save)) }
        }
    }
}

@Composable
private fun CategoryRow(
    c: CategoryEntity,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Text(if (selected) "● " else "○ ")
        Text(c.name)
    }
}
