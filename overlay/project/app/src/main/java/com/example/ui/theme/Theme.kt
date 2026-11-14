package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EditorialColorScheme = lightColorScheme(
    primary = EditorialPrimary,
    onPrimary = Color.White,
    primaryContainer = EditorialLavender,
    onPrimaryContainer = EditorialTextOnLavender,
    secondary = EditorialSecondary,
    onSecondary = Color.White,
    secondaryContainer = EditorialLilac,
    onSecondaryContainer = EditorialTextOnLilac,
    tertiary = EditorialTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EditorialBlush,
    onTertiaryContainer = EditorialTextOnBlush,
    background = EditorialBg,
    onBackground = EditorialTextPrimary,
    surface = EditorialSurface,
    onSurface = EditorialTextPrimary,
    surfaceVariant = EditorialSurfaceContainer,
    onSurfaceVariant = EditorialTextSecondary,
    outline = EditorialBorder,
    outlineVariant = EditorialBorderLight,
    error = EditorialRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EditorialColorScheme,
        typography = Typography,
        content = content
    )
}
