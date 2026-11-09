package com.shi.yun.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shi.yun.R
import com.shi.yun.data.model.Work
import com.shi.yun.data.model.formatAuthorDynasty
import com.shi.yun.ui.theme.LocalAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 滑动手势判定方向枚举
 */
enum class SwipeDirection {
    NONE,
    NEXT,
    PREVIOUS,
}

/**
 * 纯函数：多方向卡片滑动手势判定与主轴识别
 *
 * 判定规则：
 * 1. 检查位移绝对值或轻拂速度是否达到激活阈值；
 * 2. 达到阈值后，确定主轴（水平 vs 垂直）；
 * 3. 左滑 / 上滑判定为 NEXT（下一首）；
 * 4. 右滑 / 下滑判定为 PREVIOUS（上一首）；
 * 5. 斜向滑动按主轴分量决定，未达到阈值返回 NONE。
 */
fun resolveSwipeAction(
    dragX: Float,
    dragY: Float,
    velocityX: Float = 0f,
    velocityY: Float = 0f,
    distanceThreshold: Float = 200f,
    velocityThreshold: Float = 1500f,
): SwipeDirection {
    val absX = abs(dragX)
    val absY = abs(dragY)
    val absVx = abs(velocityX)
    val absVy = abs(velocityY)

    val distancePassed = absX >= distanceThreshold || absY >= distanceThreshold
    val flingXPassed = absVx >= velocityThreshold && (dragX * velocityX > 0f)
    val flingYPassed = absVy >= velocityThreshold && (dragY * velocityY > 0f)
    val velocityPassed = flingXPassed || flingYPassed

    if (!distancePassed && !velocityPassed) {
        return SwipeDirection.NONE
    }

    val isHorizontalDominant = if (velocityPassed && !distancePassed) {
        absVx >= absVy
    } else {
        absX >= absY
    }

    return if (isHorizontalDominant) {
        if (dragX < 0f || (flingXPassed && velocityX < 0f)) {
            SwipeDirection.NEXT
        } else {
            SwipeDirection.PREVIOUS
        }
    } else {
        if (dragY < 0f || (flingYPassed && velocityY < 0f)) {
            SwipeDirection.NEXT
        } else {
            SwipeDirection.PREVIOUS
        }
    }
}

/**
 * 阶段五 C：沉浸式推荐短句首页
 *
 * 核心升级点：
 * 1. 增加淡雅中国古典水墨山水背景图（bg_home_landscape.webp），弱化背景（alpha = 0.22f）；
 * 2. 诗词卡片采用半透明宣纸色（PaperCream.copy(alpha = 0.85f)），无生硬边框与突兀阴影；
 * 3. 采用自定义 CardSwipeBox 单指针事件手势容器，完美支持上下左右四向滑动与斜向滑动主轴识别；
 * 4. 首次进入呈现“滑动切换  ·  点击阅读全文”轻量指引，3秒后或交互后淡出并持久化。
 */
@Composable
fun HomeScreen(
    recommendationList: List<Work>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    isHintShown: Boolean,
    onMarkHintShown: () -> Unit,
    onWorkClick: (Work, androidx.compose.ui.geometry.Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendationList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "暂无诗词数据",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        return
    }

    val currentWork = recommendationList.getOrNull(currentIndex) ?: recommendationList.first()

    // 首次进入新手引导显示控制（3秒自动淡出，或交互时立即淡出）
    var hintVisible by remember(isHintShown) { mutableStateOf(!isHintShown) }

    LaunchedEffect(isHintShown) {
        if (!isHintShown) {
            delay(3000L)
            hintVisible = false
            onMarkHintShown()
        }
    }

    val dismissHint = {
        if (hintVisible) {
            hintVisible = false
            onMarkHintShown()
        }
    }

    val appTheme = LocalAppTheme.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gradientHeight = statusBarTop + 20.dp
    val gradientBaseColor = appTheme.customTokens.statusBarProtectionColor

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // 状态栏顶部温润渐变保护层（跟随当前主题纸张色平滑过渡到透明，不推挤主体内容，不拦截手势）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            gradientBaseColor.copy(alpha = 0.90f),
                            gradientBaseColor.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        var cardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

        // 3. 多方向卡片滑动手势容器与诗词短句（保留 statusBarsPadding 避免文字和触控区被状态栏遮挡）
        CardSwipeBox(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            canSwipePrevious = currentIndex > 0,
            onSwipeNext = {
                onIndexChange(currentIndex + 1)
            },
            onSwipePrevious = {
                if (currentIndex > 0) {
                    onIndexChange(currentIndex - 1)
                }
            },
            onClick = {
                dismissHint()
                onWorkClick(currentWork, cardBounds)
            },
            onInteractionStart = {
                dismissHint()
            },
        ) {
            ExcerptCard(
                work = currentWork,
                onPositioned = { rect -> cardBounds = rect },
            )
        }

        // 3. 首次轻触指引提示（柔和淡入淡出）
        AnimatedVisibility(
            visible = hintVisible,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "滑动切换  ·  点击阅读全文",
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = appTheme.customTokens.secondaryText.copy(alpha = 0.75f),
            )
        }
    }
}

