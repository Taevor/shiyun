package com.childe.shi.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work
import com.childe.shi.data.model.formatAuthorDynastyGenre
import com.childe.shi.data.repository.WorkRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * 分类维度枚举
 */
enum class CategoryDimension(val label: String) {
    DYNASTY("按朝代"),
    GENRE("按体裁")
}

/**
 * 体裁配置数据类
 */
data class GenreOption(val code: String, val label: String)

/**
 * 分类列表 UI 状态定义
 */
sealed interface CategoryUiState {
    data object Loading : CategoryUiState
    data class Success(val works: List<Work>) : CategoryUiState
    data object Empty : CategoryUiState
    data class Error(val message: String) : CategoryUiState
}

/**
 * 分类页面主组件（受控组件，状态由外层统一提升管理）
 *
 * 遵循严格架构约束：
 * 1. 组合树持久驻留：打开详情页时不被销毁重构；
 * 2. 状态完全提升：listState、分类维度与标签均由上层传入，双向返回无感还原；
 * 3. 维度状态完全解耦：朝代与体裁独立保存，切换维度时恢复各自上次选中的标签；
 * 4. 防竞态保护：基于 requestVersion 立即失效机制，旧查询绝不覆盖最新结果；
 * 5. 纯 Repository 查询：不读取 assets 或 JSON，UI 层零数据解析。
 */
@Composable
fun CategoryScreen(
    repository: WorkRepository,
    works: List<Work>,
    listState: LazyListState,
    selectedDimension: CategoryDimension,
    selectedDynasty: String,
    selectedGenre: String,
    requestVersion: Long,
    onDimensionChange: (CategoryDimension) -> Unit,
    onDynastyChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onRetry: () -> Unit,
    onWorkClick: (Work) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember { mutableStateOf<CategoryUiState>(CategoryUiState.Loading) }

    // 动态提取已有作品中存在的全部朝代并去重，不写死唐宋，自动支持未来扩充
    val dynastyOptions = remember(works) {
        val list = mutableListOf("全部")
        val uniqueDynasties = works.map { it.dynasty }.filter { it.isNotBlank() }.distinct()
        list.addAll(uniqueDynasties)
        list
    }

    // 体裁选项配置
    val genreOptions = remember {
        listOf(
            GenreOption("ALL", "全部"),
            GenreOption("poem", "诗"),
            GenreOption("ci", "词"),
        )
    }

    // 协程查询：基于严格的请求版本控制，防止快速切换导致旧数据覆盖
    LaunchedEffect(requestVersion) {
        val thisVersion = requestVersion
        uiState = CategoryUiState.Loading

        try {
            val results = when (selectedDimension) {
                CategoryDimension.DYNASTY -> {
                    if (selectedDynasty == "全部") {
                        repository.getAllWorks()
                    } else {
                        repository.getWorksByDynasty(selectedDynasty)
                    }
                }
                CategoryDimension.GENRE -> {
                    if (selectedGenre == "ALL" || selectedGenre == "全部") {
                        repository.getAllWorks()
                    } else {
                        repository.getWorksByGenre(selectedGenre)
                    }
                }
            }

            // 核心防护：仅当当前协程版本依然是最新的有效版本时，才允许写入 UI 状态
            if (thisVersion == requestVersion) {
                uiState = if (results.isEmpty()) {
                    CategoryUiState.Empty
                } else {
                    CategoryUiState.Success(results)
                }
            }
        } catch (e: CancellationException) {
            // 协程取消信号，必须原样重新抛出，绝不当做错误展示给用户
            throw e
        } catch (e: Exception) {
            if (thisVersion == requestVersion) {
                uiState = CategoryUiState.Error(e.message ?: "加载分类作品失败")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        // 1. 顶部标题
        Text(
            text = "分类",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 12.dp),
        )

        // 2. 一级分类维度切换（按朝代 vs 按体裁）
        PrimaryTabRow(
            selectedTabIndex = selectedDimension.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
        ) {
            CategoryDimension.entries.forEach { dimension ->
                Tab(
                    selected = selectedDimension == dimension,
                    onClick = { onDimensionChange(dimension) },
                    text = {
                        Text(
                            text = dimension.label,
                            fontSize = 15.sp,
                            fontWeight = if (selectedDimension == dimension) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        // 3. 二级筛选项栏（横向滚动，独立保存各自维度的选中状态）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (selectedDimension) {
                CategoryDimension.DYNASTY -> {
                    dynastyOptions.forEach { dynasty ->
                        val isSelected = selectedDynasty == dynasty
                        val displayLabel = if (dynasty == "全部") "全部" else "${dynasty}代"
                        FilterChip(
                            selected = isSelected,
                            onClick = { onDynastyChange(dynasty) },
                            label = { Text(text = displayLabel, fontSize = 13.sp) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
                CategoryDimension.GENRE -> {
                    genreOptions.forEach { genreOption ->
                        val isSelected = selectedGenre == genreOption.code
                        FilterChip(
                            selected = isSelected,
                            onClick = { onGenreChange(genreOption.code) },
                            label = { Text(text = genreOption.label, fontSize = 13.sp) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        // 4. 作品列表呈现区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is CategoryUiState.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is CategoryUiState.Empty -> {
                    Text(
                        text = "暂无符合条件的作品",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                is CategoryUiState.Error -> {
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
                        Button(onClick = onRetry) {
                            Text(text = "重试")
                        }
                    }
                }

                is CategoryUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.works,
                            key = { it.id },
                        ) { work ->
                            CategoryWorkItem(
                                work = work,
                                onClick = { onWorkClick(work) },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                        // 底部留白，保障视觉呼吸感
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
 * 列表项组件：纯净雅致的文字排版
 */
@Composable
private fun CategoryWorkItem(
    work: Work,
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
            text = work.excerpt,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
    }
}
