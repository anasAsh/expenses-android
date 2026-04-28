package com.anasexpenses.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_templates",
    indices = [Index(value = ["bank_id", "language"])],
)
data class BankTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bank_id") val bankId: String,
    val language: String,
    @ColumnInfo(name = "regex_pattern") val regexPattern: String,
    val version: Int,
)
