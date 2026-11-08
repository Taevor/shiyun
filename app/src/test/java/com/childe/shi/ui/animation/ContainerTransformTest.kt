package com.childe.shi.ui.animation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 阶段五 D：容器展开/收拢转场几何运算与降级逻辑单元测试
 */
class ContainerTransformTest {

    @Test
    fun testIsSourceRectValid() {
        val viewport = Size(1080f, 2400f)

        // 1. null 坐标无效
        assertFalse(isSourceRectValid(null, viewport))

        // 2. 宽高为 0 或负数无效
        assertFalse(isSourceRectValid(Rect(100f, 100f, 100f, 200f), viewport))
        assertFalse(isSourceRectValid(Rect(100f, 100f, 200f, 100f), viewport))

        // 3. 正常视口内卡片有效
        val validCard = Rect(50f, 200f, 1030f, 600f)
        assertTrue(isSourceRectValid(validCard, viewport))

        // 4. 部分在屏幕外但有交集有效
        val partialCard = Rect(-100f, 2000f, 500f, 2500f)
        assertTrue(isSourceRectValid(partialCard, viewport))

        // 5. 完全滚出屏幕上方无效
        val scrolledAbove = Rect(50f, -500f, 1030f, -50f)
        assertFalse(isSourceRectValid(scrolledAbove, viewport))

        // 6. 完全滚出屏幕下方无效
        val scrolledBelow = Rect(50f, 2500f, 1030f, 2900f)
        assertFalse(isSourceRectValid(scrolledBelow, viewport))
    }

    @Test
    fun testInterpolateRect() {
        val start = Rect(100f, 200f, 500f, 800f)
        val target = Rect(0f, 0f, 1080f, 2400f)

        // progress = 0f 时等于 start
        val r0 = interpolateRect(start, target, 0f)
        assertEquals(start.left, r0.left, 0.001f)
        assertEquals(start.top, r0.top, 0.001f)
        assertEquals(start.right, r0.right, 0.001f)
        assertEquals(start.bottom, r0.bottom, 0.001f)

        // progress = 1f 时等于 target
        val r1 = interpolateRect(start, target, 1f)
        assertEquals(target.left, r1.left, 0.001f)
        assertEquals(target.top, r1.top, 0.001f)
        assertEquals(target.right, r1.right, 0.001f)
        assertEquals(target.bottom, r1.bottom, 0.001f)

        // progress = 0.5f 中间值
        val rHalf = interpolateRect(start, target, 0.5f)
        assertEquals(50f, rHalf.left, 0.001f)
        assertEquals(100f, rHalf.top, 0.001f)
        assertEquals(790f, rHalf.right, 0.001f)
        assertEquals(1600f, rHalf.bottom, 0.001f)
    }

    @Test
    fun testInterpolateCornerRadiusPx() {
        val initialRadiusPx = 48f

        // progress = 0f 时几何处于来源卡片位置，圆角必须为完整的卡片初始圆角 (initialRadiusPx)
        assertEquals(48f, interpolateCornerRadiusPx(initialRadiusPx, 0f), 0.001f)
        // progress = 0.5f 时处于中间过渡态，圆角平滑过渡 (initialRadiusPx * 0.5)
        assertEquals(24f, interpolateCornerRadiusPx(initialRadiusPx, 0.5f), 0.001f)
        // progress = 1.0f 时几何处于全屏视口，圆角完全展开为 0 (0f)
        assertEquals(0f, interpolateCornerRadiusPx(initialRadiusPx, 1f), 0.001f)

        // 无论展开还是收拢，progress 映射到几何矩形均保持一致：0f 对应卡片 (48f)，1f 对应全屏 (0f)
        assertEquals(48f, interpolateCornerRadiusPx(initialRadiusPx, 0f, isExpanding = false), 0.001f)
        assertEquals(24f, interpolateCornerRadiusPx(initialRadiusPx, 0.5f, isExpanding = false), 0.001f)
        assertEquals(0f, interpolateCornerRadiusPx(initialRadiusPx, 1f, isExpanding = false), 0.001f)
    }

    @Test
    fun testCalculateContentAlpha() {
        val threshold = 0.45f

        // progress 未达阈值时透明度为 0
        assertEquals(0f, calculateContentAlpha(0f, threshold), 0.001f)
        assertEquals(0f, calculateContentAlpha(0.3f, threshold), 0.001f)
        assertEquals(0f, calculateContentAlpha(0.45f, threshold), 0.001f)

        // progress 达到 1.0f 时透明度完全显现 (1.0f)
        assertEquals(1.0f, calculateContentAlpha(1.0f, threshold), 0.001f)

        // 中间插值平滑递增
        val alphaHalf = calculateContentAlpha(0.725f, threshold)
        assertEquals(0.5f, alphaHalf, 0.01f)
    }
}
