package com.anasexpenses.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anasexpenses.budget.data.local.entity.AlertEventEntity

@Dao
interface AlertEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AlertEventEntity): Long

    @Query(
        """
        SELECT COUNT(*) FROM alert_events
        WHERE category_id = :categoryId AND month = :month AND threshold_type = :thresholdType
        """,
    )
    suspend fun countFor(categoryId: Long, month: String, thresholdType: String): Int

    @Query("DELETE FROM alert_events WHERE category_id = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)
}
