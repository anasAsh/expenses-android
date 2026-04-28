package com.anasexpenses.budget.data.export

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.anasexpenses.budget.data.local.BudgetDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BudgetDatabase,
) {

    /** Flush WAL then copy the main SQLite file bytes to [sink]. */
    fun checkpointAndCopyTo(sink: java.io.OutputStream) {
        database.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
        val dbFile = context.getDatabasePath("budget.db")
        FileInputStream(dbFile).use { input ->
            input.copyTo(sink)
        }
    }
}
