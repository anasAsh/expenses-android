package com.anasexpenses.budget.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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

    /** Flush WAL then copy DB bytes into [fileName] under a SAF folder [treeUri]. */
    fun checkpointAndCopyToTreeUri(treeUri: Uri, fileName: String) {
        val fileUri = getOrCreateDocumentUri(treeUri, fileName) ?: error("could not create backup document")
        context.contentResolver.openOutputStream(fileUri, "wt")?.use { out ->
            checkpointAndCopyTo(out)
        } ?: error("could not open backup output stream")
    }

    private fun getOrCreateDocumentUri(treeUri: Uri, fileName: String): Uri? {
        val resolver = context.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (nameIdx >= 0 && cursor.getString(nameIdx) == fileName && idIdx >= 0) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idIdx))
                }
            }
        }

        return DocumentsContract.createDocument(resolver, parent, "application/octet-stream", fileName)
    }
}
