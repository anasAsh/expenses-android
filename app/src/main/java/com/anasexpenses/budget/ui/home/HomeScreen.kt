package com.anasexpenses.budget.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anasexpenses.budget.R
import com.anasexpenses.budget.domain.money.formatJodFromMilli

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    var addOpen by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var excluded by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

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
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.category.id }) { row ->
                    CategoryCard(row = row)
                }
            }
        }
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
private fun CategoryCard(row: CategorySpendRow) {
    val target = row.category.monthlyTargetMilliJod
    val spent = row.spentMilliJod.coerceAtLeast(0L)
    val progress = if (target > 0L) {
        (spent.toFloat() / target.toFloat()).coerceIn(0f, 1.5f)
    } else {
        0f
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.category.name, style = MaterialTheme.typography.titleMedium)
                if (row.category.excludedFromSpend) {
                    Text(
                        stringResource(R.string.category_excluded_badge),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
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
