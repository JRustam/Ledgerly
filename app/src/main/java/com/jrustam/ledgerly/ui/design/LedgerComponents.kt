package com.jrustam.ledgerly.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R

object LedgerShapes {
    val card = RoundedCornerShape(22.dp)
    val control = RoundedCornerShape(14.dp)
    val small = RoundedCornerShape(10.dp)
}

@Composable
fun LedgerCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = LedgerShapes.card, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 1.dp, content = content)
}

@Composable
fun LedgerMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(32.dp).background(Brush.linearGradient(listOf(Color(0xFF20A4D0), Color(0xFF4158CD))), LedgerShapes.small)) {
        val bar = size.width / 9
        for (index in 0..2) {
            val height = size.height * (0.25f + index * 0.14f)
            drawRoundRect(Color.White.copy(alpha = 0.7f + index * 0.15f),
                Offset(size.width * 0.23f + index * bar * 1.8f, size.height * 0.73f - height),
                Size(bar, height), CornerRadius(bar / 2))
        }
    }
}

@Composable
fun LedgerTopBar(title: String, onBack: (() -> Unit)? = null, enabled: Boolean = true,
                 includeStatusBar: Boolean = true, action: (@Composable () -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onBackground) {
        Row(Modifier.fillMaxWidth().then(if (includeStatusBar) Modifier.statusBarsPadding() else Modifier)
            .heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) IconButton(onClick = onBack, enabled = enabled) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            } else LedgerMark()
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            action?.invoke()
        }
    }
}

@Composable
fun LedgerSelection(options: List<String>, selected: Int, onSelect: (Int) -> Unit,
                    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val motion = LocalMotionEnabled.current
    Row(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, LedgerShapes.control)
        .padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, label ->
            val color by animateColorAsState(if (index == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                tween(if (motion) 180 else 0), label = "selection background")
            Box(Modifier.weight(1f).heightIn(min = 44.dp).background(color, LedgerShapes.small)
                .selectable(index == selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(index) })
                .padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    color = if (index == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LedgerNavigation(labels: List<String>, icons: List<ImageVector>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp).selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                val background by animateColorAsState(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    tween(if (LocalMotionEnabled.current) 200 else 0), label = "navigation selection")
                Column(Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                    .heightIn(min = 56.dp).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.width(48.dp).height(28.dp).background(background, LedgerShapes.small), contentAlignment = Alignment.Center) {
                        Icon(icons[index], contentDescription = null, modifier = Modifier.size(20.dp),
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
