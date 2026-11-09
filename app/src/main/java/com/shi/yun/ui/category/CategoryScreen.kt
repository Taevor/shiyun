package com.shi.yun.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shi.yun.common.log.AppLogger
import com.shi.yun.data.model.Anthology
import com.shi.yun.data.model.AnthologyWorkItem
import com.shi.yun.data.model.Work
import com.shi.yun.data.model.formatAuthorDynasty
import com.shi.yun.data.model.formatAuthorDynastyGenre
import com.shi.yun.data.repository.WorkRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

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
 * 分类页面主组件（受控组件，状态由外层统一提升管理，支持朝代/体裁/选本横向滑动切换）
 */
@Composable
fun CategoryScreen(
    repository: WorkRepository,
    works: List<Work>,
    dynastyListState: LazyListState = rememberLazyListState(),
    genreListState: LazyListState = rememberLazyListState(),
    anthologyListState: LazyListState = rememberLazyListState(),
    selectedDimension: CategoryDimension = CategoryDimension.DYNASTY,
    selectedDynasty: String = "全部",
    selectedGenre: String = "ALL",
    selectedAnthologyId: String = "anth_ts300",
    requestVersion: Long = 0L,
    onDimensionChange: (CategoryDimension) -> Unit = {},
    onDynastyChange: (String) -> Unit = {},
    onGenreChange: (String) -> Unit = {},
    onAnthologyChange: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onWorkClick: (Work, List<Work>?, Rect?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var anthologiesList by remember { mutableStateOf<List<Anthology>>(emptyList()) }
    var dynastyUiState by remember { mutableStateOf<CategoryUiState>(CategoryUiState.Loading) }
    var genreUiState by remember { mutableStateOf<CategoryUiState>(CategoryUiState.Loading) }
    var anthologyUiState by remember { mutableStateOf<CategoryUiState>(CategoryUiState.Loading) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = selectedDimension.ordinal) { CategoryDimension.entries.size }

    // 初始异步加载选本定义列表，供选本胶囊选项展示
    LaunchedEffect(Unit) {
        try {
            anthologiesList = repository.getAnthologies()
        } catch (e: Exception) {
            AppLogger.e("CategoryScreen", "加载选本列表定义失败: ${e.message}", e)
        }
    }

    // 动态提取已有作品中存在的全部朝代并去重
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

    // 1. 朝代作品独立加载
    LaunchedEffect(requestVersion, selectedDynasty) {
        dynastyUiState = CategoryUiState.Loading
        try {
            val results = if (selectedDynasty == "全部") {
                repository.getAllWorks()
            } else {
                repository.getWorksByDynasty(selectedDynasty)
            }
            dynastyUiState = if (results.isEmpty()) CategoryUiState.Empty else CategoryUiState.Success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("CategoryScreen", "加载朝代分类失败: ${e.message}", e)
            dynastyUiState = CategoryUiState.Error(e.message ?: "加载朝代作品失败")
        }
    }

    // 2. 体裁作品独立加载
    LaunchedEffect(requestVersion, selectedGenre) {
        genreUiState = CategoryUiState.Loading
        try {
            val results = if (selectedGenre == "ALL" || selectedGenre == "全部") {
                repository.getAllWorks()
            } else {
                repository.getWorksByGenre(selectedGenre)
            }
            genreUiState = if (results.isEmpty()) CategoryUiState.Empty else CategoryUiState.Success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("CategoryScreen", "加载体裁分类失败: ${e.message}", e)
            genreUiState = CategoryUiState.Error(e.message ?: "加载体裁作品失败")
        }
    }

    // 3. 选本作品独立加载
    LaunchedEffect(requestVersion, selectedAnthologyId) {
        anthologyUiState = CategoryUiState.Loading
        try {
            val currentAnthology = repository.getAnthologyById(selectedAnthologyId)
                ?: repository.getAnthologies().firstOrNull()
            val targetAnthologyId = currentAnthology?.id ?: selectedAnthologyId
            val anthologyItems = repository.getWorksByAnthology(targetAnthologyId)
            anthologyUiState = if (anthologyItems.isEmpty()) {
                CategoryUiState.Empty
            } else {
                CategoryUiState.AnthologySuccess(anthologyItems, currentAnthology)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("CategoryScreen", "加载选本作品失败: ${e.message}", e)
            anthologyUiState = CategoryUiState.Error(e.message ?: "加载选本作品失败")
        }
    }

    // 页面滑动与顶部维度状态双向同步
    LaunchedEffect(pagerState.currentPage) {
        val newDim = CategoryDimension.entries[pagerState.currentPage]
        if (selectedDimension != newDim) {
            onDimensionChange(newDim)
        }
    }

    LaunchedEffect(selectedDimension) {
        if (pagerState.currentPage != selectedDimension.ordinal) {
            pagerState.scrollToPage(selectedDimension.ordinal)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // 1. 一级分类维度切换（按朝代 vs 按体裁 vs 按选本）固定置顶
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
        ) {
            CategoryDimension.entries.forEachIndexed { index, dimension ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                        onDimensionChange(dimension)
                    },
                    text = {
                        Text(
                            text = dimension.label,
                            fontSize = 15.sp,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        // 2. 横向滑动分页区域（朝代 / 体裁 / 选本，每个页面独立筛选条件与滚动位置）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            when (CategoryDimension.entries[page]) {
                CategoryDimension.DYNASTY -> {
                    DynastyPage(
                        dynastyOptions = dynastyOptions,
                        selectedDynasty = selectedDynasty,
                        onDynastyChange = onDynastyChange,
                        uiState = dynastyUiState,
                        listState = dynastyListState,
                        onRetry = onRetry,
                        onWorkClick = { work, rect -> onWorkClick(work, null, rect) },
                    )
                }
                CategoryDimension.GENRE -> {
                    GenrePage(
                        genreOptions = genreOptions,
                        selectedGenre = selectedGenre,
                        onGenreChange = onGenreChange,
                        uiState = genreUiState,
                        listState = genreListState,
                        onRetry = onRetry,
                        onWorkClick = { work, rect -> onWorkClick(work, null, rect) },
                    )
                }
                CategoryDimension.ANTHOLOGY -> {
                    AnthologyPage(
                        anthologiesList = anthologiesList,
                        selectedAnthologyId = selectedAnthologyId,
                        onAnthologyChange = onAnthologyChange,
                        uiState = anthologyUiState,
                        listState = anthologyListState,
                        onRetry = onRetry,
                        onWorkClick = onWorkClick,
                    )
                }
            }
        }
    }
}

/**
 * 兼容单 listState 的重载函数
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
    onWorkClick: (Work, List<Work>?, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) = CategoryScreen(
    repository = repository,
    works = works,
    dynastyListState = listState,
    genreListState = listState,
    anthologyListState = listState,
    selectedDimension = selectedDimension,
    selectedDynasty = selectedDynasty,
    selectedGenre = selectedGenre,
    selectedAnthologyId = selectedAnthologyId,
    requestVersion = requestVersion,
    onDimensionChange = onDimensionChange,
    onDynastyChange = onDynastyChange,
    onGenreChange = onGenreChange,
    onAnthologyChange = onAnthologyChange,
    onRetry = onRetry,
    onWorkClick = onWorkClick,
    modifier = modifier,
)

@Composable
private fun DynastyPage(
    dynastyOptions: List<String>,
    selectedDynasty: String,
    onDynastyChange: (String) -> Unit,
    uiState: CategoryUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onWorkClick: (Work, Rect?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is CategoryUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                is CategoryUiState.Empty -> Text(text = "暂无符合条件的作品", fontSize = 15.sp, color = MaterialTheme.colorScheme.outline)
                is CategoryUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(text = uiState.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text(text = "重试") }
                    }
                }
                is CategoryUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items = uiState.works, key = { it.id }) { work ->
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            CategoryWorkItem(
                                work = work,
                                onClick = { onWorkClick(work, itemBounds) },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    itemBounds = coordinates.boundsInRoot()
                                },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
                is CategoryUiState.AnthologySuccess -> Unit
            }
        }
    }
}

@Composable
private fun GenrePage(
    genreOptions: List<GenreOption>,
    selectedGenre: String,
    onGenreChange: (String) -> Unit,
    uiState: CategoryUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onWorkClick: (Work, Rect?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is CategoryUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                is CategoryUiState.Empty -> Text(text = "暂无符合条件的作品", fontSize = 15.sp, color = MaterialTheme.colorScheme.outline)
                is CategoryUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(text = uiState.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text(text = "重试") }
                    }
                }
                is CategoryUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items = uiState.works, key = { it.id }) { work ->
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            CategoryWorkItem(
                                work = work,
                                onClick = { onWorkClick(work, itemBounds) },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    itemBounds = coordinates.boundsInRoot()
                                },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
                is CategoryUiState.AnthologySuccess -> Unit
            }
        }
    }
}

@Composable
private fun AnthologyPage(
    anthologiesList: List<Anthology>,
    selectedAnthologyId: String,
    onAnthologyChange: (String) -> Unit,
    uiState: CategoryUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onWorkClick: (Work, List<Work>?, Rect?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is CategoryUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                is CategoryUiState.Empty -> Text(text = "暂无符合条件的作品", fontSize = 15.sp, color = MaterialTheme.colorScheme.outline)
                is CategoryUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(text = uiState.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text(text = "重试") }
                    }
                }
                is CategoryUiState.AnthologySuccess -> {
                    val anthologyWorks = remember(uiState.items) {
                        uiState.items.map { it.work }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        uiState.anthology?.let { anth ->
                            item(key = "anthology_header") {
                                val appTheme = com.shi.yun.ui.theme.LocalAppTheme.current
                                val isDark = appTheme.themeId.isDark
                                val itemCardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDark) 0.60f else 0.42f)
                                val itemBorder = appTheme.customTokens.cardBorder.copy(alpha = if (isDark) 0.40f else 0.25f)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(itemCardBg)
                                        .border(0.5.dp, itemBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = "${anth.name} · 共收录 ${uiState.items.size} 篇",
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
                            }
                        }

                        items(
                            items = uiState.items,
                            key = { it.item.id },
                        ) { item ->
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            AnthologyWorkRowItem(
                                item = item,
                                onClick = { onWorkClick(item.work, anthologyWorks, itemBounds) },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    itemBounds = coordinates.boundsInRoot()
                                },
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
                is CategoryUiState.Success -> Unit
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
    val appTheme = com.shi.yun.ui.theme.LocalAppTheme.current
    val isDark = appTheme.themeId.isDark
    val itemCardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDark) 0.60f else 0.42f)
    val itemBorder = appTheme.customTokens.cardBorder.copy(alpha = if (isDark) 0.40f else 0.25f)

    val work = item.work
    val orderStr = String.format(java.util.Locale.CHINA, "%03d", item.item.orderIndex)
    val authorText = work.formatAuthorDynasty().ifEmpty { work.author }

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
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = orderStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )

                if (item.item.volume.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = item.item.volume,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Text(
                    text = work.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Text(
                text = authorText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        if (work.excerpt.isNotBlank()) {
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
            text = work.excerpt,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
    }
}
