package com.sadra.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF00E5FF)
val Blue = Color(0xFF2979FF)
val Bg = Color(0xFF0A0F1A)
val Surface1 = Color(0xFF111827)
val Surface2 = Color(0xFF1B2638)

private val scheme = darkColorScheme(
    primary = Cyan, secondary = Blue, background = Bg, surface = Surface1,
    onPrimary = Color(0xFF00212A), onBackground = Color(0xFFE6EEF8), onSurface = Color(0xFFE6EEF8)
)

@Composable
fun SadraTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)
