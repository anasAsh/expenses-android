package com.anasexpenses.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TransactionEntity): Long

    /** Narrow duplicate candidates before Kotlin-side similarity + time window (PRD §4.2.2). */
    @Query(
        """
        SELECT * FROM transactions
        WHERE amount_milli_jod = :amountMilliJod AND date_epoch_day = :dateEpochDay
        """,
    )
    suspend fun findSameDayAndAmount(
        amountMilliJod: Long,
        dateEpochDay: Long,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE date_epoch_day >= :startEpochDay AND date_epoch_day <= :endEpochDay
        ORDER BY date_epoch_day DESC, time_second_of_day DESC
        """,
    )
    fun observeBetweenDays(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE status = 'needs_review'
        """,
    )
    fun observeNeedsReviewCount(): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(
          CASE WHEN status = 'dismissed' THEN 0
               WHEN is_refund = 1 THEN -amount_milli_jod
               ELSE amount_milli_jod END
        ), 0)
        FROM transactions
        WHERE category_id = :categoryId
          AND date_epoch_day >= :startEpochDay AND date_epoch_day <= :endEpochDay
        """,
    )
    suspend fun sumSignedMilliJodForCategoryInRange(
        categoryId: Long,
        startEpochDay: Long,
        endEpochDay: Long,
    ): Long?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE dedup_hash = :hash LIMIT 1")
    suspend fun findByDedupHash(hash: String): TransactionEntity?

    @Query(
        """
        UPDATE transactions SET category_id = :catId, updated_at_epoch_millis = :now
        WHERE normalized_merchant_token = :token AND category_id IS NULL
          AND date_epoch_day >= :startDay AND date_epoch_day <= :endDay
        """,
    )
    suspend fun backApplyCategoryForToken(
        token: String,
        catId: Long,
        startDay: Long,
        endDay: Long,
        now: Long,
    ): Int
}
