package com.anasexpenses.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CategoryEntity): Long

    @Query("SELECT * FROM categories WHERE month = :month ORDER BY name ASC")
    fun observeByMonth(month: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE month = :month ORDER BY name ASC")
    suspend fun getByMonth(month: String): List<CategoryEntity>
}
