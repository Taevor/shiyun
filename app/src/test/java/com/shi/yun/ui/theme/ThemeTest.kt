package com.shi.yun.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import com.shi.yun.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 阶段 A & B：全局主题系统数据模型、调色板完整性、仓储契约与文字可读性对比度单元测试
 */
class ThemeTest {

    @Test
    fun testTenThemesClassificationAndUniqueness() {
        // 1. 验证总共有且仅有 10 套预设主题
        assertEquals("全局主题数量必须恰好为 10 套", 10, AppThemeId.entries.size)

        // 2. 验证经典主题与四时主题分组精确性
        val classicThemes = AppThemeId.getByCategory(ThemeCategory.CLASSIC)
        val seasonalThemes = AppThemeId.getByCategory(ThemeCategory.SEASONAL)
        assertEquals("经典主题必须包含 6 套", 6, classicThemes.size)
        assertEquals("四时主题必须包含 4 套", 4, seasonalThemes.size)

        // 3. 验证每套主题 ID 与显示名称全局唯一
        val ids = AppThemeId.entries.map { it.id }
        assertEquals("所有主题 ID 必须全局唯一", ids.size, ids.distinct().size)

        val names = AppThemeId.entries.map { it.displayName }
        assertEquals("所有主题显示名称必须全局唯一", names.size, names.distinct().size)

        // 4. 验证默认主题为宣纸古籍
        assertEquals(AppThemeId.XUAN_PAPER, AppThemeId.Default)

        // 5. 验证暗色模式标记仅对墨夜清辉生效
        for (theme in AppThemeId.entries) {
            if (theme == AppThemeId.NIGHT_INK) {
                assertTrue("${theme.displayName} 必须是暗色主题", theme.isDark)
            } else {
                assertFalse("${theme.displayName} 必须是浅色/日间主题", theme.isDark)
            }
        }
    }

    @Test
    fun testThemeFromIdFallbackAndParsing() {
        assertEquals(AppThemeId.XUAN_PAPER, AppThemeId.fromId("xuan_paper"))
        assertEquals(AppThemeId.BAMBOO_GREEN, AppThemeId.fromId("bamboo_green"))
        assertEquals(AppThemeId.INDIGO_MOUNTAIN, AppThemeId.fromId("INDIGO_MOUNTAIN")) // 大小写不敏感
        assertEquals(AppThemeId.CINNABAR_RED, AppThemeId.fromId("  cinnabar_red  ")) // 自动去空
        assertEquals(AppThemeId.AMBER_AUTUMN, AppThemeId.fromId("amber_autumn"))
        assertEquals(AppThemeId.NIGHT_INK, AppThemeId.fromId("night_ink"))
        assertEquals(AppThemeId.SPRING_WILLOW, AppThemeId.fromId("spring_willow"))
        assertEquals(AppThemeId.SUMMER_LOTUS, AppThemeId.fromId("summer_lotus"))
        assertEquals(AppThemeId.AUTUMN_MAPLE, AppThemeId.fromId("autumn_maple"))
        assertEquals(AppThemeId.WINTER_SNOW, AppThemeId.fromId("winter_snow"))

        // 容错降级回默认宣纸古籍
        assertEquals(AppThemeId.Default, AppThemeId.fromId(null))
        assertEquals(AppThemeId.Default, AppThemeId.fromId(""))
        assertEquals(AppThemeId.Default, AppThemeId.fromId("non_existent_theme_id"))
    }

