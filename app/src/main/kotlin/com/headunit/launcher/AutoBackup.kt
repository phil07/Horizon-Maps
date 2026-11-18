package com.headunit.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONObject

/**
 * The daily backup: once a day, while the app is running, the settings, saved places and GPX tracks (as the Sichern button writes
 * them) go into a folder you chose. It is one file, written over the one before, so only the latest backup is kept.
 * The folder is read and written through the system's folder access (no extra permission), and its key is never backed up.
 */
object AutoBackup {
    const val FILE_NAME = "horizon-nav-auto-backup.json"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** The chosen folder (as the folder picker gives it), or "" when there is none. */
    fun folder(prefs: Prefs): String = prefs.s(PreferenceKeys.AUTO_BACKUP_FOLDER, "")

    /** Keeps the folder permission the picker grants, so it still works after a restart. */
    fun choose(ctx: Context, uri: Uri, prefs: Prefs) {
        try {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (e: Exception) { /* the picker may not allow it: the folder still works until the app ends */ }
        prefs.put(PreferenceKeys.AUTO_BACKUP_FOLDER, uri.toString())
        prefs.put(PreferenceKeys.AUTO_BACKUP_LAST, "0")
    }

    fun clear(prefs: Prefs) {
        prefs.put(PreferenceKeys.AUTO_BACKUP_FOLDER, "")
        prefs.put(PreferenceKeys.AUTO_BACKUP_LAST, "0")
    }

    /** When the last backup was written (milliseconds since 1970), or 0 when there is none. */
    fun lastMillis(prefs: Prefs): Long = prefs.s(PreferenceKeys.AUTO_BACKUP_LAST, "0").toLongOrNull() ?: 0L

    /**
     * Writes the backup when a folder is chosen and the last one is a day old or more. Call it off the main thread.
     * True when a new file was written.
     */
    fun runIfDue(ctx: Context, prefs: Prefs): Boolean {
        val folder = folder(prefs).takeIf { it.isNotBlank() } ?: return false
        val now = System.currentTimeMillis()
        if (now - lastMillis(prefs) < DAY_MS) return false
        return try {
            val tree = Uri.parse(folder)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            // the last file goes first, so the folder keeps only the newest backup
            findChild(ctx, tree, FILE_NAME)?.let { DocumentsContract.deleteDocument(ctx.contentResolver, it) }
            val file = DocumentsContract.createDocument(ctx.contentResolver, parent, "application/json", FILE_NAME) ?: return false
            val backup = JSONObject(prefs.exportJson()).put("gpx_tracks", Gpx.backupArray(ctx))
            ctx.contentResolver.openOutputStream(file, "wt")?.use { it.write(backup.toString(2).toByteArray()) } ?: return false
            prefs.put(PreferenceKeys.AUTO_BACKUP_LAST, now.toString())
            true
        } catch (e: Exception) { false }
    }

    private fun findChild(ctx: Context, tree: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        ctx.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
        }
        return null
    }
}
