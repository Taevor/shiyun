package com.shi.yun.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 主题分类：经典主题 vs 四时主题
 */
enum class ThemeCategory(val displayName: String) {
    CLASSIC("经典"),
    SEASONAL("四时"),
}

/**
 * 诗云全局 10 套预设主题标识
 *
 * 经典主题 (6套)：
 * 1. 宣纸古籍 - 典雅米黄，温润护眼（默认）
 * 2. 竹青雅韵 - 清风竹影，雅致静心
 * 3. 黛青山水 - 远岫烟峦，沉静深邃
 * 4. 朱砂雅集 - 金石文会，古朴温润
 * 5. 琥珀秋笺 - 金菊暖阳，古册书香
 * 6. 墨夜清辉 - 幽夜松烟，沉浸夜读（暗色）
 *
 * 四时主题 (4套)：
 * 7. 柳烟春晓 - 翠堤春晓，鹅黄嫩柳
 * 8. 荷风清韵 - 映日荷花，微风过池
 * 9. 枫笺染霜 - 霜叶晚林，秋色连波
 * 10. 寒江雪隐 - 孤舟钓雪，素练苍茫
 */
enum class AppThemeId(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val category: ThemeCategory,
    val isDark: Boolean = false,
    val primaryPreviewColor: Color,
    val surfacePreviewColor: Color,
    val accentPreviewColor: Color,
) {
    // ========== 经典主题 (6套) ==========
    XUAN_PAPER(
        id = "xuan_paper",
        displayName = "宣纸古籍",
        subtitle = "典雅米黄 · 温润护眼",
        category = ThemeCategory.CLASSIC,
        isDark = false,
        primaryPreviewColor = Color(0xFF2B4452),
        surfacePreviewColor = Color(0xFFF6F4EE),
        accentPreviewColor = Color(0xFFB83A2A),
    ),
    BAMBOO_GREEN(
        id = "bamboo_green",
        displayName = "竹青雅韵",
        subtitle = "清风竹影 · 雅致静心",
        category = ThemeCategory.CLASSIC,
        isDark = false,
        primaryPreviewColor = Color(0xFF3D634E),
        surfacePreviewColor = Color(0xFFF2F5F3),
        accentPreviewColor = Color(0xFFA25E35),
    ),
    INDIGO_MOUNTAIN(
        id = "indigo_mountain",
        displayName = "黛青山水",
        subtitle = "远岫烟峦 · 沉静深邃",
        category = ThemeCategory.CLASSIC,
        isDark = false,
        primaryPreviewColor = Color(0xFF2D4B66),
        surfacePreviewColor = Color(0xFFF0F3F6),
        accentPreviewColor = Color(0xFF9E443B),
    ),
    CINNABAR_RED(
        id = "cinnabar_red",
        displayName = "朱砂雅集",
        subtitle = "金石文会 · 古朴温润",
        category = ThemeCategory.CLASSIC,
        isDark = false,
        primaryPreviewColor = Color(0xFF8E3529),
        surfacePreviewColor = Color(0xFFF8F3F1),
        accentPreviewColor = Color(0xFF355E52),
    ),
    AMBER_AUTUMN(
        id = "amber_autumn",
        displayName = "琥珀秋笺",
        subtitle = "金菊暖阳 · 古册书香",
        category = ThemeCategory.CLASSIC,
        isDark = false,
        primaryPreviewColor = Color(0xFF855828),
        surfacePreviewColor = Color(0xFFF7F3EA),
        accentPreviewColor = Color(0xFF48604F),
    ),
    NIGHT_INK(
        id = "night_ink",
        displayName = "墨夜清辉",
        subtitle = "幽夜松烟 · 沉浸夜读",
        category = ThemeCategory.CLASSIC,
        isDark = true,
        primaryPreviewColor = Color(0xFF7FA8C0),
        surfacePreviewColor = Color(0xFF202321),
        accentPreviewColor = Color(0xFFC25B4E),
    ),

    // ========== 四时主题 (4套) ==========
    SPRING_WILLOW(
        id = "spring_willow",
        displayName = "柳烟春晓",
        subtitle = "翠堤春晓 · 鹅黄嫩柳",
        category = ThemeCategory.SEASONAL,
        isDark = false,
        primaryPreviewColor = Color(0xFF4A6D3A),
        surfacePreviewColor = Color(0xFFF3F6ED),
        accentPreviewColor = Color(0xFFC8853A),
    ),
    SUMMER_LOTUS(
        id = "summer_lotus",
        displayName = "荷风清韵",
        subtitle = "映日荷花 · 微风出岫",
        category = ThemeCategory.SEASONAL,
        isDark = false,
        primaryPreviewColor = Color(0xFF32616E),
        surfacePreviewColor = Color(0xFFF2F6F7),
        accentPreviewColor = Color(0xFFC2566A),
    ),
    AUTUMN_MAPLE(
        id = "autumn_maple",
        displayName = "枫笺染霜",
        subtitle = "霜叶晚林 · 秋色连波",
        category = ThemeCategory.SEASONAL,
        isDark = false,
        primaryPreviewColor = Color(0xFF944630),
        surfacePreviewColor = Color(0xFFF7F2EC),
        accentPreviewColor = Color(0xFFB87B2E),
    ),
    WINTER_SNOW(
        id = "winter_snow",
        displayName = "寒江雪隐",
        subtitle = "孤舟钓雪 · 素练苍茫",
        category = ThemeCategory.SEASONAL,
        isDark = false,
        primaryPreviewColor = Color(0xFF375069),
        surfacePreviewColor = Color(0xFFEFF3F6),
        accentPreviewColor = Color(0xFF996548),
    );

    companion object {
        val Default = XUAN_PAPER

        fun fromId(id: String?): AppThemeId {
            if (id.isNullOrBlank()) return Default
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: Default
        }

        fun getByCategory(category: ThemeCategory): List<AppThemeId> {
            return entries.filter { it.category == category }
        }
    }
}
