package com.anasexpenses.budget.domain.category

/**
 * Starter categories for the first budget month. Targets are placeholder JOD amounts (milli-JOD);
 * users can edit on Home.
 */
object DefaultCategorySeeds {

    data class Row(
        val name: String,
        val monthlyTargetMilliJod: Long,
        val excludedFromSpend: Boolean,
    )

    val rows: List<Row> = listOf(
        Row("Rent / housing", 500_000L, excludedFromSpend = false),
        Row("Utilities & bills", 150_000L, excludedFromSpend = false),
        Row("Groceries", 300_000L, excludedFromSpend = false),
        Row("Transport", 150_000L, excludedFromSpend = false),
        Row("Eating out", 200_000L, excludedFromSpend = false),
        Row("Subscriptions", 50_000L, excludedFromSpend = false),
        Row("Healthcare & pharmacy", 75_000L, excludedFromSpend = false),
        Row("Shopping & misc", 100_000L, excludedFromSpend = false),
        Row("Transfers & savings", 650_000L, excludedFromSpend = true),
    )
}
