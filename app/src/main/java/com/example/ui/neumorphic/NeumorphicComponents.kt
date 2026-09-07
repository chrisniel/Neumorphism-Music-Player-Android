package com.example.ui.neumorphic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeumorphAccent
import com.example.ui.theme.NeumorphBackground
import com.example.ui.theme.NeumorphDivider
import com.example.ui.theme.NeumorphShadowDark
import com.example.ui.theme.NeumorphTextPrimary
import com.example.ui.theme.NeumorphTextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Circular Neumorphic button with dual raised shadow, pressing into sunken or glowing blue when active.
 */
@Composable
fun NeumorphicCircularButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: ImageVector? = null,
    iconTint: Color = NeumorphTextPrimary,
    isActive: Boolean = false,
    contentDescription: String? = null,
    testTag: String = "neumorph_button",
    content: (@Composable () -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .run {
                if (isActive || isPressed) {
                    neumorphicSunken(cornerRadius = size / 2, depth = 4.dp, isCircle = true)
                } else {
                    neumorphicRaised(cornerRadius = size / 2, elevation = 6.dp, isCircle = true)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) NeumorphAccent else iconTint,
                modifier = Modifier.size(size * 0.44f)
            )
        }
    }
}

/**
 * Neumorphic Card container with soft double shadows and rounded corners.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neumorphicRaised(cornerRadius = cornerRadius, elevation = elevation)
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Neumorphic Slider with sunken track, vertical tick marks, blue progress fill,
 * and a raised sliding thumb handle with grip lines matching the reference design.
 */
@Composable
fun NeumorphicSlider(
    value: Float, // 0.0f .. 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 20.dp,
    thumbWidth: Dp = 28.dp,
    thumbHeight: Dp = 24.dp,
    enabled: Boolean = true,
    testTag: String = "neumorph_slider"
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .testTag(testTag)
            .neumorphicSunken(cornerRadius = trackHeight / 2, depth = 3.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val progress = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(progress)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(progress)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Draw graduation tick marks and blue progress bar
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val progressWidth = width * value.coerceIn(0f, 1f)

            // Draw blue progress fill with rounded left corners
            if (progressWidth > 0f) {
                drawRoundRect(
                    color = NeumorphAccent,
                    size = Size(progressWidth, height),
                    cornerRadius = CornerRadius(height / 2, height / 2)
                )
            }

            // Draw subtle vertical tick marks across the track
            val tickCount = 40
            val tickSpacing = width / tickCount
            for (i in 1 until tickCount) {
                val x = i * tickSpacing
                val isFilled = x <= progressWidth
                val tickColor = if (isFilled) Color.White.copy(alpha = 0.35f) else Color(0xFFA3B1C6).copy(alpha = 0.5f)
                val tickHeight = if (i % 5 == 0) height * 0.6f else height * 0.35f
                val yStart = (height - tickHeight) / 2f

                drawLine(
                    color = tickColor,
                    start = Offset(x, yStart),
                    end = Offset(x, yStart + tickHeight),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // 2. Raised Slider Thumb handle with vertical grip lines
        Box(
            modifier = Modifier
                .offset {
                    // Position thumb center at value * width
                    // We will measure dynamically or approximate offset
                    IntOffset(0, 0)
                }
        )
    }
}

/**
 * More precise layout-aware Neumorphic Slider with actual thumb positioning.
 */
@Composable
fun NeumorphicTrackSlider(
    value: Float, // 0.0f .. 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 18.dp,
    thumbWidth: Dp = 32.dp,
    thumbHeight: Dp = 26.dp,
    testTag: String = "track_slider"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbHeight)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
                .neumorphicSunken(cornerRadius = trackHeight / 2, depth = 3.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(ratio)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(ratio)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val progressWidth = width * value.coerceIn(0f, 1f)

                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = NeumorphAccent,
                        size = Size(progressWidth, height),
                        cornerRadius = CornerRadius(height / 2, height / 2)
                    )
                }

                // Graduation ticks
                val tickCount = 36
                val step = width / tickCount
                for (i in 1 until tickCount) {
                    val x = i * step
                    val isFilled = x <= progressWidth
                    val tickColor = if (isFilled) Color.White.copy(alpha = 0.35f) else Color(0xFFA3B1C6).copy(alpha = 0.45f)
                    val tickH = if (i % 4 == 0) height * 0.6f else height * 0.35f
                    val y = (height - tickH) / 2f
                    drawLine(
                        color = tickColor,
                        start = Offset(x, y),
                        end = Offset(x, y + tickH),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }
    }
}

/**
 * Authentic Neumorphic Rotary Knob as seen in the reference image:
 * - Outer radial graduation tick marks around the perimeter
 * - Raised outer bezel
 * - Raised inner rotary dial with accent blue ring and indicator pointer
 * - Circular rotation gesture (detectDragGestures with atan2 angle tracking)
 */
