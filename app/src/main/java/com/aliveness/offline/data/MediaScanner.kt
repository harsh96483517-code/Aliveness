package com.aliveness.offline.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** Reads the device's video index through MediaStore (scoped-storage safe, no file paths needed). */
object MediaScanner {

    fun scan(context: Context): List<VideoEntity> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val useRelative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        @Suppress("DEPRECATION")
        val pathColumn = if (useRelative) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Video.Media.DATA
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            pathColumn,
        )

        val out = ArrayList<VideoEntity>()
        context.contentResolver.query(
            collection, projection, "${MediaStore.Video.Media.DURATION} > 0", null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val iName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val iW = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val iH = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val iMime = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val iPath = c.getColumnIndexOrThrow(pathColumn)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val fileName = c.getString(iName).orEmpty()
                val folder = folderOf(c.getString(iPath).orEmpty(), useRelative)
                out += VideoEntity(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    title = fileName.substringBeforeLast('.', fileName),
                    fileName = fileName,
                    folderPath = folder,
                    folderName = folder.trimEnd('/').substringAfterLast('/').ifBlank { "Internal storage" },
                    durationMs = c.getLong(iDur),
                    sizeBytes = c.getLong(iSize),
                    dateAddedSec = c.getLong(iDate),
                    width = c.getInt(iW),
                    height = c.getInt(iH),
                    mime = c.getString(iMime).orEmpty(),
                )
            }
        }
        return out
    }

    /** Normalises to a "Movies/Trips/" style folder key on every API level. */
    internal fun folderOf(raw: String, relative: Boolean): String {
        if (relative) return raw
        val parent = raw.substringBeforeLast('/', "")
        return parent.removePrefix("/storage/emulated/0/").removePrefix("/sdcard/").trimEnd('/') + "/"
    }
}
