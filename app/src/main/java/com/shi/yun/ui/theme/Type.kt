package com.shi.yun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 动态根据指定 [FontFamily] 构建诗云全应用字阶排版规范
 */
fun createAppTypography(fontFamily: FontFamily = FontFamily.Default): Typography {
    return Typography(
        // 作品大标题 / 词牌名
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.5.sp,
        ),
        // 词题副标题 / 选本名称
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        // 诗词正文诗句（端庄舒朗，避免拥挤）
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 19.sp,
            lineHeight = 34.sp,
            letterSpacing = 1.2.sp,
        ),
        // 小序题记 / 列表摘录 / 说明正文
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp,
        ),
        // 朝代作者 / 次级说明文字
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp,
        ),
        // 标签徽标 / 辅助极简小标
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

/**
 * 默认字阶排版规范
 */
val Typography = createAppTypography(FontFamily.Default)
