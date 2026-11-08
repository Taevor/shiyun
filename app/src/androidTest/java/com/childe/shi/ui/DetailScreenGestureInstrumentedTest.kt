package com.childe.shi.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.childe.shi.data.model.AnthologyItem
import com.childe.shi.data.model.AnthologyWorkItem
import com.childe.shi.data.model.Genre
import com.childe.shi.data.model.LayoutType
import com.childe.shi.data.model.ReadingPreferences
import com.childe.shi.data.model.Work
import com.childe.shi.data.model.formatAuthorDynasty
import com.childe.shi.ui.detail.PoemLayoutConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段五 B & C：选本排版作者格式化、阅读版式契约及古典水墨背景生成插桩测试
 */
@RunWith(AndroidJUnit4::class)
class DetailScreenGestureInstrumentedTest {

    @Test
    fun testAnthologyAuthorFormattingMatchesDynastyAndGenre() {
        val workWithDynasty = Work(
            id = "w1",
            title = "行宫",
            author = "元稹",
            dynasty = "唐",
            genre = Genre.POEM,
            layout = LayoutType.FIVE_JUEJU,
            content = "寥落古行宫，宫花寂寞红。\n白头宫女在，闲坐说玄宗。",
            excerpt = "寥落古行宫，宫花寂寞红。",
        )
        val formatted = workWithDynasty.formatAuthorDynasty().ifEmpty { workWithDynasty.author }
        assertEquals("〔唐〕元稹", formatted)

        val item = AnthologyWorkItem(
            item = AnthologyItem(
                id = "ts300_001",
                anthologyId = "anth_ts300",
                workId = "w1",
                orderIndex = 1,
                volume = "五言绝句",
            ),
            work = workWithDynasty,
        )

        assertEquals("五言绝句", item.item.volume)
        assertEquals(1, item.item.orderIndex)
    }

    @Test
    fun testPoemLayoutConfigGenerationFromPreferences() {
        val prefs = ReadingPreferences(
            bodyFontSizeSp = 24f,
            lineHeightMultiplier = 1.8f,
            letterSpacingSp = 1.0f,
            horizontalPaddingDp = 28f,
            lineSpacingDp = 10f,
            coupletSpacingDp = 18f,
            stanzaSpacingDp = 20f,
            paragraphSpacingDp = 22f,
            firstLineIndentSpaces = 2,
        )

        val config: PoemLayoutConfig = prefs.toPoemLayoutConfig(
            themeSurface = Color.White,
            themeOnSurface = Color.Black,
        )

        assertEquals(24.sp, config.bodyFontSize)
        assertEquals((24 * 1.8f).sp, config.bodyLineHeight)
        assertEquals(1.0.sp, config.letterSpacing)
        assertEquals(28.dp, config.horizontalPadding)
        assertEquals(10.dp, config.lineSpacing)
        assertEquals(18.dp, config.coupletSpacing)
        assertEquals(20.dp, config.stanzaSpacing)
        assertEquals(22.dp, config.paragraphSpacing)
        assertEquals(2, config.firstLineIndentSpaces)
    }

    @Test
    fun testHomeBackgroundWebpExistsAndLoads() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, com.childe.shi.R.drawable.bg_home_landscape)
        assertNotNull(drawable)
    }
}
