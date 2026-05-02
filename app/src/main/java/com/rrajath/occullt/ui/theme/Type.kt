package com.rrajath.occullt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplayStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 54.sp,
    lineHeight = 50.sp,
    letterSpacing = (-1.2).sp,
)

val ScreenTitleStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.6).sp,
)

val GiantButtonTitleStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = (-1).sp,
)

val GiantButtonSubtitleStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

val SectionHeaderStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 17.6.sp,
    letterSpacing = 1.6.sp,
)

val BodyStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)

val LabelStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
)

val Typography = Typography(
    bodyLarge = BodyStyle,
    labelLarge = LabelStyle,
    titleLarge = ScreenTitleStyle,
    headlineLarge = DisplayStyle,
)
