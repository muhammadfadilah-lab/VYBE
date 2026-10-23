package com.vybe.music.local

import android.content.ContentUris
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Model representing an audio file stored on local device storage.
 */
data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val sizeBytes: Long,
    val dateAdded: Long,
    val mimeType: String,
    val relativePath: String?
) {
    /**
     * Converts this local track into an AndroidX Media3 MediaItem
     * for direct, seamless playback via ExoPlayer.
     */
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId("local_$id")
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Formats duration in mm:ss format.
     */
    fun formattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    companion object {
        fun albumArtUriFor(albumId: Long): Uri {
            return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
        }
    }
}
