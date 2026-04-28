package com.anasexpenses.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anasexpenses.budget.data.local.entity.BankTemplateEntity

@Dao
interface BankTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BankTemplateEntity): Long

    @Query("SELECT COUNT(*) FROM bank_templates WHERE bank_id = :bankId")
    suspend fun countByBankId(bankId: String): Int

    @Query("SELECT * FROM bank_templates WHERE bank_id = :bankId AND language = :language LIMIT 1")
    suspend fun getByBankAndLanguage(bankId: String, language: String): BankTemplateEntity?
}
