package com.childe.shi.ui.favorite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work
import com.childe.shi.data.model.formatAuthorDynasty
import com.childe.shi.data.repository.WorkRepository

/**
 * 收藏页面主组件（受控组件，列表状态由外层顶层提升管理）
 *
 * 核心架构设计：
 * 1. 响应式监听：通过 collectAsState 监听 repository.favoriteIdsFlow，一旦收藏增删立即自动刷新列表；
 * 2. 严格按收藏顺序展示：内部调用 repository.getFavoriteWorks()，保证最新收藏居于最前；
 * 3. 状态提升与保持：LazyListState 由顶层传入，切换 Tab 或进出详情页后完全保留滚动位置；
 * 4. 优雅空状态：当暂无收藏时展示中式极简书签图腾与指引文案，零崩溃零空白；
 * 5. 零外部依赖：空状态图腾采用 Compose 原生 Canvas 绘制。
 */
@Composable
fun FavoriteScreen(
    repository: WorkRepository,
    listState: LazyListState,
    onWorkClick: (Work) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteIds by repository.favoriteIdsFlow.collectAsState()
    var favoriteWorks by remember { mutableStateOf<List<Work>>(emptyList()) }

    // 监听收藏 ID 集合变化，自动同步最新的有序作品列表
    LaunchedEffect(favoriteIds) {
        favoriteWorks = repository.getFavoriteWorks()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "我的收藏",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (favoriteWorks.isNotEmpty()) {
                Text(
                    text = "共 ${favoriteWorks.size} 首",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        if (favoriteWorks.isEmpty()) {
            // 优雅空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FavoriteEmptyIcon(tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Text(
                        text = "暂无收藏诗词",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = "在诗词详情页点击右上角红心即可收藏",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    )
                }
            }
        } else {
            // 有序收藏作品列表（LazyColumn）
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(favoriteWorks, key = { it.id }) { work ->
                    FavoriteWorkItem(
                        work = work,
                        onClick = { onWorkClick(work) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

/**
 * 收藏列表单项卡片
 */
@Composable
private fun FavoriteWorkItem(
    work: Work,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = work.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            val authorDynasty = work.formatAuthorDynasty()
            if (authorDynasty.isNotEmpty()) {
                Text(
                    text = authorDynasty,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // 精选名句或首句排版
        val displayLine = work.excerpt.ifBlank {
            work.content.lines().firstOrNull { it.isNotBlank() } ?: ""
        }
        if (displayLine.isNotBlank()) {
            Text(
                text = displayLine,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 极简中式书签空状态图标（原生 Canvas 绘制，零外部依赖）
 */
@Composable
private fun FavoriteEmptyIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        // 书签外轮廓
        val bookmarkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.85f)
            lineTo(w * 0.5f, h * 0.68f)
            lineTo(w * 0.28f, h * 0.85f)
            close()
        }
        drawPath(
            path = bookmarkPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        // 书签内部小心形图案
        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.52f)
            cubicTo(w * 0.36f, h * 0.40f, w * 0.32f, h * 0.28f, w * 0.42f, h * 0.24f)
            cubicTo(w * 0.46f, h * 0.22f, w * 0.5f, h * 0.28f, w * 0.5f, h * 0.32f)
            cubicTo(w * 0.5f, h * 0.28f, w * 0.54f, h * 0.22f, w * 0.58f, h * 0.24f)
            cubicTo(w * 0.68f, h * 0.28f, w * 0.64f, h * 0.40f, w * 0.5f, h * 0.52f)
            close()
        }
        drawPath(
            path = heartPath,
            color = tint,
            style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round),
        )
    }
}
