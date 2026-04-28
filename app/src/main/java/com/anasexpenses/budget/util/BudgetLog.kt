package com.anasexpenses.budget.util

import android.util.Log
import com.anasexpenses.budget.BuildConfig

/** Never log raw SMS or card data in release builds. Debug-only verbose tag. */
object BudgetLog {
    private const val TAG = "AnasBudget"

    fun v(message: () -> String) {
        if (BuildConfig.DEBUG) Log.v(TAG, message())
    }
}