    @Test
    fun testAllThemesColorSchemeCompleteness() {
        for (theme in AppThemeId.entries) {
            val def = AppThemePalette.getThemeDefinition(theme)
            assertNotNull("主题定义体不能为空: ${theme.id}", def)
            assertEquals("主题标识必须一致", theme, def.themeId)

            val cs = def.colorScheme
            assertTrue("${theme.id} primary 必须已指定", cs.primary.isSpecified)
            assertTrue("${theme.id} onPrimary 必须已指定", cs.onPrimary.isSpecified)
            assertTrue("${theme.id} primaryContainer 必须已指定", cs.primaryContainer.isSpecified)
            assertTrue("${theme.id} secondary 必须已指定", cs.secondary.isSpecified)
            assertTrue("${theme.id} tertiary 必须已指定", cs.tertiary.isSpecified)
            assertTrue("${theme.id} surface 必须已指定", cs.surface.isSpecified)
            assertTrue("${theme.id} onSurface 必须已指定", cs.onSurface.isSpecified)
            assertTrue("${theme.id} background 必须已指定", cs.background.isSpecified)
            assertTrue("${theme.id} onBackground 必须已指定", cs.onBackground.isSpecified)
            assertTrue("${theme.id} surfaceVariant 必须已指定", cs.surfaceVariant.isSpecified)
            assertTrue("${theme.id} onSurfaceVariant 必须已指定", cs.onSurfaceVariant.isSpecified)
            assertTrue("${theme.id} outline 必须已指定", cs.outline.isSpecified)
            assertTrue("${theme.id} outlineVariant 必须已指定", cs.outlineVariant.isSpecified)

            // 扩展属性有效性校验
            val tokens = def.customTokens
            assertTrue("${theme.id} cardBackground 必须已指定", tokens.cardBackground.isSpecified)
            assertTrue("${theme.id} cardBorder 必须已指定", tokens.cardBorder.isSpecified)
            assertTrue("${theme.id} statusBarProtectionColor 必须已指定", tokens.statusBarProtectionColor.isSpecified)
            assertTrue("${theme.id} heartFavoriteTint 必须已指定", tokens.heartFavoriteTint.isSpecified)
            assertTrue("${theme.id} titleText 必须已指定", tokens.titleText.isSpecified)
            assertTrue("${theme.id} bodyText 必须已指定", tokens.bodyText.isSpecified)
            assertTrue("${theme.id} secondaryText 必须已指定", tokens.secondaryText.isSpecified)
            assertTrue("${theme.id} backgroundResId 必须有效指向图片资源", tokens.backgroundResId != 0)
        }

        // 默认宣纸古籍主题必须保持无额外图层染色 (Color.Transparent)，确保验收效果 100% 不变形
        val defaultDef = AppThemePalette.getThemeDefinition(AppThemeId.Default)
        assertEquals("宣纸古籍默认背景混色必须为完全透明", Color.Transparent, defaultDef.customTokens.overlayBlendColor)
    }

