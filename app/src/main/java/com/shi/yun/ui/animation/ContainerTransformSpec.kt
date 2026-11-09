package com.shi.yun.ui.animation

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
 * @property enterFadeStartFraction 展开进入时详情内容开始显现的进度阈值 (0.0f ~ 1.0f)
 * @property exitFadeStartFraction 收拢退出时详情内容完全消失的进度阈值 (0.0f ~ 1.0f)
 * @property initialCornerRadiusDp 来源卡片/条目初始圆角
 * @property contentFadeStartFraction 兼容旧命名
 */
data class ContainerMotionSpec(
    val enterDurationMs: Int = 300,
    val exitDurationMs: Int = 260,
    val enterFadeStartFraction: Float = 0.35f,
    val exitFadeStartFraction: Float = 0.50f,
    val initialCornerRadiusDp: Dp = 16.dp,
    val contentFadeStartFraction: Float = enterFadeStartFraction,
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
 * 纯函数：根据容器展开/收拢进度计算详情页内部正文与操作栏内容的透明度
 *
 * 方案 B 核心解耦策略：
 * 1. 展开进入时 (isDismissing = false)：
 *    在 progress <= enterFadeThreshold (默认 0.35f) 时详情内容保持 0f，让容器先展开卡片底色与轮廓；
 *    在 progress 从 0.35f 到 1.0f 期间平滑淡入，与全屏视口自然衔接。
 * 2. 收拢退出时 (isDismissing = true)：
 *    在 progress 从 1.0f 降到 exitFadeThreshold (默认 0.50f) 过程中详情内容完全淡出；
 *    在 progress <= 0.50f 时详情内容完全隐匿 (alpha = 0f)，仅保留纯宣纸卡片背景与 16dp 圆角轮廓继续收拢回来源卡片；
 *    避免缩小的操作栏、返回键与底层卡片发生元素重叠与视觉打架。
 */
fun calculateContentAlpha(
    progress: Float,
    isDismissing: Boolean = false,
    enterFadeThreshold: Float = 0.35f,
    exitFadeThreshold: Float = 0.50f,
): Float {
    val threshold = if (isDismissing) exitFadeThreshold else enterFadeThreshold
    if (progress <= threshold) return 0f
    val range = (1f - threshold).coerceAtLeast(0.01f)
    return ((progress - threshold) / range).coerceIn(0f, 1f)
}

/**
 * 兼容单阈值旧签名的重载函数
 */
fun calculateContentAlpha(progress: Float, fadeStartThreshold: Float): Float {
    return calculateContentAlpha(
        progress = progress,
        isDismissing = false,
        enterFadeThreshold = fadeStartThreshold,
    )
}

/**
 * 纯函数：根据容器展开/收拢进度计算底层页面（首页卡片、分类列表、底部导航栏）的透明度
 *
 * 核心解耦策略：
 * 1. 展开进入时 (isDismissing = false)：
 *    在 progress 从 0.0f 增长到 fadeOutThreshold (默认 0.35f) 期间，底层页面文字和组件平滑淡出至 0f；
 *    在 progress >= 0.35f 阶段（包括详情页全屏显示），底层页面透明度严格为 0f，完全不绘制任何文字，彻底杜绝底层卡片/分类列表文字与详情页诗词重叠。
 * 2. 收拢退出时 (isDismissing = true)：
 *    在 progress 从 1.0f 降至 fadeOutThreshold (默认 0.35f) 期间，底层页面保持 0f 不显现；
 *    在 progress 从 0.35f 降到 0.0f 期间，底层页面平滑淡入至 1.0f，无缝还原为初始页面。
 */
fun calculateUnderlyingAlpha(
    progress: Float,
    fadeOutThreshold: Float = 0.35f,
): Float {
    if (progress <= 0f) return 1.0f
    if (progress >= fadeOutThreshold) return 0.0f
    val range = fadeOutThreshold.coerceAtLeast(0.01f)
    return ((fadeOutThreshold - progress) / range).coerceIn(0f, 1f)
}
