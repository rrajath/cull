package com.rrajath.cull.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBg = Color(0xFF0D0D0F)
val DarkBgElev = Color(0xFF17171A)
val DarkBgElev2 = Color(0xFF1F1F23)
val DarkLine = Color(0x14FFFFFF)
val DarkLineStrong = Color(0x29FFFFFF)
val DarkFg = Color(0xFFF3F2EF)
val DarkFgDim = Color(0xA8F3F2EF)
val DarkFgFaint = Color(0x61F3F2EF)
val DarkScrim = Color(0x8C000000)

val LightBg = Color(0xFFF6F4EF)
val LightBgElev = Color(0xFFFFFFFF)
val LightBgElev2 = Color(0xFFEEEAE2)
val LightLine = Color(0x14000000)
val LightLineStrong = Color(0x29000000)
val LightFg = Color(0xFF14120E)
val LightFgDim = Color(0xA314120E)
val LightFgFaint = Color(0x6614120E)
val LightScrim = Color(0x8C000000)

data class AccentColor(val color: Color, val softDark: Color, val softLight: Color)

val CatppuccinAccents = listOf(
    AccentColor(Color(0xFFc6a0f6), Color(0xFF362a48), Color(0xFFe8daf6)), // Mauve
    AccentColor(Color(0xFFf5a97f), Color(0xFF3d281e), Color(0xFFf5dccc)), // Peach
    AccentColor(Color(0xFFeed49f), Color(0xFF3d331e), Color(0xFFf5ecd2)), // Yellow
    AccentColor(Color(0xFFa6da95), Color(0xFF273a1e), Color(0xFFdcf0d4)), // Green
    AccentColor(Color(0xFF8aadf4), Color(0xFF1e2e3d), Color(0xFFd4e2f5)), // Blue
    AccentColor(Color(0xFFf5bde6), Color(0xFF3d1e34), Color(0xFFf5dcee)), // Pink
    AccentColor(Color(0xFFed8796), Color(0xFF3d1e24), Color(0xFFf5d4da)), // Red
    AccentColor(Color(0xFF8bd5ca), Color(0xFF1e3d38), Color(0xFFd4f0ec)), // Teal
)

val Danger = Color(0xFFD94F35)
val DangerSoft = Color(0xFF4A1F18)
val LightDangerSoft = Color(0xFFF5E0D8)
val Pin = Color(0xFFF5A060)

// Wizard segment states — fixed colors, independent of the selectable accent
// so Complete (blue) and In Progress (amber) stay distinguishable on any hue
val WizardComplete = Color(0xFF8BAAFF)
val WizardCompleteOn = Color(0xFF12286A)
val WizardCompleteOnDim = Color(0xFF2D4D9A)
val WizardCompleteSoftDark = Color(0xFF1E2A4A)
val WizardCompleteSoftLight = Color(0xFFD4E0F5)
val WizardInProgressDark = Color(0xFFC17B28)
val WizardInProgressLight = Color(0xFF9A5F16)
val WizardInProgressSoftDark = Color(0xFF3D2E1A)
val WizardInProgressSoftLight = Color(0xFFF5E6CF)
