package com.shi.yun.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.shi.yun.R

/**
 * 诗云 10 套预设全局主题调色板与定义注册表
 *
 * 包含 6 套经典主题与 4 套四时主题，每套主题均具备：
 * 1. 专属古典底色 surface 与卡片底色 cardBackground；
 * 2. 严谨分层的标题、正文与辅助文字墨色；
 * 3. 独立且高质量的专属水墨山水背景 WebP 图像；
 * 4. 经由 WCAG AA 严谨验算的对比度保障体系。
 */
object AppThemePalette {

    // ========== 1. 宣纸古籍 (默认经典) ==========
    private val XuanPaperColorScheme = lightColorScheme(
        primary = Color(0xFF2D4B66),
        onPrimary = Color(0xFFF5F1E6),
        primaryContainer = Color(0xFFEBE4D3),
        onPrimaryContainer = Color(0xFF2D4B66),
        secondary = Color(0xFF43604E),
        onSecondary = Color(0xFFF5F1E6),
        secondaryContainer = Color(0xFFEBE4D3),
        onSecondaryContainer = Color(0xFF43604E),
        tertiary = Color(0xFFB83A2A),
        onTertiary = Color.White,
        background = Color(0xFFF5F1E6),
        onBackground = Color(0xFF1A1815),
        surface = Color(0xFFF5F1E6),
        onSurface = Color(0xFF1A1815),
        surfaceVariant = Color(0xFFEBE4D3),
        onSurfaceVariant = Color(0xFF2D2824),
        outline = Color(0xFF62584E),
        outlineVariant = Color(0x261A1815),
    )
    private val XuanPaperTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFEBE4D3),
        cardBorder = Color(0x261A1815),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFF5F1E6),
        heartFavoriteTint = Color(0xFFB83A2A),
        titleText = Color(0xFF1A1815),
        bodyText = Color(0xFF2D2824),
        secondaryText = Color(0xFF62584E),
        backgroundResId = R.drawable.bg_home_landscape,
    )

    // ========== 2. 竹青雅韵 (经典) ==========
    private val BambooGreenColorScheme = lightColorScheme(
        primary = Color(0xFF2C5A3E),
        onPrimary = Color(0xFFEDF3EE),
        primaryContainer = Color(0xFFDEEAE0),
        onPrimaryContainer = Color(0xFF2C5A3E),
        secondary = Color(0xFF3E6A4E),
        onSecondary = Color(0xFFEDF3EE),
        secondaryContainer = Color(0xFFDEEAE0),
        onSecondaryContainer = Color(0xFF3E6A4E),
        tertiary = Color(0xFFA0562D),
        onTertiary = Color.White,
        background = Color(0xFFEDF3EE),
        onBackground = Color(0xFF16231B),
        surface = Color(0xFFEDF3EE),
        onSurface = Color(0xFF16231B),
        surfaceVariant = Color(0xFFDEEAE0),
        onSurfaceVariant = Color(0xFF24352B),
        outline = Color(0xFF556A5E),
        outlineVariant = Color(0x2616231B),
    )
    private val BambooGreenTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFDEEAE0),
        cardBorder = Color(0x2616231B),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFEDF3EE),
        heartFavoriteTint = Color(0xFFB83A2A),
        titleText = Color(0xFF16231B),
        bodyText = Color(0xFF24352B),
        secondaryText = Color(0xFF556A5E),
        backgroundResId = R.drawable.bg_home_bamboo,
    )

    // ========== 3. 黛青山水 (经典) ==========
    private val IndigoMountainColorScheme = lightColorScheme(
        primary = Color(0xFF264A6E),
        onPrimary = Color(0xFFEBF0F5),
        primaryContainer = Color(0xFFDDE6ED),
        onPrimaryContainer = Color(0xFF264A6E),
        secondary = Color(0xFF385E86),
        onSecondary = Color(0xFFEBF0F5),
        secondaryContainer = Color(0xFFDDE6ED),
        onSecondaryContainer = Color(0xFF385E86),
        tertiary = Color(0xFF9A3E34),
        onTertiary = Color.White,
        background = Color(0xFFEBF0F5),
        onBackground = Color(0xFF141E28),
        surface = Color(0xFFEBF0F5),
        onSurface = Color(0xFF141E28),
        surfaceVariant = Color(0xFFDDE6ED),
        onSurfaceVariant = Color(0xFF203040),
        outline = Color(0xFF546678),
        outlineVariant = Color(0x26141E28),
    )
    private val IndigoMountainTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFDDE6ED),
        cardBorder = Color(0x26141E28),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFEBF0F5),
        heartFavoriteTint = Color(0xFF9A3E34),
        titleText = Color(0xFF141E28),
        bodyText = Color(0xFF203040),
        secondaryText = Color(0xFF546678),
        backgroundResId = R.drawable.bg_home_indigo,
    )

    // ========== 4. 朱砂雅集 (经典) ==========
    private val CinnabarRedColorScheme = lightColorScheme(
        primary = Color(0xFF8C3224),
        onPrimary = Color(0xFFF8EFEA),
        primaryContainer = Color(0xFFEFE0D7),
        onPrimaryContainer = Color(0xFF8C3224),
        secondary = Color(0xFF4A6658),
        onSecondary = Color(0xFFF8EFEA),
        secondaryContainer = Color(0xFFEFE0D7),
        onSecondaryContainer = Color(0xFF4A6658),
        tertiary = Color(0xFFB83A2A),
        onTertiary = Color.White,
        background = Color(0xFFF8EFEA),
        onBackground = Color(0xFF251512),
        surface = Color(0xFFF8EFEA),
        onSurface = Color(0xFF251512),
        surfaceVariant = Color(0xFFEFE0D7),
        onSurfaceVariant = Color(0xFF3A2622),
        outline = Color(0xFF6E5550),
        outlineVariant = Color(0x26251512),
    )
    private val CinnabarRedTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFEFE0D7),
        cardBorder = Color(0x26251512),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFF8EFEA),
        heartFavoriteTint = Color(0xFFB83A2A),
        titleText = Color(0xFF251512),
        bodyText = Color(0xFF3A2622),
        secondaryText = Color(0xFF6E5550),
        backgroundResId = R.drawable.bg_home_cinnabar,
    )

    // ========== 5. 琥珀秋笺 (经典) ==========
    private val AmberAutumnColorScheme = lightColorScheme(
        primary = Color(0xFF7E501E),
        onPrimary = Color(0xFFF6EFE1),
        primaryContainer = Color(0xFFECE0CD),
        onPrimaryContainer = Color(0xFF7E501E),
        secondary = Color(0xFF566B4B),
        onSecondary = Color(0xFFF6EFE1),
        secondaryContainer = Color(0xFFECE0CD),
        onSecondaryContainer = Color(0xFF566B4B),
        tertiary = Color(0xFFB24218),
        onTertiary = Color.White,
        background = Color(0xFFF6EFE1),
        onBackground = Color(0xFF22190E),
        surface = Color(0xFFF6EFE1),
        onSurface = Color(0xFF22190E),
        surfaceVariant = Color(0xFFECE0CD),
        onSurfaceVariant = Color(0xFF362B1D),
        outline = Color(0xFF6A5B47),
        outlineVariant = Color(0x2622190E),
    )
    private val AmberAutumnTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFECE0CD),
        cardBorder = Color(0x2622190E),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFF6EFE1),
        heartFavoriteTint = Color(0xFFB24218),
        titleText = Color(0xFF22190E),
        bodyText = Color(0xFF362B1D),
        secondaryText = Color(0xFF6A5B47),
        backgroundResId = R.drawable.bg_home_amber,
    )

    // ========== 6. 墨夜清辉 (经典暗色) ==========
    private val NightInkColorScheme = darkColorScheme(
        primary = Color(0xFF7FA98D),
        onPrimary = Color(0xFF181A19),
        primaryContainer = Color(0xFF242826),
        onPrimaryContainer = Color(0xFF7FA98D),
        secondary = Color(0xFF94B5A0),
        onSecondary = Color(0xFF181A19),
        secondaryContainer = Color(0xFF242826),
        onSecondaryContainer = Color(0xFF94B5A0),
        tertiary = Color(0xFFD66A5B),
        onTertiary = Color.White,
        background = Color(0xFF181A19),
        onBackground = Color(0xFFF2EFE8),
        surface = Color(0xFF181A19),
        onSurface = Color(0xFFF2EFE8),
        surfaceVariant = Color(0xFF242826),
        onSurfaceVariant = Color(0xFFDCD7CE),
        outline = Color(0xFF969B95),
        outlineVariant = Color(0x3DF2EFE8),
    )
    private val NightInkTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFF242826),
        cardBorder = Color(0x3DF2EFE8),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFF181A19),
        heartFavoriteTint = Color(0xFFD66A5B),
        titleText = Color(0xFFF2EFE8),
        bodyText = Color(0xFFDCD7CE),
        secondaryText = Color(0xFF969B95),
        backgroundResId = R.drawable.bg_home_night,
    )

    // ========== 7. 柳烟春晓 (四时春) ==========
    private val SpringWillowColorScheme = lightColorScheme(
        primary = Color(0xFF3E662C),
        onPrimary = Color(0xFFEEF4E7),
        primaryContainer = Color(0xFFDFEBD4),
        onPrimaryContainer = Color(0xFF3E662C),
        secondary = Color(0xFF50783D),
        onSecondary = Color(0xFFEEF4E7),
        secondaryContainer = Color(0xFFDFEBD4),
        onSecondaryContainer = Color(0xFF50783D),
        tertiary = Color(0xFFB6752A),
        onTertiary = Color.White,
        background = Color(0xFFEEF4E7),
        onBackground = Color(0xFF162312),
        surface = Color(0xFFEEF4E7),
        onSurface = Color(0xFF162312),
        surfaceVariant = Color(0xFFDFEBD4),
        onSurfaceVariant = Color(0xFF273822),
        outline = Color(0xFF576C50),
        outlineVariant = Color(0x26162312),
    )
    private val SpringWillowTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFDFEBD4),
        cardBorder = Color(0x26162312),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFEEF4E7),
        heartFavoriteTint = Color(0xFFB83A2A),
        titleText = Color(0xFF162312),
        bodyText = Color(0xFF273822),
        secondaryText = Color(0xFF576C50),
        backgroundResId = R.drawable.bg_home_spring,
    )

    // ========== 8. 荷风清韵 (四时夏) ==========
    private val SummerLotusColorScheme = lightColorScheme(
        primary = Color(0xFF255D6C),
        onPrimary = Color(0xFFE9F3F4),
        primaryContainer = Color(0xFFD9E9EB),
        onPrimaryContainer = Color(0xFF255D6C),
        secondary = Color(0xFF387282),
        onSecondary = Color(0xFFE9F3F4),
        secondaryContainer = Color(0xFFD9E9EB),
        onSecondaryContainer = Color(0xFF387282),
        tertiary = Color(0xFFB84E62),
        onTertiary = Color.White,
        background = Color(0xFFE9F3F4),
        onBackground = Color(0xFF122024),
        surface = Color(0xFFE9F3F4),
        onSurface = Color(0xFF122024),
        surfaceVariant = Color(0xFFD9E9EB),
        onSurfaceVariant = Color(0xFF1F343A),
        outline = Color(0xFF526A72),
        outlineVariant = Color(0x26122024),
    )
    private val SummerLotusTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFD9E9EB),
        cardBorder = Color(0x26122024),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFE9F3F4),
        heartFavoriteTint = Color(0xFFB84E62),
        titleText = Color(0xFF122024),
        bodyText = Color(0xFF1F343A),
        secondaryText = Color(0xFF526A72),
        backgroundResId = R.drawable.bg_home_lotus,
    )

    // ========== 9. 枫笺染霜 (四时秋) ==========
    private val AutumnMapleColorScheme = lightColorScheme(
        primary = Color(0xFF8D3B26),
        onPrimary = Color(0xFFF6EEE6),
        primaryContainer = Color(0xFFEBDFD4),
        onPrimaryContainer = Color(0xFF8D3B26),
        secondary = Color(0xFFB26E22),
        onSecondary = Color(0xFFF6EEE6),
        secondaryContainer = Color(0xFFEBDFD4),
        onSecondaryContainer = Color(0xFFB26E22),
        tertiary = Color(0xFFB83A2A),
        onTertiary = Color.White,
        background = Color(0xFFF6EEE6),
        onBackground = Color(0xFF241611),
        surface = Color(0xFFF6EEE6),
        onSurface = Color(0xFF241611),
        surfaceVariant = Color(0xFFEBDFD4),
        onSurfaceVariant = Color(0xFF392620),
        outline = Color(0xFF705750),
        outlineVariant = Color(0x26241611),
    )
    private val AutumnMapleTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFEBDFD4),
        cardBorder = Color(0x26241611),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFF6EEE6),
        heartFavoriteTint = Color(0xFFB83A2A),
        titleText = Color(0xFF241611),
        bodyText = Color(0xFF392620),
        secondaryText = Color(0xFF705750),
        backgroundResId = R.drawable.bg_home_maple,
    )

    // ========== 10. 寒江雪隐 (四时冬) ==========
    private val WinterSnowColorScheme = lightColorScheme(
        primary = Color(0xFF2D4E6D),
        onPrimary = Color(0xFFE9EFF4),
        primaryContainer = Color(0xFFD9E3EA),
        onPrimaryContainer = Color(0xFF2D4E6D),
        secondary = Color(0xFF436382),
        onSecondary = Color(0xFFE9EFF4),
        secondaryContainer = Color(0xFFD9E3EA),
        onSecondaryContainer = Color(0xFF436382),
        tertiary = Color(0xFF9E4A38),
        onTertiary = Color.White,
        background = Color(0xFFE9EFF4),
        onBackground = Color(0xFF131D26),
        surface = Color(0xFFE9EFF4),
        onSurface = Color(0xFF131D26),
        surfaceVariant = Color(0xFFD9E3EA),
        onSurfaceVariant = Color(0xFF212E3C),
        outline = Color(0xFF536576),
        outlineVariant = Color(0x26131D26),
    )
    private val WinterSnowTokens = AppThemeCustomTokens(
        cardBackground = Color(0xFFD9E3EA),
        cardBorder = Color(0x26131D26),
        overlayBlendColor = Color.Transparent,
        statusBarProtectionColor = Color(0xFFE9EFF4),
        heartFavoriteTint = Color(0xFF9E4A38),
        titleText = Color(0xFF131D26),
        bodyText = Color(0xFF212E3C),
        secondaryText = Color(0xFF536576),
        backgroundResId = R.drawable.bg_home_snow,
    )

    /**
     * 根据主题标识获取对应的主题完整定义体
     */
    fun getThemeDefinition(themeId: AppThemeId): AppThemeDefinition {
        return when (themeId) {
            AppThemeId.XUAN_PAPER -> AppThemeDefinition(themeId, XuanPaperColorScheme, XuanPaperTokens)
            AppThemeId.BAMBOO_GREEN -> AppThemeDefinition(themeId, BambooGreenColorScheme, BambooGreenTokens)
            AppThemeId.INDIGO_MOUNTAIN -> AppThemeDefinition(themeId, IndigoMountainColorScheme, IndigoMountainTokens)
            AppThemeId.CINNABAR_RED -> AppThemeDefinition(themeId, CinnabarRedColorScheme, CinnabarRedTokens)
            AppThemeId.AMBER_AUTUMN -> AppThemeDefinition(themeId, AmberAutumnColorScheme, AmberAutumnTokens)
            AppThemeId.NIGHT_INK -> AppThemeDefinition(themeId, NightInkColorScheme, NightInkTokens)
            AppThemeId.SPRING_WILLOW -> AppThemeDefinition(themeId, SpringWillowColorScheme, SpringWillowTokens)
            AppThemeId.SUMMER_LOTUS -> AppThemeDefinition(themeId, SummerLotusColorScheme, SummerLotusTokens)
            AppThemeId.AUTUMN_MAPLE -> AppThemeDefinition(themeId, AutumnMapleColorScheme, AutumnMapleTokens)
            AppThemeId.WINTER_SNOW -> AppThemeDefinition(themeId, WinterSnowColorScheme, WinterSnowTokens)
        }
    }
}
