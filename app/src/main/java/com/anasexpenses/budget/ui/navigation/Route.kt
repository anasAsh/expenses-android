package com.anasexpenses.budget.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Transactions : Route("transactions")
}
