package com.shi.yun.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.shi.yun.data.model.Genre
import com.shi.yun.data.model.LayoutType
import com.shi.yun.data.model.ReadingPreferences
import com.shi.yun.data.model.Work
import com.shi.yun.data.model.formatAuthorDynastyGenre
import com.shi.yun.data.repository.CustomFontRepository
import com.shi.yun.ui.animation.LocalContainerContentAlpha
import com.shi.yun.ui.animation.LocalContainerIsDismissing
import com.shi.yun.ui.favorite.FavoriteEmptyQuoteManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.shi.yun.ui.theme.LocalAppFontFamily
import com.shi.yun.ui.theme.LocalAppTheme

/**
 * 诗词阅读排版参数配置（由 ReadingPreferences 动态生成并驱动 UI 渲染）
 *
 * @property bodyFontSize 正文字号
 * @property bodyLineHeight 正文行高
 * @property letterSpacing 文字字间距
 * @property fontFamily 字体类型（系统默认/系统衬线/系统等宽）
 * @property fontWeight 字体字重
 * @property horizontalPadding 左右阅读版心边距
 * @property lineSpacing 诗句句内/句间行距
 * @property coupletSpacing 绝句/律诗分联间距
 * @property stanzaSpacing 宋词分阕留白
 * @property paragraphSpacing 古文/长篇自然段落间距
 * @property firstLineIndentSpaces 古文段落首行缩进空格数
 * @property backgroundColor 阅读背景色
 * @property textColor 正文文字色
 */
data class PoemLayoutConfig(
    val bodyFontSize: TextUnit = 22.sp,
    val bodyLineHeight: TextUnit = 37.sp,
    val letterSpacing: TextUnit = 0.5.sp,
    val fontFamily: FontFamily? = null,
    val fontWeight: FontWeight = FontWeight.Medium,
    val horizontalPadding: Dp = 24.dp,
    val lineSpacing: Dp = 8.dp,
    val coupletSpacing: Dp = 16.dp,
    val stanzaSpacing: Dp = 18.dp,
    val paragraphSpacing: Dp = 20.dp,
    val firstLineIndentSpaces: Int = 2,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
) {
    companion object {
        val Default = PoemLayoutConfig()
    }
}

/**
 * 诗词作品详情页
 *
 * 阶段五 B 全面升级：
 * 1. 词牌、词题与标题主副层级分明，避免重复；
 * 2. 小序题记独立雅致区块展示，空值不占位；
 * 3. 遵照格律验证行数后进行绝句、律诗分联分组；其他退回安全自然分行；
 * 4. 古文散文支持自然段首行缩进（TextIndent）与段落留白；
 * 5. 顶栏右侧新增「阅读版式」设置按钮，调出半屏设置面板，正文即时预览；
 * 6. 选本上一首/下一首平滑切篇与首尾禁用控制。
 */
