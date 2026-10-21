package com.jrustam.ledgerly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = line.sp,
    fontWeight = weight, letterSpacing = 0.sp,
)
val Typography = Typography(
    displaySmall = type(34, 42, FontWeight.SemiBold),
    headlineLarge = type(28, 34, FontWeight.SemiBold),
    headlineMedium = type(24, 30, FontWeight.SemiBold),
    headlineSmall = type(21, 28, FontWeight.SemiBold),
    titleLarge = type(19, 26, FontWeight.SemiBold),
    titleMedium = type(15, 22, FontWeight.SemiBold),
    titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(15, 23), bodyMedium = type(14, 21), bodySmall = type(12, 18),
    labelLarge = type(13, 18, FontWeight.SemiBold),
    labelMedium = type(12, 16, FontWeight.Medium),
    labelSmall = type(11, 15, FontWeight.Medium),
)
