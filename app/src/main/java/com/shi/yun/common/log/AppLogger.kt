package com.shi.yun.common.log

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 日志级别枚举
 */
enum class LogLevel(val priority: Int, val label: String) {
    DEBUG(Log.DEBUG, "DEBUG"),
    INFO(Log.INFO, "INFO"),
    WARN(Log.WARN, "WARN"),
    ERROR(Log.ERROR, "ERROR")
}

/**
 * 日志导出结果数据模型
 */
data class LogExportResult(
    val success: Boolean,
    val fileName: String = "",
    val relativePath: String = "",
    val totalBytes: Long = 0L,
    val uri: Uri? = null,
    val errorMessage: String? = null,
)

/**
 * 本地日志统计摘要
 */
data class LogStats(
    val fileCount: Int,
    val totalSizeBytes: Long,
    val directoryPath: String,
    val lastModifiedTimestamp: Long,
)

/**
 * 诗云统一日志基础设施 (AppLogger)
 *
 * 核心架构规范：
 * 1. 统一入口：全应用唯一日志写入入口，杜绝各页面随意创建日志文件；
 * 2. 毫秒级时间戳与级别分类：时间格式 yyyy-MM-dd HH:mm:ss.SSS，含模块标签与异常堆栈；
 * 3. 异步非阻塞与线程安全：单线程调度器顺序追加写入，绝不阻塞主线程，完全隔离写入并发；
 * 4. 容错与无损保护：所有磁盘 I/O 均受严格异常保护，日志系统自身出错绝不导致 App 崩溃；
 * 5. 文件轮换：单个日志文件上限 2 MB，最多保留 5 个轮换备份，超出自动归档与清理；
 * 6. 现代公共目录导出：基于 Android MediaStore API 导出至 Download/诗云日志，无需任何宽泛存储权限；
 * 7. 隐私合规：严禁记录完整用户输入或密码，仅记录长度、耗时与脱敏统计指标。
 */
object AppLogger {

