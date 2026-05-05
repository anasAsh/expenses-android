package com.anasexpenses.budget.ui.category

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
import com.anasexpenses.budget.domain.money.formatJodFromMilli
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }

    when (val state = uiState) {
        CategoryEditUiState.Loading -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }
        CategoryEditUiState.NotFound -> {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.category_edit_title)) },
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
                Text(
                    stringResource(R.string.category_edit_not_found),
                    modifier = Modifier.padding(padding).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return
        }
        is CategoryEditUiState.Ready -> {
            CategoryEditForm(
                modifier = modifier,
                initial = state.entity,
                monthFormatter = monthFormatter,
                viewModel = viewModel,
                onClose = onClose,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditForm(
    modifier: Modifier,
    initial: CategoryEntity,
    monthFormatter: DateTimeFormatter,
    viewModel: CategoryEditViewModel,
    onClose: () -> Unit,
) {
    val monthLabel = remember(initial.month) {
        runCatching { YearMonth.parse(initial.month).format(monthFormatter) }.getOrDefault(initial.month)
    }

    var nameText by remember(initial.id) { mutableStateOf(initial.name) }
    var targetText by remember(initial.id) { mutableStateOf(formatJodFromMilli(initial.monthlyTargetMilliJod)) }
    var excluded by remember(initial.id) { mutableStateOf(initial.excludedFromSpend) }
    var nameError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }
    var duplicateError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.category_edit_title)) },
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
            Text(
                stringResource(R.string.category_edit_month_line, monthLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = nameText,
                onValueChange = {
                    nameText = it
                    duplicateError = false
                    nameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                isError = duplicateError || nameError,
                supportingText = when {
                    duplicateError -> {
                        { Text(stringResource(R.string.category_duplicate_name)) }
                    }
                    nameError -> {
                        { Text(stringResource(R.string.category_name_invalid)) }
                    }
                    else -> null
                },
            )
            OutlinedTextField(
                value = targetText,
                onValueChange = {
                    targetText = it
                    targetError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_target_jod_label)) },
                supportingText = if (targetError) {
                    { Text(stringResource(R.string.category_target_invalid)) }
                } else null,
                isError = targetError,
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.category_excluded_label),
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = excluded, onCheckedChange = { excluded = it })
            }
            Button(
                onClick = {
                    viewModel.save(nameText, targetText, excluded) { result ->
                        when (result) {
                            CategoryEditSaveResult.Success -> onClose()
                            CategoryEditSaveResult.InvalidName -> nameError = true
                            CategoryEditSaveResult.InvalidAmount -> targetError = true
                            CategoryEditSaveResult.DuplicateName -> duplicateError = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.category_edit_save))
            }
        }
    }
}
