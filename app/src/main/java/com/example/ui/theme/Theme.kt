package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = ForestEmeraldContainer,
    onPrimaryContainer = OnForestEmeraldContainer,
    secondary = GrainAmber,
    onSecondary = Color.White,
    secondaryContainer = GrainAmberContainer,
    onSecondaryContainer = OnGrainAmberContainer,
    tertiary = SlateTeal,
    onTertiary = Color.White,
    tertiaryContainer = SlateTealContainer,
    onTertiaryContainer = OnSlateTealContainer,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8E4),
    error = StatusCritical,
    onError = Color.White,
    errorContainer = StatusCriticalContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = ForestEmeraldDark,
    primaryContainer = ForestEmerald,
    onPrimaryContainer = Color(0xFFD5F0DC),
    secondary = GrainAmberBright,
    onSecondary = Color(0xFF3A1D00),
    secondaryContainer = Color(0xFF19362C),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = SlateTealContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF26372E),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF3E1619),
    onErrorContainer = Color(0xFFFECACA)
)

val FeedMillShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FeedMillTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FeedMillShapes,
        content = content
    )
}

@Composable
fun statusCriticalColor(): Color = StatusCriticalTextDark

@Composable
fun statusHealthyColor(): Color = StatusHealthyTextDark

@Composable
fun statusWatchColor(): Color = StatusWatchTextDark

