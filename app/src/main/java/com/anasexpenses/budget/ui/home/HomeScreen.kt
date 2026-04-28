package com.anasexpenses.budget.ui.home

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasexpenses.budget.R
import com.anasexpenses.budget.domain.money.formatJodFromMilli
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onCategoryClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val unassignedSpend by viewModel.unassignedSpendMilliJod.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }

    var addOpen by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var excluded by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    var monthPickerOpen by remember { mutableStateOf(false) }
    var deleteCategoryRow by remember { mutableStateOf<CategorySpendRow?>(null) }

    val monthChoices = remember {
        val now = YearMonth.now()
        (-24..12).map { now.plusMonths(it.toLong()) }.reversed()
    }

    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    targetError = false
                    addOpen = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_category))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                    IconButton(
                    onClick = {
                        val text = HomeShareTextBuilder.build(
                            appName = appName,
                            monthLabel = selectedMonth.format(monthFormatter),
                            rows = rows,
                            unassignedMilliJod = unassignedSpend,
                        )
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.home_share_subject))
                        }
                        context.startActivity(
                            Intent.createChooser(send, context.getString(R.string.home_share_chooser)),
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.home_share_summary),
                    )
                }
                TextButton(onClick = { monthPickerOpen = true }) {
                    Text(selectedMonth.format(monthFormatter))
                }
            }
            Text(
                text = stringResource(
                    R.string.home_unassigned_line,
                    formatJodFromMilli(unassignedSpend.coerceAtLeast(0L)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.category.id }) { row ->
                    CategoryCard(
                        row = row,
                        onClick = { onCategoryClick(row.category.id) },
                        onDeleteClick = { deleteCategoryRow = row },
                    )
                }
            }
        }
    }

    val pendingDelete = deleteCategoryRow
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteCategoryRow = null },
            title = { Text(stringResource(R.string.home_delete_category_title)) },
            text = {
                Text(
                    stringResource(R.string.home_delete_category_body, pendingDelete.category.name),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingDelete.category.id
                        deleteCategoryRow = null
                        scope.launch {
                            viewModel.deleteCategory(id) {}
                        }
                    },
                ) { Text(stringResource(R.string.home_delete_category_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryRow = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (monthPickerOpen) {
        AlertDialog(
            onDismissRequest = { monthPickerOpen = false },
            title = { Text(stringResource(R.string.home_month_picker_title)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(monthChoices, key = { it.toString() }) { ym ->
                        Text(
                            text = ym.format(monthFormatter),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.selectMonth(ym)
                                        monthPickerOpen = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { monthPickerOpen = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (addOpen) {
        AlertDialog(
            onDismissRequest = { addOpen = false },
            title = { Text(stringResource(R.string.home_add_category_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                    CategoryRowSwitch(
                        label = stringResource(R.string.category_excluded_label),
                        checked = excluded,
                        onCheckedChange = { excluded = it },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCategory(categoryName, targetText, excluded) { ok ->
                            if (ok) {
                                addOpen = false
                                categoryName = ""
                                targetText = ""
                                excluded = false
                                targetError = false
                            } else {
                                targetError = true
                            }
                        }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addOpen = false
                        categoryName = ""
                        targetText = ""
                        excluded = false
                        targetError = false
                    },
                ) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CategoryRowSwitch(
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

@Composable
private fun categoryCardContainerColor(row: CategorySpendRow): Color {
    val scheme = MaterialTheme.colorScheme
    val c = row.category
    if (c.excludedFromSpend || c.monthlyTargetMilliJod <= 0L) {
        return scheme.surfaceVariant
    }
    val target = c.monthlyTargetMilliJod
    val spent = row.spentMilliJod.coerceAtLeast(0L)
    val utilization = spent.toFloat() / target.toFloat()
    return when {
        utilization >= 1f -> scheme.errorContainer
        utilization >= 0.85f -> scheme.tertiaryContainer
        utilization >= 0.70f -> scheme.secondaryContainer
        else -> scheme.primaryContainer
    }
}

@Composable
private fun CategoryCard(
    row: CategorySpendRow,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val target = row.category.monthlyTargetMilliJod
    val spent = row.spentMilliJod.coerceAtLeast(0L)
    val progress = if (target > 0L) {
        (spent.toFloat() / target.toFloat()).coerceIn(0f, 1.5f)
    } else {
        0f
    }
    val cardColors = CardDefaults.cardColors(
        containerColor = categoryCardContainerColor(row),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                ) {
                    Text(row.category.name, style = MaterialTheme.typography.titleMedium)
                    if (row.category.excludedFromSpend) {
                        Text(
                            stringResource(R.string.category_excluded_badge),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.home_delete_category_cd),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(
                        R.string.home_spent_vs_target,
                        formatJodFromMilli(spent),
                        formatJodFromMilli(target),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!row.category.excludedFromSpend && target > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
