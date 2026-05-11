package com.anasexpenses.budget.domain.merchant

import com.anasexpenses.budget.domain.category.DefaultCategorySeeds

/**
 * Default merchant → category rules derived from a user backup, filtered to remove personal
 * buckets ("Anas", "Yahia") and [UNKNOWN_TOKEN], then mapped onto [DefaultCategorySeeds] names.
 *
 * Category names must match [DefaultCategorySeeds.rows] exactly for lookup at seed time.
 */
object MerchantRuleSeeds {

    /** Excluded from backup-derived seeding (catch-all; too broad for auto-routing). */
    const val UNKNOWN_TOKEN = "unknown"

    /**
     * Pairs of `(merchant_token, default_category_name)` where [default_category_name] is a
     * [DefaultCategorySeeds] category `name`.
     */
    val pairs: List<Pair<String, String>> = listOf(
        // Groceries
        "abuodehstores" to "Groceries",
        "abuodehstoresja" to "Groceries",
        "alhadbaabakeryandswe" to "Groceries",
        "frozenfoodhousemarj" to "Groceries",
        "ghaithbakerymarjh" to "Groceries",
        "khairatalquds" to "Groceries",
        "khairatalqudshealthy" to "Groceries",
        "olivestone" to "Groceries",
        "talabat" to "Groceries",
        // Utilities & bills
        "gas" to "Utilities & bills",
        "jordanelectricity" to "Utilities & bills",
        "selena" to "Utilities & bills",
        "watermiyahunaamman" to "Utilities & bills",
        "zain" to "Utilities & bills",
        // Healthcare & pharmacy
        "alfarahclinics" to "Healthcare & pharmacy",
        "alhadafpharmacyal" to "Healthcare & pharmacy",
        "crownpharmacyal2" to "Healthcare & pharmacy",
        "drramifarah" to "Healthcare & pharmacy",
        // Shopping & misc
        "abusarajewelrys" to "Shopping & misc",
        "americanfactoryoutlet" to "Shopping & misc",
        "bluelinesweifieh" to "Shopping & misc",
        "fintesa2" to "Shopping & misc",
        "masshawishoestore" to "Shopping & misc",
        "temucom" to "Shopping & misc",
        "abusaralera" to "Shopping & misc",
        "tirefix" to "Shopping & misc",
        // Subscriptions
        "careemplus" to "Subscriptions",
        "googlegoogleone" to "Subscriptions",
        "googleyoutube" to "Subscriptions",
        "openai" to "Subscriptions",
        // Eating out
        "cyclingjordanmec" to "Eating out",
        "maroufcafebikers" to "Eating out",
        "saisonspatisserieab" to "Eating out",
        "saisonspatisserieum" to "Eating out",
        "strawberry" to "Eating out",
        "usra" to "Eating out",
        "hotel" to "Eating out",
        "hoteldownpayment" to "Eating out",
        // Transfers & savings
        "mom" to "Transfers & savings",
        "zainab" to "Transfers & savings",
    )

    private val allowedCategoryNames: Set<String> = DefaultCategorySeeds.rows.map { it.name }.toSet()

    init {
        require(pairs.map { it.first }.toSet().size == pairs.size) { "duplicate merchant_token in MerchantRuleSeeds" }
        for ((_, cat) in pairs) {
            require(cat in allowedCategoryNames) { "unknown default category: $cat" }
        }
    }
}
