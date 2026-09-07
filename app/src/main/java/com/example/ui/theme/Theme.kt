package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NeumorphicColorScheme = lightColorScheme(
    primary = NeumorphAccent,
    onPrimary = NeumorphShadowLight,
    primaryContainer = NeumorphAccentLight,
    onPrimaryContainer = NeumorphTextPrimary,
    secondary = NeumorphTextSecondary,
    onSecondary = NeumorphShadowLight,
    background = NeumorphBackground,
    onBackground = NeumorphTextPrimary,
    surface = NeumorphSurface,
    onSurface = NeumorphTextPrimary,
    surfaceVariant = NeumorphBackground,
    onSurfaceVariant = NeumorphTextSecondary,
    outline = NeumorphDivider
)

@Composable
fun NeumorphicMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeumorphicColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) = NeumorphicMusicTheme(content = content)

