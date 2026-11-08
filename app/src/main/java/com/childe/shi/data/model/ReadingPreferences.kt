package com.childe.shi.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.ui.detail.PoemLayoutConfig
import org.json.JSONObject

/**
 * 第一阶段支持的系统内置字体族
 */
enum class ReadingFontFamily(val id: String, val displayName: String) {
    SYSTEM_DEFAULT("system_default", "系统默认"),
    SYSTEM_SERIF("system_serif", "系统衬线"),
    SYSTEM_MONOSPACE("system_monospace", "系统等宽");

    companion object {
        fun fromId(id: String?): ReadingFontFamily {
            if (id.isNullOrBlank()) return SYSTEM_DEFAULT
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: SYSTEM_DEFAULT
        }
    }
}

/**
 * 阅读背景色彩风格配置
 *
 * FOLLOW_THEME: 统一遵循全局主题（默认，未来全局主题切换时自动联动，阅读页无需重新配置）
 * 保留其他枚举项作为底层技术扩展，阶段五 B 暂不在设置面板中开放独立纸张选择，避免产生两套主题系统。
 */
enum class ReadingPaperStyle(val id: String, val displayName: String) {
    FOLLOW_THEME("follow_theme", "随系统主题"),
    PAPER_CREAM("paper_cream", "宣纸微黄"),
    PAPER_SILK("paper_silk", "素绢浅米"),
    PAPER_WHITE("paper_white", "素白洁净"),
    NIGHT_INK("night_ink", "墨夜清辉");

    companion object {
        fun fromId(id: String?): ReadingPaperStyle {
            if (id.isNullOrBlank()) return FOLLOW_THEME
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: FOLLOW_THEME
        }
    }
}

/**
 * 诗云全局阅读偏好配置实体（轻量、跨作品全局生效、持久化落盘）
 */
