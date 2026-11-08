package com.childe.shi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.data.model.Work
import com.childe.shi.data.repository.LocalReadingPreferencesRepository
import com.childe.shi.data.repository.ReadingPreferencesRepository
import com.childe.shi.data.repository.WorkRepository
import com.childe.shi.ui.category.CategoryDimension
import com.childe.shi.ui.category.CategoryScreen
import com.childe.shi.ui.detail.DetailScreen
import com.childe.shi.ui.favorite.FavoriteScreen
import com.childe.shi.ui.home.HomeScreen
import com.childe.shi.ui.home.generateRecommendations
import com.childe.shi.common.log.AppLogger
import com.childe.shi.ui.search.SearchScreen
import kotlinx.coroutines.launch

/**
 * 底部导航入口枚举（支持【首页】、【分类】、【搜索】与【收藏】四个一级入口）
 */
enum class AppTab(val label: String) {
    HOME("首页"),
    CATEGORY("分类"),
    SEARCH("搜索"),
    FAVORITE("收藏")
}

/**
 * 应用根组件与主导航控制器
 *
 * 核心架构设计：
 * 1. 状态提升（State Hoisting）：
 *    - 首页 PagerState 与推荐序列由顶层持久维护；
 *    - 分类页 LazyListState、维度（朝代 vs 体裁）、各维度独立标签及请求版本号由顶层持久维护；
 *    - 搜索页 searchKeyword（rememberSaveable）与 searchListState 由顶层持久维护；
 *    - 全局阅读偏好 ReadingPreferences 由顶层持久化仓储管理并实时响应；
 * 2. 交互安全隔离：
 *    - 采用标准条件分支（when）组合，未展示的页面不进入布局树，绝对阻断滑动与点击干扰；
 * 3. 详情页全屏覆盖与底栏自适应：
 *    - 打开详情页时覆盖全屏并隐藏底栏；返回后状态完全恢复；
 * 4. 纯原生绘制：底栏图标采用 Canvas 绘制，零外部图标库依赖。
 */
@Composable
fun ShiApp(
    repository: WorkRepository,
    preferencesRepository: ReadingPreferencesRepository? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefsRepo = remember(preferencesRepository, context) {
        preferencesRepository ?: LocalReadingPreferencesRepository(context.applicationContext)
    }

    var allWorks by remember { mutableStateOf<List<Work>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        errorMessage = null
        AppLogger.i("ShiApp", "发起全量诗词数据异步装载")
        val startTime = System.currentTimeMillis()
        scope.launch {
            try {
                val works = repository.getAllWorks()
                allWorks = works
                val duration = System.currentTimeMillis() - startTime
                AppLogger.i("ShiApp", "全量诗词数据装载成功: 总数=${works.size}首, 耗时=${duration}ms")
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                AppLogger.e("ShiApp", "全量诗词数据装载失败: 耗时=${duration}ms, 原因=${e.message}", e)
                errorMessage = e.message ?: "读取本地诗词数据失败"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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
                    repository = repository,
                    works = works,
                    selectedWork = selectedWork,
                    preferencesRepository = prefsRepo,
                    onSelectWork = { work ->
                        AppLogger.i("Detail", "进入作品详情: ID=${work.id}, 标题=${work.title}, 朝代=${work.dynasty}, 作者=${work.author}")
                        selectedWork = work
                    },
                    onDismissDetail = {
                        AppLogger.i("Detail", "离开作品详情")
                        selectedWork = null
                    },
                )
            }
        }
    }
}

/**
 * 主内容区域组件
 */
