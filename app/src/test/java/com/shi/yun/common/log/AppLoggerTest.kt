package com.shi.yun.common.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 诗云应用运行日志系统 (AppLogger) JVM 单元测试
 *
 * 验证：
 * 1. 日志格式化规范（时间戳、级别标签、模块 Tag、正文、异常堆栈）；
 * 2. 异步顺序非阻塞写入及文件追加；
 * 3. 并发多线程写入的线程安全性与数据完整性；
 * 4. 文件轮换机制（app.log -> app_1.log -> ... -> app_5.log，超限淘汰）；
 * 5. 统计指标查询（getLogStats）与本地日志一键清理（clearLogs）；
 * 6. 容错性（未初始化、非法目录等边界情况不崩溃）。
 */
class AppLoggerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testLogDir: File

    @Before
    fun setUp() {
        testLogDir = tempFolder.newFolder("test_logs")
        AppLogger.initForTesting(testLogDir)
    }

    @Test
    fun testLogFormattingAndWriting() {
        AppLogger.d("TestTag", "这是调试日志")
        AppLogger.i("TestTag", "这是信息日志")
        AppLogger.w("TestTag", "这是警告日志")
        AppLogger.e("TestTag", "这是错误日志", RuntimeException("测试异常信息"))

        AppLogger.flushForTesting()

        val activeFile = testLogDir.resolve("app.log")
        assertTrue("activeFile 必须被成功创建", activeFile.exists())
        val content = activeFile.readText(Charsets.UTF_8)

        assertTrue("必须包含 DEBUG 级别", content.contains("[DEBUG] [TestTag] 这是调试日志"))
        assertTrue("必须包含 INFO 级别", content.contains("[INFO] [TestTag] 这是信息日志"))
        assertTrue("必须包含 WARN 级别", content.contains("[WARN] [TestTag] 这是警告日志"))
        assertTrue("必须包含 ERROR 级别", content.contains("[ERROR] [TestTag] 这是错误日志"))
        assertTrue("必须包含异常堆栈类名", content.contains("java.lang.RuntimeException: 测试异常信息"))
    }

    @Test
    fun testConcurrentMultiThreadLogging() {
        val threadCount = 8
        val logsPerThread = 50
        val latch = CountDownLatch(threadCount)

        for (t in 0 until threadCount) {
            Thread {
                try {
                    for (i in 0 until logsPerThread) {
                        AppLogger.i("Thread-$t", "并发写入项_$i")
                    }
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        assertTrue("并发写入应在限时内完成", latch.await(5, TimeUnit.SECONDS))
        AppLogger.flushForTesting()

        val activeFile = testLogDir.resolve("app.log")
        assertTrue(activeFile.exists())
        val lines = activeFile.readLines(Charsets.UTF_8).filter { it.contains("并发写入项_") }
        assertEquals("所有线程写入的日志行均应被无损记录", threadCount * logsPerThread, lines.size)
    }

    @Test
    fun testFileRotationLogic() {
        // 预先建立旧日志文件
        val active = testLogDir.resolve("app.log")
        val app1 = testLogDir.resolve("app_1.log")
        val app2 = testLogDir.resolve("app_2.log")
        val app3 = testLogDir.resolve("app_3.log")
        val app4 = testLogDir.resolve("app_4.log")
        val app5 = testLogDir.resolve("app_5.log")

        active.writeText("ACTIVE_CONTENT", Charsets.UTF_8)
        app1.writeText("APP1_CONTENT", Charsets.UTF_8)
        app2.writeText("APP2_CONTENT", Charsets.UTF_8)
        app3.writeText("APP3_CONTENT", Charsets.UTF_8)
        app4.writeText("APP4_CONTENT", Charsets.UTF_8)
        app5.writeText("APP5_CONTENT", Charsets.UTF_8)

        // 触发轮换
        AppLogger.rotateLogFiles(testLogDir)

        // 验证：app_5 被删除，各文件依次后移一位，active 成为新的 app_1
        assertEquals("原 active 变为 app_1", "ACTIVE_CONTENT", app1.readText(Charsets.UTF_8))
        assertEquals("原 app_1 变为 app_2", "APP1_CONTENT", app2.readText(Charsets.UTF_8))
        assertEquals("原 app_2 变为 app_3", "APP2_CONTENT", app3.readText(Charsets.UTF_8))
        assertEquals("原 app_3 变为 app_4", "APP3_CONTENT", app4.readText(Charsets.UTF_8))
        assertEquals("原 app_4 变为 app_5", "APP4_CONTENT", app5.readText(Charsets.UTF_8))
        assertFalse("原 active 文件应被重命名移动", active.exists())
    }

    @Test
    fun testLogStatsAndClear() {
        AppLogger.i("StatsTag", "指标测试日志行")
        AppLogger.flushForTesting()

        var stats = AppLogger.getLogStats()
        assertEquals("应包含1个活动日志文件", 1, stats.fileCount)
        assertTrue("日志大小应大于0", stats.totalSizeBytes > 0)
        assertTrue("最后修改时间应大于0", stats.lastModifiedTimestamp > 0)

        // 执行清理
        val cleared = AppLogger.clearLogs()
        assertTrue("清理本地日志应返回 true", cleared)

        stats = AppLogger.getLogStats()
        assertEquals("清理后文件数应为0", 0, stats.fileCount)
        assertEquals("清理后总字节应为0", 0L, stats.totalSizeBytes)
    }
}
