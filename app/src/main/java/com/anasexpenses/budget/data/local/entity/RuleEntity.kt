package com.anasexpenses.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rules",
    indices = [Index(value = ["merchant_token"], unique = true)],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "merchant_token") val merchantToken: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val source: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
)
