package com.jqwave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForestGreen = Color(0xFF1B5E20)
private val LeafGreen = Color(0xFF2E7D32)
private val MintContainer = Color(0xFFC8E6C9)
private val SkyBlue = Color(0xFF0277BD)
private val BlueContainer = Color(0xFFB3E5FC)
private val TealAccent = Color(0xFF00897B)

private val JqLightScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestGreen,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = Color(0xFF01579B),
    tertiary = TealAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF004D40),
    background = Color(0xFFF2FAF5),
    onBackground = Color(0xFF0D1F14),
    surface = Color(0xFFFAFFFC),
    onSurface = Color(0xFF0D1F14),
    surfaceVariant = Color(0xFFE3F4ED),
    onSurfaceVariant = Color(0xFF2D4A3A),
    outline = Color(0xFF6BA88A),
    outlineVariant = Color(0xFFB8D9C8),
)

@Composable
fun JQWaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JqLightScheme,
        content = content,
    )
}
