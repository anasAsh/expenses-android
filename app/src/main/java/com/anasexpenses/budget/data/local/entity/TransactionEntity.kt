package com.anasexpenses.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["category_id", "date_epoch_day"]),
        Index(value = ["normalized_merchant_token", "date_epoch_day"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Amount in thousandths of JOD (e.g. 12.410 → 12410). Always positive; use [isRefund] for sign in rollups. */
    @ColumnInfo(name = "amount_milli_jod") val amountMilliJod: Long,
    val currency: String = "JOD",
    val merchant: String,
    @ColumnInfo(name = "normalized_merchant") val normalizedMerchant: String,
    @ColumnInfo(name = "normalized_merchant_token") val normalizedMerchantToken: String,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    /** Seconds since local midnight (0 .. 86_399). */
    @ColumnInfo(name = "time_second_of_day") val timeSecondOfDay: Int,
    val source: String,
    val confidence: Float,
    val status: String,
    @ColumnInfo(name = "is_refund") val isRefund: Boolean = false,
    @ColumnInfo(name = "raw_sms") val rawSms: String?,
    @ColumnInfo(name = "card_last4") val cardLast4: String?,
    @ColumnInfo(name = "dedup_hash") val dedupHash: String?,
    @ColumnInfo(name = "bank_template_id") val bankTemplateId: Long?,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)