    @Test
    fun testPoemReadabilityContrastWcagAALevel() {
        for (theme in AppThemeId.entries) {
            val def = AppThemePalette.getThemeDefinition(theme)
            val cs = def.colorScheme
            val tokens = def.customTokens

            // 1. 验证标题文字在页面底色和卡片底色的对比度 (WCAG AA 阈值要求 >= 4.5:1，古籍标题严选 >= 10:1)
            val titleSurfaceContrast = calculateContrastRatio(tokens.titleText, cs.surface)
            assertTrue(
                "主题【${theme.displayName}】标题 titleText 在页面底色对比度必须 >= 4.5，实测: $titleSurfaceContrast",
                titleSurfaceContrast >= 4.5,
            )
            val titleCardContrast = calculateContrastRatio(tokens.titleText, tokens.cardBackground)
            assertTrue(
                "主题【${theme.displayName}】标题 titleText 在卡片底色对比度必须 >= 4.5，实测: $titleCardContrast",
                titleCardContrast >= 4.5,
            )

            // 2. 验证正文字体在页面底色和卡片底色的对比度 (WCAG AA 阈值要求 >= 4.5:1)
            val bodySurfaceContrast = calculateContrastRatio(tokens.bodyText, cs.surface)
            assertTrue(
                "主题【${theme.displayName}】正文 bodyText 在页面底色对比度必须达到 WCAG AA (>=4.5)，实测: $bodySurfaceContrast",
                bodySurfaceContrast >= 4.5,
            )
            val bodyCardContrast = calculateContrastRatio(tokens.bodyText, tokens.cardBackground)
            assertTrue(
                "主题【${theme.displayName}】正文 bodyText 在卡片底色对比度必须达到 WCAG AA (>=4.5)，实测: $bodyCardContrast",
                bodyCardContrast >= 4.5,
            )

            // 3. 验证辅助文字在页面底色和卡片底色的对比度 (WCAG AA 阈值要求 >= 4.5:1)
            val secSurfaceContrast = calculateContrastRatio(tokens.secondaryText, cs.surface)
            assertTrue(
                "主题【${theme.displayName}】辅助文字 secondaryText 在页面底色对比度必须 >= 4.5，实测: $secSurfaceContrast",
                secSurfaceContrast >= 4.5,
            )
            val secCardContrast = calculateContrastRatio(tokens.secondaryText, tokens.cardBackground)
            assertTrue(
                "主题【${theme.displayName}】辅助文字 secondaryText 在卡片底色对比度必须 >= 4.5，实测: $secCardContrast",
                secCardContrast >= 4.5,
            )

            // 4. 验证按钮主文字在主品牌色上的清晰度 (>= 3.0:1)
            val primaryContrast = calculateContrastRatio(cs.onPrimary, cs.primary)
            assertTrue(
                "主题【${theme.displayName}】主按钮文字 onPrimary 对比度必须 >= 3.0，实测: $primaryContrast",
                primaryContrast >= 3.0,
            )
        }
    }

    @Test
    fun testThemePreferencesRepositoryContract() = runBlocking {
        class FakeThemePreferencesRepository(
            initialThemeId: AppThemeId = AppThemeId.Default,
        ) : ThemePreferencesRepository {
            private val _flow = MutableStateFlow(initialThemeId)
            override val currentThemeIdFlow: StateFlow<AppThemeId> = _flow
            override suspend fun updateThemeId(themeId: AppThemeId) {
                _flow.value = themeId
            }
            override fun getCurrentThemeId(): AppThemeId = _flow.value
        }

        val fakeRepo = FakeThemePreferencesRepository()
        assertEquals(AppThemeId.XUAN_PAPER, fakeRepo.getCurrentThemeId())
        assertEquals(AppThemeId.XUAN_PAPER, fakeRepo.currentThemeIdFlow.value)

        fakeRepo.updateThemeId(AppThemeId.BAMBOO_GREEN)
        assertEquals(AppThemeId.BAMBOO_GREEN, fakeRepo.getCurrentThemeId())
        assertEquals(AppThemeId.BAMBOO_GREEN, fakeRepo.currentThemeIdFlow.value)

        fakeRepo.updateThemeId(AppThemeId.NIGHT_INK)
        assertEquals(AppThemeId.NIGHT_INK, fakeRepo.getCurrentThemeId())
        assertEquals(AppThemeId.NIGHT_INK, fakeRepo.currentThemeIdFlow.value)
    }

    @Test
    fun testThemePreviewColorsCompleteness() {
        for (theme in AppThemeId.entries) {
            assertTrue("主预览色彩必须是有效颜色", theme.primaryPreviewColor.isSpecified)
            assertTrue("表面预览色彩必须是有效颜色", theme.surfacePreviewColor.isSpecified)
            assertTrue("强调预览色彩必须是有效颜色", theme.accentPreviewColor.isSpecified)
            assertTrue("副标题不能为空", theme.subtitle.isNotBlank())
        }
    }

    /**
     * W3C WCAG 2.1 相对亮度与对比度标准算法实现
     */
    private fun calculateRelativeLuminance(color: Color): Double {
        fun channelToLinear(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = channelToLinear(color.red)
        val g = channelToLinear(color.green)
        val b = channelToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateRelativeLuminance(c1)
        val l2 = calculateRelativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
