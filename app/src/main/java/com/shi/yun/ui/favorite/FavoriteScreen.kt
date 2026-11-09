package com.shi.yun.ui.favorite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.shi.yun.data.model.Work
import com.shi.yun.data.model.formatAuthorDynasty
import com.shi.yun.data.repository.WorkRepository
import com.shi.yun.ui.log.LogManagementDialog

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
    onWorkClick: (Work, Rect?) -> Unit,
    onOpenThemeSelection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val favoriteIds by repository.favoriteIdsFlow.collectAsState()
    var favoriteWorks by remember { mutableStateOf<List<Work>>(emptyList()) }
    var showLogDialog by remember { mutableStateOf(false) }

    // 监听收藏 ID 集合变化，自动同步最新的有序作品列表
    LaunchedEffect(favoriteIds) {
        favoriteWorks = repository.getFavoriteWorks()
    }

    if (showLogDialog) {
        LogManagementDialog(onDismissRequest = { showLogDialog = false })
    }

    val appTheme = com.shi.yun.ui.theme.LocalAppTheme.current
    val isDark = appTheme.themeId.isDark
    val itemCardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDark) 0.60f else 0.42f)
    val itemBorder = appTheme.customTokens.cardBorder.copy(alpha = if (isDark) 0.40f else 0.25f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // 顶部操作状态栏（移除多余大标题，优雅呈现收藏统计、主题外观与运行日志入口）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(itemCardBg)
                .border(0.5.dp, itemBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (favoriteWorks.isNotEmpty()) "藏卷 · 共 ${favoriteWorks.size} 首" else "藏卷",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "主题外观",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onOpenThemeSelection() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Text(
                    text = "运行日志",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showLogDialog = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }

        if (favoriteWorks.isEmpty()) {
            val context = LocalContext.current
            val emptyQuote = remember(favoriteWorks.isEmpty()) {
                val prefs = context.getSharedPreferences(
                    FavoriteEmptyQuoteManager.PREFS_NAME,
                    Context.MODE_PRIVATE,
                )
                FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(prefs)
            }

            // 优雅空状态（展示每日轮播四句文案与统一辅助指引）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(itemCardBg)
                        .border(0.5.dp, itemBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                ) {
                    FavoriteEmptyIcon(tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Text(
                        text = emptyQuote,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = FavoriteEmptyQuoteManager.AUXILIARY_TEXT,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
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
                    var itemBounds by remember { mutableStateOf<Rect?>(null) }
                    FavoriteWorkItem(
                        work = work,
                        onClick = { onWorkClick(work, itemBounds) },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            itemBounds = coordinates.boundsInRoot()
                        },
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
    val appTheme = com.shi.yun.ui.theme.LocalAppTheme.current
    val isDark = appTheme.themeId.isDark
    val itemCardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDark) 0.60f else 0.42f)
    val itemBorder = appTheme.customTokens.cardBorder.copy(alpha = if (isDark) 0.40f else 0.25f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(itemCardBg)
            .border(0.5.dp, itemBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
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
