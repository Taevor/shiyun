package com.shi.yun.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shi.yun.data.repository.CustomFontItem
import com.shi.yun.data.repository.LocalCustomFontRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 自定义字体管理仓储设备插桩测试
 */
@RunWith(AndroidJUnit4::class)
class CustomFontInstrumentedTest {

    private lateinit var context: Context
    private val testPrefsName = "test_custom_fonts_prefs"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE).edit().clear().commit()
        val fontsDir = File(context.filesDir, "fonts")
        if (fontsDir.exists()) {
            fontsDir.listFiles()?.forEach { it.delete() }
        }
    }

    @Test
    fun testCustomFontItemJsonSerialization() {
        val item = CustomFontItem(
            id = "font_123456",
            name = "霞鹜文楷",
            fileName = "font_123456.ttf",
            fileSize = 1048576L,
        )

        val json = item.toJson()
        val restored = CustomFontItem.fromJson(json)

        assertNotNull(restored)
        assertEquals(item.id, restored?.id)
        assertEquals(item.name, restored?.name)
        assertEquals(item.fileName, restored?.fileName)
        assertEquals(item.fileSize, restored?.fileSize)
    }

    @Test
    fun testInitialEmptyListAndMissingFontHandling() {
        val repo = LocalCustomFontRepository(context = context, prefsName = testPrefsName)
        val initialList = repo.getCustomFonts()
        assertTrue(initialList.isEmpty())

        // 查询不存在的字体应安全返回 null，不抛出异常
        assertNull(repo.getFontFile("non_existent_font"))
        assertNull(repo.getFontFamily("non_existent_font"))
    }

    @Test
    fun testDeleteFont() = runBlocking {
        val repo = LocalCustomFontRepository(context = context, prefsName = testPrefsName)
        // 删除不存在的字体应安全返回 false
        val deleted = repo.deleteFont("non_existent")
        assertFalse(deleted)
    }
}
