package com.shi.yun.ui.search

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shi.yun.common.log.AppLogger
import com.shi.yun.data.model.Work
import com.shi.yun.data.model.formatAuthorDynastyGenre
import com.shi.yun.data.repository.WorkRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * 搜索界面 UI 状态定义
 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Success(val results: List<Work>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

/**
 * 搜索页面主组件
 *
 * 遵循阶段三 B 架构规范：
 * 1. 状态提升：searchKeyword 与 listState 由外层 MainContent 持有，Tab 切换与进出详情均能保留状态；
 * 2. 本地内存过滤：基于 WorkRepository.searchWorks 接口，零网络、零第三方依赖；
 * 3. 关键词实质变动时（非 Tab 切换），列表滚动位置自动回到顶部；
 * 4. 协程取消安全保护：正确捕获并重新抛出 CancellationException；
 * 5. 键盘避让：集成 imePadding 避免输入法遮挡底部内容；
 * 6. 命中正文片段提取：标题/作者命中优先展示 excerpt，正文命中提取对应分句，失败回退 excerpt。
 */
@Composable
fun SearchScreen(
    repository: WorkRepository,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    listState: LazyListState,
    onWorkClick: (Work, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember { mutableStateOf<SearchUiState>(SearchUiState.Idle) }
    val focusManager = LocalFocusManager.current
    var lastSearchedKeyword by rememberSaveable { mutableStateOf(keyword) }

    // 搜索执行协程（基于关键词变动触发，支持协程安全取消）
    LaunchedEffect(keyword) {
        val trimmed = keyword.trim()

        if (trimmed.isEmpty()) {
            lastSearchedKeyword = ""
            uiState = SearchUiState.Idle
            AppLogger.d("SearchScreen", "搜索输入清空，重置为空闲状态")
            return@LaunchedEffect
        }

        val isKeywordChanged = trimmed != lastSearchedKeyword.trim()
        lastSearchedKeyword = keyword

        AppLogger.d("SearchScreen", "发起搜索: 关键词长度=${trimmed.length}, 内容实质变化=$isKeywordChanged")
        uiState = SearchUiState.Searching
        try {
            val results = repository.searchWorks(trimmed)
            uiState = if (results.isEmpty()) {
                SearchUiState.Empty
            } else {
                SearchUiState.Success(results)
            }

            // 仅当关键词实质变动且列表已有滚动偏移时安全重置到顶部（避免未挂载时挂起协程）
            if (isKeywordChanged && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
                listState.scrollToItem(0)
            }
        } catch (e: CancellationException) {
            // 协程正常取消，必须重新抛出，绝不当成异常提示
            throw e
        } catch (e: Exception) {
            AppLogger.e("SearchScreen", "搜索执行发生异常: ${e.message}", e)
            uiState = SearchUiState.Error(e.message ?: "搜索遇到错误")
        }
    }

    val hasQuery = keyword.trim().isNotEmpty()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // 以「墨香静候」文字居于屏幕垂直中心为基准，计算搜索框在文字上方的协调初位
        val idleTopSpacer = (maxHeight * 0.5f - 115.dp).coerceAtLeast(80.dp)
        val topSpacerHeight by animateDpAsState(
            targetValue = if (hasQuery) 8.dp else idleTopSpacer,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "SearchBoxTopOffset",
        )

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 平滑动效间距：未搜索状态位于「墨香静候」上方，开始输入后平滑移动到顶部
            Spacer(modifier = Modifier.height(topSpacerHeight))

            // 搜索输入框
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                placeholder = {
                    Text(
                        text = "搜索作品",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                ),
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onKeywordChange("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "×",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. 搜索内容呈现区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (val state = uiState) {
                    is SearchUiState.Idle -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp, start = 24.dp, end = 24.dp),
                        ) {
                            Text(
                                text = "众里寻他千百度",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            )
                            Text(
                                text = "支持按作品标题、作者姓名或正文内容搜索",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                is SearchUiState.Searching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                is SearchUiState.Empty -> {
                    val appTheme = com.shi.yun.ui.theme.LocalAppTheme.current
                    val isDark = appTheme.themeId.isDark
                    val promptBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDark) 0.60f else 0.42f)
                    val promptBorder = appTheme.customTokens.cardBorder.copy(alpha = if (isDark) 0.40f else 0.25f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(promptBg)
                            .border(0.5.dp, promptBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = "未找到相关诗词",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "可尝试精简字数或更换其他关键词",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                is SearchUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 64.dp, start = 24.dp, end = 24.dp),
                    ) {
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = { onKeywordChange(keyword) }) {
                            Text(text = "重试")
                        }
                    }
                }

                is SearchUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.results,
                            key = { it.id },
                        ) { work ->
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            val snippet = getSearchSnippet(work = work, keyword = keyword)
                            SearchWorkItem(
                                work = work,
                                snippet = snippet,
                                onClick = { onWorkClick(work, itemBounds) },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    itemBounds = coordinates.boundsInRoot()
                                },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
    }
}

/**
 * 搜索结果列表项组件
 */
@Composable
private fun SearchWorkItem(
    work: Work,
    snippet: String,
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
            Text(
                text = work.formatAuthorDynastyGenre(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = snippet,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
    }
}

/**
 * 命中正文片段提取算法
 *
 * 1. 标题或作者命中时优先展示 excerpt；
 * 2. 正文命中时提取包含关键词的完整句子，并保留标点；
 * 3. 提取失败回退显示 excerpt。
 */
private fun getSearchSnippet(work: Work, keyword: String): String {
    val trimmed = keyword.trim()
    if (trimmed.isEmpty()) return work.excerpt

    // 1. 若标题或作者包含关键词，优先展示推荐短句 excerpt
    if (work.title.contains(trimmed, ignoreCase = true) ||
        work.author.contains(trimmed, ignoreCase = true)
    ) {
        return work.excerpt
    }

    // 2. 正文命中：优先提取包含关键词的行（保留原有标点）
    if (work.content.contains(trimmed, ignoreCase = true)) {
        val lines = work.content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val matchedLine = lines.firstOrNull { it.contains(trimmed, ignoreCase = true) }
        if (!matchedLine.isNullOrBlank()) {
            return matchedLine
        }
    }

    // 3. 提取失败回退
    return work.excerpt
}
