package com.anasexpenses.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["month"])],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Calendar month key `YYYY-MM`. */
    val month: String,
    val name: String,
    /** Monthly target in thousandths of JOD. */
    @ColumnInfo(name = "monthly_target_milli_jod") val monthlyTargetMilliJod: Long,
    @ColumnInfo(name = "excluded_from_spend") val excludedFromSpend: Boolean = false,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
)
