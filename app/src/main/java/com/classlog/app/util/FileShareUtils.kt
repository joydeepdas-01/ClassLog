package com.classlog.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileShareUtils {

    /**
     * Writes content to the app's cache directory under "exports/" and launches a system share chooser.
     */
    fun shareFile(
        context: Context,
        fileName: String,
        mimeType: String,
        chooserTitle: String,
        writeContent: (FileOutputStream) -> Unit
    ) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, fileName)

        FileOutputStream(targetFile).use { fos ->
            writeContent(fos)
            fos.flush()
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
