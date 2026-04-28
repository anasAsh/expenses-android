package com.anasexpenses.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alert_events",
    indices = [
        Index(
            value = ["category_id", "month", "threshold_type"],
            unique = true,
        ),
    ],
)
data class AlertEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val month: String,
    @ColumnInfo(name = "threshold_type") val thresholdType: String,
    @ColumnInfo(name = "sent_at_epoch_millis") val sentAtEpochMillis: Long,
)
