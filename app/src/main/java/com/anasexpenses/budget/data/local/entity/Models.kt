package com.anasexpenses.budget.data.local.entity

/** Stored as string in Room — matches PRD §5. */
object TxSource {
    const val SMS = "SMS"
    const val MANUAL = "MANUAL"
}

object TxStatus {
    const val AUTO = "auto"
    const val NEEDS_REVIEW = "needs_review"
    const val MANUAL = "manual"
    const val DISMISSED = "dismissed"
}

object RuleSource {
    const val USER_CORRECTION = "user_correction"
    const val SEED = "seed"
}

object AlertThresholdType {
    const val T70 = "70"
    const val T85 = "85"
    const val T100 = "100"
    const val PREDICTIVE = "predictive"
}