/**
 * 多方向卡片滑动手势容器
 *
 * 关键特性：
 * 1. 采用单个 awaitEachGesture 事件流，基于 touchSlop 严格区分普通点击与拖拽；
 * 2. 动画过程中（isAnimating）屏蔽新手势，杜绝卡片重叠与竞态错乱；
 * 3. 边界 index=0 下滑/右滑优雅弹性回弹，不产生负索引；
 * 4. 卡片拖拽过程中自然跟随位移、微倾斜（rotationZ）与轻微缩放。
 */
@Composable
private fun CardSwipeBox(
    modifier: Modifier = Modifier,
    canSwipePrevious: Boolean = true,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onClick: () -> Unit,
    onInteractionStart: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)
    val currentCanSwipePrevious by rememberUpdatedState(canSwipePrevious)
    val currentOnInteractionStart by rememberUpdatedState(onInteractionStart)

    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }
    var isAnimating by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop
    val distanceThresholdPx = with(density) { 72.dp.toPx() }
    val velocityThresholdPx = with(density) { 1000.dp.toPx() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(isAnimating) {
                if (isAnimating) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    var totalDrag = Offset.Zero
                    var isDragging = false
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (change.isConsumed) break

                        if (change.changedToUp()) {
                            if (!isDragging) {
                                change.consume()
                                currentOnClick()
                            } else {
                                change.consume()
                                val velocity = velocityTracker.calculateVelocity()
                                val action = resolveSwipeAction(
                                    dragX = totalDrag.x,
                                    dragY = totalDrag.y,
                                    velocityX = velocity.x,
                                    velocityY = velocity.y,
                                    distanceThreshold = distanceThresholdPx,
                                    velocityThreshold = velocityThresholdPx,
                                )

                                when {
                                    action == SwipeDirection.NONE || (action == SwipeDirection.PREVIOUS && !currentCanSwipePrevious) -> {
                                        // 未达到阈值或首篇不可后退：弹性回弹至原位
                                        isAnimating = true
                                        scope.launch {
                                            launch {
                                                animOffsetX.animateTo(
                                                    0f,
                                                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                                )
                                            }
                                            launch {
                                                animOffsetY.animateTo(
                                                    0f,
                                                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                                )
                                            }
                                        }.invokeOnCompletion {
                                            isAnimating = false
                                        }
                                    }
                                    action == SwipeDirection.NEXT || action == SwipeDirection.PREVIOUS -> {
                                        // 达到切换阈值：执行离场及入场动画
                                        isAnimating = true
                                        val isHorizontal = abs(totalDrag.x) >= abs(totalDrag.y)
                                        val screenW = if (containerSize.width > 0) containerSize.width.toFloat() else 1080f
                                        val screenH = if (containerSize.height > 0) containerSize.height.toFloat() else 2400f

                                        val targetX: Float
                                        val targetY: Float
                                        val enterX: Float
                                        val enterY: Float

                                        if (action == SwipeDirection.NEXT) {
                                            if (isHorizontal) {
                                                targetX = -screenW * 1.2f
                                                targetY = animOffsetY.value
                                                enterX = screenW * 0.35f
                                                enterY = 0f
                                            } else {
                                                targetX = animOffsetX.value
                                                targetY = -screenH * 1.2f
                                                enterX = 0f
                                                enterY = screenH * 0.35f
                                            }
                                        } else {
                                            if (isHorizontal) {
                                                targetX = screenW * 1.2f
                                                targetY = animOffsetY.value
                                                enterX = -screenW * 0.35f
                                                enterY = 0f
                                            } else {
                                                targetX = animOffsetX.value
                                                targetY = screenH * 1.2f
                                                enterX = 0f
                                                enterY = -screenH * 0.35f
                                            }
                                        }

                                        scope.launch {
                                            launch { animOffsetX.animateTo(targetX, tween(160, easing = FastOutLinearInEasing)) }
                                            launch { animOffsetY.animateTo(targetY, tween(160, easing = FastOutLinearInEasing)) }
                                            launch { animAlpha.animateTo(0.15f, tween(160)) }
                                        }.invokeOnCompletion {
                                            if (action == SwipeDirection.NEXT) {
                                                currentOnSwipeNext()
                                            } else {
                                                currentOnSwipePrevious()
                                            }
                                            scope.launch {
                                                animOffsetX.snapTo(enterX)
                                                animOffsetY.snapTo(enterY)
                                                animAlpha.snapTo(0.2f)
                                                launch { animOffsetX.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) }
                                                launch { animOffsetY.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) }
                                                launch { animAlpha.animateTo(1f, tween(180)) }
                                            }.invokeOnCompletion {
                                                isAnimating = false
                                            }
                                        }
                                    }
                                }
                            }
                            break
                        }

                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val dragDelta = change.position - change.previousPosition
                        totalDrag += dragDelta

                        if (!isDragging) {
                            if (sqrt(totalDrag.x * totalDrag.x + totalDrag.y * totalDrag.y) > touchSlop) {
                                isDragging = true
                                change.consume()
                                currentOnInteractionStart()
                            }
                        } else {
                            change.consume()
                            scope.launch {
                                animOffsetX.snapTo(totalDrag.x)
                                animOffsetY.snapTo(totalDrag.y)
                            }
                        }
                    }

                    // 手势取消保护
                    if (isDragging && !isAnimating) {
                        isAnimating = true
                        scope.launch {
                            launch { animOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
                            launch { animOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
                        }.invokeOnCompletion {
                            isAnimating = false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val dragDistance = sqrt(animOffsetX.value * animOffsetX.value + animOffsetY.value * animOffsetY.value)
        val rotationDeg = (animOffsetX.value / 25f).coerceIn(-8f, 8f)
        val scale = (1f - (dragDistance / 2600f)).coerceIn(0.92f, 1f)

        Box(
            modifier = Modifier.graphicsLayer {
                translationX = animOffsetX.value
                translationY = animOffsetY.value
                rotationZ = rotationDeg
                scaleX = scale
                scaleY = scale
                alpha = animAlpha.value
            },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * 半透明温润宣纸卡片展示单元
 */
@Composable
private fun ExcerptCard(
    work: Work,
    modifier: Modifier = Modifier,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
) {
    val appTheme = LocalAppTheme.current
    val isDarkTheme = appTheme.themeId.isDark
    // 适度透出底层全局山水背景纹理，同时确保核心诗词短句阅读对比度与独立卡片层级
    val cardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDarkTheme) 0.78f else 0.65f)

    Surface(
        modifier = modifier
            .widthIn(max = 520.dp)
            .padding(horizontal = 24.dp)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot())
            },
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(0.6.dp, appTheme.customTokens.cardBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 核心视觉焦点：推荐诗词短句
            Text(
                text = work.excerpt,
                fontSize = 24.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Normal,
                color = appTheme.customTokens.titleText,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 辅助信息：朝代、作者与作品标题
            val authorDynasty = work.formatAuthorDynasty()
            val attributionText = if (authorDynasty.isNotEmpty()) {
                "—— $authorDynasty《${work.title}》"
            } else {
                "—— 《${work.title}》"
            }
            Text(
                text = attributionText,
                fontSize = 14.sp,
                color = appTheme.customTokens.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 推荐序列生成算法
 *
 * 保证在作品数量 > 1 时，相邻两个位置绝不出现同一首作品。
 */
fun generateRecommendations(
    works: List<Work>,
    count: Int,
    lastWork: Work? = null,
): List<Work> {
    if (works.isEmpty()) return emptyList()
    if (works.size == 1) return List(count) { works.first() }

    val result = mutableListOf<Work>()
    var previous = lastWork

    repeat(count) {
        val candidates = works.filter { it.id != previous?.id }
        val next = candidates.random()
        result.add(next)
        previous = next
    }

    return result
}
