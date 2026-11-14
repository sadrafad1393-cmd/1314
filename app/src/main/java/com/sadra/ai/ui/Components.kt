package com.sadra.ai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin

/** Animated only while listening, so idle CPU stays near zero. */
@Composable
fun Waveform(level: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "phase")
    val amp by animateFloatAsState(level.coerceIn(0.12f, 1f), label = "amp")
    Canvas(modifier) {
        val n = 28
        val step = size.width / n
        for (i in 0 until n) {
            val h = size.height * (0.12f + 0.88f * amp * abs(sin(phase + i * 0.45f)))
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Cyan, Blue)),
                topLeft = Offset(i * step + step * 0.2f, (size.height - h) / 2f),
                size = Size(step * 0.6f, h),
                cornerRadius = CornerRadius(step * 0.3f)
            )
        }
    }
}

@Composable
private fun PulseRing() {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 1.7f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "s")
    val a by t.animateFloat(0.5f, 0f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "a")
    Box(Modifier.size(84.dp).scale(s).alpha(a).clip(CircleShape).background(Cyan))
}

@Composable
fun MicButton(listening: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        if (listening) PulseRing()
        Box(
            Modifier.size(84.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Cyan, Blue)))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Text(if (listening) "■" else "🎙", fontSize = 32.sp, color = Color.White) }
    }
}

@Composable
fun Avatar(size: Int = 36) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Cyan, Blue))),
        contentAlignment = Alignment.Center
    ) { Text("ص", color = Color(0xFF00212A), fontSize = (size / 2).sp) }
}

fun toFa(n: Int): String = n.toString().map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")
