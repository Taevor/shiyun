package com.shi.yun.ui.animation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 局部组合项：向下传递容器内正文渐进呈现的透明度 (0.0f ~ 1.0f)
 */
val LocalContainerContentAlpha = compositionLocalOf { 1.0f }

/**
 * 局部组合项：向下传递当前转场容器是否处于收拢退出阶段 (isDismissing)
 */
val LocalContainerIsDismissing = compositionLocalOf { false }

/**
 * 原生容器展开/收拢转场覆盖层组件 (ContainerTransformBox)
 *
 * 核心特性：
 * 1. 矩形尺寸、位置、圆角动态插值与硬件加速裁剪；
 * 2. 详情页背景渐显，标题自然过渡，正文通过 LocalContainerContentAlpha 延迟淡入；
 * 3. 来源矩形不可用或超出视口时，自动降级为中央微缩放 (0.94f -> 1.0f) 与柔和渐隐；
 * 4. 动画执行中全程拦截底层一切触摸事件，杜绝竞态与重复创建；
 * 5. 系统返回键与返回按钮行为严格统一，均触发平滑收拢动画；
 * 6. 收拢终点处平滑过渡，圆角精确还原，杜绝直角闪现。
 */
@Composable
fun ContainerTransformBox(
    visible: Boolean,
    sourceRect: Rect?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    spec: ContainerMotionSpec = remember { ContainerMotionSpec() },
    underlyingContent: @Composable () -> Unit = {},
    content: @Composable (onRequestDismiss: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    var viewportSize by remember { mutableStateOf(Size.Zero) }
    val progress = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    // 检查来源矩形是否有效
    val hasValidSource = remember(sourceRect, viewportSize) {
        isSourceRectValid(sourceRect, viewportSize)
    }

    // 统一的关闭触发逻辑
    val requestDismiss = {
        if (!isDismissing) {
            isDismissing = true
        }
    }

    // 拦截系统返回键：在转场容器内响应返回，动画执行中持续拦截防止二次重入
    BackHandler(enabled = visible) {
        requestDismiss()
    }

    // 当 visible 从 false 变 true 时驱动展开动画 (0f -> 1f)；从 true 变 false 时重置状态
    LaunchedEffect(visible) {
        if (visible) {
            isDismissing = false
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = spec.enterDurationMs,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            isDismissing = false
            progress.snapTo(0f)
        }
    }

    // 驱动收拢退出动画 (1f -> 0f)
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = spec.exitDurationMs,
                    easing = FastOutSlowInEasing,
                ),
            )
            onDismissRequest()
        }
    }

    val currentProgress = progress.value
    val initialRadiusPx = with(density) { spec.initialCornerRadiusDp.toPx() }

    // 底层内容透明度：展开时迅速淡出（0.0 -> 0.35），退出时平滑淡入（0.35 -> 0.0）
    // 当处于详情页中（visible 且 currentProgress >= 0.35f），底层完全不可见（alpha = 0f），彻底消除文字重叠
    val underlyingAlpha = if (!visible) {
        1f
    } else {
        calculateUnderlyingAlpha(currentProgress, spec.enterFadeStartFraction)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewportSize = Size(it.width.toFloat(), it.height.toFloat())
            },
    ) {
        // 底层页面内容区（当转场发生并进入详情页时，平滑淡出，彻底避免底层文字、卡片与底部导航栏与详情页内容重叠）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = underlyingAlpha
                },
        ) {
            underlyingContent()
        }

        // 详情页浮层及转场动画（仅在 visible 激活时进入渲染与交互拦截）
        if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // 彻底拦截转场层底层的所有触控事件，防止穿透到底层页面与底部导航栏
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                if (event.changes.all { !it.pressed }) break
                            }
                        }
                    },
            ) {
                if (viewportSize.width > 0f && viewportSize.height > 0f) {
                    val fullScreenRect = Rect(0f, 0f, viewportSize.width, viewportSize.height)

                    if (hasValidSource && sourceRect != null) {
                        // ========== 方案 A：有效的来源几何展开/收拢 ==========
                        val currentRect = interpolateRect(sourceRect, fullScreenRect, currentProgress)
                        val currentRadiusPx = interpolateCornerRadiusPx(
                            initialCornerRadiusPx = initialRadiusPx,
                            progress = currentProgress,
                            isExpanding = !isDismissing,
                        )
                        val currentRadiusDp = with(density) { currentRadiusPx.toDp() }
                        val contentAlpha = calculateContentAlpha(
                            progress = currentProgress,
                            isDismissing = isDismissing,
                            enterFadeThreshold = spec.enterFadeStartFraction,
                            exitFadeThreshold = spec.exitFadeStartFraction,
                        )

                        // 终点交接平滑淡出：在最接近来源卡片终点（progress < 0.04f）时平滑淡入/淡出，与底层卡片无缝衔接
                        val containerAlpha = if (currentProgress < 0.04f) {
                            (currentProgress / 0.04f).coerceIn(0f, 1f)
                        } else {
                            1f
                        }

                        // 内部固定全屏视口偏移计算：
                        // 水平居中对齐外层卡片；垂直方向顶端对齐外层卡片，使详情内容直接从来源卡片位置展开
                        val innerOffsetX = ((currentRect.width - viewportSize.width) / 2f).roundToInt()
                        val innerOffsetY = 0

                        // 运动中的卡片容器 (动态裁剪视口)
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        currentRect.left.roundToInt(),
                                        currentRect.top.roundToInt(),
                                    )
                                }
                                .size(
                                    width = with(density) { currentRect.width.toDp() },
                                    height = with(density) { currentRect.height.toDp() },
                                )
                                .graphicsLayer {
                                    this.alpha = containerAlpha
                                }
                                .shadow(
                                    elevation = if (isDismissing) 0.dp else with(density) { (8.dp.toPx() * (1f - currentProgress)).toDp() },
                                    shape = RoundedCornerShape(currentRadiusDp),
                                    clip = false,
                                    ambientColor = if (isDismissing) Color.Transparent else Color.Black.copy(alpha = 0.08f),
                                    spotColor = if (isDismissing) Color.Transparent else Color.Black.copy(alpha = 0.15f),
                                )
                                .clip(RoundedCornerShape(currentRadiusDp))
                                .background(Color.Transparent),
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(innerOffsetX, innerOffsetY)
                                    }
                                    .requiredSize(
                                        width = with(density) { viewportSize.width.toDp() },
                                        height = with(density) { viewportSize.height.toDp() },
                                    ),
                            ) {
                                CompositionLocalProvider(
                                    LocalContainerContentAlpha provides contentAlpha,
                                    LocalContainerIsDismissing provides isDismissing,
                                ) {
                                    content(requestDismiss)
                                }
                            }
                        }
                    } else {
                        // ========== 方案 B：平滑降级（中心微缩放与淡出） ==========
                        val scale = 0.94f + 0.06f * currentProgress
                        val alpha = currentProgress.coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .background(Color.Transparent),
                        ) {
                            CompositionLocalProvider(
                                LocalContainerContentAlpha provides alpha,
                                LocalContainerIsDismissing provides isDismissing,
                            ) {
                                content(requestDismiss)
                            }
                        }
                    }
                }
            }
        }
    }
}
