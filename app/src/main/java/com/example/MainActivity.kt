package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.neumorphic.NeumorphicCircularButton
import com.example.ui.neumorphic.neumorphicRaised
import com.example.ui.neumorphic.neumorphicSunken
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistHistoryScreen
import com.example.ui.theme.NeumorphAccent
import com.example.ui.theme.NeumorphBackground
import com.example.ui.theme.NeumorphTextPrimary
import com.example.ui.theme.NeumorphTextSecondary
import com.example.ui.theme.NeumorphTextTertiary
import com.example.ui.theme.NeumorphicMusicTheme
import com.example.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeumorphicMusicTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Permission launcher for audio reading & notifications
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.scanDeviceMusic()
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                        }
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NeumorphBackground)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = NeumorphBackground,
                    bottomBar = {
                        // Show bottom mini-player when on Equalizer or Library tab
                        if (uiState.activeTab != 0 && uiState.currentTrack != null) {
                            BottomMiniPlayer(
                                title = uiState.currentTrack?.title ?: "",
                                artist = uiState.currentTrack?.artist ?: "",
                                isPlaying = uiState.isPlaying,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onOpenNowPlaying = { viewModel.setActiveTab(0) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = uiState.activeTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_animation"
                        ) { tabIndex ->
                            when (tabIndex) {
                                0 -> NowPlayingScreen(
                                    state = uiState,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSkipNext = { viewModel.skipNext() },
                                    onSkipPrevious = { viewModel.skipPrevious() },
                                    onSeekTo = { ratio -> viewModel.seekToRatio(ratio) },
                                    onVolumeChange = { vol -> viewModel.setVolume(vol) },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onToggleRepeat = { viewModel.toggleRepeat() },
                                    onNavigateToEq = { viewModel.setActiveTab(1) },
                                    onNavigateToLibrary = { viewModel.setActiveTab(2) }
                                )
                                1 -> EqualizerScreen(
                                    state = uiState,
                                    onBassChange = { ratio -> viewModel.setBass(ratio) },
                                    onMidChange = { ratio -> viewModel.setMid(ratio) },
                                    onTrebleChange = { ratio -> viewModel.setTreble(ratio) },
                                    onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                                    onNavigateBack = { viewModel.setActiveTab(0) }
                                )
                                2 -> PlaylistHistoryScreen(
                                    state = uiState,
                                    onTrackSelected = { track ->
                                        viewModel.playTrack(track)
                                        viewModel.setActiveTab(0)
                                    },
                                    onScanAudio = { viewModel.scanDeviceMusic() },
                                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                    onDeletePlaylist = { p -> viewModel.deletePlaylist(p) },
                                    onClearHistory = { viewModel.clearHistory() },
                                    onNavigateBack = { viewModel.setActiveTab(0) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom persistent mini-player with soft neumorphic elevation.
 */
@Composable
fun BottomMiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .neumorphicRaised(cornerRadius = 24.dp, elevation = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenNowPlaying
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vinyl mini indicator
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .run {
                        if (isPlaying) {
                            neumorphicRaised(cornerRadius = 19.dp, elevation = 2.dp, backgroundColor = NeumorphAccent)
                        } else {
                            neumorphicSunken(cornerRadius = 19.dp, depth = 2.dp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = if (isPlaying) Color.White else NeumorphAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    fontSize = 11.sp,
                    color = NeumorphTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            NeumorphicCircularButton(
                onClick = onTogglePlayPause,
                size = 38.dp,
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                isActive = isPlaying,
                contentDescription = "Play/Pause"
            )

            Spacer(modifier = Modifier.width(8.dp))

            NeumorphicCircularButton(
                onClick = onSkipNext,
                size = 38.dp,
                icon = Icons.Default.SkipNext,
                contentDescription = "Next"
            )
        }
    }
}
