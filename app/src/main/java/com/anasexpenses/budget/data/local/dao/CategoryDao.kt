package com.anasexpenses.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Update
    suspend fun update(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(entity: CategoryEntity): Long

    @Query(
        """
        SELECT COALESCE(SUM(monthly_target_milli_jod), 0) FROM categories
        WHERE month = :month AND excluded_from_spend = 0
        """,
    )
    suspend fun sumTargetsIncludedForMonth(month: String): Long

    @Query("DELETE FROM categories WHERE month = :month")
    suspend fun deleteMonth(month: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
