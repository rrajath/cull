package com.rrajath.occullt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

private fun accentPalette(index: Int): AccentColor {
    val i = if (index in CatppuccinAccents.indices) index else 0
    return CatppuccinAccents[i]
}

private fun DarkExtendedColorScheme(accentIndex: Int) = ExtendedColorScheme(
    bg = DarkBg,
    bgElev = DarkBgElev,
    bgElev2 = DarkBgElev2,
    line = DarkLine,
    lineStrong = DarkLineStrong,
    fg = DarkFg,
    fgDim = DarkFgDim,
    fgFaint = DarkFgFaint,
    accent = accentPalette(accentIndex).color,
    accentSoft = accentPalette(accentIndex).softDark,
    danger = Danger,
    dangerSoft = DangerSoft,
    pin = Pin,
    scrim = DarkScrim,
)

private fun LightExtendedColorScheme(accentIndex: Int) = ExtendedColorScheme(
    bg = LightBg,
    bgElev = LightBgElev,
    bgElev2 = LightBgElev2,
    line = LightLine,
    lineStrong = LightLineStrong,
    fg = LightFg,
    fgDim = LightFgDim,
    fgFaint = LightFgFaint,
    accent = accentPalette(accentIndex).color,
    accentSoft = accentPalette(accentIndex).softLight,
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

object ThemeColors {
    var current by mutableStateOf(DarkExtendedColorScheme(0))
}

@Composable
fun OcculltTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkExtendedColorScheme(accentIndex) else LightExtendedColorScheme(accentIndex)
    ThemeColors.current = scheme

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
