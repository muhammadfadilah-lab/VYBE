package com.vybe.music.local

import android.content.Context
import com.example.obsidian.core.data.local.PlaylistDao
import com.example.obsidian.core.data.local.PlaylistEntity
import com.example.obsidian.core.data.local.PlaylistTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPlaylistManager @Inject constructor(
    private val context: Context,
    private val playlistDao: PlaylistDao,
    private val localAudioRepository: LocalAudioRepository
) {
    /**
     * Observes all user playlists created in the app.
     */
    fun getPlaylists(): Flow<List<PlaylistEntity>> {
        return playlistDao.getAllPlaylists()
    }

    /**
     * Creates a new local playlist in the Room database.
     */
    suspend fun createLocalPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(
            id = 0L,
            name = name,
            isLikedSongs = false,
            isDownloadFolder = false,
            coverArtUrl = null,
            trackCount = 0,
            spotifyPlaylistId = null,
            syncState = "LOCAL",
            syncProgress = 0,
            syncTotal = 0,
            lastPlayedTimestamp = System.currentTimeMillis(),
            customCoverUri = null
        )
        playlistDao.insertPlaylist(playlist)
    }

    /**
     * Adds a local device audio track into an existing playlist.
     */
    suspend fun addLocalTrackToPlaylist(
        playlistId: Long,
        track: LocalAudioTrack
    ): Long = withContext(Dispatchers.IO) {
        val playlistTrack = PlaylistTrackEntity(
            id = 0L,
            playlistId = playlistId,
            videoId = "local_${track.id}",
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.albumArtUri?.toString() ?: "",
            timestamp = System.currentTimeMillis()
        )
        val id = playlistDao.insertTrack(playlistTrack)
        playlistDao.updateTrackCount(playlistId)
        id
    }

    /**
     * Adds multiple local device audio tracks into an existing playlist.
     */
    suspend fun addLocalTracksToPlaylist(
        playlistId: Long,
        tracks: List<LocalAudioTrack>
    ) = withContext(Dispatchers.IO) {
        val entities = tracks.map { track ->
            PlaylistTrackEntity(
                id = 0L,
                playlistId = playlistId,
                videoId = "local_${track.id}",
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.albumArtUri?.toString() ?: "",
                timestamp = System.currentTimeMillis()
            )
        }
        playlistDao.insertTracks(entities)
        playlistDao.updateTrackCount(playlistId)
    }

    /**
     * Exports a playlist as a standard .m3u8 playlist file.
     */
    suspend fun exportPlaylistToM3u(playlistId: Long, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val tracks = playlistDao.getTracksForPlaylistSync(playlistId)
            val allLocal = localAudioRepository.getLocalAudioTracks().associateBy { "local_${it.id}" }

            outputFile.bufferedWriter().use { writer ->
                writer.write("#EXTM3U\n")
                for (t in tracks) {
                    val local = allLocal[t.videoId]
                    writer.write("#EXTINF:${(local?.durationMs ?: 0L) / 1000},${t.artist} - ${t.title}\n")
                    writer.write("${local?.contentUri ?: t.videoId}\n")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
