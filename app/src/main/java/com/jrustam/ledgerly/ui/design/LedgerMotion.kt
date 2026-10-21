package com.jrustam.ledgerly.ui.design

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
fun LedgerMotion(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver

    fun enabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    var motion by remember { mutableStateOf(enabled()) }

    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                motion = enabled()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalMotionEnabled provides motion, content = content)
}

@Composable
fun AnimatedMoney(value: String, style: TextStyle, color: Color = LocalContentColor.current) {
    val motion = LocalMotionEnabled.current
    AnimatedContent(value, transitionSpec = {
        (fadeIn(tween(if (motion) 180 else 0)) + slideInVertically(tween(if (motion) 220 else 0)) { it / 3 })
            .togetherWith(fadeOut(tween(if (motion) 100 else 0)))
    }, label = "exact money change") { text -> Text(text, style = style, color = color) }
}

@Composable
fun CardEntrance(content: @Composable () -> Unit) {
    val motion = LocalMotionEnabled.current
    var visible by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        if (visible) 1f else 0f,
        animationSpec = if (motion) spring(dampingRatio = 0.85f, stiffness = 260f) else snap(),
        label = "card entrance"
    )
    Box(Modifier.graphicsLayer {
        alpha = progress; translationY = (1f - progress) * 24f
    }) { content() }
}

@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.98f else 1f,
        animationSpec = if (LocalMotionEnabled.current) spring(stiffness = 500f) else snap(),
        label = "button press"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = LedgerShapes.control,
        modifier = modifier
            .heightIn(min = 50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }) {
        Text(text)
    }
}