@Composable
private fun MainContent(
    repository: WorkRepository,
    works: List<Work>,
    selectedWork: Work?,
    preferencesRepository: ReadingPreferencesRepository,
    onSelectWork: (Work) -> Unit,
    onDismissDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readingPreferences by preferencesRepository.preferencesFlow.collectAsState()

    // 顶层状态：当前选中的主功能 Tab
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }

    // 在分类页或搜索页按下系统返回键时回到首页
    BackHandler(enabled = selectedWork == null && currentTab != AppTab.HOME) {
        currentTab = AppTab.HOME
    }

    // ========== 首页状态提升（持久留存） ==========
    val recommendationList = remember(works) {
        mutableStateListOf<Work>().apply {
            addAll(generateRecommendations(works = works, count = 40))
        }
    }
    var homeCurrentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isHomeHintShown by remember { mutableStateOf(preferencesRepository.isHomeGestureHintShown()) }

    LaunchedEffect(homeCurrentIndex) {
        if (recommendationList.size - homeCurrentIndex <= 8 && works.isNotEmpty()) {
            val more = generateRecommendations(
                works = works,
                count = 20,
                lastWork = recommendationList.lastOrNull(),
            )
            recommendationList.addAll(more)
        }
    }

    // ========== 分类页状态提升（持久留存，维度解耦） ==========
    val categoryListState = rememberLazyListState()
    var selectedDimension by rememberSaveable { mutableStateOf(CategoryDimension.DYNASTY) }
    var selectedDynasty by rememberSaveable { mutableStateOf("全部") }
    var selectedGenre by rememberSaveable { mutableStateOf("ALL") }
    var selectedAnthologyId by rememberSaveable { mutableStateOf("anth_ts300") }
    var requestVersion by remember { mutableLongStateOf(0L) }

    fun onDimensionChange(newDimension: CategoryDimension) {
        if (selectedDimension != newDimension) {
            selectedDimension = newDimension
            requestVersion++
        }
    }

    fun onDynastyChange(newDynasty: String) {
        if (selectedDynasty != newDynasty) {
            selectedDynasty = newDynasty
            requestVersion++
        }
    }

    fun onGenreChange(newGenre: String) {
        if (selectedGenre != newGenre) {
            selectedGenre = newGenre
            requestVersion++
        }
    }

    fun onAnthologyChange(newAnthologyId: String) {
        if (selectedAnthologyId != newAnthologyId) {
            selectedAnthologyId = newAnthologyId
            requestVersion++
        }
    }

    fun onRetryCategory() {
        requestVersion++
    }

    // ========== 选本阅读切篇上下文状态 ==========
    var anthologyContextWorks by remember { mutableStateOf<List<Work>?>(null) }
    var anthologyCurrentIndex by remember { mutableIntStateOf(-1) }

    // 容器转场来源矩形坐标快照与交互锁
    var transitionSourceRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // 统一处理作品点击进入详情（记录来源矩形快照，并在进入转场期间防二次重入）
    fun navigateToDetail(work: Work, contextWorks: List<Work>?, sourceRect: androidx.compose.ui.geometry.Rect?) {
        if (isTransitioning || selectedWork != null) return
        isTransitioning = true
        transitionSourceRect = sourceRect
        anthologyContextWorks = contextWorks
        anthologyCurrentIndex = if (contextWorks != null) contextWorks.indexOfFirst { it.id == work.id } else -1
        onSelectWork(work)
    }

    // 选本内切篇：上一首（切篇后清除来源矩形，返回时自动优雅降级为淡出）
    fun onPreviousAnthologyWork() {
        val list = anthologyContextWorks ?: return
        if (anthologyCurrentIndex > 0) {
            val prevIndex = anthologyCurrentIndex - 1
            val prevWork = list[prevIndex]
            anthologyCurrentIndex = prevIndex
            transitionSourceRect = null // 切篇后坐标失效，标记为 null 以便返回时平滑降级
            AppLogger.i("Detail", "选本切篇 [上一首]: 序号=$prevIndex, 作品 ID=${prevWork.id}, 标题=${prevWork.title}")
            onSelectWork(prevWork)
        }
    }

    // 选本内切篇：下一首（切篇后清除来源矩形，返回时自动优雅降级为淡出）
    fun onNextAnthologyWork() {
        val list = anthologyContextWorks ?: return
        if (anthologyCurrentIndex in 0 until list.size - 1) {
            val nextIndex = anthologyCurrentIndex + 1
            val nextWork = list[nextIndex]
            anthologyCurrentIndex = nextIndex
            transitionSourceRect = null // 切篇后坐标失效，标记为 null 以便返回时平滑降级
            AppLogger.i("Detail", "选本切篇 [下一首]: 序号=$nextIndex, 作品 ID=${nextWork.id}, 标题=${nextWork.title}")
            onSelectWork(nextWork)
        }
    }

    // ========== 搜索页状态提升（持久留存） ==========
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    val searchListState = rememberLazyListState()

    // ========== 收藏页状态提升与响应式数据监听 ==========
    val favoriteListState = rememberLazyListState()
    val favoriteIds by repository.favoriteIdsFlow.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 页面内容显示区：Tab 之间采用轻量克制的淡入淡出动画过渡，未处于活跃状态的页面不进入渲染树，绝对杜绝触控穿透
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                androidx.compose.animation.AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                    },
                    label = "TabTransition",
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.HOME -> {
                            HomeScreen(
                                recommendationList = recommendationList,
                                currentIndex = homeCurrentIndex,
                                onIndexChange = { newIndex -> homeCurrentIndex = newIndex },
                                isHintShown = isHomeHintShown,
                                onMarkHintShown = {
                                    scope.launch {
                                        preferencesRepository.markHomeGestureHintShown()
                                        isHomeHintShown = true
                                    }
                                },
                                onWorkClick = { work, rect ->
                                    navigateToDetail(work, null, rect)
                                },
                            )
                        }
                        AppTab.CATEGORY -> {
                            CategoryScreen(
                                repository = repository,
                                works = works,
                                listState = categoryListState,
                                selectedDimension = selectedDimension,
                                selectedDynasty = selectedDynasty,
                                selectedGenre = selectedGenre,
                                selectedAnthologyId = selectedAnthologyId,
                                requestVersion = requestVersion,
                                onDimensionChange = ::onDimensionChange,
                                onDynastyChange = ::onDynastyChange,
                                onGenreChange = ::onGenreChange,
                                onAnthologyChange = ::onAnthologyChange,
                                onRetry = ::onRetryCategory,
                                onWorkClick = { work, ctxWorks, rect ->
                                    navigateToDetail(work, ctxWorks, rect)
                                },
                            )
                        }
                        AppTab.SEARCH -> {
                            SearchScreen(
                                repository = repository,
                                keyword = searchKeyword,
                                onKeywordChange = { searchKeyword = it },
                                listState = searchListState,
                                onWorkClick = { work, rect ->
                                    navigateToDetail(work, null, rect)
                                },
                            )
                        }
                        AppTab.FAVORITE -> {
                            FavoriteScreen(
                                repository = repository,
                                listState = favoriteListState,
                                onWorkClick = { work, rect ->
                                    navigateToDetail(work, null, rect)
                                },
                            )
                        }
                    }
                }
            }

            // 底部导航栏：始终稳定驻留于原有 Column 底部，不随详情页展开而改变高度与挤压内容区
            ShiBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { newTab ->
                    if (selectedWork == null && currentTab != newTab) {
                        AppLogger.i("Navigation", "切换底部主导航: ${currentTab.label} -> ${newTab.label}")
                        currentTab = newTab
                    }
                },
            )
        }

        // 详情页全屏浮层：采用原生容器展开/收拢转场，来源矩形无效或切篇后自动平滑降级
        com.childe.shi.ui.animation.ContainerTransformBox(
            visible = selectedWork != null,
            sourceRect = transitionSourceRect,
            onDismissRequest = {
                anthologyContextWorks = null
                anthologyCurrentIndex = -1
                transitionSourceRect = null
                isTransitioning = false
                onDismissDetail()
            },
        ) { requestDismiss ->
            selectedWork?.let { work ->
                val isFav = favoriteIds.contains(work.id)
                val inAnthology = anthologyContextWorks != null
                val hasPrev = inAnthology && anthologyCurrentIndex > 0
                val hasNxt = inAnthology && anthologyContextWorks?.let { anthologyCurrentIndex in 0 until it.size - 1 } == true

                DetailScreen(
                    work = work,
                    isFavorite = isFav,
                    onToggleFavorite = {
                        scope.launch {
                            repository.toggleFavorite(work.id)
                        }
                    },
                    onBack = requestDismiss,
                    onPreviousWork = if (inAnthology) ::onPreviousAnthologyWork else null,
                    onNextWork = if (inAnthology) ::onNextAnthologyWork else null,
                    hasPrevious = hasPrev,
                    hasNext = hasNxt,
                    preferences = readingPreferences,
                    onPreferencesChange = { newPrefs ->
                        scope.launch {
                            preferencesRepository.updatePreferences(newPrefs)
                        }
                    },
                    onResetPreferences = {
                        scope.launch {
                            preferencesRepository.resetToDefault()
                        }
                    },
                )
            }
        }
    }
}

