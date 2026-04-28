package com.anasexpenses.budget.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Transactions : Route("transactions")
    data object Settings : Route("settings")
    data object TransactionEdit : Route("transactionEdit/{id}") {
        fun routeWithArgs(id: Long) = "transactionEdit/$id"
    }
}
