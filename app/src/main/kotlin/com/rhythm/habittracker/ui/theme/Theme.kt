package com.rhythm.habittracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val RhythmLightColorScheme = lightColorScheme(
    primary = RhythmPrimary,
    onPrimary = RhythmOnPrimary,
    primaryContainer = RhythmPrimaryContainer,
    onPrimaryContainer = RhythmOnPrimaryContainer,
    secondary = RhythmSecondary,
    onSecondary = RhythmOnSecondary,
    secondaryContainer = RhythmSecondaryContainer,
    onSecondaryContainer = RhythmOnSecondaryContainer,
    tertiary = RhythmDeepGraphite,
    onTertiary = RhythmOnPrimary,
    background = RhythmBackground,
    onBackground = RhythmOnSurface,
    surface = RhythmSurface,
    onSurface = RhythmOnSurface,
    surfaceVariant = RhythmSurfaceContainerHighest,
    onSurfaceVariant = RhythmOnSurfaceVariant,
    surfaceContainerLowest = RhythmSurfaceContainerLowest,
    surfaceContainerLow = RhythmSurfaceContainerLow,
    surfaceContainer = RhythmSurfaceContainer,
    surfaceContainerHigh = RhythmSurfaceContainerHigh,
    surfaceContainerHighest = RhythmSurfaceContainerHighest,
    surfaceDim = RhythmSurfaceDim,
    surfaceBright = RhythmSurfaceBright,
    outline = RhythmOutline,
    outlineVariant = RhythmOutlineVariant,
    inverseSurface = RhythmInverseSurface,
    inverseOnSurface = RhythmInverseOnSurface,
    error = RhythmError,
    onError = RhythmOnError,
    errorContainer = RhythmErrorContainer,
    onErrorContainer = RhythmOnErrorContainer,
)

object RhythmTheme {
    val tokens: RhythmTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalRhythmTypography.current
}

@Composable
fun RhythmTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRhythmTypography provides RhythmTypographyTokens()) {
        MaterialTheme(
            colorScheme = RhythmLightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
