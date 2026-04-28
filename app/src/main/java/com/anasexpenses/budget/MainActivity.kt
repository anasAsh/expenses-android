package com.anasexpenses.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anasexpenses.budget.ui.home.HomeScreen
import com.anasexpenses.budget.ui.navigation.Route
import com.anasexpenses.budget.ui.permissions.PostNotificationsPermissionEffect
import com.anasexpenses.budget.ui.settings.SettingsScreen
import com.anasexpenses.budget.ui.transactions.TransactionEditScreen
import com.anasexpenses.budget.ui.transactions.TransactionsScreen
import com.anasexpenses.budget.ui.onboarding.OnboardingScreen
import com.anasexpenses.budget.ui.root.RootUiState
import com.anasexpenses.budget.ui.root.RootViewModel
import com.anasexpenses.budget.ui.theme.BudgetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTheme {
                BudgetAppEntry()
            }
        }
    }
}

@Composable
private fun BudgetAppEntry() {
    val rootVm: RootViewModel = hiltViewModel()
    val state by rootVm.uiState.collectAsStateWithLifecycle()
    when (state) {
        RootUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        RootUiState.NeedsOnboarding -> OnboardingScreen()
        RootUiState.Ready -> {
            PostNotificationsPermissionEffect()
            BudgetRootScaffold()
        }
    }
}

@Composable
private fun BudgetRootScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) },
                    selected = currentDestination?.hierarchy?.any { it.route == Route.Home.route } == true,
                    onClick = {
                        navController.navigate(Route.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_transactions)) },
                    selected = currentDestination?.hierarchy?.any { it.route == Route.Transactions.route } == true,
                    onClick = {
                        navController.navigate(Route.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    selected = currentDestination?.hierarchy?.any { it.route == Route.Settings.route } == true,
                    onClick = {
                        navController.navigate(Route.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Route.Home.route) { HomeScreen() }
            composable(Route.Transactions.route) {
                TransactionsScreen(
                    onEditTransaction = { id ->
                        navController.navigate(Route.TransactionEdit.routeWithArgs(id))
                    },
                )
            }
            composable(
                route = Route.TransactionEdit.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                ),
            ) {
                TransactionEditScreen(onClose = { navController.popBackStack() })
            }
            composable(Route.Settings.route) { SettingsScreen() }
        }
    }
}
