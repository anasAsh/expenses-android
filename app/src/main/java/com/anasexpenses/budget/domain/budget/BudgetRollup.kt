package com.anasexpenses.budget.domain.budget

import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus

/** Spend contribution for totals & alerts (PRD §4.1 / §5). */
object BudgetRollup {
    fun signedAmountMilliJod(t: TransactionEntity): Long =
        when {
            t.status == TxStatus.DISMISSED -> 0L
            t.isRefund -> -t.amountMilliJod
            else -> t.amountMilliJod
        }
}
