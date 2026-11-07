package com.childe.shi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work

/**
 * 极简推荐短句首页（纯 UI 呈现组件）
 *
 * 状态提升设计（State Hoisting）：
 * 首页不再在内部私自持有 recommendationList 和 pagerState，
 * 而是由顶层 ShiApp 统一持有并注入。
 * 这样无论如何在首页与详情页之间切换，推荐序列和滑动页码都会被完整保留。
 */
@Composable
fun HomeScreen(
    recommendationList: List<Work>,
    pagerState: PagerState,
    onWorkClick: (Work) -> Unit,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val work = recommendationList.getOrNull(page)
            if (work != null) {
                ExcerptPage(
                    work = work,
                    onClick = { onWorkClick(work) },
                )
            }
        }

        // 底部极简隐微提示（不喧宾夺主，保持安静中式美感）
        Text(
            text = "上下滑动切换  ·  点击阅读全文",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
        )
    }
}

/**
 * 单页推荐短句展示单元
 */
@Composable
private fun ExcerptPage(
    work: Work,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // 移除突兀的水波纹，营造纸质安静触感
                onClick = onClick,
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 核心视觉焦点：推荐诗词短句
            Text(
                text = work.excerpt,
                fontSize = 26.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 辅助信息：朝代、作者与作品标题（字级较小、视觉权重弱化）
            Text(
                text = "—— 〔${work.dynasty}〕${work.author}《${work.title}》",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
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
