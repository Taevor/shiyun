package com.shi.yun.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shi.yun.data.model.ReadingFontFamily
import com.shi.yun.data.model.ReadingPaperStyle
import com.shi.yun.data.model.ReadingPreferences
import com.shi.yun.data.repository.LocalReadingPreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段五 B：阅读偏好持久化与 JSON 序列化 Android 设备插桩测试
 */
@RunWith(AndroidJUnit4::class)
class ReadingPreferencesInstrumentedTest {

    private lateinit var context: Context
    private val testPrefsName = "test_shiyun_reading_preferences"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val original = ReadingPreferences(
            fontType = ReadingFontFamily.SYSTEM_SERIF,
            fontWeightLevel = 2,
            bodyFontSizeSp = 28f,
            letterSpacingSp = 1.5f,
            lineHeightMultiplier = 1.9f,
            horizontalPaddingDp = 30f,
            lineSpacingDp = 12f,
            coupletSpacingDp = 20f,
            stanzaSpacingDp = 22f,
            paragraphSpacingDp = 24f,
            firstLineIndentSpaces = 0,
            paperStyle = ReadingPaperStyle.NIGHT_INK,
            customFontId = "custom_test_font_01",
            customFontName = "思源宋体",
        )

        val json = original.toJson()
        val restored = ReadingPreferences.fromJson(json)

        assertEquals(original.fontType, restored.fontType)
        assertEquals(original.fontWeightLevel, restored.fontWeightLevel)
        assertEquals(original.bodyFontSizeSp, restored.bodyFontSizeSp, 0.001f)
        assertEquals(original.letterSpacingSp, restored.letterSpacingSp, 0.001f)
        assertEquals(original.lineHeightMultiplier, restored.lineHeightMultiplier, 0.001f)
        assertEquals(original.horizontalPaddingDp, restored.horizontalPaddingDp, 0.001f)
        assertEquals(original.lineSpacingDp, restored.lineSpacingDp, 0.001f)
        assertEquals(original.coupletSpacingDp, restored.coupletSpacingDp, 0.001f)
        assertEquals(original.stanzaSpacingDp, restored.stanzaSpacingDp, 0.001f)
        assertEquals(original.paragraphSpacingDp, restored.paragraphSpacingDp, 0.001f)
        assertEquals(original.firstLineIndentSpaces, restored.firstLineIndentSpaces)
        assertEquals(original.paperStyle, restored.paperStyle)
        assertEquals("custom_test_font_01", restored.customFontId)
        assertEquals("思源宋体", restored.customFontName)
    }

    @Test
    fun testRepositorySaveAndRestoreAcrossInstances() = runBlocking {
        val repo1 = LocalReadingPreferencesRepository(context = context, prefsName = testPrefsName)
        val customPrefs = ReadingPreferences(
            fontType = ReadingFontFamily.SYSTEM_MONOSPACE,
            fontWeightLevel = 0,
            bodyFontSizeSp = 25f,
            letterSpacingSp = 0.8f,
            lineHeightMultiplier = 1.75f,
            horizontalPaddingDp = 20f,
            lineSpacingDp = 9f,
            coupletSpacingDp = 18f,
            stanzaSpacingDp = 20f,
            paragraphSpacingDp = 22f,
            firstLineIndentSpaces = 2,
            paperStyle = ReadingPaperStyle.FOLLOW_THEME,
        )

        repo1.updatePreferences(customPrefs)
        assertEquals(customPrefs, repo1.preferencesFlow.value)

        // 创建新实例，模拟应用进程重启冷启动
        val repo2 = LocalReadingPreferencesRepository(context = context, prefsName = testPrefsName)
        val restored = repo2.preferencesFlow.value

        assertEquals(customPrefs.fontType, restored.fontType)
        assertEquals(customPrefs.fontWeightLevel, restored.fontWeightLevel)
        assertEquals(customPrefs.bodyFontSizeSp, restored.bodyFontSizeSp, 0.001f)
        assertEquals(customPrefs.letterSpacingSp, restored.letterSpacingSp, 0.001f)
        assertEquals(customPrefs.lineHeightMultiplier, restored.lineHeightMultiplier, 0.001f)
        assertEquals(customPrefs.horizontalPaddingDp, restored.horizontalPaddingDp, 0.001f)
        assertEquals(customPrefs.lineSpacingDp, restored.lineSpacingDp, 0.001f)
        assertEquals(customPrefs.coupletSpacingDp, restored.coupletSpacingDp, 0.001f)
        assertEquals(customPrefs.stanzaSpacingDp, restored.stanzaSpacingDp, 0.001f)
        assertEquals(customPrefs.paragraphSpacingDp, restored.paragraphSpacingDp, 0.001f)
        assertEquals(customPrefs.firstLineIndentSpaces, restored.firstLineIndentSpaces)
        assertEquals(customPrefs.paperStyle, restored.paperStyle)
    }

    @Test
    fun testRepositoryResetToDefault() = runBlocking {
        val repo = LocalReadingPreferencesRepository(context = context, prefsName = testPrefsName)
        repo.updatePreferences(ReadingPreferences(bodyFontSizeSp = 30f, fontWeightLevel = 2))

        val reset = repo.resetToDefault()
        assertEquals(ReadingPreferences.Default, reset)
        assertEquals(ReadingPreferences.Default, repo.preferencesFlow.value)

        // 重建实例验证持久化结果也是默认值
        val repoReborn = LocalReadingPreferencesRepository(context = context, prefsName = testPrefsName)
        assertEquals(ReadingPreferences.Default.bodyFontSizeSp, repoReborn.preferencesFlow.value.bodyFontSizeSp, 0.001f)
    }

    @Test
    fun testCorruptedJsonFallback() {
        // 传入非法损坏的 JSON 格式
        val corruptedJson = "{ this is not a valid json !!! }"
        val fallback = ReadingPreferences.fromJson(corruptedJson)
        assertEquals(ReadingPreferences.Default, fallback)

        // 传入空字符串
        val emptyFallback = ReadingPreferences.fromJson("")
        assertEquals(ReadingPreferences.Default, emptyFallback)

        // 传入 null
        val nullFallback = ReadingPreferences.fromJson(null)
        assertEquals(ReadingPreferences.Default, nullFallback)
    }

    @Test
    fun testHomeGestureHintStatePersistence() = runBlocking {
        val repo = LocalReadingPreferencesRepository(context = context, prefsName = testPrefsName)
        // 初始状态应为 false
        org.junit.Assert.assertFalse(repo.isHomeGestureHintShown())

        // 标记为已展示
        repo.markHomeGestureHintShown()
        org.junit.Assert.assertTrue(repo.isHomeGestureHintShown())

        // 模拟重启新实例，状态必须持久保存
        val repoRestart = LocalReadingPreferencesRepository(context = context, prefsName = testPrefsName)
        org.junit.Assert.assertTrue(repoRestart.isHomeGestureHintShown())
    }
}
