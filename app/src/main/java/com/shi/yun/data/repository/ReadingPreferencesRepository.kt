package com.shi.yun.data.repository

import android.content.Context
import com.shi.yun.common.log.AppLogger
import com.shi.yun.data.model.ReadingPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 阅读偏好持久化仓储接口
 */
interface ReadingPreferencesRepository {
    val preferencesFlow: StateFlow<ReadingPreferences>
    suspend fun updatePreferences(preferences: ReadingPreferences)
    suspend fun resetToDefault(): ReadingPreferences
    fun isHomeGestureHintShown(): Boolean
    suspend fun markHomeGestureHintShown()
}

/**
 * 基于 SharedPreferences 的轻量级阅读偏好持久化实现
 */
class LocalReadingPreferencesRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val prefsName: String = "shiyun_reading_preferences",
) : ReadingPreferencesRepository {

    private val lock = Any()
    private val prefs by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private val _preferencesFlow = MutableStateFlow(loadPreferencesInternal())
    override val preferencesFlow: StateFlow<ReadingPreferences> = _preferencesFlow.asStateFlow()

    override suspend fun updatePreferences(preferences: ReadingPreferences) {
        val safe = preferences.coerced()
        _preferencesFlow.value = safe
        withContext(ioDispatcher) {
            savePreferencesInternal(safe)
        }
    }

    override suspend fun resetToDefault(): ReadingPreferences {
        val default = ReadingPreferences.Default
        _preferencesFlow.value = default
        withContext(ioDispatcher) {
            savePreferencesInternal(default)
        }
        AppLogger.i("ReadingPreferences", "阅读偏好已重置为出厂默认设置")
        return default
    }

    override fun isHomeGestureHintShown(): Boolean {
        return prefs.getBoolean(KEY_HOME_GESTURE_HINT_SHOWN, false)
    }

    override suspend fun markHomeGestureHintShown() {
        withContext(ioDispatcher) {
            synchronized(lock) {
                prefs.edit().putBoolean(KEY_HOME_GESTURE_HINT_SHOWN, true).apply()
                AppLogger.d("ReadingPreferences", "首页手势提示状态已记录为已展示")
            }
        }
    }

    private fun loadPreferencesInternal(): ReadingPreferences {
        return try {
            val json = prefs.getString(KEY_PREFERENCES_JSON, null)
            if (json.isNullOrBlank()) {
                ReadingPreferences.Default
            } else {
                ReadingPreferences.fromJson(json)
            }
        } catch (e: Exception) {
            AppLogger.w("ReadingPreferences", "读取阅读偏好失败，已自动恢复默认: ${e.message}")
            ReadingPreferences.Default
        }
    }

    private fun savePreferencesInternal(preferences: ReadingPreferences) {
        synchronized(lock) {
            try {
                val json = preferences.toJson()
                prefs.edit().putString(KEY_PREFERENCES_JSON, json).apply()
                AppLogger.d("ReadingPreferences", "阅读偏好已成功持久化落盘")
            } catch (e: Exception) {
                AppLogger.e("ReadingPreferences", "保存阅读偏好失败: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val KEY_PREFERENCES_JSON = "key_reading_preferences_json"
        private const val KEY_HOME_GESTURE_HINT_SHOWN = "key_home_gesture_hint_shown"
    }
}
