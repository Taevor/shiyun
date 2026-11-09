package com.shi.yun.data.repository

import android.content.Context
import com.shi.yun.common.log.AppLogger
import com.shi.yun.ui.theme.AppThemeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 全局主题偏好持久化仓储接口
 */
interface ThemePreferencesRepository {
    val currentThemeIdFlow: StateFlow<AppThemeId>
    suspend fun updateThemeId(themeId: AppThemeId)
    fun getCurrentThemeId(): AppThemeId
}

/**
 * 基于 SharedPreferences 的轻量级全局主题持久化实现
 *
 * 遵循原则：
 * 1. 仅存储纯净主题 ID 字符串（如 "xuan_paper"），不存储臃肿颜色值；
 * 2. 首次启动或无配置时默认返回 [AppThemeId.Default]（宣纸古籍）；
 * 3. 面对非法或未知 ID 时自动安全回退至默认主题；
 * 4. 线程安全，通过 StateFlow 响应式分发变更。
 */
class LocalThemePreferencesRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val prefsName: String = "shiyun_theme_preferences",
) : ThemePreferencesRepository {

    private val lock = Any()
    private val prefs by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private val _currentThemeIdFlow = MutableStateFlow(loadThemeIdInternal())
    override val currentThemeIdFlow: StateFlow<AppThemeId> = _currentThemeIdFlow.asStateFlow()

    override fun getCurrentThemeId(): AppThemeId {
        return _currentThemeIdFlow.value
    }

    override suspend fun updateThemeId(themeId: AppThemeId) {
        _currentThemeIdFlow.value = themeId
        withContext(ioDispatcher) {
            saveThemeIdInternal(themeId)
        }
    }

    private fun loadThemeIdInternal(): AppThemeId {
        return try {
            val rawId = prefs.getString(KEY_THEME_ID, null)
            val theme = AppThemeId.fromId(rawId)
            AppLogger.i("ThemePreferences", "加载已保存主题: ${theme.displayName} (${theme.id})")
            theme
        } catch (e: Exception) {
            AppLogger.w("ThemePreferences", "读取保存主题失败，已自动回退默认: ${e.message}")
            AppThemeId.Default
        }
    }

    private fun saveThemeIdInternal(themeId: AppThemeId) {
        synchronized(lock) {
            try {
                prefs.edit().putString(KEY_THEME_ID, themeId.id).apply()
                AppLogger.d("ThemePreferences", "全局主题已成功持久化落盘: ${themeId.id}")
            } catch (e: Exception) {
                AppLogger.e("ThemePreferences", "保存全局主题失败: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val KEY_THEME_ID = "key_app_theme_id"
    }
}
