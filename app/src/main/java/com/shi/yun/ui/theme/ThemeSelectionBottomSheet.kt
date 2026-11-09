package com.shi.yun.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 诗云全局主题外观选择半屏浮层 (ThemeSelectionBottomSheet)
 *
 * 核心特性：
 * 1. 独立于阅读排版参数，专职负责全应用视觉基调；
 * 2. 严格按【经典主题】(6套) 与【四时主题】(4套) 清晰分组排布；
 * 3. 每套主题展示雅致三色调色板预览、主标题与诗意副标题；
 * 4. 即触即换：点击后毫秒级全局响应换肤，界面自适应重绘，无需二次确认或重启；
 * 5. 自身 100% 消费当前主题色彩，切换瞬间浮层本体随之同步蝶变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionBottomSheet(
    currentThemeId: AppThemeId,
    onThemeSelected: (AppThemeId) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val customTokens = LocalAppTheme.current.customTokens

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 顶层标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "全局外观主题",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "十套古典配色 · 点击即时生效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "完成",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismissRequest)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ========== 分组一：经典主题 (6套) ==========
            ThemeGroupHeader(title = "经典主题", count = 6)
            Spacer(modifier = Modifier.height(10.dp))
            AppThemeId.getByCategory(ThemeCategory.CLASSIC).forEach { theme ->
                ThemeItemCard(
                    theme = theme,
                    isSelected = theme == currentThemeId,
                    onSelect = { onThemeSelected(theme) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ========== 分组二：四时主题 (4套) ==========
            ThemeGroupHeader(title = "四时流转", count = 4)
            Spacer(modifier = Modifier.height(10.dp))
            AppThemeId.getByCategory(ThemeCategory.SEASONAL).forEach { theme ->
                ThemeItemCard(
                    theme = theme,
                    isSelected = theme == currentThemeId,
                    onSelect = { onThemeSelected(theme) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 底部安全留白
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * 主题分组小标题
 */
@Composable
private fun ThemeGroupHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${count}款",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * 单个主题展示卡片（雅致三色调色板圆环、主副标题、明确选中态）
 */
@Composable
private fun ThemeItemCard(
    theme: AppThemeId,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 1.2.dp else 0.6.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onSelect,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 三色预览色盘（代表色、纸张底色、点睛色）
        ThemeColorPaletteSwatch(
            primary = theme.primaryPreviewColor,
            surface = theme.surfacePreviewColor,
            accent = theme.accentPreviewColor,
        )

        Spacer(modifier = Modifier.width(14.dp))

        // 2. 主题名称与意境副标题
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (theme.isDark) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "夜读",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = theme.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 3. 选中指示徽印
        if (isSelected) {
            ThemeCheckedStamp(tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * 典雅三色调色板组合（纸张底色大圆 + 主色与点睛色双半圆叠合）
 */
@Composable
private fun ThemeColorPaletteSwatch(
    primary: Color,
    surface: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(surface)
            .border(0.6.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(20.dp)
                    .background(primary),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(20.dp)
                    .background(accent),
            )
        }
    }
}

/**
 * 极简中式对勾印记
 */
@Composable
private fun ThemeCheckedStamp(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.52f),
            end = Offset(w * 0.45f, h * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.45f, h * 0.78f),
            end = Offset(w * 0.85f, h * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
