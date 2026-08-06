package com.greenleaf.paygo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF0B6E4F)
private val GreenDark = Color(0xFF07543C)
private val Amber = Color(0xFFF2A900)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Amber,
    primaryContainer = Color(0xFFB8F1D9),
    onPrimaryContainer = GreenDark
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD6AC),
    onPrimary = Color(0xFF00382A),
    secondary = Amber,
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFB8F1D9)
)

@Composable
fun PaygoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
