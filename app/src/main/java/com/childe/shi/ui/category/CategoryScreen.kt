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
import androidx.compose.material3.Surface
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
import com.childe.shi.common.log.AppLogger
import com.childe.shi.data.model.Anthology
import com.childe.shi.data.model.AnthologyWorkItem
import com.childe.shi.data.model.Work
import com.childe.shi.data.model.formatAuthorDynastyGenre
import com.childe.shi.data.repository.WorkRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * 分类维度枚举
 */
enum class CategoryDimension(val label: String) {
    DYNASTY("按朝代"),
    GENRE("按体裁"),
    ANTHOLOGY("按选本")
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
    data class AnthologySuccess(val items: List<AnthologyWorkItem>, val anthology: Anthology?) : CategoryUiState
    data object Empty : CategoryUiState
    data class Error(val message: String) : CategoryUiState
}

/**
 * 分类页面主组件（受控组件，状态由外层统一提升管理）
 *
 * 遵循严格架构约束：
 * 1. 组合树持久驻留：打开详情页时不被销毁重构；
 * 2. 状态完全提升：listState、分类维度与标签均由上层传入，双向返回无感还原；
 * 3. 维度状态完全解耦：朝代、体裁与选本独立保存，切换维度时恢复各自上次选中的标签；
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
    selectedAnthologyId: String,
    requestVersion: Long,
    onDimensionChange: (CategoryDimension) -> Unit,
    onDynastyChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onAnthologyChange: (String) -> Unit,
    onRetry: () -> Unit,
    onWorkClick: (Work, List<Work>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember { mutableStateOf<CategoryUiState>(CategoryUiState.Loading) }
    var anthologiesList by remember { mutableStateOf<List<Anthology>>(emptyList()) }

    // 初始异步加载选本定义列表，供选本胶囊选项展示
    LaunchedEffect(Unit) {
        try {
            anthologiesList = repository.getAnthologies()
        } catch (e: Exception) {
            AppLogger.e("CategoryScreen", "加载选本列表定义失败: ${e.message}", e)
        }
    }

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
    LaunchedEffect(requestVersion, selectedDimension, selectedDynasty, selectedGenre, selectedAnthologyId) {
        val thisVersion = requestVersion
        val filterDesc = when (selectedDimension) {
            CategoryDimension.DYNASTY -> selectedDynasty
            CategoryDimension.GENRE -> selectedGenre
            CategoryDimension.ANTHOLOGY -> selectedAnthologyId
        }
        AppLogger.d("CategoryScreen", "加载分类列表: 维度=${selectedDimension.label}, 选中标签=$filterDesc, 版本=$thisVersion")
        uiState = CategoryUiState.Loading

        try {
            when (selectedDimension) {
                CategoryDimension.DYNASTY -> {
                    val results = if (selectedDynasty == "全部") {
                        repository.getAllWorks()
                    } else {
                        repository.getWorksByDynasty(selectedDynasty)
                    }
                    if (thisVersion == requestVersion) {
                        AppLogger.d("CategoryScreen", "朝代分类加载完成: 结果篇数=${results.size}, 版本=$thisVersion")
                        uiState = if (results.isEmpty()) CategoryUiState.Empty else CategoryUiState.Success(results)
                    }
                }
                CategoryDimension.GENRE -> {
                    val results = if (selectedGenre == "ALL" || selectedGenre == "全部") {
                        repository.getAllWorks()
                    } else {
                        repository.getWorksByGenre(selectedGenre)
                    }
                    if (thisVersion == requestVersion) {
                        AppLogger.d("CategoryScreen", "体裁分类加载完成: 结果篇数=${results.size}, 版本=$thisVersion")
                        uiState = if (results.isEmpty()) CategoryUiState.Empty else CategoryUiState.Success(results)
                    }
                }
                CategoryDimension.ANTHOLOGY -> {
                    val currentAnthology = repository.getAnthologyById(selectedAnthologyId)
                        ?: repository.getAnthologies().firstOrNull()
                    val targetAnthologyId = currentAnthology?.id ?: selectedAnthologyId
                    val anthologyItems = repository.getWorksByAnthology(targetAnthologyId)
                    if (thisVersion == requestVersion) {
                        AppLogger.d("CategoryScreen", "选本加载完成: 选本ID=$targetAnthologyId, 篇目数=${anthologyItems.size}, 版本=$thisVersion")
                        uiState = if (anthologyItems.isEmpty()) {
                            CategoryUiState.Empty
                        } else {
                            CategoryUiState.AnthologySuccess(anthologyItems, currentAnthology)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            // 协程取消信号，必须原样重新抛出，绝不当做错误展示给用户
            throw e
        } catch (e: Exception) {
            AppLogger.e("CategoryScreen", "加载分类作品失败: ${e.message}", e)
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
                CategoryDimension.ANTHOLOGY -> {
                    anthologiesList.forEach { anthology ->
                        val isSelected = selectedAnthologyId == anthology.id
                        val countSuffix = if (anthology.targetCount > 0) "${anthology.targetCount}首" else ""
                        val labelText = if (countSuffix.isNotEmpty()) "${anthology.name} $countSuffix" else anthology.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAnthologyChange(anthology.id) },
                            label = { Text(text = labelText, fontSize = 13.sp) },
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
                                onClick = { onWorkClick(work, null) },
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

                is CategoryUiState.AnthologySuccess -> {
                    val anthologyWorks = remember(state.items) {
                        state.items.map { it.work }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // 选本顶部信息卡片（显示编选者、朝代及收录数量）
                        state.anthology?.let { anth ->
                            item(key = "anthology_header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = "${anth.name} · 共收录 ${state.items.size} 篇",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (anth.compiler.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "编选：${anth.compiler}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        }

                        // 按 orderIndex 升序展示的选本篇目列表
                        items(
                            items = state.items,
                            key = { it.item.id },
                        ) { item ->
                            AnthologyWorkRowItem(
                                item = item,
                                onClick = { onWorkClick(item.work, anthologyWorks) },
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
 * 选本专属列表项：包含原书篇目序号（如 001）、分卷体裁、标题、作者与名句摘录
 */
@Composable
private fun AnthologyWorkRowItem(
    item: AnthologyWorkItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val work = item.work
    val orderStr = String.format(java.util.Locale.CHINA, "%03d", item.item.orderIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 序号标号
            Text(
                text = orderStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 10.dp),
            )

            // 分卷/体裁标牌（若存在，如 "五言绝句"、"五言古诗"）
            if (item.item.volume.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 10.dp),
                ) {
                    Text(
                        text = item.item.volume,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // 标题
            Text(
                text = work.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Spacer(modifier = Modifier.weight(1f))

            // 作者与朝代
            Text(
                text = if (work.dynasty.isNotBlank()) "〔${work.dynasty}〕${work.author}" else work.author,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (work.excerpt.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = work.excerpt,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 32.dp),
            )
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
