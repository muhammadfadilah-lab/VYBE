package com.vybe.music.local

import android.content.ContentResolver
import android.content.Context
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAudioRepository @Inject constructor(
    private val context: Context
) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    /**
     * Scans and returns all music tracks stored on the user's device.
     * Automatically filters out files shorter than 15s (ringtones, notifications).
     */
    suspend fun getLocalAudioTracks(
        minDurationMs: Long = 15_000L,
        sortBy: SortOrder = SortOrder.TITLE_ASC
    ): List<LocalAudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalAudioTrack>()

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            }
        )

        // Only query real music files with non-zero duration
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())

        val sortOrder = when (sortBy) {
            SortOrder.TITLE_ASC -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            SortOrder.ARTIST_ASC -> "${MediaStore.Audio.Media.ARTIST} COLLATE NOCASE ASC, ${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            SortOrder.DATE_ADDED_DESC -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            SortOrder.DURATION_DESC -> "${MediaStore.Audio.Media.DURATION} DESC"
        }

        contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol).takeIf { it != "<unknown>" } ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val mimeType = cursor.getString(mimeCol) ?: "audio/*"
                val albumId = cursor.getLong(albumIdCol)
                val path = if (pathCol != -1) cursor.getString(pathCol) else null

                val contentUri = ContentUris.withAppendedId(collectionUri, id)
                val albumArtUri = LocalAudioTrack.albumArtUriFor(albumId)

                tracks.add(
                    LocalAudioTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        sizeBytes = size,
                        dateAdded = dateAdded,
                        mimeType = mimeType,
                        relativePath = path
                    )
                )
            }
        }

        tracks
    }

    /**
     * Filters tracks matching query in title, artist, or album.
     */
    suspend fun searchLocalTracks(query: String): List<LocalAudioTrack> {
        val all = getLocalAudioTracks()
        if (query.isBlank()) return all
        val lower = query.lowercase().trim()
        return all.filter {
            it.title.lowercase().contains(lower) ||
            it.artist.lowercase().contains(lower) ||
            it.album.lowercase().contains(lower)
        }
    }

    enum class SortOrder {
        TITLE_ASC,
        ARTIST_ASC,
        DATE_ADDED_DESC,
        DURATION_DESC
    }
}
