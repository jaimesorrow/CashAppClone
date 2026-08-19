package com.cashclone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CashGreen = Color(0xFF00D54B)
val CashBlack = Color(0xFF0B0B0B)
val CashGray = Color(0xFFF5F5F5)
val CashRed = Color(0xFFE03131)

private val DarkColors = darkColorScheme(
    primary = CashGreen,
    onPrimary = CashBlack,
    background = CashBlack,
    surface = Color(0xFF1A1A1A),
    error = CashRed,
)

private val LightColors = lightColorScheme(
    primary = CashGreen,
    onPrimary = CashBlack,
    background = Color.White,
    surface = CashGray,
    error = CashRed,
)

@Composable
fun CashCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
