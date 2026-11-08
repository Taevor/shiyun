package com.childe.shi.common.log

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 诗云应用运行日志系统 (AppLogger) Android 设备插桩测试
 *
 * 验证：
 * 1. 真实 Android Context 初始化下的日志文件创建与追加写入；
 * 2. 基于 Android MediaStore API 导出日志至公共 Download/诗云日志；
 * 3. 验证导出文件的 URI、名称格式、文件头部信息与合并内容完整性；
 * 4. 验证本地日志统计与清空功能在真实 Android 存储环境下的可靠性。
 */
@RunWith(AndroidJUnit4::class)
class AppLoggerInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        AppLogger.init(context)
    }

    @Test
    fun testAppLoggerWritingAndStatsOnDevice() {
        AppLogger.i("InstrumentedTest", "设备插桩日志写入测试项")
        AppLogger.flushForTesting()

        val stats = AppLogger.getLogStats()
        assertTrue("在设备上应存在有效日志目录", stats.directoryPath.contains("files/logs") || stats.directoryPath.contains("files\\logs"))
        assertTrue("设备上日志文件数应大于等于1", stats.fileCount >= 1)
        assertTrue("设备上日志总字节数应大于0", stats.totalSizeBytes > 0)
    }

    @Test
    fun testExportLogsToDownloadsOnDevice() {
        // 写入特征标识日志
        val uniqueTag = "ExportTest_${System.currentTimeMillis()}"
        AppLogger.i("InstrumentedTest", "准备导出日志: tag=$uniqueTag")
        AppLogger.flushForTesting()

        val exportResult = AppLogger.exportLogsToDownloads(context)
        assertTrue("导出日志应返回成功: ${exportResult.errorMessage}", exportResult.success)
        assertNotNull("导出文件的 MediaStore Uri 不能为空", exportResult.uri)
        assertTrue("导出文件名必须符合前缀规范", exportResult.fileName.startsWith("shiyun_log_"))
        assertTrue("导出文件名必须为 .txt 格式", exportResult.fileName.endsWith(".txt"))
        assertEquals("相对路径应为 Download/诗云日志", "Download/诗云日志", exportResult.relativePath)
        assertTrue("导出总字节数应大于0", exportResult.totalBytes > 0)

        // 通过 ContentResolver 读取导出的内容，验证文件头及日志正文完整性
        val uri = exportResult.uri!!
        val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: ""

        assertTrue("导出内容必须包含诗云应用运行日志头部", content.contains("诗云应用运行日志导出文件"))
        assertTrue("导出内容必须包含特征日志项", content.contains(uniqueTag))

        // 清理测试生成的导出文件，避免污染设备公共存储
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
        }
    }

    @Test
    fun testClearLogsOnDevice() {
        AppLogger.w("InstrumentedTest", "清理前待删除日志项")
        AppLogger.flushForTesting()

        val statsBefore = AppLogger.getLogStats()
        assertTrue("清理前存在日志", statsBefore.fileCount > 0)

        val cleared = AppLogger.clearLogs()
        assertTrue("清理本地日志成功", cleared)

        val statsAfter = AppLogger.getLogStats()
        assertEquals("清理后本地日志文件数为0", 0, statsAfter.fileCount)
        assertEquals("清理后本地日志总大小为0", 0L, statsAfter.totalSizeBytes)
    }
}
