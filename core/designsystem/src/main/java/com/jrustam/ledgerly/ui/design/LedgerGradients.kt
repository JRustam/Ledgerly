package com.jrustam.ledgerly.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Restrained gradients that keep foreground contrast independent of the system theme. */
object LedgerGradients {
    val hero: Brush = Brush.linearGradient(listOf(Color(0xFF123852), Color(0xFF254873), Color(0xFF413D72)))

    @Composable
    fun selection(strength: Float): Brush = Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f * strength),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f * strength)
    ))
}

@Composable
fun LedgerGradientCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(modifier, shape = LedgerShapes.card, color = Color.Transparent, contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))) {
        Box(Modifier.background(LedgerGradients.hero), content = content)
    }
}

/** Apply before clickable/selectable so feedback stays inside the rounded surface. */
@Composable
fun LedgerSelectionSurface(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val strength by animateFloatAsState(if (selected) 1f else 0f,
        tween(if (LocalMotionEnabled.current) 180 else 0), label = "selection tint")
    Surface(modifier, shape = LedgerShapes.control, color = Color.Transparent,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Box(Modifier.background(LedgerGradients.selection(strength)), content = content)
    }
}
