package com.anasexpenses.budget.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.domain.money.formatJodFromMilli
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var manualOpen by remember { mutableStateOf(false) }
    var manualLine by remember { mutableStateOf("") }
    var manualCategoryId by remember { mutableLongStateOf(-1L) }

    var assignOpen by remember { mutableStateOf(false) }
    var assignTxnId by remember { mutableLongStateOf(-1L) }
    var assignCatId by remember { mutableLongStateOf(-1L) }
    var createRule by remember { mutableStateOf(true) }
    var backApply by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                manualCategoryId = categories.firstOrNull()?.id ?: -1L
                manualOpen = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.manual_entry_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.padding(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { t ->
                    TransactionRow(
                        transaction = t,
                        onClick = {
                            assignTxnId = t.id
                            assignCatId = t.categoryId ?: categories.firstOrNull()?.id ?: -1L
                            assignOpen = true
                        },
                    )
                }
            }
        }
    }

    if (manualOpen) {
        AlertDialog(
            onDismissRequest = { manualOpen = false },
            title = { Text(stringResource(R.string.manual_entry_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = manualLine,
                        onValueChange = { manualLine = it },
                        label = { Text(stringResource(R.string.manual_entry_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.assign_category_title), style = MaterialTheme.typography.labelMedium)
                    categories.forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    manualCategoryId = c.id
                                },
                        ) {
                            Text(
                                if (manualCategoryId == c.id) "● " else "○ ",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(c.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cat = manualCategoryId.takeIf { it > 0L }
                        viewModel.insertManualLine(manualLine, cat) { ok ->
                            if (ok) {
                                manualOpen = false
                                manualLine = ""
                                manualCategoryId = -1L
                            }
                        }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { manualOpen = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (assignOpen && assignTxnId > 0) {
        AlertDialog(
            onDismissRequest = { assignOpen = false },
            title = { Text(stringResource(R.string.assign_category_title)) },
            text = {
                Column {
                    categories.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { assignCatId = c.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (assignCatId == c.id) "● " else "○ ")
                            Text(c.name)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = createRule, onCheckedChange = { createRule = it })
                        Text(stringResource(R.string.rule_create_future))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backApply, onCheckedChange = { backApply = it })
                        Text(stringResource(R.string.rule_back_apply_month))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (assignCatId > 0) {
                            viewModel.assignCategory(assignTxnId, assignCatId, createRule, backApply)
                            assignOpen = false
                        }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { assignOpen = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
) {
    val dateStr = remember(transaction.dateEpochDay) {
        LocalDate.ofEpochDay(transaction.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.merchant, style = MaterialTheme.typography.titleMedium)
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                if (transaction.status == TxStatus.NEEDS_REVIEW) {
                    Text(stringResource(R.string.needs_review_badge), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                formatJodFromMilli(transaction.amountMilliJod) + " JOD",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
