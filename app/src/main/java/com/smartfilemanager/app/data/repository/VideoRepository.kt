package com.smartfilemanager.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.smartfilemanager.app.domain.model.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    private val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.RELATIVE_PATH,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT
    )

    private val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    suspend fun getAllVideos(): List<ScannedFile> = withContext(Dispatchers.IO) {
        queryVideos(selection = null, selectionArgs = null)
    }

    suspend fun getVideosInDirectory(directory: String): List<ScannedFile> = withContext(Dispatchers.IO) {
        queryVideos(
            selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            selectionArgs = arrayOf("%$directory%")
        )
    }

    private fun queryVideos(selection: String?, selectionArgs: Array<String>?): List<ScannedFile> {
        val videos = mutableListOf<ScannedFile>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val widthCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                videos.add(
                    ScannedFile(
                        id           = id,
                        uri          = uri,
                        name         = cursor.getString(nameCol) ?: "",
                        path         = cursor.getString(pathCol) ?: "",
                        sizeBytes    = cursor.getLong(sizeCol),
                        durationMs   = cursor.getLong(durationCol),
                        // MediaStore DATE_MODIFIED is in seconds; convert to millis
                        lastModified = cursor.getLong(dateCol) * 1000L,
                        width        = cursor.getInt(widthCol).takeIf { it > 0 },
                        height       = cursor.getInt(heightCol).takeIf { it > 0 }
                    )
                )
            }
        }

        return videos
    }
}
