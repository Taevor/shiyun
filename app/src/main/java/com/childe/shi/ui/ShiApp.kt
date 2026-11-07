package com.childe.shi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work
import com.childe.shi.data.repository.WorkRepository
import com.childe.shi.ui.detail.DetailScreen
import com.childe.shi.ui.home.HomeScreen
import com.childe.shi.ui.home.generateRecommendations
import kotlinx.coroutines.launch

/**
 * 应用根组件与主导航控制器
 *
 * 核心状态提升设计（State Hoisting）：
 * 1. 首页推荐序列 (recommendationList) 与 Pager 翻页状态 (pagerState)
 *    提升至 ShiApp 顶层长期维护；
 * 2. 首页 HomeScreen 常驻组合树，跳转到详情页时通过覆盖式 AnimatedVisibility 呈现 DetailScreen；
 * 3. 从而彻底杜绝了 HomeScreen 离开组合树导致的页码重置、序列重新随机问题；
 * 4. 从详情页返回时，100% 停留在刚才查看的同一推荐位置与同一作品。
 */
@Composable
fun ShiApp(
    repository: WorkRepository,
    modifier: Modifier = Modifier,
) {
    var allWorks by remember { mutableStateOf<List<Work>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        errorMessage = null
        scope.launch {
            try {
                val works = repository.getAllWorks()
                allWorks = works
            } catch (e: Exception) {
                errorMessage = e.message ?: "读取本地诗词数据失败"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // 拦截系统返回键：在详情页时返回首页，避免误退应用
    BackHandler(enabled = selectedWork != null) {
        selectedWork = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            // 1. 错误状态
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "数据加载失败",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = errorMessage.orEmpty(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Button(onClick = { loadData() }) {
                        Text(text = "重试")
                    }
                }
            }

            // 2. 加载中状态
            allWorks == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 3. 数据加载成功：渲染主界面与详情页
            else -> {
                val works = allWorks.orEmpty()
                MainContent(
                    works = works,
                    selectedWork = selectedWork,
                    onSelectWork = { selectedWork = it },
                    onDismissDetail = { selectedWork = null },
                )
            }
        }
    }
}

/**
 * 主内容区域组件
 *
 * 将推荐序列与 Pager 状态稳定维系在组合树中。
 */
@Composable
private fun MainContent(
    works: List<Work>,
    selectedWork: Work?,
    onSelectWork: (Work) -> Unit,
    onDismissDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 提升状态：推荐序列由顶层长期维护
    val recommendationList = remember(works) {
        mutableStateListOf<Work>().apply {
            addAll(generateRecommendations(works = works, count = 40))
        }
    }

    // 提升状态：Pager 翻页位置由顶层长期维护
    val pagerState = rememberPagerState(initialPage = 0) {
        recommendationList.size
    }

    // 随着滑动静默追加更多推荐项
    LaunchedEffect(pagerState.currentPage) {
        if (recommendationList.size - pagerState.currentPage <= 8 && works.isNotEmpty()) {
            val more = generateRecommendations(
                works = works,
                count = 20,
                lastWork = recommendationList.lastOrNull(),
            )
            recommendationList.addAll(more)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 首页常驻：保持其当前的 Pager 滚动位置与视图
        HomeScreen(
            recommendationList = recommendationList,
            pagerState = pagerState,
            onWorkClick = onSelectWork,
        )

        // 详情页平滑淡入淡出覆盖在顶层
        AnimatedVisibility(
            visible = selectedWork != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            selectedWork?.let { work ->
                DetailScreen(
                    work = work,
                    onBack = onDismissDetail,
                )
            }
        }
    }
}
