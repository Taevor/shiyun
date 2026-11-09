package com.shi.yun.ui.detail

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shi.yun.data.model.ReadingFontFamily
import com.shi.yun.data.model.ReadingPreferences
import com.shi.yun.data.repository.CustomFontRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 个性化阅读版式调节紧凑底部弹窗 (ModalBottomSheet)
 *
 * 核心特性：
 * 1. 严格单行布局：左侧参数名，中间细微递减[-]、可拖动滑块、细微递增[+]，右侧数值清晰右对齐；
 * 2. 支持内置系统字体与从本地直接导入 TTF/OTF 自定义字体，彻底解决 OPPO 等品牌机型字体不生效问题；
 * 3. 字体选择后即时全局联动，包含导入、选择、删除与安全容错；
 * 4. 面板最大高度严格约束在屏幕高度 65% 以内，超出内部滚动；
 * 5. 保留全部排版调节参数、步进精度、实时预览与一键恢复默认功能。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    preferences: ReadingPreferences,
    onPreferencesChange: (ReadingPreferences) -> Unit,
    onResetToDefault: () -> Unit,
    onDismissRequest: () -> Unit,
    customFontRepository: CustomFontRepository? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.65f).dp

    var customFonts by remember(customFontRepository) {
        mutableStateOf(customFontRepository?.getCustomFonts().orEmpty())
    }
    var isImporting by remember { mutableStateOf(false) }

    // 系统文件选择器：支持导入 TTF 与 OTF
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && customFontRepository != null) {
            scope.launch {
                isImporting = true
                val result = customFontRepository.importFont(uri)
                isImporting = false
                result.onSuccess { item ->
                    customFonts = customFontRepository.getCustomFonts()
                    onPreferencesChange(
                        preferences.copy(
                            customFontId = item.id,
                            customFontName = item.name,
                        )
                    )
                    Toast.makeText(context, "已成功导入字体：${item.name}", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "字体导入失败，请检查文件格式", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(bottom = 16.dp),
        ) {
            // 1. 顶部标题栏与恢复默认按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "阅读版式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TextButton(
                    onClick = onResetToDefault,
                ) {
                    Text(
                        text = "恢复默认",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 2. 基础外观与高级排版双 Tab（固定不滚动）
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)) },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("基础外观", fontSize = 14.sp) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("高级排版", fontSize = 14.sp) },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 内容设置区域（内部可滚动，最大高度受控，避免挤占上方正文）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selectedTab == 0) {
                    // ========== 选项卡一：基础外观 ==========
                    // 1. 系统内置字体类型选择
                    CompactChoiceRow(
                        label = "内置字体",
                        options = ReadingFontFamily.entries.map { it to it.displayName },
                        selected = if (preferences.customFontId == null) preferences.fontType else null,
                        onSelect = {
                            onPreferencesChange(
                                preferences.copy(
                                    fontType = it,
                                    customFontId = null,
                                    customFontName = null,
                                )
                            )
                        },
                    )

                    // 2. 已导入的自定义字体列表
                    if (customFonts.isNotEmpty()) {
                        customFonts.forEach { item ->
                            val isSelected = preferences.customFontId == item.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        onPreferencesChange(
                                            preferences.copy(
                                                customFontId = item.id,
                                                customFontName = item.name,
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "使用中",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                }

                                // 删除自定义字体按钮
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            scope.launch {
                                                customFontRepository?.deleteFont(item.id)
                                                customFonts = customFontRepository?.getCustomFonts().orEmpty()
                                                if (preferences.customFontId == item.id) {
                                                    onPreferencesChange(
                                                        preferences.copy(
                                                            customFontId = null,
                                                            customFontName = null,
                                                            fontType = ReadingFontFamily.SYSTEM_DEFAULT,
                                                        )
                                                    )
                                                }
                                                Toast.makeText(context, "已删除字体: ${item.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "×",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }

                    // 3. 从手机本地导入字体按钮（TTF / OTF）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = !isImporting) {
                                fontPickerLauncher.launch(
                                    arrayOf(
                                        "font/ttf",
                                        "font/otf",
                                        "font/*",
                                        "application/x-font-ttf",
                                        "application/x-font-opentype",
                                        "application/octet-stream",
                                        "*/*",
                                    )
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isImporting) "正在导入并校验字体..." else "+ 导入本地字体 (TTF / OTF)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // 字体粗细（单行紧凑分段选择）
                    val weights = listOf(0 to "常规", 1 to "适中", 2 to "加粗")
                    CompactChoiceRow(
                        label = "字体粗细",
                        options = weights,
                        selected = preferences.fontWeightLevel,
                        onSelect = { onPreferencesChange(preferences.copy(fontWeightLevel = it)) },
                    )

                    // 正文字号调整 (14 ~ 34 sp, 默认 22)
                    StepperSliderRow(
                        label = "正文字号",
                        displayValue = "${preferences.bodyFontSizeSp.roundToInt()} sp",
                        currentValue = preferences.bodyFontSizeSp,
                        range = 14f..34f,
                        step = 1f,
                        onValueChange = { onPreferencesChange(preferences.copy(bodyFontSizeSp = it)) },
                    )

                    // 正文行高倍率调整 (1.30x ~ 2.40x, 默认 1.70x)
                    StepperSliderRow(
                        label = "正文行高",
                        displayValue = "%.2fx".format(preferences.lineHeightMultiplier),
                        currentValue = preferences.lineHeightMultiplier,
                        range = 1.30f..2.40f,
                        step = 0.05f,
                        onValueChange = { onPreferencesChange(preferences.copy(lineHeightMultiplier = it)) },
                    )
                } else {
                    // ========== 选项卡二：高级排版 ==========
                    // 字间距 (0.0 ~ 3.0 sp)
                    StepperSliderRow(
                        label = "文字字距",
                        displayValue = "%.1f sp".format(preferences.letterSpacingSp),
                        currentValue = preferences.letterSpacingSp,
                        range = 0.0f..3.0f,
                        step = 0.1f,
                        onValueChange = { onPreferencesChange(preferences.copy(letterSpacingSp = it)) },
                    )

                    // 左右阅读边距 (12 ~ 40 dp)
                    StepperSliderRow(
                        label = "左右边距",
                        displayValue = "${preferences.horizontalPaddingDp.roundToInt()} dp",
                        currentValue = preferences.horizontalPaddingDp,
                        range = 12f..40f,
                        step = 2f,
                        onValueChange = { onPreferencesChange(preferences.copy(horizontalPaddingDp = it)) },
                    )

                    // 诗句原始行距 (4 ~ 18 dp)
                    StepperSliderRow(
                        label = "诗句间距",
                        displayValue = "${preferences.lineSpacingDp.roundToInt()} dp",
                        currentValue = preferences.lineSpacingDp,
                        range = 4f..18f,
                        step = 1f,
                        onValueChange = { onPreferencesChange(preferences.copy(lineSpacingDp = it)) },
                    )

                    // 绝句/律诗分联间距 (8 ~ 32 dp)
                    StepperSliderRow(
                        label = "分联留白",
                        displayValue = "${preferences.coupletSpacingDp.roundToInt()} dp",
                        currentValue = preferences.coupletSpacingDp,
                        range = 8f..32f,
                        step = 2f,
                        onValueChange = { onPreferencesChange(preferences.copy(coupletSpacingDp = it)) },
                    )

                    // 词阕 / 段落留白 (10 ~ 40 dp)
                    StepperSliderRow(
                        label = "阕段留白",
                        displayValue = "${preferences.paragraphSpacingDp.roundToInt()} dp",
                        currentValue = preferences.paragraphSpacingDp,
                        range = 10f..40f,
                        step = 2f,
                        onValueChange = {
                            onPreferencesChange(
                                preferences.copy(
                                    stanzaSpacingDp = it,
                                    paragraphSpacingDp = it,
                                )
                            )
                        },
                    )

                    // 古文自然段首行缩进开关（单行紧凑开关）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "首行缩进",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(66.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "仅作用于古文自然段",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }

                        Switch(
                            checked = preferences.firstLineIndentSpaces >= 2,
                            onCheckedChange = { checked ->
                                onPreferencesChange(
                                    preferences.copy(firstLineIndentSpaces = if (checked) 2 else 0)
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单行紧凑分段选择器行（用于字体类型与字体粗细切换）
 */
@Composable
private fun <T> CompactChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(66.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (option, title) ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 紧凑型双向步进滑块控制行
 */
@Composable
private fun StepperSliderRow(
    label: String,
    displayValue: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(66.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 微调减号按钮 [-]
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(enabled = currentValue > range.start) {
                    val newVal = (currentValue - step).coerceIn(range.start, range.endInclusive)
                    onValueChange(newVal)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "−",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentValue > range.start) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }

        // 平滑滑块
        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        )

        // 微调加号按钮 [+]
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(enabled = currentValue < range.endInclusive) {
                    val newVal = (currentValue + step).coerceIn(range.start, range.endInclusive)
                    onValueChange(newVal)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentValue < range.endInclusive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧数值标签
        Text(
            text = displayValue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}
