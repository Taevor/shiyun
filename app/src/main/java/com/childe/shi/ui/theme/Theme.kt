package com.childe.shi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 诗云淡雅古风配色方案（浅色为主：宣纸米白、青墨、黛青、竹青、朱砂）
 */
private val ClassicalLightColorScheme = lightColorScheme(
    primary = DaiIndigo,
    onPrimary = PaperCream,
    primaryContainer = PaperSilk,
    onPrimaryContainer = DaiIndigo,
    secondary = BambooGreen,
    onSecondary = PaperCream,
    secondaryContainer = PaperSilk,
    onSecondaryContainer = BambooGreen,
    tertiary = CinnabarRed,
    onTertiary = Color.White,
    background = PaperCream,
    onBackground = InkDark,
    surface = PaperCream,
    onSurface = InkDark,
    surfaceVariant = PaperSilk,
    onSurfaceVariant = InkMedium,
    outline = InkMedium,
    outlineVariant = InkDivider,
)

/**
 * 诗云夜读古风配色方案（深色底墨竹灰）
 */
private val ClassicalDarkColorScheme = darkColorScheme(
    primary = DarkDaiIndigo,
    onPrimary = DarkPaperBackground,
    primaryContainer = DarkPaperCard,
    onPrimaryContainer = DarkDaiIndigo,
    secondary = DarkBambooGreen,
    onSecondary = DarkPaperBackground,
    secondaryContainer = DarkPaperCard,
    onSecondaryContainer = DarkBambooGreen,
    tertiary = CinnabarRed,
    onTertiary = Color.White,
    background = DarkPaperBackground,
    onBackground = DarkInkText,
    surface = DarkPaperSurface,
    onSurface = DarkInkText,
    surfaceVariant = DarkPaperCard,
    onSurfaceVariant = DarkInkMedium,
    outline = DarkInkMedium,
    outlineVariant = Color(0x33E4E1DA),
)

@Composable
fun 诗云Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 强制关闭动态彩色取色，确保无论用户系统壁纸为何色，诗云都严格维持统一淡雅古风
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ClassicalDarkColorScheme else ClassicalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
