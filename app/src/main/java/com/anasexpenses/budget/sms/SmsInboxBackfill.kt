package com.anasexpenses.budget.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Optional 60-day inbox scan (PRD §4.0) — READ_SMS required. */
@Singleton
class SmsInboxBackfill @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun collectRecentBankBodies(maxDays: Long = 60): List<String> {
        val resolver: ContentResolver = context.contentResolver
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxDays)
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        val cursor: Cursor? = resolver.query(
            uri,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(cutoff.toString()),
            "${Telephony.Sms.DATE} DESC",
        )
        val out = ArrayList<String>()
        cursor?.use { c ->
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (c.moveToNext()) {
                val body = c.getString(bodyIdx) ?: continue
                if (ArabBankSmsFilter.likelyArabBankTrx(body)) {
                    out.add(body)
                }
            }
        }
        return out
    }
}
