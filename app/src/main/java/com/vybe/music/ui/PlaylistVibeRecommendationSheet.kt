package com.vybe.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.obsidian.core.data.local.PlaylistDao
import com.example.obsidian.core.data.local.PlaylistTrackEntity
import com.vybe.music.ai.PlaylistVibeAiEngine
import com.vybe.music.ai.PlaylistVibeProfile
import com.vybe.music.ai.VibeMatchResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistVibeRecommendationSheet(
    playlistId: Long,
    vibeAiEngine: PlaylistVibeAiEngine,
    playlistDao: PlaylistDao,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<PlaylistVibeProfile?>(null) }
    var recommendations by remember { mutableStateOf<List<VibeMatchResult>>(emptyList()) }
    var selectedMode by remember { mutableStateOf(PlaylistVibeAiEngine.RecommendationMode.BALANCED) }
    var addedTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playlistId, selectedMode) {
        isLoading = true
        profile = vibeAiEngine.analyzePlaylistVibe(playlistId)
        recommendations = vibeAiEngine.recommendForPlaylist(
            playlistId = playlistId,
            limit = 20,
            mode = selectedMode
        )
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "VYBE AI Vibe Match",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Music recommendations tuned to this playlist's acoustic profile",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Playlist Vibe Profile Banner
            profile?.let { p ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Acoustic Vibe: ",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = p.dominantVibe,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Energy: ${(p.energyScore * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                            Text("Mood: ${(p.moodScore * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                            Text("Cohesion: High", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Mode Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMode == PlaylistVibeAiEngine.RecommendationMode.STRICT_MATCH,
                    onClick = { selectedMode = PlaylistVibeAiEngine.RecommendationMode.STRICT_MATCH },
                    label = { Text("Strict Vibe", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedMode == PlaylistVibeAiEngine.RecommendationMode.BALANCED,
                    onClick = { selectedMode = PlaylistVibeAiEngine.RecommendationMode.BALANCED },
                    label = { Text("Balanced", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedMode == PlaylistVibeAiEngine.RecommendationMode.VIBE_DISCOVERY,
                    onClick = { selectedMode = PlaylistVibeAiEngine.RecommendationMode.VIBE_DISCOVERY },
                    label = { Text("Discovery", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (recommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add more songs to the playlist for AI to analyze its vibe!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recommendations) { item ->
                        val isAdded = addedTrackIds.contains(item.track.id)
                        VibeRecommendationRow(
                            item = item,
                            isAdded = isAdded,
                            onAddClick = {
                                scope.launch {
                                    val entity = PlaylistTrackEntity(
                                        id = 0L,
                                        playlistId = playlistId,
                                        videoId = item.track.id,
                                        title = item.track.name ?: "Unknown",
                                        artist = item.track.artists ?: "Unknown Artist",
                                        thumbnailUrl = "",
                                        timestamp = System.currentTimeMillis()
                                    )
                                    playlistDao.insertTrack(entity)
                                    playlistDao.updateTrackCount(playlistId)
                                    addedTrackIds = addedTrackIds + item.track.id
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // One-tap Add All button
                Button(
                    onClick = {
                        scope.launch {
                            val newTracks = recommendations.filterNot { addedTrackIds.contains(it.track.id) }
                            val entities = newTracks.map {
                                PlaylistTrackEntity(
                                    id = 0L,
                                    playlistId = playlistId,
                                    videoId = it.track.id,
                                    title = it.track.name ?: "Unknown",
                                    artist = it.track.artists ?: "Unknown Artist",
                                    thumbnailUrl = "",
                                    timestamp = System.currentTimeMillis()
                                )
                            }
                            playlistDao.insertTracks(entities)
                            playlistDao.updateTrackCount(playlistId)
                            addedTrackIds = addedTrackIds + recommendations.map { it.track.id }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Top Matches to Playlist")
                }
            }
        }
    }
}

@Composable
fun VibeRecommendationRow(
    item: VibeMatchResult,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vibe Match Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.matchPercentage}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.track.name ?: "Unknown Song",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.track.artists ?: "Unknown Artist",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onAddClick,
                enabled = !isAdded
            ) {
                if (isAdded) {
                    Icon(Icons.Rounded.Check, contentDescription = "Added", tint = Color.Green)
                } else {
                    Icon(Icons.Rounded.Add, contentDescription = "Add to Playlist")
                }
            }
        }
    }
}
