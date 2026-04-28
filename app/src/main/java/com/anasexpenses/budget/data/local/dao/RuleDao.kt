package com.anasexpenses.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anasexpenses.budget.data.local.entity.RuleEntity

@Dao
interface RuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(entity: RuleEntity): Long

    @Query("SELECT * FROM rules WHERE merchant_token = :token LIMIT 1")
    suspend fun getByMerchantToken(token: String): RuleEntity?

    @Query("DELETE FROM rules WHERE category_id = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)
}
