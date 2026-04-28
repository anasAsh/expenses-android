package com.anasexpenses.budget.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Transactions : Route("transactions")

    /** Transactions list filtered to one category (same month as global selection). */
    data object TransactionsForCategory : Route("transactions/category/{categoryId}") {
        fun routeWithArgs(categoryId: Long) = "transactions/category/$categoryId"
    }
    data object Settings : Route("settings")
    data object TransactionEdit : Route("transactionEdit/{id}") {
        fun routeWithArgs(id: Long) = "transactionEdit/$id"
    }
}
