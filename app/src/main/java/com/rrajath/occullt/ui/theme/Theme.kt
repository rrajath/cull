package com.rrajath.occullt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val bg: Color,
    val bgElev: Color,
    val bgElev2: Color,
    val line: Color,
    val lineStrong: Color,
    val fg: Color,
    val fgDim: Color,
    val fgFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val pin: Color,
    val scrim: Color,
)

private val DarkExtendedColorScheme = ExtendedColorScheme(
    bg = DarkBg,
    bgElev = DarkBgElev,
    bgElev2 = DarkBgElev2,
    line = DarkLine,
    lineStrong = DarkLineStrong,
    fg = DarkFg,
    fgDim = DarkFgDim,
    fgFaint = DarkFgFaint,
    accent = AccentTangerine,
    accentSoft = AccentTangerineSoft,
    danger = Danger,
    dangerSoft = DangerSoft,
    pin = Pin,
    scrim = DarkScrim,
)

private val LightExtendedColorScheme = ExtendedColorScheme(
    bg = LightBg,
    bgElev = LightBgElev,
    bgElev2 = LightBgElev2,
    line = LightLine,
    lineStrong = LightLineStrong,
    fg = LightFg,
    fgDim = LightFgDim,
    fgFaint = LightFgFaint,
    accent = AccentTangerine,
    accentSoft = LightAccentTangerineSoft,
    danger = Danger,
    dangerSoft = LightDangerSoft,
    pin = Pin,
    scrim = LightScrim,
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBg,
    surface = DarkBgElev,
    onBackground = DarkFg,
    onSurface = DarkFg,
)

private val LightColorScheme = lightColorScheme(
    background = LightBg,
    surface = LightBgElev,
    onBackground = LightFg,
    onSurface = LightFg,
)

val LocalExtendedColorScheme = staticCompositionLocalOf { DarkExtendedColorScheme }

@Composable
fun OcculltTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColorScheme = if (darkTheme) DarkExtendedColorScheme else LightExtendedColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
