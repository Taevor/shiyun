package com.shi.yun.ui

import com.shi.yun.ui.home.SwipeDirection
import com.shi.yun.ui.home.resolveSwipeAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 阶段五 C：首页多方向卡片滑动手势判定与主轴识别单元测试
 */
class HomeSwipeDirectionTest {

    private val distanceThreshold = 200f
    private val velocityThreshold = 1500f

    @Test
    fun testSubThresholdReturnsNone() {
        // 未达到距离且未达到速度阈值
        val result = resolveSwipeAction(
            dragX = 50f,
            dragY = 50f,
            distanceThreshold = distanceThreshold,
            velocityThreshold = velocityThreshold,
        )
        assertEquals(SwipeDirection.NONE, result)
    }

    @Test
    fun testHorizontalSwipes() {
        // 向左滑动达到阈值 -> 下一首
        val leftSwipe = resolveSwipeAction(
            dragX = -250f,
            dragY = 30f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.NEXT, leftSwipe)

        // 向右滑动达到阈值 -> 上一首
        val rightSwipe = resolveSwipeAction(
            dragX = 250f,
            dragY = -20f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.PREVIOUS, rightSwipe)
    }

    @Test
    fun testVerticalSwipes() {
        // 向上滑动达到阈值 -> 下一首
        val upSwipe = resolveSwipeAction(
            dragX = 10f,
            dragY = -260f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.NEXT, upSwipe)

        // 向下滑动达到阈值 -> 上一首
        val downSwipe = resolveSwipeAction(
            dragX = -10f,
            dragY = 260f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.PREVIOUS, downSwipe)
    }

    @Test
    fun testDiagonalSwipesDominantAxis() {
        // 斜向左上，但水平分量更大 (|dragX| > |dragY|) -> 判定为左滑 (下一首)
        val diagHorizontalNext = resolveSwipeAction(
            dragX = -240f,
            dragY = -180f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.NEXT, diagHorizontalNext)

        // 斜向左上，但垂直分量更大 (|dragY| > |dragX|) -> 判定为上滑 (下一首)
        val diagVerticalNext = resolveSwipeAction(
            dragX = -150f,
            dragY = -250f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.NEXT, diagVerticalNext)

        // 斜向右上，垂直分量更大 (|dragY| > |dragX|，向下) -> 判定为下滑 (上一首)
        val diagVerticalPrev = resolveSwipeAction(
            dragX = 100f,
            dragY = 250f,
            distanceThreshold = distanceThreshold,
        )
        assertEquals(SwipeDirection.PREVIOUS, diagVerticalPrev)
    }

    @Test
    fun testVelocityFlingTrigger() {
        // 位移较小（100f < 200f），但轻拂滑动速度很大（2000f > 1500f）且同向 -> 判定有效切换
        val fastFlingLeft = resolveSwipeAction(
            dragX = -100f,
            dragY = 10f,
            velocityX = -2000f,
            velocityY = 0f,
            distanceThreshold = distanceThreshold,
            velocityThreshold = velocityThreshold,
        )
        assertEquals(SwipeDirection.NEXT, fastFlingLeft)
    }
}
