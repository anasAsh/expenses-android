package com.anasexpenses.budget.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anasexpenses.budget.R
import com.anasexpenses.budget.util.BudgetLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class BudgetNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val summaryPi by lazy { NotificationDeepLinks.transactionsContentIntent(context, 0x5200) }
    private val alertSinglePi by lazy { NotificationDeepLinks.transactionsContentIntent(context, 0x5201) }
    private val alertDigestPi by lazy { NotificationDeepLinks.transactionsContentIntent(context, 0x5202) }

    fun showBudgetAlert(notificationId: Int, title: String, body: String) {
        val notification: Notification = NotificationCompat.Builder(context, BudgetNotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(alertSinglePi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()
        notifySafe(notificationId, notification)
    }

    /** Merges multiple threshold lines into a single InboxStyle notification. */
    fun showBudgetAlertDigest(
        monthKey: String,
        lines: List<Pair<String, String>>,
    ) {
        if (lines.isEmpty()) return
        val inbox = NotificationCompat.InboxStyle()
            .setBigContentTitle("Budget alerts ($monthKey)")
        lines.forEach { (t, b) -> inbox.addLine("$t: $b") }
        val digestId: Int = abs("digest_$monthKey".hashCode()) and 0x5fff_ffff
        val firstTitle = lines.first().first
        val firstBody = lines.first().second
        val notif: Notification = NotificationCompat.Builder(context, BudgetNotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget alerts")
            .setContentText(firstBody)
            .setContentIntent(alertDigestPi)
            .setStyle(inbox)
            .setSubText(firstTitle)
            .setAutoCancel(true)
            .setContentInfo("${lines.size} alerts")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifySafe(digestId, notif)
    }

    fun showSummary(notificationId: Int, title: String, body: String) {
        val notification: Notification = NotificationCompat.Builder(context, BudgetNotificationChannels.SUMMARY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(summaryPi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifySafe(notificationId, notification)
    }

    private fun notifySafe(notificationId: Int, notification: Notification) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        try {
            nm.notify(notificationId, notification)
        } catch (e: SecurityException) {
            BudgetLog.v { "notify blocked: ${e.message}" }
        }
    }
}
