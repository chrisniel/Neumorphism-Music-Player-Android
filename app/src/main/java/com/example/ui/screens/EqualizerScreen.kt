package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.neumorphic.NeumorphicCard
import com.example.ui.neumorphic.NeumorphicCircularButton
import com.example.ui.neumorphic.NeumorphicRotaryKnob
import com.example.ui.neumorphic.neumorphicRaised
import com.example.ui.neumorphic.neumorphicSunken
import com.example.ui.theme.NeumorphAccent
import com.example.ui.theme.NeumorphBackground
import com.example.ui.theme.NeumorphTextPrimary
import com.example.ui.theme.NeumorphTextSecondary
import com.example.ui.theme.NeumorphTextTertiary
import com.example.ui.viewmodel.EqPreset
import com.example.ui.viewmodel.MusicUiState
import com.example.ui.viewmodel.PRESETS

@Composable
fun EqualizerScreen(
    state: MusicUiState,
    onBassChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Map -100..100 to 0..1 for rotary knobs
    val bassRatio = ((state.eqBass + 100) / 200f).coerceIn(0f, 1f)
    val midRatio = ((state.eqMid + 100) / 200f).coerceIn(0f, 1f)
    val trebleRatio = ((state.eqTreble + 100) / 200f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Top Bar ---
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
                testTag = "eq_btn_back"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AUDIO EQUALIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextTertiary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "3-Band Parametric",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextPrimary
                )
            }

            NeumorphicCircularButton(
                onClick = {
                    val flatPreset = PRESETS.first { it.name == "Flat" }
                    onApplyPreset(flatPreset)
                },
                size = 46.dp,
                icon = Icons.Default.Refresh,
                contentDescription = "Reset",
                testTag = "eq_btn_reset"
            )
        }

        // --- Live EQ Frequency Response Curve Visualizer ---
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeumorphAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FREQUENCY RESPONSE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = state.selectedPresetName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sunken curve display canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .neumorphicSunken(cornerRadius = 16.dp, depth = 3.dp)
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        // Grid lines
                        drawLine(
                            color = Color(0xFFA3B1C6).copy(alpha = 0.3f),
                            start = Offset(0f, midY),
                            end = Offset(w, midY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Points mapped from bass, mid, treble
                        // -100..100 dB maps to +h/2 to -h/2
                        val pBassY = midY - (state.eqBass / 100f) * (h * 0.4f)
                        val pMidY = midY - (state.eqMid / 100f) * (h * 0.4f)
                        val pTrebleY = midY - (state.eqTreble / 100f) * (h * 0.4f)

                        val path = Path().apply {
                            moveTo(0f, pBassY)
                            cubicTo(
                                w * 0.25f, pBassY,
                                w * 0.25f, pMidY,
                                w * 0.5f, pMidY
                            )
                            cubicTo(
                                w * 0.75f, pMidY,
                                w * 0.75f, pTrebleY,
                                w, pTrebleY
                            )
                        }

                        // Gradient fill under curve
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(NeumorphAccent.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Glowing curve stroke
                        drawPath(
                            path = path,
                            color = NeumorphAccent,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Frequency control anchor dots
                        val dots = listOf(
                            Offset(w * 0.15f, pBassY),
                            Offset(w * 0.5f, pMidY),
                            Offset(w * 0.85f, pTrebleY)
                        )
                        dots.forEach { pt ->
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                            drawCircle(color = NeumorphAccent, radius = 3.dp.toPx(), center = pt)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("100 Hz (Bass)", fontSize = 11.sp, color = NeumorphTextSecondary)
                    Text("1 kHz (Mid)", fontSize = 11.sp, color = NeumorphTextSecondary)
                    Text("10 kHz (Treble)", fontSize = 11.sp, color = NeumorphTextSecondary)
                }
            }
        }

        // --- 3-Band Rotary Knobs ---
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = NeumorphAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BAND CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // BASS KNOB
                    NeumorphicRotaryKnob(
                        value = bassRatio,
                        onValueChange = onBassChange,
                        size = 96.dp,
                        label = "BASS",
                        displayValue = "${if (state.eqBass > 0) "+" else ""}${state.eqBass} dB",
                        testTag = "knob_bass"
                    )

                    // MID KNOB
                    NeumorphicRotaryKnob(
                        value = midRatio,
                        onValueChange = onMidChange,
                        size = 96.dp,
                        label = "MID",
                        displayValue = "${if (state.eqMid > 0) "+" else ""}${state.eqMid} dB",
                        testTag = "knob_mid"
                    )

                    // TREBLE KNOB
                    NeumorphicRotaryKnob(
                        value = trebleRatio,
                        onValueChange = onTrebleChange,
                        size = 96.dp,
                        label = "TREBLE",
                        displayValue = "${if (state.eqTreble > 0) "+" else ""}${state.eqTreble} dB",
                        testTag = "knob_treble"
                    )
                }
            }
        }

        // --- Preset Selection Chips ---
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "EQUALIZER PRESETS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphTextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2x3 Grid of preset chips
                val chunkedPresets = PRESETS.chunked(3)
                chunkedPresets.forEach { rowPresets ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = state.selectedPresetName == preset.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onApplyPreset(preset) }
                                    )
                                    .run {
                                        if (isSelected) {
                                            neumorphicSunken(
                                                cornerRadius = 20.dp,
                                                depth = 3.dp,
                                                backgroundColor = NeumorphAccent
                                            )
                                        } else {
                                            neumorphicRaised(
                                                cornerRadius = 20.dp,
                                                elevation = 4.dp
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else NeumorphTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
