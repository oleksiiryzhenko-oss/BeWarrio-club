package com.katadojo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KataDojoColors = darkColorScheme(
    primary = DojoRed,
    onPrimary = Rice,
    primaryContainer = DojoRedDark,
    onPrimaryContainer = Rice,
    secondary = Beige,
    onSecondary = Ink,
    secondaryContainer = CharcoalLight,
    onSecondaryContainer = Rice,
    tertiary = Beige,
    onTertiary = Ink,
    background = Ink,
    onBackground = Rice,
    surface = Ink,
    onSurface = Rice,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = Stone,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Charcoal,
    surfaceContainer = Charcoal,
    surfaceContainerHigh = CharcoalLight,
    surfaceContainerHighest = CharcoalLight,
    outline = CharcoalBorder,
    outlineVariant = CharcoalBorder,
    error = ErrorRed,
    onError = Ink
)

private val KataDojoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun KataDojoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KataDojoColors, typography = KataDojoTypography, shapes = KataDojoShapes, content = content)
}
