package com.childe.shi.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.ReadingFontFamily
import com.childe.shi.data.model.ReadingPaperStyle
import com.childe.shi.data.model.ReadingPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 阶段五 B：阅读偏好模型与排版参数映射单元测试
 */
class ReadingPreferencesTest {

    @Test
    fun testDefaultValues() {
        val def = ReadingPreferences.Default
        assertEquals(ReadingFontFamily.SYSTEM_DEFAULT, def.fontType)
        assertEquals(1, def.fontWeightLevel)
        assertEquals(22f, def.bodyFontSizeSp, 0.001f)
        assertEquals(0.5f, def.letterSpacingSp, 0.001f)
        assertEquals(1.7f, def.lineHeightMultiplier, 0.001f)
        assertEquals(24f, def.horizontalPaddingDp, 0.001f)
        assertEquals(8f, def.lineSpacingDp, 0.001f)
        assertEquals(16f, def.coupletSpacingDp, 0.001f)
        assertEquals(18f, def.stanzaSpacingDp, 0.001f)
        assertEquals(20f, def.paragraphSpacingDp, 0.001f)
        assertEquals(2, def.firstLineIndentSpaces)
        assertEquals(ReadingPaperStyle.FOLLOW_THEME, def.paperStyle)
    }

    @Test
    fun testBoundaryCoercion() {
        // 测试过小值越界限制
        val tooSmall = ReadingPreferences(
            fontWeightLevel = -5,
            bodyFontSizeSp = 8f,
            letterSpacingSp = -1f,
            lineHeightMultiplier = 0.5f,
            horizontalPaddingDp = 2f,
            lineSpacingDp = 1f,
            coupletSpacingDp = 2f,
            stanzaSpacingDp = 3f,
            paragraphSpacingDp = 4f,
            firstLineIndentSpaces = -1,
        ).coerced()

        assertEquals(0, tooSmall.fontWeightLevel)
        assertEquals(14f, tooSmall.bodyFontSizeSp, 0.001f)
        assertEquals(0.0f, tooSmall.letterSpacingSp, 0.001f)
        assertEquals(1.3f, tooSmall.lineHeightMultiplier, 0.001f)
        assertEquals(12f, tooSmall.horizontalPaddingDp, 0.001f)
        assertEquals(4f, tooSmall.lineSpacingDp, 0.001f)
        assertEquals(8f, tooSmall.coupletSpacingDp, 0.001f)
        assertEquals(10f, tooSmall.stanzaSpacingDp, 0.001f)
        assertEquals(10f, tooSmall.paragraphSpacingDp, 0.001f)
        assertEquals(0, tooSmall.firstLineIndentSpaces)

        // 测试过大值越界限制
        val tooLarge = ReadingPreferences(
            fontWeightLevel = 10,
            bodyFontSizeSp = 100f,
            letterSpacingSp = 10f,
            lineHeightMultiplier = 5f,
            horizontalPaddingDp = 80f,
            lineSpacingDp = 50f,
            coupletSpacingDp = 80f,
            stanzaSpacingDp = 80f,
            paragraphSpacingDp = 100f,
            firstLineIndentSpaces = 5,
        ).coerced()

        assertEquals(2, tooLarge.fontWeightLevel)
        assertEquals(34f, tooLarge.bodyFontSizeSp, 0.001f)
        assertEquals(3.0f, tooLarge.letterSpacingSp, 0.001f)
        assertEquals(2.4f, tooLarge.lineHeightMultiplier, 0.001f)
        assertEquals(40f, tooLarge.horizontalPaddingDp, 0.001f)
        assertEquals(18f, tooLarge.lineSpacingDp, 0.001f)
        assertEquals(32f, tooLarge.coupletSpacingDp, 0.001f)
        assertEquals(36f, tooLarge.stanzaSpacingDp, 0.001f)
        assertEquals(40f, tooLarge.paragraphSpacingDp, 0.001f)
        assertEquals(2, tooLarge.firstLineIndentSpaces)
    }

    @Test
    fun testToPoemLayoutConfigMapping() {
        val prefs = ReadingPreferences(
            fontType = ReadingFontFamily.SYSTEM_SERIF,
            fontWeightLevel = 2,
            bodyFontSizeSp = 26f,
            letterSpacingSp = 1.2f,
            lineHeightMultiplier = 1.8f,
            horizontalPaddingDp = 28f,
            lineSpacingDp = 10f,
            coupletSpacingDp = 22f,
            stanzaSpacingDp = 24f,
            paragraphSpacingDp = 25f,
            firstLineIndentSpaces = 2,
            paperStyle = ReadingPaperStyle.FOLLOW_THEME,
        )

        val surfaceColor = Color(0xFFF6F4EE)
        val onSurfaceColor = Color(0xFF232624)
        val layout = prefs.toPoemLayoutConfig(surfaceColor, onSurfaceColor)

        assertEquals(26.sp, layout.bodyFontSize)
        assertEquals((26f * 1.8f).sp, layout.bodyLineHeight)
        assertEquals(1.2.sp, layout.letterSpacing)
        assertEquals(FontFamily.Serif, layout.fontFamily)
        assertEquals(FontWeight.Bold, layout.fontWeight)
        assertEquals(28.dp, layout.horizontalPadding)
        assertEquals(10.dp, layout.lineSpacing)
        assertEquals(22.dp, layout.coupletSpacing)
        assertEquals(24.dp, layout.stanzaSpacing)
        assertEquals(25.dp, layout.paragraphSpacing)
        assertEquals(2, layout.firstLineIndentSpaces)
        assertEquals(surfaceColor, layout.backgroundColor)
        assertEquals(onSurfaceColor, layout.textColor)
    }

    @Test
    fun testFontFamilyMapping() {
        val def = ReadingPreferences(fontType = ReadingFontFamily.SYSTEM_DEFAULT)
            .toPoemLayoutConfig(Color.White, Color.Black)
        assertEquals(FontFamily.Default, def.fontFamily)

        val serif = ReadingPreferences(fontType = ReadingFontFamily.SYSTEM_SERIF)
            .toPoemLayoutConfig(Color.White, Color.Black)
        assertEquals(FontFamily.Serif, serif.fontFamily)

        val mono = ReadingPreferences(fontType = ReadingFontFamily.SYSTEM_MONOSPACE)
            .toPoemLayoutConfig(Color.White, Color.Black)
        assertEquals(FontFamily.Monospace, mono.fontFamily)
    }

    @Test
    fun testFontWeightMapping() {
        val normal = ReadingPreferences(fontWeightLevel = 0)
            .toPoemLayoutConfig(Color.White, Color.Black)
        assertEquals(FontWeight.Normal, normal.fontWeight)

        val medium = ReadingPreferences(fontWeightLevel = 1)
            .toPoemLayoutConfig(Color.White, Color.Black)
        assertEquals(FontWeight.Medium, medium.fontWeight)

        val bold = ReadingPreferences(fontWeightLevel = 2)
            .toPoemLayoutConfig(Color.White, Color.Black)
        assertEquals(FontWeight.Bold, bold.fontWeight)
    }
}
