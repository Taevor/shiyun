package com.shi.yun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shi.yun.common.log.AppLogger
import com.shi.yun.data.model.ReadingFontFamily
import com.shi.yun.data.repository.CustomFontRepository
import com.shi.yun.data.repository.LocalCustomFontRepository
import com.shi.yun.data.repository.LocalJsonWorkRepository
import com.shi.yun.data.repository.LocalReadingPreferencesRepository
import com.shi.yun.data.repository.LocalThemePreferencesRepository
import com.shi.yun.data.repository.ReadingPreferencesRepository
import com.shi.yun.data.repository.ThemePreferencesRepository
import com.shi.yun.data.repository.WorkRepository
import com.shi.yun.ui.ShiApp
import com.shi.yun.ui.theme.诗云Theme

/**
 * 诗云应用主入口 Activity
 *
 * 遵循极简职责划分：
 * 1. 负责窗口与 Edge-to-Edge 全屏沉浸式配置；
 * 2. 唯一定义并持有本地数据仓库、主题仓储、排版偏好仓储与自定义字体仓储单例；
 * 3. 在顶层响应式监听全局主题与用户字体，驱动 [诗云Theme] 与 [ShiApp]，切换后即时刷新，无需重启应用。
 */
class MainActivity : ComponentActivity() {

    private val workRepository: WorkRepository by lazy {
        LocalJsonWorkRepository(applicationContext)
    }

    private val themeRepository: ThemePreferencesRepository by lazy {
        LocalThemePreferencesRepository(applicationContext)
    }

    private val readingPreferencesRepository: ReadingPreferencesRepository by lazy {
        LocalReadingPreferencesRepository(applicationContext)
    }

    private val customFontRepository: CustomFontRepository by lazy {
        LocalCustomFontRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化应用日志系统
        AppLogger.init(applicationContext)
        AppLogger.i("MainActivity", "应用主 Activity 启动 (onCreate)")

        enableEdgeToEdge()
        setContent {
            val currentThemeId by themeRepository.currentThemeIdFlow.collectAsStateWithLifecycle()
            val readingPrefs by readingPreferencesRepository.preferencesFlow.collectAsStateWithLifecycle()

            val activeFontFamily = remember(readingPrefs.fontType, readingPrefs.customFontId) {
                val customId = readingPrefs.customFontId
                if (!customId.isNullOrBlank()) {
                    customFontRepository.getFontFamily(customId) ?: when (readingPrefs.fontType) {
                        ReadingFontFamily.SYSTEM_SERIF -> FontFamily.Serif
                        ReadingFontFamily.SYSTEM_MONOSPACE -> FontFamily.Monospace
                        ReadingFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
                    }
                } else {
                    when (readingPrefs.fontType) {
                        ReadingFontFamily.SYSTEM_SERIF -> FontFamily.Serif
                        ReadingFontFamily.SYSTEM_MONOSPACE -> FontFamily.Monospace
                        ReadingFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
                    }
                }
            }

            诗云Theme(
                themeId = currentThemeId,
                fontFamily = activeFontFamily,
            ) {
                ShiApp(
                    repository = workRepository,
                    preferencesRepository = readingPreferencesRepository,
                    themeRepository = themeRepository,
                    customFontRepository = customFontRepository,
                )
            }
        }
    }
}
