package com.anasexpenses.budget.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.anasexpenses.budget.R
import com.anasexpenses.budget.ui.navigation.Route

private const val TOTAL_STEPS = 5

@Composable
fun FirstLaunchTourOverlay(
    navController: NavController,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(step) {
        when (step) {
            0, 1, 4 -> navigateToTab(navController, Route.Home.route)
            2 -> navigateToTab(navController, Route.Transactions.route)
            3 -> navigateToTab(navController, Route.Settings.route)
        }
    }

    BackHandler { onFinish() }

    val title = when (step) {
        0 -> stringResource(R.string.first_launch_tour_step1_title)
        1 -> stringResource(R.string.first_launch_tour_step2_title)
        2 -> stringResource(R.string.first_launch_tour_step3_title)
        3 -> stringResource(R.string.first_launch_tour_step4_title)
        else -> stringResource(R.string.first_launch_tour_step5_title)
    }
    val body = when (step) {
        0 -> stringResource(R.string.first_launch_tour_step1_body)
        1 -> stringResource(R.string.first_launch_tour_step2_body)
        2 -> stringResource(R.string.first_launch_tour_step3_body)
        3 -> stringResource(R.string.first_launch_tour_step4_body)
        else -> stringResource(R.string.first_launch_tour_step5_body)
    }

    AlertDialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Column {
                Text(
                    text = stringResource(
                        R.string.first_launch_tour_step_indicator,
                        step + 1,
                        TOTAL_STEPS,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(text = title)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step < TOTAL_STEPS - 1) {
                        step++
                    } else {
                        onFinish()
                    }
                },
            ) {
                Text(
                    if (step < TOTAL_STEPS - 1) {
                        stringResource(R.string.first_launch_tour_next)
                    } else {
                        stringResource(R.string.first_launch_tour_done)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onFinish) {
                Text(stringResource(R.string.first_launch_tour_skip))
            }
        },
    )
}

private fun navigateToTab(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
