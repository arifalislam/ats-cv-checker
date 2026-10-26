package com.arif.cvcheck.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF14161F),
    surface = Color.White,
    onSurface = Color(0xFF14161F),
    surfaceVariant = Color(0xFFECEEF5),
    onSurfaceVariant = Color(0xFF5B6070),
    outline = Color(0xFFD3D8E4),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9A96FF),
    onPrimary = Color(0xFF14123A),
    primaryContainer = Color(0xFF2A2766),
    onPrimaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF0E1015),
    onBackground = Color(0xFFE8EAF2),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE8EAF2),
    surfaceVariant = Color(0xFF232836),
    onSurfaceVariant = Color(0xFF9AA0B2),
    outline = Color(0xFF353B4C),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF4C1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun CvCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

/** Traffic-light colours used for scores and priorities. */
object Signal {
    @Composable fun good(): Color = if (isSystemInDarkTheme()) Color(0xFF4ADE80) else Color(0xFF16A34A)
    @Composable fun warn(): Color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706)
    @Composable fun bad(): Color = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFDC2626)
    @Composable fun info(): Color = if (isSystemInDarkTheme()) Color(0xFF93C5FD) else Color(0xFF2563EB)

    @Composable
    fun forFraction(f: Float): Color = when {
        f >= 0.75f -> good()
        f >= 0.5f -> warn()
        else -> bad()
    }
}
