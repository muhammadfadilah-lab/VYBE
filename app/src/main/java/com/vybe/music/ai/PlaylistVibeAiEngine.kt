package com.vybe.music.ai

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.obsidian.core.data.local.PlaylistDao
import com.example.obsidian.core.data.local.PlaylistTrackEntity
import com.example.obsidian.core.data.repository.MlTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class PlaylistVibeAiEngine @Inject constructor(
    private val context: Context,
    private val playlistDao: PlaylistDao
) {
    private var cachedTracks: List<MlTrack>? = null
    private val dbFile: File by lazy { initDatabase() }

    private fun initDatabase(): File {
        val target = File(context.filesDir, "recommendations.db")
        if (!target.exists()) {
            context.assets.open("recommendations.db").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return target
    }

    /**
     * Loads the 170k ML track database into memory for rapid vector similarity calculations.
     */
    private fun loadTracks(): List<MlTrack> {
        cachedTracks?.let { return it }
        val list = mutableListOf<MlTrack>()
        try {
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            db.rawQuery("SELECT id, name, artists, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15 FROM tracks", null).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val artistsIdx = cursor.getColumnIndexOrThrow("artists")

                while (cursor.moveToNext()) {
                    val vector = FloatArray(15) { i -> cursor.getFloat(i + 3) }
                    val id = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx)
                    val artists = cursor.getString(artistsIdx)
                    list.add(MlTrack(id, name, artists, vector))
                }
            }
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedTracks = list
        return list
    }

    /**
     * Searches a single track in the vector DB by title/artist keywords.
     */
    private fun findTrackVector(title: String, artist: String? = null): MlTrack? {
        val cleanTitle = title.substringBefore(" (")
            .substringBefore(" [")
            .substringBefore(" - ")
            .trim()
            .lowercase()

        try {
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val query = if (artist != null && artist.isNotBlank()) {
                "SELECT id, name, artists, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15 FROM tracks WHERE LOWER(name) LIKE ? OR (LOWER(name) LIKE ? AND LOWER(artists) LIKE ?) LIMIT 1"
            } else {
                "SELECT id, name, artists, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15 FROM tracks WHERE LOWER(name) LIKE ? LIMIT 1"
            }

            val args = if (artist != null && artist.isNotBlank()) {
                arrayOf("$cleanTitle%", "%$cleanTitle%", "%${artist.lowercase()}%")
            } else {
                arrayOf("$cleanTitle%")
            }

            db.rawQuery(query, args).use { cursor ->
                if (cursor.moveToFirst()) {
                    val vector = FloatArray(15) { i -> cursor.getFloat(i + 3) }
                    val track = MlTrack(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        vector
                    )
                    db.close()
                    return track
                }
            }
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Analyzes any playlist and generates an AI Vibe Profile.
     */
    suspend fun analyzePlaylistVibe(playlistId: Long): PlaylistVibeProfile? = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistByIdSync(playlistId) ?: return@withContext null
        val playlistTracks = playlistDao.getTracksForPlaylistSync(playlistId)
        if (playlistTracks.isEmpty()) return@withContext null

        val matchedVectors = mutableListOf<FloatArray>()
        for (track in playlistTracks) {
            val mlTrack = findTrackVector(track.title, track.artist)
            if (mlTrack != null) {
                matchedVectors.add(mlTrack.vector)
            }
        }

        if (matchedVectors.isEmpty()) return@withContext null

        // Calculate 15-dimensional Centroid
        val centroid = FloatArray(15)
        for (vec in matchedVectors) {
            for (i in 0 until 15) {
                centroid[i] += vec[i]
            }
        }
        for (i in 0 until 15) {
            centroid[i] /= matchedVectors.size.toFloat()
        }

        // Calculate acoustic dispersion (cohesiveness)
        var totalDistanceSq = 0f
        for (vec in matchedVectors) {
            var distSq = 0f
            for (i in 0 until 15) {
                val diff = vec[i] - centroid[i]
                distSq += diff * diff
            }
            totalDistanceSq += distSq
        }
        val dispersion = sqrt(totalDistanceSq / matchedVectors.size)

        // Derive mood heuristics from latent dimensions
        val energy = ((centroid.getOrNull(0) ?: 0f) + 3f).coerceIn(0f, 6f) / 6f
        val valence = ((centroid.getOrNull(1) ?: 0f) + 3f).coerceIn(0f, 6f) / 6f

        val dominantVibe = when {
            energy > 0.65f && valence > 0.6f -> "High-Energy Feel Good Anthem"
            energy > 0.65f && valence <= 0.6f -> "Intense & Driving Rhythms"
            energy <= 0.45f && valence <= 0.45f -> "Moody Late-Night Introspective"
            energy <= 0.45f && valence > 0.45f -> "Mellow Warm Acoustic Chill"
            else -> "Balanced Modern Groove"
        }

        PlaylistVibeProfile(
            playlistName = playlist.name,
            songCount = playlistTracks.size,
            matchedInDbCount = matchedVectors.size,
            dominantVibe = dominantVibe,
            energyScore = energy,
            moodScore = valence,
            acousticSpread = dispersion,
            centroidVector = centroid
        )
    }

    /**
     * Generates song recommendations that fit the exact vibe of the given playlist.
     */
    suspend fun recommendForPlaylist(
        playlistId: Long,
        limit: Int = 20,
        mode: RecommendationMode = RecommendationMode.BALANCED
    ): List<VibeMatchResult> = withContext(Dispatchers.IO) {
        val allCatalog = loadTracks()
        if (allCatalog.isEmpty()) return@withContext emptyList()

        val playlistTracks = playlistDao.getTracksForPlaylistSync(playlistId)
        val existingTitles = playlistTracks.map { it.title.lowercase().trim() }.toSet()

        val profile = analyzePlaylistVibe(playlistId)
        val centroid = profile?.centroidVector ?: return@withContext emptyList()

        // Norm of Centroid
        var centroidNormSq = 0f
        for (c in centroid) centroidNormSq += c * c
        val centroidNorm = sqrt(centroidNormSq)
        if (centroidNorm <= 0f) return@withContext emptyList()

        // Score each candidate track by Cosine Similarity
        val scoredList = mutableListOf<Pair<MlTrack, Float>>()
        for (candidate in allCatalog) {
            val candTitle = candidate.name?.lowercase()?.trim() ?: continue
            if (existingTitles.contains(candTitle)) continue

            val vec = candidate.vector
            var dot = 0f
            var candNormSq = 0f
            for (i in 0 until 15) {
                dot += centroid[i] * vec[i]
                candNormSq += vec[i] * vec[i]
            }
            val candNorm = sqrt(candNormSq)
            if (candNorm > 0f) {
                val cosSim = (dot / (centroidNorm * candNorm)).coerceIn(-1f, 1f)
                scoredList.add(candidate to cosSim)
            }
        }

        // Filter and sort based on mode
        val sorted = when (mode) {
            RecommendationMode.STRICT_MATCH -> {
                scoredList.filter { it.second >= 0.70f }.sortedByDescending { it.second }
            }
            RecommendationMode.BALANCED -> {
                scoredList.sortedByDescending { it.second }
            }
            RecommendationMode.VIBE_DISCOVERY -> {
                // Select tracks with moderate similarity to introduce musical variety without drifting
                scoredList.filter { it.second in 0.50f..0.85f }.shuffled()
            }
        }

        sorted.take(limit).map { (track, sim) ->
            val matchPct = (((sim + 1f) / 2f) * 100).toInt().coerceIn(50, 99)
            val tags = mutableListOf<String>()
            if (sim > 0.85f) tags.add("Exact Vibe")
            if (profile.energyScore > 0.6f) tags.add("High Energy") else tags.add("Chill")
            if (profile.moodScore > 0.5f) tags.add("Upbeat") else tags.add("Deep")

            VibeMatchResult(
                track = track,
                matchPercentage = matchPct,
                similarityScore = sim,
                moodTags = tags,
                reason = "${matchPct}% similarity to '${profile.playlistName}' (${profile.dominantVibe})"
            )
        }
    }

    enum class RecommendationMode {
        STRICT_MATCH,
        BALANCED,
        VIBE_DISCOVERY
    }
}