@Composable
fun DetailScreen(
    work: Work,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onPreviousWork: (() -> Unit)? = null,
    onNextWork: (() -> Unit)? = null,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    preferences: ReadingPreferences = ReadingPreferences.Default,
    onPreferencesChange: ((ReadingPreferences) -> Unit)? = null,
    onResetPreferences: (() -> Unit)? = null,
    customFontRepository: CustomFontRepository? = null,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    // 双击收藏轻量反馈状态
    var feedbackTrigger by remember { mutableIntStateOf(0) }
    var feedbackIsFavorite by remember { mutableStateOf(false) }
    val feedbackAlpha = remember { Animatable(0f) }
    val feedbackScale = remember { Animatable(0.7f) }

    LaunchedEffect(feedbackTrigger) {
        if (feedbackTrigger > 0) {
            feedbackAlpha.snapTo(0f)
            feedbackScale.snapTo(0.7f)
            launch {
                feedbackScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
                )
            }
            feedbackAlpha.animateTo(1f, animationSpec = tween(150))
            delay(500)
            feedbackAlpha.animateTo(0f, animationSpec = tween(250))
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val activeFont = LocalAppFontFamily.current
    val isDarkTheme = LocalAppTheme.current.themeId.isDark
    val appTheme = LocalAppTheme.current
    // 低透明度局部宣纸背景，保证诗词文字清晰，同时完全展现底层山水纹理细节
    val poemCardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDarkTheme) 0.65f else 0.45f)
    val isContainerDismissing = LocalContainerIsDismissing.current
    // 退场收拢阶段彻底取消边框绘制（设为透明），杜绝收拢缩放过程中产生的黑边瑕疵
    val poemCardBorder = if (isContainerDismissing) {
        Color.Transparent
    } else {
        appTheme.customTokens.cardBorder.copy(alpha = if (isDarkTheme) 0.40f else 0.25f)
    }

    // 将全局阅读偏好动态解析为当前排版契约配置
    val layoutConfig = remember(preferences, surfaceColor, onSurfaceColor, activeFont) {
        preferences.toPoemLayoutConfig(
            themeSurface = surfaceColor,
            themeOnSurface = onSurfaceColor,
            activeFontFamily = activeFont,
        )
    }

    val contentAlpha = LocalContainerContentAlpha.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // 消费背景点击，阻断触摸下透
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                },
        ) {
            // 顶部操作栏（左侧极简返回键，右侧排版设置与收藏红心按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                BackArrowIcon(tint = MaterialTheme.colorScheme.onSurface)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 阅读版式调节按钮
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    SettingsSlidersIcon(tint = MaterialTheme.colorScheme.onSurface)
                }

                // 收藏红心按钮（全区域 48dp 响应，并与正文双击共用同一收藏状态及视觉反馈动效）
                val heartTint = LocalAppTheme.current.customTokens.heartFavoriteTint
                IconButton(
                    onClick = {
                        val next = !isFavorite
                        onToggleFavorite()
                        feedbackIsFavorite = next
                        feedbackTrigger++
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = FavoriteEmptyQuoteManager.FIRST_TIME_FAVORITE_HINT
                        },
                ) {
                    FavoriteHeartIcon(
                        isFavorite = isFavorite,
                        tint = if (isFavorite) heartTint else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        // 详情内容可滚动区域（全区域双击手势绑定：从标题栏下方至页面底部整个视口覆盖）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(work.id, isFavorite) {
                    detectTapGestures(
                        onDoubleTap = {
                            val next = !isFavorite
                            onToggleFavorite()
                            feedbackIsFavorite = next
                            feedbackTrigger++
                        },
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(poemCardBg)
                    .border(0.6.dp, poemCardBorder, RoundedCornerShape(16.dp))
                    .verticalScroll(scrollState)
                    .padding(horizontal = layoutConfig.horizontalPadding, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. 标题与作者信息层级
                PoemTitleSection(work = work)

                // 2. 小序 / 题记区块（若存在且不为空白，且过滤脏数据 "null"）
                val preface = work.preface?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
                if (preface != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    PoemPrefaceSection(preface = preface)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 3. 完整正文排版（根据体裁与排版契约动态渲染，受全屏统一透明度驱动，绝不修改原始数据）
                PoemContentLayout(work = work, config = layoutConfig)

                // 底部留白，保障视觉呼吸感
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 收藏轻量浮层反馈（居中展示心形与提示，温润宣纸卡片质感，自动柔和淡出）
            if (feedbackAlpha.value > 0f) {
                val appTheme = LocalAppTheme.current
                val isDark = appTheme.themeId.isDark
                val cardBg = appTheme.customTokens.cardBackground.copy(alpha = if (isDark) 0.96f else 0.95f)
                val borderColor = appTheme.customTokens.cardBorder
                val textColor = MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha = feedbackAlpha.value
                            scaleX = feedbackScale.value
                            scaleY = feedbackScale.value
                        }
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color.Black.copy(alpha = 0.08f),
                            ambientColor = Color.Black.copy(alpha = 0.04f),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(
                            width = 0.6.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FavoriteHeartIcon(
                            isFavorite = feedbackIsFavorite,
                            tint = if (feedbackIsFavorite) appTheme.customTokens.heartFavoriteTint else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = if (feedbackIsFavorite) "已收藏" else "已取消收藏",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                        )
                    }
                }
            }
        }

        // 选本场景专属：上一首 / 下一首 控制栏（非选本场景完全不显示）
        if (onPreviousWork != null || onNextWork != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(poemCardBg)
                    .border(
                        width = 0.5.dp,
                        color = poemCardBorder,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 上一首
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            enabled = hasPrevious,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 24.dp),
                            onClick = { onPreviousWork?.invoke() },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SmallArrowLeftIcon(
                        tint = if (hasPrevious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                    Text(
                        text = "上一首",
                        fontSize = 14.sp,
                        color = if (hasPrevious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }

                // 下一首
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            enabled = hasNext,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 24.dp),
                            onClick = { onNextWork?.invoke() },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "下一首",
                        fontSize = 14.sp,
                        color = if (hasNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                    SmallArrowRightIcon(
                        tint = if (hasNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }
            }
        }
        }
    }

    // 呼出个性化阅读版式调节半屏弹窗
    if (showSettingsSheet && onPreferencesChange != null) {
        ReadingSettingsSheet(
            preferences = preferences,
            onPreferencesChange = onPreferencesChange,
            onResetToDefault = { onResetPreferences?.invoke() },
            onDismissRequest = { showSettingsSheet = false },
            customFontRepository = customFontRepository,
        )
    }
}

/**
 * 诗词标题与元信息分层排版
 */
@Composable
private fun PoemTitleSection(work: Work) {
    val (primaryTitle, subtitle) = remember(work) {
        resolveTitleAndSubtitle(work)
    }
    val activeFont = LocalAppFontFamily.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 主标题 / 词牌
        Text(
            text = primaryTitle,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = activeFont),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        // 词题 / 副标题（避免与主标题重复）
        if (!subtitle.isNullOrBlank() && subtitle != primaryTitle) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = activeFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 作者、朝代与体裁
        Text(
            text = work.formatAuthorDynastyGenre(),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = activeFont),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 解析并拆分主标题与副标题，优先使用结构化 cipai 与 subtitle，并兼容含 · 的复合标题
 */
private fun resolveTitleAndSubtitle(work: Work): Pair<String, String?> {
    val rawCipai = work.cipai?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    val rawSubtitle = work.subtitle?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    if (!rawCipai.isNullOrEmpty()) {
        val sub = if (!rawSubtitle.isNullOrEmpty()) {
            rawSubtitle
        } else if (work.title.contains("·")) {
            val parts = work.title.split("·")
            if (parts.size >= 2 && parts[0].trim() == rawCipai) {
                parts.subList(1, parts.size).joinToString("·").trim()
            } else null
        } else null
        return Pair(rawCipai, sub)
    }

    if (work.title.contains("·")) {
        val parts = work.title.split("·")
        if (parts.size >= 2 && parts[0].isNotBlank()) {
            val main = parts[0].trim()
            val sub = parts.subList(1, parts.size).joinToString("·").trim()
            return Pair(main, sub.ifEmpty { null })
        }
    }

    return Pair(work.title, rawSubtitle.takeIf { !it.isNullOrBlank() })
}

/**
 * 小序 / 题记展示区块
 */
@Composable
private fun PoemPrefaceSection(
    preface: String,
    modifier: Modifier = Modifier,
) {
    val activeFont = LocalAppFontFamily.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column {
            Text(
                text = "【小序】",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = activeFont),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preface,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = activeFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 诗词正文精细化排版组件
 *
 * 遵循严格原则：
 * 1. 绝不修改或截断原始正文文字与标点；
 * 2. 古文散文支持自然段落首行缩进（TextIndent）与段间距；
 * 3. 绝句验证 4 行后分两联；
 * 4. 律诗验证 8 行后分四联（首联、颔联、颈联、尾联）；
 * 5. 宋词若有明确 \n\n 则按分阕留白，否则安全保留原本行序与行距，绝不猜测切阕；
 * 6. 全面接入 PoemLayoutConfig 参数，支持字号、行距、字距、字重与边距。
 */
@Composable
private fun PoemContentLayout(
    work: Work,
    config: PoemLayoutConfig = PoemLayoutConfig.Default,
    modifier: Modifier = Modifier,
) {
    val rawContent = work.content
    val isProse = work.genre == Genre.WEN || work.layout == LayoutType.PROSE

    // 1. 古文散文排版分支：自然段首行缩进，居左/两端对齐，段落间应用 paragraphSpacing
    if (isProse) {
        val paragraphs = if (rawContent.contains("\n\n")) {
            rawContent.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            rawContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val baseStyle = MaterialTheme.typography.bodyLarge
        val textStyle = baseStyle.copy(
            fontSize = config.bodyFontSize,
            lineHeight = config.bodyLineHeight,
            letterSpacing = config.letterSpacing,
            fontFamily = config.fontFamily ?: baseStyle.fontFamily,
            fontWeight = config.fontWeight,
            textIndent = if (config.firstLineIndentSpaces > 0) {
                TextIndent(firstLine = config.bodyFontSize * config.firstLineIndentSpaces)
            } else TextIndent.None,
        )
        val textColor = config.textColor ?: MaterialTheme.colorScheme.onSurface

        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(config.paragraphSpacing),
        ) {
            paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = textStyle,
                    color = textColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    // 2. 宋词排版分支：若原始文本有明确 \n\n，则按阕应用 stanzaSpacing；否则按原始换行应用 lineSpacing
    if (work.genre == Genre.CI) {
        if (rawContent.contains("\n\n")) {
            val stanzas = rawContent.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(config.stanzaSpacing),
            ) {
                stanzas.forEach { stanza ->
                    val lines = stanza.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    PoemLineGroup(lines = lines, config = config)
                }
            }
            return
        }

        // 无明确双换行分阕的宋词，安全退回自然分行，绝不根据标点或字数机械切阕
        val lines = rawContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(config.lineSpacing),
        ) {
            PoemLineGroup(lines = lines, config = config)
        }
        return
    }

    val lines = rawContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val layout = work.layout

    // 3. 绝句：必须严格验证行数为 4 句才分两联
    if ((layout == LayoutType.FIVE_JUEJU || layout == LayoutType.SEVEN_JUEJU) && lines.size == 4) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(config.coupletSpacing),
        ) {
            PoemLineGroup(lines = lines.subList(0, 2), config = config)
            PoemLineGroup(lines = lines.subList(2, 4), config = config)
        }
        return
    }

    // 4. 律诗：必须严格验证行数为 8 句才分四联
    if ((layout == LayoutType.FIVE_LVSHI || layout == LayoutType.SEVEN_LVSHI) && lines.size == 8) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(config.coupletSpacing),
        ) {
            PoemLineGroup(lines = lines.subList(0, 2), config = config) // 首联
            PoemLineGroup(lines = lines.subList(2, 4), config = config) // 颔联
            PoemLineGroup(lines = lines.subList(4, 6), config = config) // 颈联
            PoemLineGroup(lines = lines.subList(6, 8), config = config) // 尾联
        }
        return
    }

    // 5. 长篇古体诗或行数非标准的诗作：
    // 若有自然段落，使用 paragraphSpacing；否则安全分行，使用 lineSpacing
    if (rawContent.contains("\n\n")) {
        val sections = rawContent.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(config.paragraphSpacing),
        ) {
            sections.forEach { section ->
                val sectionLines = section.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                PoemLineGroup(lines = sectionLines, config = config)
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(config.lineSpacing),
    ) {
        PoemLineGroup(lines = lines, config = config)
    }
}

/**
 * 单个诗联或自然段落内的诗句渲染
 */
@Composable
private fun PoemLineGroup(
    lines: List<String>,
    config: PoemLayoutConfig = PoemLayoutConfig.Default,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.bodyLarge
    val textStyle = baseStyle.copy(
        fontSize = config.bodyFontSize,
        lineHeight = config.bodyLineHeight,
        letterSpacing = config.letterSpacing,
        fontFamily = config.fontFamily ?: baseStyle.fontFamily,
        fontWeight = config.fontWeight,
    )
    val textColor = config.textColor ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(config.lineSpacing),
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 极简中式返回箭头图标（原生 Canvas 绘制，零外部依赖）
 */
@Composable
private fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = Modifier.size(20.dp).then(modifier)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.85f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 极简中式排版调节图标（原生 Canvas 绘制，三条滑道与滑钮）
 */
@Composable
private fun SettingsSlidersIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = Modifier.size(20.dp).then(modifier)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height

        // 顶层滑轨与滑块
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.25f),
            end = Offset(w * 0.88f, h * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = tint,
            radius = 2.5.dp.toPx(),
            center = Offset(w * 0.35f, h * 0.25f),
        )

        // 中层滑轨与滑块
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.5f),
            end = Offset(w * 0.88f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = tint,
            radius = 2.5.dp.toPx(),
            center = Offset(w * 0.68f, h * 0.5f),
        )

        // 底层滑轨与滑块
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.75f),
            end = Offset(w * 0.88f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = tint,
            radius = 2.5.dp.toPx(),
            center = Offset(w * 0.42f, h * 0.75f),
        )
    }
}

