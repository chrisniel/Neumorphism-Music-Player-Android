package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.neumorphic.NeumorphicCard
import com.example.ui.neumorphic.NeumorphicCircularButton
import com.example.ui.neumorphic.NeumorphicRotaryKnob
import com.example.ui.neumorphic.NeumorphicTrackSlider
import com.example.ui.neumorphic.neumorphicRaised
import com.example.ui.neumorphic.neumorphicSunken
import com.example.ui.theme.NeumorphAccent
import com.example.ui.theme.NeumorphBackground
import com.example.ui.theme.NeumorphDivider
import com.example.ui.theme.NeumorphTextPrimary
import com.example.ui.theme.NeumorphTextSecondary
import com.example.ui.theme.NeumorphTextTertiary
import com.example.ui.viewmodel.MusicUiState
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    state: MusicUiState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onNavigateToEq: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Vinyl spinning rotation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (state.isPlaying) discRotation else 0f
    val currentTrack = state.currentTrack

    val progressRatio = if (state.durationMs > 0) {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Top Action Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeumorphicCircularButton(
                onClick = onNavigateToLibrary,
                size = 46.dp,
                icon = Icons.Default.QueueMusic,
                contentDescription = "Library",
                testTag = "btn_library"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextTertiary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = currentTrack?.album ?: "Offline Audio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            NeumorphicCircularButton(
                onClick = onNavigateToEq,
                size = 46.dp,
                icon = Icons.Default.Equalizer,
                contentDescription = "Equalizer",
                testTag = "btn_equalizer"
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Circular Neumorphic Album Frame ---
        Box(
            modifier = Modifier
                .size(230.dp)
                .neumorphicRaised(cornerRadius = 115.dp, elevation = 8.dp, isCircle = true)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Inner sunken ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .neumorphicSunken(cornerRadius = 100.dp, depth = 4.dp, isCircle = true)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Disc with concentric grooves and center blue label
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation)
                        .clip(CircleShape)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Vinyl dark body
                    drawCircle(
                        color = Color(0xFF23272F),
                        radius = radius,
                        center = center
                    )

                    // Concentric grooves
                    val grooveSteps = 8
                    for (i in 1..grooveSteps) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = radius * (0.35f + (i * 0.075f)),
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }

                    // Center label (Neumorphic Accent Blue)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeumorphAccent, Color(0xFF1D4ED8)),
                            center = center,
                            radius = radius * 0.35f
                        ),
                        radius = radius * 0.35f,
                        center = center
                    )

                    // Center spindle hole
                    drawCircle(
                        color = NeumorphBackground,
                        radius = radius * 0.08f,
                        center = center
                    )
                }

                // Music Note Center Overlay
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --- Track Title & Artist Information ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Text(
                text = currentTrack?.title ?: "Select a Track",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentTrack?.artist ?: "Unknown Artist",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- Seekbar Slider with Sunken Track & Graduation Ticks ---
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            NeumorphicTrackSlider(
                value = progressRatio,
                onValueChange = onSeekTo,
                testTag = "now_playing_seekbar"
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(state.currentPositionMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphTextSecondary
                )
                Text(
                    text = formatTime(state.durationMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphTextSecondary
                )
            }
        }

        // --- Playback Control Buttons ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button
            NeumorphicCircularButton(
                onClick = onToggleShuffle,
                size = 46.dp,
                icon = Icons.Default.Shuffle,
                isActive = state.isShuffle,
                contentDescription = "Shuffle",
                testTag = "btn_shuffle"
            )

            // Previous Button
            NeumorphicCircularButton(
                onClick = onSkipPrevious,
                size = 54.dp,
                icon = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                testTag = "btn_prev"
            )

            // Primary Play / Pause Button (Large 72dp)
            NeumorphicCircularButton(
                onClick = onTogglePlayPause,
                size = 72.dp,
                icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                isActive = state.isPlaying,
                iconTint = if (state.isPlaying) NeumorphAccent else NeumorphTextPrimary,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                testTag = "btn_play_pause"
            )

            // Next Button
            NeumorphicCircularButton(
                onClick = onSkipNext,
                size = 54.dp,
                icon = Icons.Default.SkipNext,
                contentDescription = "Next",
                testTag = "btn_next"
            )

            // Repeat Mode Button
            NeumorphicCircularButton(
                onClick = onToggleRepeat,
                size = 46.dp,
                icon = if (state.repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                isActive = state.repeatMode != 0,
                contentDescription = "Repeat",
                testTag = "btn_repeat"
            )
        }

        // --- Rotary Knob Volume Dial (Reference Design Top Center) ---
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = NeumorphAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MASTER VOLUME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(state.volume * 100).roundToInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphAccent
                    )
                    Text(
                        text = "Rotary Dial Control",
                        fontSize = 11.sp,
                        color = NeumorphTextTertiary
                    )
                }

                NeumorphicRotaryKnob(
                    value = state.volume,
                    onValueChange = onVolumeChange,
                    size = 110.dp,
                    label = "",
                    displayValue = "",
                    testTag = "volume_rotary_knob"
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
