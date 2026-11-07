package com.childe.shi.data.repository

import android.content.Context
import com.childe.shi.data.model.Work
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 基于本地 Assets JSON 文件的诗词数据仓库实现
 *
 * 核心设计遵循规范要求：
 * 1. 采用纯原生 Android SDK 自带的 org.json，无需添加任何第三方依赖。
 * 2. 所有的文件 I/O 与 JSON 解析都在 Dispatchers.IO 调度器中执行，严禁阻塞主线程。
 * 3. 内存缓存机制：首次加载后常驻内存，后续快速检索与滑动推荐时直接读取内存，不重复解析文件。
 * 4. 严谨的异常抛出：当文件缺失、读取失败或格式损坏时明确抛出异常，由上层处理，不吞掉异常。
 */
class LocalJsonWorkRepository(
    private val context: Context,
    private val assetPath: String = "data/sample_works.json",
) : WorkRepository {

    // 内存缓存，避免重复读取与解析文件
    @Volatile
    private var cachedWorks: List<Work>? = null
    private val mutex = Mutex()

    override suspend fun getAllWorks(): List<Work> {
        val currentCache = cachedWorks
        if (currentCache != null) {
            return currentCache
        }
        return mutex.withLock {
            cachedWorks ?: loadWorksFromAsset().also { cachedWorks = it }
        }
    }

    override suspend fun getWorkById(id: String): Work? {
        return getAllWorks().find { it.id == id }
    }

    override suspend fun getRandomWork(): Work? {
        val works = getAllWorks()
        return if (works.isNotEmpty()) works.random() else null
    }

    override suspend fun getWorksByGenre(genre: String): List<Work> {
        return getAllWorks().filter { it.genre.equals(genre, ignoreCase = true) }
    }

    /**
     * 在后台 IO 线程中读取并解析 assets 目录下的 JSON 文件
     *
     * 出现异常时不吞错，明确向上抛出，确保调用方获知真正的加载状态。
     */
    private suspend fun loadWorksFromAsset(): List<Work> = withContext(Dispatchers.IO) {
        val jsonString = try {
            context.assets.open(assetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("读取 assets 文件 [$assetPath] 失败: ${e.message}", e)
        }

        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            throw IllegalStateException("解析 JSON 数据格式失败: ${e.message}", e)
        }

        val workList = mutableListOf<Work>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val work = Work(
                id = jsonObject.optString("id", ""),
                title = jsonObject.optString("title", ""),
                author = jsonObject.optString("author", ""),
                dynasty = jsonObject.optString("dynasty", ""),
                genre = jsonObject.optString("genre", ""),
                content = jsonObject.optString("content", ""),
                excerpt = jsonObject.optString("excerpt", ""),
                source = jsonObject.optString("source", ""),
                sourceId = jsonObject.optString("sourceId", ""),
                licenseNote = jsonObject.optString("licenseNote", "")
            )
            if (work.id.isNotBlank() && work.title.isNotBlank()) {
                workList.add(work)
            }
        }
        workList
    }
}
