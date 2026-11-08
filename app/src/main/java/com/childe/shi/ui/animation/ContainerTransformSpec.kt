package com.childe.shi.ui.animation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 阶段五 D：容器展开/收拢转场核心几何状态与插值契约
 *
 * 核心设计原则：
 * 1. 严格使用物理像素 (px) 作为几何运算基准，杜绝 dp 与 px 混用造成的尺度畸变；
 * 2. 纯数学与状态映射函数，100% 独立于 Android 平台，保证纯 JVM 单元测试覆盖率；
 * 3. 稳健的坐标有效性验证与降级判断，防御屏幕外坐标或历史过期数据。
 */

/**
 * 转场阶段枚举
 */
enum class TransitionState {
    EXPANDING,  // 从来源项展开至全屏
    COLLAPSING, // 从全屏收拢回来源项
    IDLE        // 空闲
}

/**
 * 容器展开与收拢运动曲线参数配置
 *
 * @property enterDurationMs 展开进入耗时（毫秒）
 * @property exitDurationMs 收拢退出耗时（毫秒）
 * @property contentFadeStartFraction 正文内容开始显现的容器展开进度阈值 (0.0f ~ 1.0f)
 * @property initialCornerRadiusDp 来源卡片/条目初始圆角
 */
data class ContainerMotionSpec(
    val enterDurationMs: Int = 300,
    val exitDurationMs: Int = 260,
    val contentFadeStartFraction: Float = 0.45f,
    val initialCornerRadiusDp: Dp = 16.dp,
)

/**
 * 来源项矩形坐标有效性检查
 *
 * 判定规则：
 * 1. 矩形不能为 null，宽高必须大于 0；
 * 2. 矩形必须与全屏视口存在有效相交（至少部分可见）；
 * 3. 避免历史过期或完全滚出屏幕的坐标产生飞出屏幕的错乱视觉轨迹。
 */
fun isSourceRectValid(sourceRect: Rect?, viewportSize: Size): Boolean {
    if (sourceRect == null) return false
    if (sourceRect.width <= 0f || sourceRect.height <= 0f) return false
    if (viewportSize.width <= 0f || viewportSize.height <= 0f) return false

    // 检查是否与视口相交
    val viewportRect = Rect(0f, 0f, viewportSize.width, viewportSize.height)
    val intersects = sourceRect.left < viewportRect.right &&
            sourceRect.right > viewportRect.left &&
            sourceRect.top < viewportRect.bottom &&
            sourceRect.bottom > viewportRect.top

    return intersects
}

/**
 * 纯函数：根据动画进度 progress (0.0f -> 1.0f) 计算容器当前几何矩形 (px)
 *
 * progress = 0.0f 时返回 startRect
 * progress = 1.0f 时返回 targetRect
 */
fun interpolateRect(startRect: Rect, targetRect: Rect, progress: Float): Rect {
    val p = progress.coerceIn(0f, 1f)
    return Rect(
        left = startRect.left + (targetRect.left - startRect.left) * p,
        top = startRect.top + (targetRect.top - startRect.top) * p,
        right = startRect.right + (targetRect.right - startRect.right) * p,
        bottom = startRect.bottom + (targetRect.bottom - startRect.bottom) * p,
    )
}

/**
 * 纯函数：根据动画进度计算圆角大小 (px)
 *
 * 展开时：从初始卡片圆角逐渐过渡到全屏的 0px
 * 收拢时：从全屏的 0px 逐渐恢复到初始卡片圆角
 */
@Suppress("UNUSED_PARAMETER")
fun interpolateCornerRadiusPx(
    initialCornerRadiusPx: Float,
    progress: Float,
    isExpanding: Boolean = true,
): Float {
    val p = progress.coerceIn(0f, 1f)
    // progress = 0.0f（卡片态）圆角为 initialCornerRadiusPx；progress = 1.0f（全屏态）圆角为 0
    return (initialCornerRadiusPx * (1f - p)).coerceAtLeast(0f)
}

/**
 * 纯函数：根据容器展开进度计算详情页内部正文内容的透明度
 *
 * 避免整页文字随着外框夸张放大，确保在容器展开过半后，正文才平滑淡入；
 * 收拢时相反，正文先快速淡出，容器再聚焦收拢回原卡片。
 */
fun calculateContentAlpha(progress: Float, fadeStartThreshold: Float = 0.45f): Float {
    if (progress <= fadeStartThreshold) return 0f
    val range = (1f - fadeStartThreshold).coerceAtLeast(0.01f)
    return ((progress - fadeStartThreshold) / range).coerceIn(0f, 1f)
}
