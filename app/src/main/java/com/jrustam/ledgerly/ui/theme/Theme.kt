package com.jrustam.ledgerly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jrustam.ledgerly.ui.design.LedgerMotion

private val LightColors = lightColorScheme(
    primary = Color(0xFF385FD5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EEFF), onPrimaryContainer = Color(0xFF24438F),
    secondary = Color(0xFF626A85), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EBF6), onSecondaryContainer = Color(0xFF343C59),
    tertiary = Color(0xFF237A67), tertiaryContainer = Color(0xFFE2F3EC),
    background = Color(0xFFF6F7FB), onBackground = Color(0xFF181E34),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF181E34),
    surfaceVariant = Color(0xFFF0F1F8), onSurfaceVariant = Color(0xFF636A80),
    surfaceContainer = Color(0xFFF0F2F8), surfaceContainerLow = Color(0xFFFAFAFD),
    surfaceContainerHigh = Color(0xFFECEEF6), surfaceContainerHighest = Color(0xFFE5E8F2),
    outline = Color(0xFF858BA0), outlineVariant = Color(0xFFE2E5EF),
    error = Color(0xFFB54158), errorContainer = Color(0xFFFFE9ED),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9B7FF), onPrimary = Color(0xFF24216B),
    primaryContainer = Color(0xFF333064), onPrimaryContainer = Color(0xFFE5E2FF),
    secondary = Color(0xFFC0C6DF), secondaryContainer = Color(0xFF2A324B),
    onSecondaryContainer = Color(0xFFDCE1FA),
    tertiary = Color(0xFF8BD5BB), tertiaryContainer = Color(0xFF173C34),
    background = Color(0xFF0D1120), onBackground = Color(0xFFE8EBF7),
    surface = Color(0xFF161C30), onSurface = Color(0xFFE8EBF7),
    surfaceVariant = Color(0xFF242C43), onSurfaceVariant = Color(0xFFA5AEC8),
    surfaceContainer = Color(0xFF1C2339), surfaceContainerLow = Color(0xFF12182A),
    surfaceContainerHigh = Color(0xFF252D45), surfaceContainerHighest = Color(0xFF2D3650),
    outline = Color(0xFF78829F), outlineVariant = Color(0xFF2C354D),
    error = Color(0xFFFFAABD), errorContainer = Color(0xFF532B39),
)

@Composable
fun LedgerlyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, shapes = Shapes(), content = { LedgerMotion(content) })
}
