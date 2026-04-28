package com.anasexpenses.budget.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.PendingIntentCompat

object NotificationDeepLinks {
    const val U_TRANSACTIONS: String = "anasexpenses://app/transactions"
    const val U_HOME: String = "anasexpenses://app/home"

    private fun viewIntent(ctx: Context, deepLink: String, requestCode: Int): PendingIntent {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            .setPackage(ctx.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return requireNotNull(
            PendingIntentCompat.getActivity(
                ctx,
                requestCode,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            ),
        )
    }

    fun transactionsContentIntent(ctx: Context, requestCode: Int) =
        viewIntent(ctx, U_TRANSACTIONS, requestCode)

    fun homeContentIntent(ctx: Context, requestCode: Int) =
        viewIntent(ctx, U_HOME, requestCode)
}
