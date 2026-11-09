package com.shi.yun.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shi.yun.data.repository.LocalThemePreferencesRepository
import com.shi.yun.ui.theme.AppThemeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段 B：全局主题持久化与响应式流 Android 设备插桩测试
 */
@RunWith(AndroidJUnit4::class)
class ThemePreferencesInstrumentedTest {

    private lateinit var context: Context
    private val testPrefsName = "test_shiyun_theme_preferences"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testInitialDefaultThemeIsXuanPaper() {
        val repo = LocalThemePreferencesRepository(
            context = context,
            prefsName = testPrefsName,
        )

        assertEquals(AppThemeId.XUAN_PAPER, repo.getCurrentThemeId())
        assertEquals(AppThemeId.XUAN_PAPER, repo.currentThemeIdFlow.value)
    }

    @Test
    fun testUpdateThemeAndPersistence() = runBlocking {
        val repo1 = LocalThemePreferencesRepository(
            context = context,
            prefsName = testPrefsName,
        )

        // 切换到竹青雅韵
        repo1.updateThemeId(AppThemeId.BAMBOO_GREEN)
        assertEquals(AppThemeId.BAMBOO_GREEN, repo1.getCurrentThemeId())
        assertEquals(AppThemeId.BAMBOO_GREEN, repo1.currentThemeIdFlow.value)

        // 模拟重启应用，重新创建仓库实例读取
        val repo2 = LocalThemePreferencesRepository(
            context = context,
            prefsName = testPrefsName,
        )
        assertEquals(AppThemeId.BAMBOO_GREEN, repo2.getCurrentThemeId())
        assertEquals(AppThemeId.BAMBOO_GREEN, repo2.currentThemeIdFlow.value)
    }

    @Test
    fun testFallbackOnInvalidOrCorruptedThemeId() {
        // 直接在 SharedPreferences 中写入非法 ID
        context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("key_app_theme_id", "invalid_corrupted_theme_xyz")
            .commit()

        val repo = LocalThemePreferencesRepository(
            context = context,
            prefsName = testPrefsName,
        )

        // 验证安全回退至默认宣纸古籍
        assertEquals(AppThemeId.XUAN_PAPER, repo.getCurrentThemeId())
        assertEquals(AppThemeId.XUAN_PAPER, repo.currentThemeIdFlow.value)
    }

    @Test
    fun testMultipleSequentialThemeTransitions() = runBlocking {
        val repo = LocalThemePreferencesRepository(
            context = context,
            prefsName = testPrefsName,
        )

        val targetThemes = listOf(
            AppThemeId.NIGHT_INK,
            AppThemeId.SPRING_WILLOW,
            AppThemeId.INDIGO_MOUNTAIN,
            AppThemeId.AUTUMN_MAPLE,
            AppThemeId.XUAN_PAPER,
        )

        for (theme in targetThemes) {
            repo.updateThemeId(theme)
            assertEquals(theme, repo.getCurrentThemeId())
            assertEquals(theme, repo.currentThemeIdFlow.value)
        }

        // 验证持久化落盘正确
        val repoReopened = LocalThemePreferencesRepository(
            context = context,
            prefsName = testPrefsName,
        )
        assertEquals(AppThemeId.XUAN_PAPER, repoReopened.getCurrentThemeId())
    }
}
