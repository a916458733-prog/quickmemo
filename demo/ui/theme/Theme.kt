package com.quickmemo.demo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 品牌色 — 温暖橙色，传达"快速、活力"
val BrandOrange = Color(0xFFFF6B35)
val BrandOrangeDark = Color(0xFFFF8A5C)
val RecordingRed = Color(0xFFE53935)
val RecordingRedDark = Color(0xFFEF5350)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D0),
    secondary = Color(0xFF5C6BC0),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    error = RecordingRed,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    outline = Color(0xFFE0E0E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrangeDark,
    onPrimary = Color(0xFF1C1B1F),
    primaryContainer = Color(0xFF5D2E1A),
    secondary = Color(0xFF7986CB),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    error = RecordingRedDark,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    outline = Color(0xFF3A3A3A)
)

@Composable
fun QuickMemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
