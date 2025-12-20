package com.example.isro_app.mqtt

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

fun downloadAttachment(
    context: Context,
    url: String,
    filename: String
) {
    try {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Downloading attachment")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            // ✅ Save to specific folder
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "ISRO_ATTACHMENTS/$filename"
            )

            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        Toast.makeText(
            context,
            "Downloading $filename",
            Toast.LENGTH_SHORT
        ).show()

    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Download failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

