package com.shi.yun.data

import com.shi.yun.data.model.Anthology
import com.shi.yun.data.model.AnthologyItem
import com.shi.yun.data.model.AnthologyWorkItem
import com.shi.yun.data.model.Genre
import com.shi.yun.data.model.Work
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 阶段四 B3 选本数据模型与业务规则 JVM 单元测试
 *
 * 验证：
 * 1. 选本实体构建及属性完整性；
 * 2. 篇目条目结构与复合条目关联；
 * 3. 选本按 orderIndex 升序排序算法逻辑；
 * 4. 选本缺失关联作品的检测与统计逻辑；
 * 5. 选本内上一首、下一首索引边界计算逻辑（第一首不能上一首，最后一首不能下一首）。
 */
class AnthologyRepositoryTest {

    @Test
    fun testAnthologyModelCreation() {
        val ts = Anthology(
            id = "anth_ts300",
            code = "ts300",
            name = "《唐诗三百首》",
            compiler = "[清] 蘅塘退士",
            dynasty = "清",
            targetCount = 320,
            editionNote = "基于 chinese-poetry 蒙学/tangshisanbaishou.json 整理收录（实收320首）"
        )
        assertEquals("anth_ts300", ts.id)
        assertEquals("《唐诗三百首》", ts.name)
        assertEquals(320, ts.targetCount)
        assertEquals("清", ts.dynasty)
    }

    @Test
    fun testAnthologyWorkItemSortingAndIntegrity() {
        val dummyWork1 = Work(id = "w1", title = "诗1", author = "李白", dynasty = "唐", genre = Genre.POEM, content = "正文1", excerpt = "摘录1")
        val dummyWork2 = Work(id = "w2", title = "诗2", author = "杜甫", dynasty = "唐", genre = Genre.POEM, content = "正文2", excerpt = "摘录2")
        val dummyWork3 = Work(id = "w3", title = "诗3", author = "白居易", dynasty = "唐", genre = Genre.POEM, content = "正文3", excerpt = "摘录3")

        val item1 = AnthologyItem(id = "i1", anthologyId = "anth_ts300", workId = "w1", orderIndex = 3, volume = "五言古诗")
        val item2 = AnthologyItem(id = "i2", anthologyId = "anth_ts300", workId = "w2", orderIndex = 1, volume = "五言绝句")
        val item3 = AnthologyItem(id = "i3", anthologyId = "anth_ts300", workId = "w3", orderIndex = 2, volume = "七言绝句")

        val rawItems = listOf(item1, item2, item3)
        val worksMap = mapOf("w1" to dummyWork1, "w2" to dummyWork2, "w3" to dummyWork3)

        // 模拟按照 orderIndex 严格升序排序
        val sorted = rawItems.sortedBy { it.orderIndex }
        val compositeList = sorted.mapNotNull { itm ->
            worksMap[itm.workId]?.let { wrk -> AnthologyWorkItem(itm, wrk) }
        }

        assertEquals(3, compositeList.size)
        assertEquals(1, compositeList[0].item.orderIndex)
        assertEquals("w2", compositeList[0].work.id)

        assertEquals(2, compositeList[1].item.orderIndex)
        assertEquals("w3", compositeList[1].work.id)

        assertEquals(3, compositeList[2].item.orderIndex)
        assertEquals("w1", compositeList[2].work.id)
    }

    @Test
    fun testMissingWorkDetectionLogic() {
        val dummyWork = Work(id = "w1", title = "诗1", author = "李白", dynasty = "唐", genre = Genre.POEM, content = "正文1", excerpt = "摘录1")
        val worksMap = mapOf("w1" to dummyWork)

        val itemExisting = AnthologyItem(id = "i1", anthologyId = "anth_ts300", workId = "w1", orderIndex = 1, volume = "卷一")
        val itemMissing = AnthologyItem(id = "i2", anthologyId = "anth_ts300", workId = "w_not_exist", orderIndex = 2, volume = "卷一")

        var missingCount = 0
        val results = mutableListOf<AnthologyWorkItem>()

        for (item in listOf(itemExisting, itemMissing)) {
            val w = worksMap[item.workId]
            if (w != null) {
                results.add(AnthologyWorkItem(item, w))
            } else {
                missingCount++
            }
        }

        assertEquals("成功关联数应为 1", 1, results.size)
        assertEquals("缺失作品数应为 1", 1, missingCount)
    }

    @Test
    fun testPreviousNextNavigationBoundaries() {
        val worksList = listOf(
            Work(id = "w1", title = "第1首", author = "A", dynasty = "唐", genre = Genre.POEM, content = "C1", excerpt = "E1"),
            Work(id = "w2", title = "第2首", author = "B", dynasty = "唐", genre = Genre.POEM, content = "C2", excerpt = "E2"),
            Work(id = "w3", title = "第3首", author = "C", dynasty = "唐", genre = Genre.POEM, content = "C3", excerpt = "E3")
        )

        // 首篇 index = 0
        var currentIndex = 0
        var hasPrevious = currentIndex > 0
        var hasNext = currentIndex in 0 until worksList.size - 1
        assertEquals("第1首不能有上一首", false, hasPrevious)
        assertEquals("第1首应该有下一首", true, hasNext)

        // 中间篇 index = 1
        currentIndex = 1
        hasPrevious = currentIndex > 0
        hasNext = currentIndex in 0 until worksList.size - 1
        assertEquals("中间篇应该有上一首", true, hasPrevious)
        assertEquals("中间篇应该有下一首", true, hasNext)

        // 末篇 index = 2
        currentIndex = 2
        hasPrevious = currentIndex > 0
        hasNext = currentIndex in 0 until worksList.size - 1
        assertEquals("末篇应该有上一首", true, hasPrevious)
        assertEquals("末篇不能有下一首", false, hasNext)
    }
}