data class ReadingPreferences(
    // ========== 1. 字体与字形 ==========
    val fontType: ReadingFontFamily = ReadingFontFamily.SYSTEM_DEFAULT,
    val fontWeightLevel: Int = 1,                 // 0: 常规(Normal), 1: 适中(Medium), 2: 加粗(Bold)
    val bodyFontSizeSp: Float = 22f,              // 正文字号 (sp, 范围 14~34)
    val letterSpacingSp: Float = 0.5f,            // 字间距 (sp, 范围 0.0~3.0)
    val lineHeightMultiplier: Float = 1.7f,       // 行高倍率 (范围 1.3~2.4)

    // ========== 2. 空间与结构间距 ==========
    val horizontalPaddingDp: Float = 24f,         // 左右阅读边距 (dp, 范围 12~40)
    val lineSpacingDp: Float = 8f,                // 诗句原始行间距 (dp, 范围 4~18)
    val coupletSpacingDp: Float = 16f,            // 绝句/律诗分联留白 (dp, 范围 8~32)
    val stanzaSpacingDp: Float = 18f,             // 宋词明确分阕留白 (dp, 范围 10~36)
    val paragraphSpacingDp: Float = 20f,          // 古文/长篇自然段落间距 (dp, 范围 10~40)
    val firstLineIndentSpaces: Int = 2,           // 古文段落首行缩进全角空格数 (0 或 2)

    // ========== 3. 纸张色彩 ==========
    val paperStyle: ReadingPaperStyle = ReadingPaperStyle.FOLLOW_THEME,
) {
    /**
     * 针对各排版参数施加边界约束与安全修正，防止非法数值输入导致渲染异常
     */
    fun coerced(): ReadingPreferences {
        return copy(
            fontWeightLevel = fontWeightLevel.coerceIn(0, 2),
            bodyFontSizeSp = bodyFontSizeSp.coerceIn(14f, 34f),
            letterSpacingSp = letterSpacingSp.coerceIn(0.0f, 3.0f),
            lineHeightMultiplier = lineHeightMultiplier.coerceIn(1.3f, 2.4f),
            horizontalPaddingDp = horizontalPaddingDp.coerceIn(12f, 40f),
            lineSpacingDp = lineSpacingDp.coerceIn(4f, 18f),
            coupletSpacingDp = coupletSpacingDp.coerceIn(8f, 32f),
            stanzaSpacingDp = stanzaSpacingDp.coerceIn(10f, 36f),
            paragraphSpacingDp = paragraphSpacingDp.coerceIn(10f, 40f),
            firstLineIndentSpaces = if (firstLineIndentSpaces >= 1) 2 else 0,
        )
    }

    /**
     * 将业务偏好完整无缝映射为 Compose UI 排版契约 [PoemLayoutConfig]
     *
     * @param themeSurface 当前主题的页面纸张底色（如宣纸米白）
     * @param themeOnSurface 当前主题的主要文字松墨色
     */
    fun toPoemLayoutConfig(
        themeSurface: Color,
        themeOnSurface: Color,
    ): PoemLayoutConfig {
        val safe = coerced()
        val family = when (safe.fontType) {
            ReadingFontFamily.SYSTEM_SERIF -> FontFamily.Serif
            ReadingFontFamily.SYSTEM_MONOSPACE -> FontFamily.Monospace
            ReadingFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
        }
        val weight = when (safe.fontWeightLevel) {
            0 -> FontWeight.Normal
            2 -> FontWeight.Bold
            else -> FontWeight.Medium
        }
        val (bg, txt) = when (safe.paperStyle) {
            ReadingPaperStyle.FOLLOW_THEME -> Pair(themeSurface, themeOnSurface)
            ReadingPaperStyle.PAPER_CREAM -> Pair(Color(0xFFF6F4EE), Color(0xFF232624))
            ReadingPaperStyle.PAPER_SILK -> Pair(Color(0xFFEDE9DF), Color(0xFF232624))
            ReadingPaperStyle.PAPER_WHITE -> Pair(Color(0xFFFAFAFA), Color(0xFF202020))
            ReadingPaperStyle.NIGHT_INK -> Pair(Color(0xFF161817), Color(0xFFE4E1DA))
        }

        return PoemLayoutConfig(
            bodyFontSize = safe.bodyFontSizeSp.sp,
            bodyLineHeight = (safe.bodyFontSizeSp * safe.lineHeightMultiplier).sp,
            letterSpacing = safe.letterSpacingSp.sp,
            fontFamily = family,
            fontWeight = weight,
            horizontalPadding = safe.horizontalPaddingDp.dp,
            lineSpacing = safe.lineSpacingDp.dp,
            coupletSpacing = safe.coupletSpacingDp.dp,
            stanzaSpacing = safe.stanzaSpacingDp.dp,
            paragraphSpacing = safe.paragraphSpacingDp.dp,
            firstLineIndentSpaces = safe.firstLineIndentSpaces,
            backgroundColor = bg,
            textColor = txt,
        )
    }

    /**
     * 序列化为 JSON 字符串
     */
    fun toJson(): String {
        val safe = coerced()
        val obj = JSONObject()
        obj.put("fontType", safe.fontType.id)
        obj.put("fontWeightLevel", safe.fontWeightLevel)
        obj.put("bodyFontSizeSp", safe.bodyFontSizeSp.toDouble())
        obj.put("letterSpacingSp", safe.letterSpacingSp.toDouble())
        obj.put("lineHeightMultiplier", safe.lineHeightMultiplier.toDouble())
        obj.put("horizontalPaddingDp", safe.horizontalPaddingDp.toDouble())
        obj.put("lineSpacingDp", safe.lineSpacingDp.toDouble())
        obj.put("coupletSpacingDp", safe.coupletSpacingDp.toDouble())
        obj.put("stanzaSpacingDp", safe.stanzaSpacingDp.toDouble())
        obj.put("paragraphSpacingDp", safe.paragraphSpacingDp.toDouble())
        obj.put("firstLineIndentSpaces", safe.firstLineIndentSpaces)
        obj.put("paperStyle", safe.paperStyle.id)
        return obj.toString()
    }

    companion object {
        val Default = ReadingPreferences()

        /**
         * 从 JSON 反序列化，严格容错：字段缺失或非法时回退到默认常量
         */
        fun fromJson(json: String?): ReadingPreferences {
            if (json.isNullOrBlank()) return Default
            return try {
                val obj = JSONObject(json)
                ReadingPreferences(
                    fontType = ReadingFontFamily.fromId(obj.optString("fontType", Default.fontType.id)),
                    fontWeightLevel = obj.optInt("fontWeightLevel", Default.fontWeightLevel),
                    bodyFontSizeSp = obj.optDouble("bodyFontSizeSp", Default.bodyFontSizeSp.toDouble()).toFloat(),
                    letterSpacingSp = obj.optDouble("letterSpacingSp", Default.letterSpacingSp.toDouble()).toFloat(),
                    lineHeightMultiplier = obj.optDouble("lineHeightMultiplier", Default.lineHeightMultiplier.toDouble()).toFloat(),
                    horizontalPaddingDp = obj.optDouble("horizontalPaddingDp", Default.horizontalPaddingDp.toDouble()).toFloat(),
                    lineSpacingDp = obj.optDouble("lineSpacingDp", Default.lineSpacingDp.toDouble()).toFloat(),
                    coupletSpacingDp = obj.optDouble("coupletSpacingDp", Default.coupletSpacingDp.toDouble()).toFloat(),
                    stanzaSpacingDp = obj.optDouble("stanzaSpacingDp", Default.stanzaSpacingDp.toDouble()).toFloat(),
                    paragraphSpacingDp = obj.optDouble("paragraphSpacingDp", Default.paragraphSpacingDp.toDouble()).toFloat(),
                    firstLineIndentSpaces = obj.optInt("firstLineIndentSpaces", Default.firstLineIndentSpaces),
                    paperStyle = ReadingPaperStyle.fromId(obj.optString("paperStyle", Default.paperStyle.id)),
                ).coerced()
            } catch (_: Exception) {
                Default
            }
        }
    }
}
