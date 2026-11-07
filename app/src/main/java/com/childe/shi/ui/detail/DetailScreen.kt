package com.childe.shi.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work
import com.childe.shi.data.model.formatAuthorDynastyGenre

/**
 * 诗词作品详情页
 *
 * 遵循规范要求：
 * 1. 顶部提供明确的返回按钮与对称的收藏按钮；
 * 2. 依次展示作品标题、朝代、作者、体裁；
 * 3. 完整展示正文，严格保留换行与分段，长文本支持上下平滑滚动；
 * 4. 适配底部与顶部系统栏，避免被系统导航条遮挡；
 * 5. 纯净安静的现代中式排版风格。
 */
@Composable
fun DetailScreen(
    work: Work,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // 消费背景点击，阻断触摸下透
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // 顶部导航栏（左侧极简返回键，右侧收藏红心按钮，原生 Canvas 绘制，零外部图标库依赖）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 20.dp),
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BackArrowIcon(tint = MaterialTheme.colorScheme.onSurface)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 20.dp),
                        onClick = onToggleFavorite,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                FavoriteHeartIcon(
                    isFavorite = isFavorite,
                    tint = if (isFavorite) Color(0xFFC04851) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 详情内容可滚动区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. 作品标题
                Text(
                    text = work.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. 作者、朝代与体裁信息
                Text(
                    text = work.formatAuthorDynastyGenre(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3. 完整正文排版
                // 按照空行 (\n\n) 切分为段落/阕，按换行 (\n) 切分为诗句
                PoemContentLayout(content = work.content)

                // 底部留白，保障视觉呼吸感
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

/**
 * 极简中式返回箭头图标（原生 Canvas 绘制，零外部依赖）
 */
@Composable
private fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height

        // 主横线
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.85f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // 上折线
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // 下折线
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 极简中式收藏心形图标（原生 Canvas 绘制，零外部依赖）
 *
 * @param isFavorite 是否已收藏。未收藏时绘制纤细描边轮廓，已收藏时实心填色。
 */
@Composable
private fun FavoriteHeartIcon(
    isFavorite: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.12f, h * 0.54f, 0f, h * 0.32f, w * 0.25f, h * 0.16f)
            cubicTo(w * 0.38f, h * 0.08f, w * 0.5f, h * 0.22f, w * 0.5f, h * 0.32f)
            cubicTo(w * 0.5f, h * 0.22f, w * 0.62f, h * 0.08f, w * 0.75f, h * 0.16f)
            cubicTo(w * 1.0f, h * 0.32f, w * 0.88f, h * 0.54f, w * 0.5f, h * 0.82f)
            close()
        }
        if (isFavorite) {
            drawPath(path = heartPath, color = tint)
        } else {
            drawPath(
                path = heartPath,
                color = tint,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

/**
 * 诗词正文精细化排版组件
 *
 * 将古诗词按照双换行分段（如词作的上阕与下阕、题记），
 * 并逐行渲染诗句，保持严谨的行距与舒适的段落间距。
 */
@Composable
private fun PoemContentLayout(
    content: String,
    modifier: Modifier = Modifier,
) {
    val sections = content.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sections.forEach { section ->
            val lines = section.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = 19.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
