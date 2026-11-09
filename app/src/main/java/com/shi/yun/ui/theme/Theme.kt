package com.shi.yun.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

/**
 * 全局通用字体 CompositionLocal，供所有界面直接读取当前生效字体
 */
val LocalAppFontFamily = compositionLocalOf<FontFamily> {
    FontFamily.Default
}

/**
 * 诗云全局主题与字体装配入口
 *
 * 遵循架构原则：
 * 1. 响应式装配：直接接收当前 [AppThemeId]，根据 ID 提取统一的 [AppThemeDefinition]；
 * 2. 严格同步：Material 3 的 ColorScheme 与自定义扩展 [LocalAppTheme] 同步由单一事实来源驱动；
 * 3. 全局字体联动：根据传入的 [FontFamily] 动态构建 [MaterialTheme.typography] 并注入 [LocalAppFontFamily]，彻底解决 OEM 厂商字体不生效问题；
 * 4. 系统栏联动：自适应同步系统状态栏与导航栏图标明暗风格（暗色主题白图标，浅色主题黑图标）；
 * 5. 强制关闭动态壁纸取色（Material You dynamicColor），确保诗云自成一体的纯正东方古籍美感。
 *
 * @param themeId 当前选中的全局主题标识，默认 [AppThemeId.Default]（宣纸古籍）
 * @param fontFamily 当前选中的全局字体，默认 [FontFamily.Default]
 * @param content 内容插槽
 */
@Composable
fun 诗云Theme(
    themeId: AppThemeId = AppThemeId.Default,
    fontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit,
) {
    val themeDefinition = remember(themeId) {
        AppThemePalette.getThemeDefinition(themeId)
    }

    val typography = remember(fontFamily) {
        createAppTypography(fontFamily)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // 亮色模式使用深色状态栏图标，暗色主题（如墨夜清辉）使用浅色状态栏图标
                insetsController.isAppearanceLightStatusBars = !themeDefinition.themeId.isDark
                insetsController.isAppearanceLightNavigationBars = !themeDefinition.themeId.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides themeDefinition,
        LocalAppFontFamily provides fontFamily,
    ) {
        MaterialTheme(
            colorScheme = themeDefinition.colorScheme,
            typography = typography,
            content = content,
        )
    }
}
