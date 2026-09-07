package com.example.ui.neumorphic

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeumorphBackground
import com.example.ui.theme.NeumorphShadowDark
import com.example.ui.theme.NeumorphShadowLight

enum class NeumorphState {
    RAISED,
    SUNKEN,
    FLAT
}

/**
 * Draws soft dual shadows (light top-left, dark bottom-right) for an elevated neumorphic component.
 */
fun Modifier.neumorphicRaised(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp,
    lightShadowColor: Color = Color.White.copy(alpha = 0.85f),
    darkShadowColor: Color = Color(0xFFA3B1C6).copy(alpha = 0.65f),
    backgroundColor: Color = NeumorphBackground,
    isCircle: Boolean = false
): Modifier = this.drawBehind {
    val cornerPx = cornerRadius.toPx()
    val elevationPx = elevation.toPx()
    val blurRadius = (elevationPx * 1.4f).coerceAtLeast(1f)

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        // 1. Light Top-Left Shadow
        val lightPaint = Paint().apply {
            color = lightShadowColor.toArgb()
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }

        if (isCircle) {
            val radius = size.minDimension / 2f
            nativeCanvas.drawCircle(
                size.width / 2f - elevationPx * 0.75f,
                size.height / 2f - elevationPx * 0.75f,
                radius,
                lightPaint
            )
        } else {
            val rect = RectF(
                -elevationPx * 0.75f,
                -elevationPx * 0.75f,
                size.width - elevationPx * 0.75f,
                size.height - elevationPx * 0.75f
            )
            nativeCanvas.drawRoundRect(rect, cornerPx, cornerPx, lightPaint)
        }

        // 2. Dark Bottom-Right Shadow
        val darkPaint = Paint().apply {
            color = darkShadowColor.toArgb()
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }

        if (isCircle) {
            val radius = size.minDimension / 2f
            nativeCanvas.drawCircle(
                size.width / 2f + elevationPx * 0.75f,
                size.height / 2f + elevationPx * 0.75f,
                radius,
                darkPaint
            )
        } else {
            val rect = RectF(
                elevationPx * 0.75f,
                elevationPx * 0.75f,
                size.width + elevationPx * 0.75f,
                size.height + elevationPx * 0.75f
            )
            nativeCanvas.drawRoundRect(rect, cornerPx, cornerPx, darkPaint)
        }
    }

    // 3. Draw Base Surface
    if (isCircle) {
        drawCircle(
            color = backgroundColor,
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    } else {
        drawRoundRect(
            color = backgroundColor,
            size = size,
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
    }
}

/**
 * Draws inset dual shadows for sunken/pressed components (slots, seekbar tracks, active buttons).
 */
fun Modifier.neumorphicSunken(
    cornerRadius: Dp = 16.dp,
    depth: Dp = 4.dp,
    lightShadowColor: Color = Color.White.copy(alpha = 0.8f),
    darkShadowColor: Color = Color(0xFFA3B1C6).copy(alpha = 0.7f),
    backgroundColor: Color = Color(0xFFD9DFE8),
    isCircle: Boolean = false
): Modifier = this.drawWithContent {
    val cornerPx = cornerRadius.toPx()
    val depthPx = depth.toPx()

    // 1. Draw base sunken background
    if (isCircle) {
        drawCircle(
            color = backgroundColor,
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    } else {
        drawRoundRect(
            color = backgroundColor,
            size = size,
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
    }

    // 2. Draw content (children)
    drawContent()

    // 3. Inner dark top-left gradient overlay
    val darkGradient = Brush.linearGradient(
        colors = listOf(darkShadowColor, Color.Transparent),
        start = Offset.Zero,
        end = Offset(depthPx * 3f, depthPx * 3f)
    )
    if (isCircle) {
        drawCircle(
            brush = darkGradient,
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    } else {
        drawRoundRect(
            brush = darkGradient,
            size = size,
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
    }

    // 4. Inner light bottom-right gradient highlight
    val lightGradient = Brush.linearGradient(
        colors = listOf(Color.Transparent, lightShadowColor),
        start = Offset(size.width - depthPx * 3f, size.height - depthPx * 3f),
        end = Offset(size.width, size.height)
    )
    if (isCircle) {
        drawCircle(
            brush = lightGradient,
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    } else {
        drawRoundRect(
            brush = lightGradient,
            size = size,
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
    }
}

/**
 * Interactive Neumorphic click modifier that presses into a sunken state when touched.
 */
fun Modifier.neumorphicPress(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp,
    isCircle: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit
): Modifier = this.then(
    Modifier
        .clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onClick
        )
        .run {
            if (isActive) {
                neumorphicSunken(
                    cornerRadius = cornerRadius,
                    depth = 4.dp,
                    isCircle = isCircle
                )
            } else {
                neumorphicRaised(
                    cornerRadius = cornerRadius,
                    elevation = elevation,
                    isCircle = isCircle
                )
            }
        }
)