    private const val LOG_SUBDIR = "logs"
    private const val ACTIVE_LOG_FILE = "app.log"
    private const val MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L // 单文件 2 MB
    private const val MAX_BACKUP_FILES = 5 // 最多保留 5 个备份文件

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)
    private val exportFileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)

    @Volatile
    private var logDir: File? = null

    @Volatile
    private var isInitialized = false

    // 单线程守护线程池，用于保证日志追加写入绝对线程安全且非阻塞
    private val writerExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ShiYun-AppLogger-Worker").apply {
            isDaemon = true
        }
    }

    /**
     * 初始化日志系统（可在 Application 或 MainActivity.onCreate 中调用）
     */
    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                val baseDir = context.applicationContext.filesDir.resolve(LOG_SUBDIR)
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }
                logDir = baseDir
                isInitialized = true

                // 注册全局未捕获异常处理器
                installCrashHandler()

                i("AppLogger", "诗云运行日志系统初始化就绪: 存储目录=${baseDir.absolutePath}")
                i("AppLogger", "运行环境: Android SDK=${Build.VERSION.SDK_INT}, 机型=${Build.MANUFACTURER} ${Build.MODEL}, 架构=${Build.SUPPORTED_ABIS.joinToString()}")
            } catch (e: Exception) {
                // 初始化异常绝不抛出崩溃
                try {
                    Log.e("AppLogger", "初始化日志目录失败: ${e.message}", e)
                } catch (_: Throwable) {
                }
            }
        }
    }

    /**
     * 仅供测试使用的初始化方法，允许指定自定义目录并重置状态
     */
    fun initForTesting(testDir: File) {
        synchronized(this) {
            if (!testDir.exists()) {
                testDir.mkdirs()
            }
            logDir = testDir
            isInitialized = true
        }
    }

    /**
     * 等待所有后台写入任务完成（供测试验证异步写入结果）
     */
    fun flushForTesting(timeoutMs: Long = 3000L) {
        val future = writerExecutor.submit { }
        try {
            future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        }
    }

    /**
     * 注册全局未捕获异常崩溃日志兜底
     */
    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                e("CrashHandler", "捕获到未捕获异常，线程=[${thread.name}]: ${throwable.message}", throwable)
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message, null)
    }

    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    /**
     * 核心格式化与写入派发逻辑
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        // 1. 同步镜像输出至系统 Logcat，方便开发期调试
        try {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message, throwable)
                LogLevel.INFO -> Log.i(tag, message, throwable)
                LogLevel.WARN -> Log.w(tag, message, throwable)
                LogLevel.ERROR -> Log.e(tag, message, throwable)
            }
        } catch (_: Throwable) {
            // 在未 Mock 的纯 JVM 单元测试环境中忽略 Logcat 抛错
        }

        val targetDir = logDir ?: return
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }

        // 2. 格式化日志内容
        val logLine = buildString {
            append(timestamp)
            append(" [")
            append(level.label)
            append("] [")
            append(tag)
            append("] ")
            append(message)
            append("\n")
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                append(sw.toString())
                if (!sw.toString().endsWith("\n")) {
                    append("\n")
                }
            }
        }

        // 3. 异步提交至后台线程写入私有文件
        try {
            writerExecutor.execute {
                writeLogLineToFile(targetDir, logLine)
            }
        } catch (_: Exception) {
            // 调度器关闭或队列满时静默忽略，绝不干扰业务
        }
    }

    /**
     * 文件追加写入与大小轮换
     */
    @Synchronized
    internal fun writeLogLineToFile(dir: File, content: String) {
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val activeFile = dir.resolve(ACTIVE_LOG_FILE)

            // 检查当前文件大小，达到 2 MB 时触发轮换
            if (activeFile.exists() && activeFile.length() >= MAX_FILE_SIZE_BYTES) {
                rotateLogFiles(dir)
            }

            // 追加写入
            FileOutputStream(activeFile, true).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
        } catch (e: Exception) {
            try {
                Log.e("AppLogger", "写入日志文件异常: ${e.message}")
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * 日志轮换：
     * app.log -> app_1.log -> app_2.log -> ... -> app_5.log (超出即删除)
     */
    internal fun rotateLogFiles(dir: File) {
        try {
            val oldestFile = dir.resolve("app_$MAX_BACKUP_FILES.log")
            if (oldestFile.exists()) {
                oldestFile.delete()
            }

            for (i in (MAX_BACKUP_FILES - 1) downTo 1) {
                val src = dir.resolve("app_$i.log")
                val dst = dir.resolve("app_${i + 1}.log")
                if (src.exists()) {
                    src.renameTo(dst)
                }
            }

            val activeFile = dir.resolve(ACTIVE_LOG_FILE)
            val firstBackup = dir.resolve("app_1.log")
            if (activeFile.exists()) {
                activeFile.renameTo(firstBackup)
            }
        } catch (e: Exception) {
            try {
                Log.e("AppLogger", "日志文件轮换失败: ${e.message}")
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * 获取当前本地日志统计指标
     */
    fun getLogStats(): LogStats {
        val dir = logDir ?: return LogStats(0, 0L, "未初始化", 0L)
        if (!dir.exists()) {
            return LogStats(0, 0L, dir.absolutePath, 0L)
        }
        val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".log") } ?: emptyArray()
        val totalSize = files.sumOf { it.length() }
        val latestModified = files.maxOfOrNull { it.lastModified() } ?: 0L
        return LogStats(
            fileCount = files.size,
            totalSizeBytes = totalSize,
            directoryPath = dir.absolutePath,
            lastModifiedTimestamp = latestModified,
        )
    }

    /**
     * 清理所有本地内部存储日志文件
     */
    fun clearLogs(): Boolean {
        val dir = logDir ?: return false
        return try {
            val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".log") } ?: emptyArray()
            var allDeleted = true
            for (f in files) {
                if (!f.delete()) {
                    allDeleted = false
                }
            }
            i("AppLogger", "已执行本地运行日志清空操作: 清除文件数=${files.size}, 成功=$allDeleted")
            allDeleted
        } catch (e: Exception) {
            e("AppLogger", "清空本地日志失败: ${e.message}", e)
            false
        }
    }

    /**
     * 导出所有保留日志至公共 Download/诗云日志 目录
     *
     * 采用 Android 原生 MediaStore 规范，兼容 Android 10+ (API 29+)，零存储权限请求。
     */
    fun exportLogsToDownloads(context: Context): LogExportResult {
        val dir = logDir ?: return LogExportResult(
            success = false,
            errorMessage = "日志系统尚未初始化",
        )

        val timestampStr = synchronized(exportFileNameFormat) { exportFileNameFormat.format(Date()) }
        val exportFileName = "shiyun_log_$timestampStr.txt"
        val relativeSubDir = "${Environment.DIRECTORY_DOWNLOADS}/诗云日志"

        return try {
            // 按历史时序收集所有日志文件（app_5.log -> ... -> app_1.log -> app.log）
            val logFilesToExport = mutableListOf<File>()
            for (i in MAX_BACKUP_FILES downTo 1) {
                val f = dir.resolve("app_$i.log")
                if (f.exists() && f.length() > 0) {
                    logFilesToExport.add(f)
                }
            }
            val active = dir.resolve(ACTIVE_LOG_FILE)
            if (active.exists() && active.length() > 0) {
                logFilesToExport.add(active)
            }

            val resolver = context.applicationContext.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, exportFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubDir)
            }

            val targetCollectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val targetUri = resolver.insert(targetCollectionUri, contentValues)
                ?: return LogExportResult(
                    success = false,
                    errorMessage = "无法在公共 Download 目录创建导出文件",
                )

            var writtenBytes = 0L
            resolver.openOutputStream(targetUri)?.use { outputStream ->
                // 1. 写入导出头部信息
                val header = buildString {
                    append("================================================================\n")
                    append("诗云应用运行日志导出文件\n")
                    append("导出时间: ").append(synchronized(dateFormat) { dateFormat.format(Date()) }).append("\n")
                    append("设备信息: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
                    append("系统版本: Android SDK ").append(Build.VERSION.SDK_INT).append(" (").append(Build.VERSION.RELEASE).append(")\n")
                    append("日志文件数: ").append(logFilesToExport.size).append("\n")
                    append("================================================================\n\n")
                }
                val headerBytes = header.toByteArray(Charsets.UTF_8)
                outputStream.write(headerBytes)
                writtenBytes += headerBytes.size

                // 2. 按顺序合并写入历史日志文件
                for (file in logFilesToExport) {
                    val fileHeader = "\n--- [归档日志: ${file.name}] ---\n".toByteArray(Charsets.UTF_8)
                    outputStream.write(fileHeader)
                    writtenBytes += fileHeader.size

                    file.inputStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var readLen: Int
                        while (inputStream.read(buffer).also { readLen = it } != -1) {
                            outputStream.write(buffer, 0, readLen)
                            writtenBytes += readLen
                        }
                    }
                }
                outputStream.flush()
            }

            i("AppLogger", "日志成功导出至公共目录: 文件名=$exportFileName, 路径=$relativeSubDir, 大小=$writtenBytes 字节")

            LogExportResult(
                success = true,
                fileName = exportFileName,
                relativePath = relativeSubDir,
                totalBytes = writtenBytes,
                uri = targetUri,
            )
        } catch (e: Exception) {
            e("AppLogger", "导出日志到 Downloads 异常: ${e.message}", e)
            LogExportResult(
                success = false,
                errorMessage = e.message ?: "导出日志失败",
            )
        }
    }
}