/**
 * 极简心形收藏图标（原生 Canvas 绘制，黄金比例对称典雅心形，零外部依赖）
 */
@Composable
private fun FavoriteHeartIcon(
    isFavorite: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = Modifier.size(22.dp).then(modifier)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            // 左上圆弧段：从中央凹陷向左上方优雅隆起
            cubicTo(
                w * 0.44f, h * 0.14f,
                w * 0.30f, h * 0.10f,
                w * 0.20f, h * 0.16f,
            )
            // 左侧肩部向左下过渡段
            cubicTo(
                w * 0.07f, h * 0.24f,
                w * 0.05f, h * 0.44f,
                w * 0.16f, h * 0.58f,
            )
            // 左侧下弧向底部心尖收束
            cubicTo(
                w * 0.25f, h * 0.70f,
                w * 0.42f, h * 0.80f,
                w * 0.50f, h * 0.88f,
            )
            // 右侧从心尖向右上方展开
            cubicTo(
                w * 0.58f, h * 0.80f,
                w * 0.75f, h * 0.70f,
                w * 0.84f, h * 0.58f,
            )
            // 右侧肩部向上隆起
            cubicTo(
                w * 0.95f, h * 0.44f,
                w * 0.93f, h * 0.24f,
                w * 0.80f, h * 0.16f,
            )
            // 右上圆弧向中央凹陷平滑闭合
            cubicTo(
                w * 0.70f, h * 0.10f,
                w * 0.56f, h * 0.14f,
                w * 0.50f, h * 0.25f,
            )
            close()
        }

        if (isFavorite) {
            drawPath(path = path, color = tint)
        } else {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

/**
 * 选本翻页极简左箭头
 */
@Composable
private fun SmallArrowLeftIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.2f),
            end = Offset(w * 0.35f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.5f),
            end = Offset(w * 0.65f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 选本翻页极简右箭头
 */
@Composable
private fun SmallArrowRightIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.2f),
            end = Offset(w * 0.65f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.5f),
            end = Offset(w * 0.35f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
