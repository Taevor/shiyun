package com.shi.yun.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 诗云古典扩展主题属性（超越 M3 默认字段的古籍定制视觉属性）
 *
 * @property cardBackground 诗词卡片、反馈浮层底色
 * @property cardBorder 卡片细微墨线边框色
 * @property overlayBlendColor 首页山水插画的主题微调混色（专属背景图模式下默认为完全透明）
 * @property statusBarProtectionColor 状态栏顶部宣纸渐变保护底色
 * @property heartFavoriteTint 收藏红心高亮强调色（各主题可典雅微调）
 * @property titleText 诗词主标题、大字摘录核心标题色
 * @property bodyText 诗词正文、小序内容、释义说明主文字色
 * @property secondaryText 作者、朝代、体裁、注脚辅助文字色
 * @property backgroundResId 首页专属水墨山水背景图 Drawable 资源 ID
 */
data class AppThemeCustomTokens(
    val cardBackground: Color,
    val cardBorder: Color,
    val overlayBlendColor: Color = Color.Transparent,
    val statusBarProtectionColor: Color,
    val heartFavoriteTint: Color = Color(0xFFB83A2A),
    val titleText: Color,
    val bodyText: Color,
    val secondaryText: Color,
    val backgroundResId: Int,
)

/**
 * 单套全局主题的完整定义体
 */
data class AppThemeDefinition(
    val themeId: AppThemeId,
    val colorScheme: ColorScheme,
    val customTokens: AppThemeCustomTokens,
)

/**
 * 全局主题 CompositionLocal，供顶层和特殊视觉组件（如首页山水滤镜、反馈卡片）获取扩展属性
 */
val LocalAppTheme = compositionLocalOf<AppThemeDefinition> {
    AppThemePalette.getThemeDefinition(AppThemeId.Default)
}
