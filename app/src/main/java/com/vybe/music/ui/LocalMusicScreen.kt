package com.vybe.music.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.obsidian.core.playback.PlaybackController
import com.vybe.music.local.LocalAudioRepository
import com.vybe.music.local.LocalAudioTrack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicScreen(
    localAudioRepository: LocalAudioRepository,
    playbackController: PlaybackController,
    onTrackSelect: (LocalAudioTrack) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<LocalAudioTrack>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            scope.launch {
                isLoading = true
                tracks = localAudioRepository.getLocalAudioTracks()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionToRequest)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    scope.launch {
                        tracks = localAudioRepository.searchLocalTracks(query)
                    }
                },
                placeholder = { Text("Search local songs, artists, albums...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (!hasPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Storage Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "VYBE needs permission to scan and play music from your device storage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                        Button(onClick = { launcher.launch(permissionToRequest) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No local music found on device", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tracks, key = { it.id }) { track ->
                        LocalTrackRow(
                            track = track,
                            onClick = {
                                onTrackSelect(track)
                                playbackController.play(track.toMediaItem())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocalTrackRow(
    track: LocalAudioTrack,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (track.albumArtUri != null) {
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = track.formattedDuration(),
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
