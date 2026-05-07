package com.anasexpenses.budget.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasexpenses.budget.R
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.domain.money.formatJodFromMilli
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    onEditTransaction: (Long) -> Unit = {},
    onNavigateUp: () -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedBudgetMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val filterCategoryId = viewModel.filterCategoryId
    val filterCategoryName =
        filterCategoryId?.let { fid -> categories.find { it.id == fid }?.name }

    var manualOpen by remember { mutableStateOf(false) }
    var manualMerchant by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("") }
    var manualCategoryId by remember { mutableLongStateOf(-1L) }
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val manualParseFailedMessage = stringResource(R.string.manual_entry_parse_failed)

    var assignOpen by remember { mutableStateOf(false) }
    var assignTxnId by remember { mutableLongStateOf(-1L) }
    var assignCatId by remember { mutableLongStateOf(-1L) }
    var createRule by remember { mutableStateOf(true) }
    var backApply by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (filterCategoryId != null) {
                TopAppBar(
                    title = {
                        Text(
                            filterCategoryName?.let {
                                stringResource(R.string.transactions_category_filter, it)
                            } ?: stringResource(R.string.transactions_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_up),
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                manualCategoryId = filterCategoryId ?: -1L
                manualMerchant = ""
                manualAmount = ""
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
            if (filterCategoryId == null) {
                Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.padding(8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { t ->
                    TransactionRow(
                        transaction = t,
                        onClick = {
                            assignTxnId = t.id
                            assignCatId = t.categoryId ?: -1L
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(
                            R.string.category_edit_month_line,
                            selectedBudgetMonth.format(monthFormatter),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = manualMerchant,
                        onValueChange = { manualMerchant = it },
                        label = { Text(stringResource(R.string.manual_entry_merchant_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = manualAmount,
                        onValueChange = { manualAmount = it },
                        label = { Text(stringResource(R.string.manual_entry_amount_label)) },
                        supportingText = { Text(stringResource(R.string.manual_entry_amount_help)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryDropdownField(
                        categories = categories,
                        selectedCategoryId = manualCategoryId,
                        onCategoryIdChange = { manualCategoryId = it },
                        label = stringResource(R.string.assign_category_title),
                        includeUnassigned = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cat = manualCategoryId.takeIf { it > 0L }
                        viewModel.insertManualEntry(manualMerchant, manualAmount, cat) { ok ->
                            if (ok) {
                                manualOpen = false
                                manualMerchant = ""
                                manualAmount = ""
                                manualCategoryId = -1L
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(message = manualParseFailedMessage)
                                }
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
                    FilledTonalButton(
                        onClick = {
                            onEditTransaction(assignTxnId)
                            assignOpen = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(stringResource(R.string.transaction_edit_from_list))
                    }
                    CategoryDropdownField(
                        categories = categories,
                        selectedCategoryId = assignCatId,
                        onCategoryIdChange = { assignCatId = it },
                        label = stringResource(R.string.assign_category_title),
                        includeUnassigned = false,
                    )
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
private fun CategoryDropdownField(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long,
    onCategoryIdChange: (Long) -> Unit,
    label: String,
    includeUnassigned: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val unassignedLabel = stringResource(R.string.transaction_unassigned)
    val pickLabel = stringResource(R.string.transactions_category_pick)
    val valueText = when {
        includeUnassigned && selectedCategoryId <= 0L -> unassignedLabel
        selectedCategoryId > 0L -> categories.find { it.id == selectedCategoryId }?.name.orEmpty()
        else -> ""
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = if (!includeUnassigned && selectedCategoryId <= 0L) {
                { Text(pickLabel) }
            } else {
                null
            },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (includeUnassigned) {
                DropdownMenuItem(
                    text = { Text(unassignedLabel) },
                    onClick = {
                        onCategoryIdChange(-1L)
                        expanded = false
                    },
                )
            }
            categories.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    onClick = {
                        onCategoryIdChange(c.id)
                        expanded = false
                    },
                )
            }
        }
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
    val needsCategory =
        transaction.categoryId == null && transaction.status != TxStatus.DISMISSED
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (needsCategory) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.merchant, style = MaterialTheme.typography.titleMedium)
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                if (needsCategory) {
                    Text(stringResource(R.string.transaction_needs_category), style = MaterialTheme.typography.labelSmall)
                }
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
