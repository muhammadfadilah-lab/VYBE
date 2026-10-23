package com.vybe.music.ai

import com.example.obsidian.core.data.repository.MlTrack

/**
 * Result representing a song recommended by the AI Vibe Engine
 * with acoustic compatibility score and mood tags.
 */
data class VibeMatchResult(
    val track: MlTrack,
    val matchPercentage: Int, // e.g., 96%
    val similarityScore: Float, // 0.0f - 1.0f
    val moodTags: List<String>, // e.g., ["High Energy", "Upbeat", "Late Night"]
    val reason: String // Explanation why the AI picked this track
)

/**
 * AI Vibe Profile calculated for a given playlist.
 */
data class PlaylistVibeProfile(
    val playlistName: String,
    val songCount: Int,
    val matchedInDbCount: Int,
    val dominantVibe: String, // e.g., "Mellow Acoustic Chill", "Uptempo Energetic"
    val energyScore: Float, // 0.0 - 1.0
    val moodScore: Float, // 0.0 (Melancholic) - 1.0 (Euphoric)
    val acousticSpread: Float, // Low = Very cohesive, High = Eclectic/diverse
    val centroidVector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlaylistVibeProfile
        return playlistName == other.playlistName && centroidVector.contentEquals(other.centroidVector)
    }

    override fun hashCode(): Int {
        var result = playlistName.hashCode()
        result = 31 * result + centroidVector.contentHashCode()
        return result
    }
}
