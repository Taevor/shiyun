package com.shi.yun.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 阶段四 B1 纯数据模型与校验断言 JVM 单元测试
 *
 * 覆盖：
 * 1. 四大受控枚举的合法解析与非法编码严格拦截；
 * 2. AuxiliaryContent 来源强一致性约束校验（正文非空时必须成对存在来源）；
 * 3. AnnotationItem.quote 与 note 非空校验；
 * 4. AttributionInfo 允许多学术参考来源；
 * 5. Work 实体构建与各属性默认值完备性。
 */
class DataModelTest {

    // -------------------------------------------------------------------------
    // 1. 枚举合法与非法编码严格测试
    // -------------------------------------------------------------------------

    @Test
    fun testGenre_strictParsing() {
        assertEquals(Genre.POEM, Genre.fromCode("poem"))
        assertEquals(Genre.CI, Genre.fromCode("CI"))
        assertEquals(Genre.QU, Genre.fromCode(" Qu "))
        assertEquals(Genre.WEN, Genre.fromCode("wen"))

        try {
            Genre.fromCode("tang")
            fail("未知体裁编码 'tang' 必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("未知的体裁编码") == true)
        }

        try {
            Genre.fromCode("")
            fail("空字符串编码必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("未知的体裁编码") == true)
        }
    }

    @Test
    fun testLayoutType_strictParsing() {
        assertEquals(LayoutType.FIVE_JUEJU, LayoutType.fromCode("five_jueju"))
        assertEquals(LayoutType.CI_DOUBLE, LayoutType.fromCode("CI_DOUBLE"))

        try {
            LayoutType.fromCode("invalid_layout")
            fail("未知排版编码必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("未知的排版编码") == true)
        }
    }

    @Test
    fun testLicenseType_strictParsing() {
        assertEquals(LicenseType.PUBLIC_DOMAIN, LicenseType.fromCode("PUBLIC_DOMAIN"))
        assertEquals(LicenseType.UNKNOWN, LicenseType.fromCode("unknown"))

        try {
            LicenseType.fromCode("FORBIDDEN_CUSTOM")
            fail("未知授权编码必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("未知的授权类型编码") == true)
        }
    }

    @Test
    fun testAttributionStatus_strictParsing() {
        assertEquals(AttributionStatus.CONFIRMED, AttributionStatus.fromCode("CONFIRMED"))
        assertEquals(AttributionStatus.DISPUTED, AttributionStatus.fromCode("disputed"))

        try {
            AttributionStatus.fromCode("UNRESOLVED")
            fail("未知归属状态编码必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("未知的归属状态编码") == true)
        }
    }

    // -------------------------------------------------------------------------
    // 2. AuxiliaryContent 来源强一致性约束测试
    // -------------------------------------------------------------------------

    @Test
    fun testAuxiliaryContent_sourceEnforcement() {
        // 全部为 null 时允许成功构造
        val emptyAux = AuxiliaryContent()
        assertNull(emptyAux.translation)
        assertNull(emptyAux.translationSource)

        val validSource = SourceInfo(sourceName = "测试来源", licenseType = LicenseType.PUBLIC_DOMAIN)

        // 正文存在且来源存在时允许成功构造
        val validAux = AuxiliaryContent(
            translation = "白话译文内容",
            translationSource = validSource
        )
        assertNotNull(validAux.translation)
        assertEquals(validSource, validAux.translationSource)

        // 有译文但无来源 -> 必须抛错
        try {
            AuxiliaryContent(translation = "白话译文", translationSource = null)
            fail("存在译文但无来源必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("translationSource") == true)
        }

        // 有注释但无来源 -> 必须抛错
        try {
            AuxiliaryContent(
                annotations = listOf(AnnotationItem(1, "床", "井栏")),
                annotationSource = null
            )
            fail("存在注释但无来源必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("annotationSource") == true)
        }

        // 有赏析但无来源 -> 必须抛错
        try {
            AuxiliaryContent(appreciation = "赏析文章", appreciationSource = null)
            fail("存在赏析但无来源必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("appreciationSource") == true)
        }

        // 有背景但无来源 -> 必须抛错
        try {
            AuxiliaryContent(background = "背景介绍", backgroundSource = null)
            fail("存在背景但无来源必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("backgroundSource") == true)
        }
    }

    // -------------------------------------------------------------------------
    // 3. AnnotationItem.quote 与 note 非空校验测试
    // -------------------------------------------------------------------------

    @Test
    fun testAnnotationItem_quoteValidation() {
        try {
            AnnotationItem(index = 1, quote = "", note = "有效解释")
            fail("quote 为空字符串必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("quote") == true)
        }

        try {
            AnnotationItem(index = 1, quote = "   ", note = "有效解释")
            fail("quote 纯空格必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("quote") == true)
        }

        try {
            AnnotationItem(index = 1, quote = "明月", note = "   ")
            fail("note 纯空格必须抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("note") == true)
        }

        val item = AnnotationItem(index = 1, quote = "明月", note = "指月亮", phonetic = "yuè", targetLine = 2)
        assertEquals("明月", item.quote)
        assertEquals("指月亮", item.note)
        assertEquals(2, item.targetLine)
    }

    // -------------------------------------------------------------------------
    // 4. AttributionInfo 允许多学术参考来源
    // -------------------------------------------------------------------------

    @Test
    fun testAttributionInfo_multipleAcademicSources() {
        val attribution = AttributionInfo(
            status = AttributionStatus.DISPUTED,
            candidateAuthors = listOf("李白", "温庭筠"),
            note = "宋代有分歧",
            academicSources = listOf("《全唐诗》", "《宋六十名家词》")
        )
        assertEquals(AttributionStatus.DISPUTED, attribution.status)
        assertEquals(2, attribution.candidateAuthors.size)
        assertEquals(2, attribution.academicSources.size)
        assertEquals("《全唐诗》", attribution.academicSources[0])
    }

    // -------------------------------------------------------------------------
    // 5. Work 实体构建与属性验证
    // -------------------------------------------------------------------------

    @Test
    fun testWork_constructionAndDefaults() {
        val work = Work(
            id = "work_000001",
            title = "静夜思",
            author = "李白",
            dynasty = "唐",
            genre = Genre.POEM,
            content = "床前明月光",
            excerpt = "床前明月光"
        )
        assertEquals("work_000001", work.id)
        assertEquals(Genre.POEM, work.genre)
        assertEquals("诗", work.genre.displayName)
        assertNull(work.cipai)
        assertNull(work.layout)
        assertNull(work.textSource)
        assertNull(work.auxiliary)
    }

    // -------------------------------------------------------------------------
    // 6. Work 格式化作者与朝代显示测试（避免空括号与悬挂标点）
    // -------------------------------------------------------------------------

    @Test
    fun testWork_formatAuthorDynasty() {
        val fullWork = Work(
            id = "1", title = "诗", author = "李白", dynasty = "唐",
            genre = Genre.POEM, content = "正文", excerpt = "摘录"
        )
        assertEquals("〔唐〕李白", fullWork.formatAuthorDynasty())
        assertEquals("〔唐〕李白  ·  诗", fullWork.formatAuthorDynastyGenre())

        val authorOnlyWork = Work(
            id = "2", title = "诗", author = "佚名", dynasty = "",
            genre = Genre.POEM, content = "正文", excerpt = "摘录"
        )
        assertEquals("佚名", authorOnlyWork.formatAuthorDynasty())
        assertEquals("佚名  ·  诗", authorOnlyWork.formatAuthorDynastyGenre())

        val dynastyOnlyWork = Work(
            id = "3", title = "诗", author = "", dynasty = "宋",
            genre = Genre.CI, content = "正文", excerpt = "摘录"
        )
        assertEquals("〔宋〕", dynastyOnlyWork.formatAuthorDynasty())
        assertEquals("〔宋〕  ·  词", dynastyOnlyWork.formatAuthorDynastyGenre())

        val emptyWork = Work(
            id = "4", title = "古诗", author = "  ", dynasty = "",
            genre = Genre.POEM, content = "正文", excerpt = "摘录"
        )
        assertEquals("", emptyWork.formatAuthorDynasty())
        assertEquals("诗", emptyWork.formatAuthorDynastyGenre())
    }

    // -------------------------------------------------------------------------
    // 7. Anthology 与 AnthologyItem 实体测试
    // -------------------------------------------------------------------------

    @Test
    fun testAnthology_constructionAndProperties() {
        val anthology = Anthology(
            id = "anth_ts300",
            code = "ts300",
            name = "《唐诗三百首》",
            compiler = "[清] 蘅塘退士",
            dynasty = "清",
            targetCount = 320,
            editionNote = "基于 chinese-poetry 整理"
        )
        assertEquals("anth_ts300", anthology.id)
        assertEquals("ts300", anthology.code)
        assertEquals(320, anthology.targetCount)

        val item = AnthologyItem(
            id = "item_tang_000001",
            anthologyId = "anth_ts300",
            workId = "tang_000001",
            orderIndex = 1,
            volume = "五言绝句"
        )
        assertEquals("item_tang_000001", item.id)
        assertEquals("anth_ts300", item.anthologyId)
        assertEquals("tang_000001", item.workId)
        assertEquals(1, item.orderIndex)
        assertEquals("五言绝句", item.volume)
    }
}