/**
 * 底部原生导航栏（展示【首页】、【分类】、【搜索】与【收藏】四项，原生 Canvas 图标绘制）
 */
@Composable
private fun ShiBottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = {
                HomeNavIcon(
                    tint = if (currentTab == AppTab.HOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            },
            label = { Text(text = AppTab.HOME.label, fontSize = 12.sp) },
        )

        NavigationBarItem(
            selected = currentTab == AppTab.CATEGORY,
            onClick = { onTabSelected(AppTab.CATEGORY) },
            icon = {
                CategoryNavIcon(
                    tint = if (currentTab == AppTab.CATEGORY) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            },
            label = { Text(text = AppTab.CATEGORY.label, fontSize = 12.sp) },
        )

        NavigationBarItem(
            selected = currentTab == AppTab.SEARCH,
            onClick = { onTabSelected(AppTab.SEARCH) },
            icon = {
                SearchNavIcon(
                    tint = if (currentTab == AppTab.SEARCH) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            },
            label = { Text(text = AppTab.SEARCH.label, fontSize = 12.sp) },
        )

        NavigationBarItem(
            selected = currentTab == AppTab.FAVORITE,
            onClick = { onTabSelected(AppTab.FAVORITE) },
            icon = {
                FavoriteNavIcon(
                    tint = if (currentTab == AppTab.FAVORITE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            },
            label = { Text(text = AppTab.FAVORITE.label, fontSize = 12.sp) },
        )
    }
}

/**
 * 首页图标：极简古典亭台/屋脊造型（原生 Canvas 绘制）
 */
@Composable
private fun HomeNavIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // 飞檐屋顶
        val roofPath = Path().apply {
            moveTo(w * 0.15f, h * 0.46f)
            lineTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.46f)
        }
        drawPath(
            path = roofPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        // 屋身立柱与基底
        val bodyPath = Path().apply {
            moveTo(w * 0.28f, h * 0.46f)
            lineTo(w * 0.28f, h * 0.78f)
            lineTo(w * 0.72f, h * 0.78f)
            lineTo(w * 0.72f, h * 0.46f)
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * 分类图标：极简书卷/竹简折叠造型（原生 Canvas 绘制）
 */
@Composable
private fun CategoryNavIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.3f),
            end = Offset(w * 0.8f, h * 0.3f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.65f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.7f),
            end = Offset(w * 0.8f, h * 0.7f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 搜索图标：极简放大镜造型（原生 Canvas 绘制）
 */
@Composable
private fun SearchNavIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // 镜框圆环
        val center = Offset(w * 0.44f, h * 0.44f)
        val radius = w * 0.25f
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )

        // 镜柄斜线
        drawLine(
            color = tint,
            start = Offset(center.x + radius * 0.707f, center.y + radius * 0.707f),
            end = Offset(w * 0.82f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 收藏图标：极简古典书签造型（原生 Canvas 绘制）
 */
@Composable
private fun FavoriteNavIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val bookmarkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.2f)
            lineTo(w * 0.72f, h * 0.2f)
            lineTo(w * 0.72f, h * 0.8f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.28f, h * 0.8f)
            close()
        }
        drawPath(
            path = bookmarkPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
