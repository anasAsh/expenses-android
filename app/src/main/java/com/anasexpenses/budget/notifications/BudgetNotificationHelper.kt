package com.anasexpenses.budget.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun showBudgetAlert(notificationId: Int, title: String, body: String) {
        val notification: Notification = NotificationCompat.Builder(context, BudgetNotificationChannels.ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showSummary(notificationId: Int, title: String, body: String) {
        val notification: Notification = NotificationCompat.Builder(context, BudgetNotificationChannels.SUMMARY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
