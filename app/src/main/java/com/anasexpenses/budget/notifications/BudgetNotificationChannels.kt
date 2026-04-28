package com.anasexpenses.budget.notifications

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

object BudgetNotificationChannels {
    const val ALERTS = "budget_alerts"
    const val SUMMARY = "budget_summary"

    fun ensureCreated(app: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = app.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(ALERTS, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(SUMMARY, "Monthly summary", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }
}
