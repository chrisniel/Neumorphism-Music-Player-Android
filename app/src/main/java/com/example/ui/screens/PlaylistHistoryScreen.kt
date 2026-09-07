package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.TrackEntity
import com.example.ui.neumorphic.NeumorphicCard
import com.example.ui.neumorphic.NeumorphicCircularButton
import com.example.ui.neumorphic.NeumorphicSegmentedPill
import com.example.ui.neumorphic.neumorphicRaised
import com.example.ui.neumorphic.neumorphicSunken
import com.example.ui.theme.NeumorphAccent
import com.example.ui.theme.NeumorphBackground
import com.example.ui.theme.NeumorphTextPrimary
import com.example.ui.theme.NeumorphTextSecondary
import com.example.ui.theme.NeumorphTextTertiary
import com.example.ui.viewmodel.MusicUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistHistoryScreen(
    state: MusicUiState,
    onTrackSelected: (TrackEntity) -> Unit,
    onScanAudio: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Tracks, 1 = Playlists, 2 = History
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val filteredTracks = remember(state.tracks, searchQuery) {
        if (searchQuery.isBlank()) state.tracks
        else state.tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeumorphicCircularButton(
                onClick = onNavigateBack,
                size = 46.dp,
                icon = Icons.Default.ArrowBack,
                contentDescription = "Back",
                testTag = "lib_btn_back"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AUDIO LIBRARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextTertiary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "${state.tracks.size} Audio Tracks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphTextSecondary
                )
            }

            NeumorphicCircularButton(
                onClick = onScanAudio,
                size = 46.dp,
                icon = Icons.Default.Refresh,
                contentDescription = "Scan MediaStore",
                testTag = "lib_btn_scan"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Section Selector Segmented Control ---
        NeumorphicSegmentedPill(
            options = listOf("All Tracks", "Playlists", "History"),
            selectedIndex = selectedSection,
            onSelect = { selectedSection = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- Status Banner if Scanning ---
        if (state.isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = NeumorphAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scanning device storage for music...",
                    fontSize = 12.sp,
                    color = NeumorphTextSecondary
                )
            }
        }

        when (selectedSection) {
            0 -> {
                // --- Search Input Box with sunken slot ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .neumorphicSunken(cornerRadius = 24.dp, depth = 3.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeumorphTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = NeumorphTextPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(NeumorphAccent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_track_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by title, artist, or album...",
                                        fontSize = 13.sp,
                                        color = NeumorphTextTertiary
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = NeumorphTextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- Tracks List ---
                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeumorphTextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No audio tracks found" else "No matching tracks",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = NeumorphTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isCurrent = state.currentTrack?.id == track.id
                            TrackCardItem(
                                track = track,
                                isCurrentlyPlaying = isCurrent && state.isPlaying,
                                isSelected = isCurrent,
                                onClick = { onTrackSelected(track) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // --- Playlists Section ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR PLAYLISTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphTextPrimary,
                        letterSpacing = 1.sp
                    )

                    NeumorphicCircularButton(
                        onClick = { showCreatePlaylistDialog = true },
                        size = 38.dp,
                        icon = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        testTag = "btn_create_playlist"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = NeumorphTextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No playlists created yet",
                                fontSize = 14.sp,
                                color = NeumorphTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .neumorphicRaised(cornerRadius = 18.dp, elevation = 4.dp)
                                    .clickable { showCreatePlaylistDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Create First Playlist", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeumorphAccent)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistCardItem(
                                playlist = playlist,
                                onDelete = { onDeletePlaylist(playlist) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // --- History Section ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT PLAYBACK HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphTextPrimary,
                        letterSpacing = 1.sp
                    )

                    if (state.history.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphAccent,
                            modifier = Modifier
                                .clickable { onClearHistory() }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = NeumorphTextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No recent playback history",
                                fontSize = 14.sp,
                                color = NeumorphTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.history, key = { it.id }) { item ->
                            val track = state.tracks.find { it.id == item.trackId }
                            if (track != null) {
                                HistoryCardItem(
                                    track = track,
                                    history = item,
                                    onClick = { onTrackSelected(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Playlist Dialog ---
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a name for your new playlist:", fontSize = 13.sp, color = NeumorphTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .neumorphicSunken(cornerRadius = 14.dp, depth = 3.dp)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = NeumorphTextPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphAccent)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = NeumorphTextSecondary)
                }
            },
            containerColor = NeumorphBackground
        )
    }
}

/**
 * Soft Card list item for a track with quick actions.
 */
@Composable
fun TrackCardItem(
    track: TrackEntity,
    isCurrentlyPlaying: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .run {
                if (isSelected) {
                    neumorphicSunken(
                        cornerRadius = 18.dp,
                        depth = 3.dp,
                        backgroundColor = Color(0xFFD4DDE8)
                    )
                } else {
                    neumorphicRaised(cornerRadius = 18.dp, elevation = 5.dp)
                }
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Music Icon Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .run {
                        if (isSelected) {
                            neumorphicRaised(cornerRadius = 21.dp, elevation = 3.dp, backgroundColor = NeumorphAccent)
                        } else {
                            neumorphicSunken(cornerRadius = 21.dp, depth = 2.dp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else NeumorphAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) NeumorphAccent else NeumorphTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    fontSize = 12.sp,
                    color = NeumorphTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatTime(track.duration),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeumorphTextTertiary
            )
        }
    }
}

@Composable
fun PlaylistCardItem(
    playlist: PlaylistEntity,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(cornerRadius = 18.dp, elevation = 5.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .neumorphicSunken(cornerRadius = 21.dp, depth = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    tint = NeumorphAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.playlistName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextPrimary
                )
                Text(
                    text = "Playlist",
                    fontSize = 12.sp,
                    color = NeumorphTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = NeumorphTextTertiary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

@Composable
fun HistoryCardItem(
    track: TrackEntity,
    history: HistoryEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(history.timestamp) {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        sdf.format(Date(history.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(cornerRadius = 18.dp, elevation = 5.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .neumorphicSunken(cornerRadius = 21.dp, depth = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = NeumorphAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • Stopped at ${formatTime(history.lastPositionMs)}",
                    fontSize = 12.sp,
                    color = NeumorphTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = NeumorphTextTertiary
            )
        }
    }
}
