package com.childe.shi.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work
import com.childe.shi.data.model.formatAuthorDynastyGenre
import com.childe.shi.data.repository.WorkRepository
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
    onWorkClick: (Work) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember { mutableStateOf<SearchUiState>(SearchUiState.Idle) }
    val focusManager = LocalFocusManager.current
    var lastSearchedKeyword by remember { mutableStateOf(keyword) }

    // 搜索执行协程（带防抖和自动取消机制）
    LaunchedEffect(keyword) {
        val trimmed = keyword.trim()

        // 仅当关键词发生实质变化时重置列表滚动至顶部（Tab 切换不触发）
        if (trimmed != lastSearchedKeyword.trim()) {
            lastSearchedKeyword = keyword
            listState.scrollToItem(0)
        }

        if (trimmed.isEmpty()) {
            uiState = SearchUiState.Idle
            return@LaunchedEffect
        }

        uiState = SearchUiState.Searching
        try {
            val results = repository.searchWorks(trimmed)
            uiState = if (results.isEmpty()) {
                SearchUiState.Empty
            } else {
                SearchUiState.Success(results)
            }
        } catch (e: CancellationException) {
            // 协程正常取消，必须重新抛出，绝不当成异常提示
            throw e
        } catch (e: Exception) {
            uiState = SearchUiState.Error(e.message ?: "搜索遇到错误")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // 1. 顶部标题
        Text(
            text = "搜索",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 12.dp),
        )

        // 2. 搜索输入框
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            placeholder = {
                Text(
                    text = "输入诗词标题、作者或正文内容",
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

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 搜索内容呈现区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(
                            text = "墨香静候",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "支持输入作品标题、作者姓名或诗词名句搜索",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                is SearchUiState.Searching -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is SearchUiState.Empty -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp),
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
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = { /* 重新触发 LaunchedEffect */ onKeywordChange(keyword) }) {
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
                            val snippet = getSearchSnippet(work = work, keyword = keyword)
                            SearchWorkItem(
                                work = work,
                                snippet = snippet,
                                onClick = { onWorkClick(work) },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 24.dp),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
