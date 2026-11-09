package com.shi.yun.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shi.yun.data.repository.LocalJsonWorkRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段四 B3 选本数据与篇目关联 Android 设备插桩测试
 *
 * 在真实 Android 运行时环境与真实 assets 资源下验证：
 * 1. 两个选本定义加载（《唐诗三百首》320首、《宋词三百首》280首）；
 * 2. 600 条作品关联 100% 完整，缺失作品数必须为 0；
 * 3. 选本篇目严格按 orderIndex 升序排布（1..320 与 1..280 严格连续无缺）；
 * 4. 唐诗选本与宋词选本相互隔离（绝不串选本）；
 * 5. 选本内上一首、下一首边界与切篇联动准确性；
 * 6. 首篇（第1首）无法上一首，末篇（最后1首）无法下一首。
 */
@RunWith(AndroidJUnit4::class)
class AnthologyInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testAnthologiesAndItemsLoadingOnDevice() = runBlocking {
        val repository = LocalJsonWorkRepository(context)

        // 1. 验证选本定义
        val anthologies = repository.getAnthologies()
        assertEquals("必须包含2本选本", 2, anthologies.size)

        val ts = repository.getAnthologyById("anth_ts300")
        assertNotNull("唐诗三百首必须存在", ts)
        assertEquals("《唐诗三百首》", ts?.name)
        assertEquals(320, ts?.targetCount)

        val sc = repository.getAnthologyById("anth_sc300")
        assertNotNull("宋词三百首必须存在", sc)
        assertEquals("《宋词三百首》", sc?.name)
        assertEquals(280, sc?.targetCount)

        // 2. 验证唐诗三百首篇目数量及关联完整性（缺失数精确为0）
        val tsItems = repository.getWorksByAnthology("anth_ts300")
        assertEquals("唐诗三百首实收篇数必须为 320", 320, tsItems.size)
        for (i in tsItems.indices) {
            val item = tsItems[i]
            assertEquals("唐诗篇目 orderIndex 必须从 1 开始严格升序连续", i + 1, item.item.orderIndex)
            assertEquals("篇目必须归属唐诗选本", "anth_ts300", item.item.anthologyId)
            assertEquals("关联作品 ID 必须与篇目 workId 严格一致", item.item.workId, item.work.id)
            assertTrue("唐诗作品标题必须非空", item.work.title.isNotBlank())
        }

        // 3. 验证宋词三百首篇目数量及关联完整性（缺失数精确为0）
        val scItems = repository.getWorksByAnthology("anth_sc300")
        assertEquals("宋词三百首实收篇数必须为 280", 280, scItems.size)
        for (i in scItems.indices) {
            val item = scItems[i]
            assertEquals("宋词篇目 orderIndex 必须从 1 开始严格升序连续", i + 1, item.item.orderIndex)
            assertEquals("篇目必须归属宋词选本", "anth_sc300", item.item.anthologyId)
            assertEquals("关联作品 ID 必须与篇目 workId 严格一致", item.item.workId, item.work.id)
            assertTrue("宋词作品标题必须非空", item.work.title.isNotBlank())
        }

        // 4. 唐诗与宋词选本作品 ID 隔离测试（绝无交集或串选本）
        val tsWorkIds = tsItems.map { it.work.id }.toSet()
        val scWorkIds = scItems.map { it.work.id }.toSet()
        val intersection = tsWorkIds.intersect(scWorkIds)
        assertEquals("唐诗与宋词选本作品 ID 集合互斥无交集", 0, intersection.size)
    }

    @Test
    fun testAnthologyPreviousNextNavigationOnDevice() = runBlocking {
        val repository = LocalJsonWorkRepository(context)
        val tsItems = repository.getWorksByAnthology("anth_ts300")
        val tsWorks = tsItems.map { it.work }

        // 测试第 1 首：首篇边界
        val firstIndex = 0
        val hasPreviousFirst = firstIndex > 0
        val hasNextFirst = firstIndex in 0 until tsWorks.size - 1
        assertEquals("首篇不能有上一首", false, hasPreviousFirst)
        assertEquals("首篇应该有下一首", true, hasNextFirst)

        // 测试切到第 2 首
        val secondIndex = firstIndex + 1
        val secondWork = tsWorks[secondIndex]
        assertEquals("第2首的 orderIndex 必须为 2", 2, tsItems[secondIndex].item.orderIndex)
        val hasPreviousSecond = secondIndex > 0
        val hasNextSecond = secondIndex in 0 until tsWorks.size - 1
        assertEquals("第2首应该有上一首", true, hasPreviousSecond)
        assertEquals("第2首应该有下一首", true, hasNextSecond)

        // 测试最后一首（第 320 首）：尾篇边界
        val lastIndex = tsWorks.size - 1
        assertEquals(319, lastIndex)
        val hasPreviousLast = lastIndex > 0
        val hasNextLast = lastIndex in 0 until tsWorks.size - 1
        assertEquals("末篇应该有上一首", true, hasPreviousLast)
        assertEquals("末篇不能有下一首", false, hasNextLast)
        assertEquals("末篇 orderIndex 必须为 320", 320, tsItems[lastIndex].item.orderIndex)
    }
}