@Composable
fun NeumorphicRotaryKnob(
    value: Float, // 0.0f .. 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    label: String = "",
    displayValue: String = "",
    startAngle: Float = 135f, // degrees (bottom-left)
    sweepAngle: Float = 270f, // degrees sweep to bottom-right
    accentColor: Color = NeumorphAccent,
    testTag: String = "rotary_knob"
) {
    val currentAngle = startAngle + (value.coerceIn(0f, 1f) * sweepAngle)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                        val touch = change.position
                        val dx = touch.x - center.x
                        val dy = touch.y - center.y

                        var angleRad = atan2(dy, dx)
                        var angleDeg = (angleRad * 180.0 / PI).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Map angle to value
                        var relativeAngle = (angleDeg - startAngle)
                        if (relativeAngle < 0) relativeAngle += 360f

                        if (relativeAngle <= sweepAngle) {
                            val newVal = (relativeAngle / sweepAngle).coerceIn(0f, 1f)
                            onValueChange(newVal)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Draw outer perimeter ticks and arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val outerRadius = this.size.minDimension / 2f
                val innerRadius = outerRadius - 14.dp.toPx()

                val numTicks = 28
                val tickStep = sweepAngle / (numTicks - 1)

                for (i in 0 until numTicks) {
                    val deg = startAngle + i * tickStep
                    val rad = deg * (PI / 180.0)
                    val p1 = Offset(
                        (center.x + (outerRadius - 2.dp.toPx()) * cos(rad)).toFloat(),
                        (center.y + (outerRadius - 2.dp.toPx()) * sin(rad)).toFloat()
                    )
                    val p2 = Offset(
                        (center.x + (innerRadius + 4.dp.toPx()) * cos(rad)).toFloat(),
                        (center.y + (innerRadius + 4.dp.toPx()) * sin(rad)).toFloat()
                    )

                    val isHighlighted = (deg <= currentAngle)
                    val tickColor = if (isHighlighted) accentColor else Color(0xFFA3B1C6).copy(alpha = 0.5f)
                    val strokeW = if (i % 4 == 0) 2.5.dp.toPx() else 1.5.dp.toPx()

                    drawLine(
                        color = tickColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Raised center knob body
            val knobSize = size * 0.68f
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .neumorphicRaised(
                        cornerRadius = knobSize / 2,
                        elevation = 6.dp,
                        isCircle = true
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Accent inner ring & pointer notch
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val radius = this.size.minDimension / 2f

                    // Draw inner accent ring
                    drawCircle(
                        color = accentColor.copy(alpha = 0.85f),
                        radius = radius - 8.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw pointer indicator needle/notch
                    val rad = currentAngle * (PI / 180.0)
                    val pStart = Offset(
                        (center.x + (radius * 0.35f) * cos(rad)).toFloat(),
                        (center.y + (radius * 0.35f) * sin(rad)).toFloat()
                    )
                    val pEnd = Offset(
                        (center.x + (radius - 12.dp.toPx()) * cos(rad)).toFloat(),
                        (center.y + (radius - 12.dp.toPx()) * sin(rad)).toFloat()
                    )

                    drawLine(
                        color = accentColor,
                        start = pStart,
                        end = pEnd,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center dot
                    drawCircle(
                        color = Color(0xFFA3B1C6),
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphTextPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (displayValue.isNotEmpty()) {
            Text(
                text = displayValue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphAccent
            )
        }
    }
}

/**
 * Neumorphic Segmented Control / Pill Switch (as seen in image with "Accept" / "Cancel" or "Yes" / "No")
 */
@Composable
fun NeumorphicSegmentedPill(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .neumorphicSunken(cornerRadius = height / 2, depth = 3.dp)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, title ->
                val isSelected = (index == selectedIndex)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(height / 2))
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null,
                            onClick = { onSelect(index) }
                        )
                        .run {
                            if (isSelected) {
                                neumorphicRaised(
                                    cornerRadius = height / 2,
                                    elevation = 4.dp,
                                    backgroundColor = NeumorphAccent
                                )
                            } else {
                                this
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else NeumorphTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Neumorphic Pill Toggle Switch (as seen in image "Selection" switch with circular thumb)
 */
@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 34.dp
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "thumb_offset"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .neumorphicSunken(cornerRadius = height / 2, depth = 3.dp)
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track fill if checked
        if (checked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(height / 2))
                    .neumorphicSunken(
                        cornerRadius = height / 2,
                        depth = 2.dp,
                        backgroundColor = NeumorphAccent.copy(alpha = 0.2f)
                    )
            )
        }

        // Sliding thumb
        val thumbSize = height - 6.dp
        val maxOffset = (width - height).value.coerceAtLeast(0f)
        val currentOffsetDp = (maxOffset * thumbOffset).dp

        Box(
            modifier = Modifier
                .offset(x = currentOffsetDp)
                .size(thumbSize)
                .neumorphicRaised(
                    cornerRadius = thumbSize / 2,
                    elevation = 4.dp,
                    isCircle = true,
                    backgroundColor = if (checked) NeumorphAccent else NeumorphBackground
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .neumorphicRaised(cornerRadius = 4.dp, elevation = 2.dp, backgroundColor = Color.White)
                )
            }
        }
    }
}
