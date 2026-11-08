package com.childe.shi.data.repository

import android.content.Context
import com.childe.shi.common.log.AppLogger
import com.childe.shi.data.model.AnnotationItem
import com.childe.shi.data.model.Anthology
import com.childe.shi.data.model.AnthologyItem
import com.childe.shi.data.model.AnthologyWorkItem
import com.childe.shi.data.model.AttributionInfo
import com.childe.shi.data.model.AttributionStatus
import com.childe.shi.data.model.AuxiliaryContent
import com.childe.shi.data.model.Genre
import com.childe.shi.data.model.LayoutType
import com.childe.shi.data.model.LicenseType
import com.childe.shi.data.model.SourceInfo
import com.childe.shi.data.model.Work
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 基于本地 Assets JSON 文件的诗词数据仓库实现
 *
 * 核心设计遵循规范要求：
 * 1. 采用纯原生 Android SDK 自带的 org.json 与 SharedPreferences，无需添加任何第三方依赖。
 * 2. 所有的文件 I/O 与 JSON 解析都在 Dispatchers.IO 调度器中执行，严禁阻塞主线程。
 * 3. 内存缓存机制：首次加载后常驻内存，后续快速检索与滑动推荐时直接读取内存，不重复解析文件。
 * 4. 严谨的异常抛出：当文件缺失、读取失败或格式损坏时明确抛出异常，由上层处理，不吞掉异常。
 * 5. 线程安全的收藏机制：支持有序本地持久化（最新收藏居首）、防竞态初始化与响应式 StateFlow 同步。
 */
class LocalJsonWorkRepository(
    private val context: Context,
    private val assetPath: String = "data/works.json",
    private val anthologiesAssetPath: String = "data/anthologies.json",
    private val anthologyItemsAssetPath: String = "data/anthology_items.json",
    private val prefsName: String = PREFS_NAME,
) : WorkRepository {

    companion object {
        private const val PREFS_NAME = "shi_preferences"
        private const val KEY_FAVORITE_IDS = "favorite_ids"

        /**
         * 静态/公共 JSON 作品解析函数，供 Repository 和测试统一调用
         *
         * 遵循阶段四 B1 严格规范：
         * 1. id, title, genre, content 为必填核心字符串，缺失或类型错误抛出包含作品上下文的异常；
         * 2. author, dynasty 允许缺失或显式 null（解析为空字符串），类型错误时严禁静默容忍；
         * 3. textSource 显式区分缺失（回退检查旧 source）、显式 null、合法对象与类型错误四种互斥状态；
         * 4. 严格解析所有受控枚举，禁止静默降级；
         * 5. 完整校验 auxiliary 与 attribution 结构及其来源强一致性。
         */
        fun parseWorkFromJson(jsonObject: JSONObject, index: Int = 0): Work {
            var currentId: String? = null
            var currentTitle: String? = null
            try {
                // 1. 必填核心字段校验（id, title, genre, content）与非必填作者/朝代解析
                currentId = requireNonBlankString(jsonObject, "id")
                currentTitle = requireNonBlankString(jsonObject, "title")
                val author = parseOptionalStringField(jsonObject, "author")
                val dynasty = parseOptionalStringField(jsonObject, "dynasty")
                val content = requireNonBlankString(jsonObject, "content")
                val excerpt = jsonObject.optString("excerpt", "")

                // 2. 体裁枚举严格解析（非静默）
                val rawGenre = requireNonBlankString(jsonObject, "genre")
                val genre = Genre.fromCode(rawGenre)

                // 3. 可选排版枚举严格解析（缺失或显式 null 为 null，非字符串、空白或非法编码立即报错）
                val layout = if (jsonObject.has("layout") && !jsonObject.isNull("layout")) {
                    val raw = jsonObject.opt("layout")
                    if (raw !is String) {
                        throw IllegalArgumentException("字段 'layout' 必须为字符串类型")
                    }
                    if (raw.isBlank()) {
                        throw IllegalArgumentException("字段 'layout' 存在时不能为空白字符串")
                    }
                    LayoutType.fromCode(raw.trim())
                } else null

                // 4. 可选字符串字段
                val cipai = jsonObject.optString("cipai", "").takeIf { it.isNotBlank() }
                val subtitle = jsonObject.optString("subtitle", "").takeIf { it.isNotBlank() }
                val preface = jsonObject.optString("preface", "").takeIf { it.isNotBlank() }

                // 5. 旧来源字段读取（保留向后兼容）
                val source = jsonObject.optString("source", "")
                val sourceId = jsonObject.optString("sourceId", "")
                val licenseNote = jsonObject.optString("licenseNote", "")

                // 6. 底本来源 textSource 显式状态判定
                val textSource = parseTextSource(jsonObject, source, licenseNote)

                // 7. 作者争议考订 attribution 解析
                val attribution = parseAttribution(jsonObject)

                // 8. 辅助内容 auxiliary 解析
                val auxiliary = parseAuxiliary(jsonObject)

                return Work(
                    id = currentId,
                    title = currentTitle,
                    author = author,
                    dynasty = dynasty,
                    genre = genre,
                    content = content,
                    excerpt = excerpt,
                    cipai = cipai,
                    subtitle = subtitle,
                    layout = layout,
                    preface = preface,
                    textSource = textSource,
                    attribution = attribution,
                    auxiliary = auxiliary,
                    source = source,
                    sourceId = sourceId,
                    licenseNote = licenseNote,
                )
            } catch (e: Exception) {
                throw IllegalStateException("解析作品失败 [序号: $index, ID: ${currentId ?: "未知"}, 标题: ${currentTitle ?: "未知"}]: ${e.message}", e)
            }
        }

        private fun requireNonBlankString(json: JSONObject, key: String): String {
            if (!json.has(key) || json.isNull(key)) {
                throw IllegalArgumentException("缺少必填字段: '$key'")
            }
            val value = try {
                json.getString(key)
            } catch (e: Exception) {
                throw IllegalArgumentException("字段 '$key' 必须为字符串类型", e)
            }
            if (value.isBlank()) {
                throw IllegalArgumentException("字段 '$key' 不能为空白字符串")
            }
            return value.trim()
        }

        private fun parseOptionalStringField(json: JSONObject, key: String): String {
            if (!json.has(key) || json.isNull(key)) {
                return ""
            }
            val raw = json.opt(key)
            if (raw !is String) {
                throw IllegalArgumentException("字段 '$key' 必须为字符串类型")
            }
            return raw.trim()
        }

        private fun parseTextSource(jsonObject: JSONObject, oldSource: String, oldLicenseNote: String): SourceInfo? {
            return when {
                // 状态 2: 显式声明为 null -> 不回退检查旧 source
                jsonObject.has("textSource") && jsonObject.isNull("textSource") -> null

                // 状态 3: 键存在且为合法对象
                jsonObject.has("textSource") -> {
                    val rawObj = jsonObject.optJSONObject("textSource")
                        ?: throw IllegalArgumentException("字段 [textSource] 类型错误，必须为 JSON 对象或 null")
                    parseSourceInfoObject(rawObj)
                }

                // 状态 1: 键完全不存在 -> 允许后备检查旧 source
                oldSource.isNotBlank() -> {
                    SourceInfo(
                        sourceName = oldSource.trim(),
                        licenseType = LicenseType.UNKNOWN,
                        referenceUrl = null,
                        licenseNote = oldLicenseNote.trim().ifBlank { "待核实" }
                    )
                }

                // 旧 source 亦缺失或空白 -> null，严禁凭空创建来源名称
                else -> null
            }
        }

        private fun parseSourceInfoObject(obj: JSONObject): SourceInfo {
            val sourceName = requireNonBlankString(obj, "sourceName")
            val licenseType = if (obj.has("licenseType") && !obj.isNull("licenseType")) {
                val rawLicense = obj.getString("licenseType")
                if (rawLicense.isBlank()) {
                    throw IllegalArgumentException("字段 [licenseType] 不能为空白字符串")
                }
                LicenseType.fromCode(rawLicense)
            } else {
                LicenseType.UNKNOWN
            }
            val referenceUrl = if (obj.has("referenceUrl") && !obj.isNull("referenceUrl")) {
                obj.getString("referenceUrl").takeIf { it.isNotBlank() }
            } else null
            val licenseNote = if (obj.has("licenseNote") && !obj.isNull("licenseNote")) {
                obj.getString("licenseNote").trim().ifBlank { "待核实" }
            } else "待核实"

            return SourceInfo(
                sourceName = sourceName,
                licenseType = licenseType,
                referenceUrl = referenceUrl,
                licenseNote = licenseNote
            )
        }

        private fun parseAttribution(jsonObject: JSONObject): AttributionInfo? {
            if (!jsonObject.has("attribution") || jsonObject.isNull("attribution")) {
                return null
            }
            val attrObj = jsonObject.optJSONObject("attribution")
                ?: throw IllegalArgumentException("字段 [attribution] 类型错误，必须为 JSON 对象或 null")

            val rawStatus = requireNonBlankString(attrObj, "status")
            val status = AttributionStatus.fromCode(rawStatus)

            val candidateAuthors = if (attrObj.has("candidateAuthors") && !attrObj.isNull("candidateAuthors")) {
                val array = attrObj.optJSONArray("candidateAuthors")
                    ?: throw IllegalArgumentException("字段 [candidateAuthors] 必须为 JSON 数组")
                (0 until array.length()).map { array.getString(it) }
            } else emptyList()

            val note = if (attrObj.has("note") && !attrObj.isNull("note")) {
                attrObj.getString("note")
            } else ""

            val academicSources = if (attrObj.has("academicSources") && !attrObj.isNull("academicSources")) {
                val array = attrObj.optJSONArray("academicSources")
                    ?: throw IllegalArgumentException("字段 [academicSources] 必须为 JSON 数组")
                (0 until array.length()).map { array.getString(it) }
            } else emptyList()

            return AttributionInfo(
                status = status,
                candidateAuthors = candidateAuthors,
                note = note,
                academicSources = academicSources
            )
        }

        private fun parseAuxiliary(jsonObject: JSONObject): AuxiliaryContent? {
            if (!jsonObject.has("auxiliary") || jsonObject.isNull("auxiliary")) {
                return null
            }
            val auxObj = jsonObject.optJSONObject("auxiliary")
                ?: throw IllegalArgumentException("字段 [auxiliary] 类型错误，必须为 JSON 对象或 null")

            val translation = if (auxObj.has("translation") && !auxObj.isNull("translation")) {
                auxObj.getString("translation").takeIf { it.isNotBlank() }
            } else null

            val translationSource = if (auxObj.has("translationSource") && !auxObj.isNull("translationSource")) {
                val srcObj = auxObj.optJSONObject("translationSource")
                    ?: throw IllegalArgumentException("字段 [translationSource] 必须为 JSON 对象")
                parseSourceInfoObject(srcObj)
            } else null

            val annotations = if (auxObj.has("annotations") && !auxObj.isNull("annotations")) {
                val array = auxObj.optJSONArray("annotations")
                    ?: throw IllegalArgumentException("字段 [annotations] 必须为 JSON 数组")
                val list = mutableListOf<AnnotationItem>()
                for (i in 0 until array.length()) {
                    val itemObj = array.optJSONObject(i)
                        ?: throw IllegalArgumentException("注释条目 [索引: $i] 必须为 JSON 对象")
                    val index = itemObj.optInt("index", i + 1)
                    val quote = requireNonBlankString(itemObj, "quote")
                    val note = requireNonBlankString(itemObj, "note")
                    val phonetic = itemObj.optString("phonetic", "").takeIf { it.isNotBlank() }
                    val targetLine = if (itemObj.has("targetLine") && !itemObj.isNull("targetLine")) itemObj.getInt("targetLine") else null
                    list.add(AnnotationItem(index, quote, note, phonetic, targetLine))
                }
                list
            } else null

            val annotationSource = if (auxObj.has("annotationSource") && !auxObj.isNull("annotationSource")) {
                val srcObj = auxObj.optJSONObject("annotationSource")
                    ?: throw IllegalArgumentException("字段 [annotationSource] 必须为 JSON 对象")
                parseSourceInfoObject(srcObj)
            } else null

            val appreciation = if (auxObj.has("appreciation") && !auxObj.isNull("appreciation")) {
                auxObj.getString("appreciation").takeIf { it.isNotBlank() }
            } else null

            val appreciationSource = if (auxObj.has("appreciationSource") && !auxObj.isNull("appreciationSource")) {
                val srcObj = auxObj.optJSONObject("appreciationSource")
                    ?: throw IllegalArgumentException("字段 [appreciationSource] 必须为 JSON 对象")
                parseSourceInfoObject(srcObj)
            } else null

            val background = if (auxObj.has("background") && !auxObj.isNull("background")) {
                auxObj.getString("background").takeIf { it.isNotBlank() }
            } else null

            val backgroundSource = if (auxObj.has("backgroundSource") && !auxObj.isNull("backgroundSource")) {
                val srcObj = auxObj.optJSONObject("backgroundSource")
                    ?: throw IllegalArgumentException("字段 [backgroundSource] 必须为 JSON 对象")
                parseSourceInfoObject(srcObj)
            } else null

            return AuxiliaryContent(
                translation = translation,
                translationSource = translationSource,
                annotations = annotations,
                annotationSource = annotationSource,
                appreciation = appreciation,
                appreciationSource = appreciationSource,
                background = background,
                backgroundSource = backgroundSource
            )
        }

        /**
         * 批量解析作品 JSON 数组，执行严格的元素对象类型校验与上下文定位
         *
         * 确保：
         * 1. 数组元素非 JSONObject 时抛出包含数组序号的 IllegalStateException；
         * 2. 保留原始 cause，绝不跳过错误元素，绝不返回部分损坏列表。
         */
        fun parseWorkListFromJsonArray(jsonArray: JSONArray): List<Work> {
            val workList = mutableListOf<Work>()
            for (i in 0 until jsonArray.length()) {
                val rawItem = jsonArray.opt(i)
                if (rawItem !is JSONObject) {
                    val cause = IllegalArgumentException("数组元素必须为 JSON 对象，但实际类型为: ${rawItem?.javaClass?.simpleName ?: "null"}")
                    throw IllegalStateException("解析作品列表失败 [序号: $i, ID: 未知, 标题: 未知]: ${cause.message}", cause)
                }
                val work = parseWorkFromJson(rawItem, i)
                workList.add(work)
            }
            return workList
        }
    }

    // 内存缓存，避免重复读取与解析文件
    @Volatile
    private var cachedWorks: List<Work>? = null

    @Volatile
    private var cachedAnthologies: List<Anthology>? = null

    @Volatile
    private var cachedAnthologyItems: List<AnthologyItem>? = null

    private val mutex = Mutex()
    private val anthologyMutex = Mutex()

    // 收藏管理内部数据结构与同步锁
    private val favoriteLock = Any()
    private val favoriteIdList = mutableListOf<String>()
    private val _favoriteIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteIdsFlow: StateFlow<Set<String>> = _favoriteIdsFlow.asStateFlow()

    init {
        // 在仓库初始化时同步原子读取 SharedPreferences，保证在 UI 首次组合前数据完全就绪，杜绝竞态与空集合覆盖
        synchronized(favoriteLock) {
            val loadedIds = loadFavoritesFromPrefs()
            favoriteIdList.clear()
            favoriteIdList.addAll(loadedIds)
            _favoriteIdsFlow.value = favoriteIdList.toSet()
        }
    }

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
        return getAllWorks().filter { it.genre.code.equals(genre, ignoreCase = true) }
    }

    override suspend fun getWorksByDynasty(dynasty: String): List<Work> {
        return getAllWorks().filter { it.dynasty == dynasty }
    }

    override suspend fun searchWorks(keyword: String): List<Work> = withContext(Dispatchers.IO) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val startTime = System.currentTimeMillis()
        val all = getAllWorks()
        val results = all.filter { work ->
            work.title.contains(trimmed, ignoreCase = true) ||
            work.author.contains(trimmed, ignoreCase = true) ||
            work.content.contains(trimmed, ignoreCase = true)
        }
        val duration = System.currentTimeMillis() - startTime
        AppLogger.i("Repository", "执行本地全文检索: 关键词字数=${trimmed.length}, 耗时=${duration}ms, 命中数量=${results.size}")
        results
    }

    override suspend fun getFavoriteWorks(): List<Work> {
        val allWorksMap = getAllWorks().associateBy { it.id }
        val idsSnapshot = synchronized(favoriteLock) {
            favoriteIdList.toList()
        }
        // 按照收藏顺序倒序输出，并自动过滤掉数据源中可能已被删除或不存在的无效 ID
        return idsSnapshot.mapNotNull { allWorksMap[it] }
    }

    override suspend fun toggleFavorite(id: String): Boolean {
        // 检查作品 ID 是否真实存在，不能收藏不存在的作品
        if (getWorkById(id) == null) {
            AppLogger.w("Repository", "收藏操作忽略: 目标作品 ID 不存在: $id")
            return false
        }

        val isAdded: Boolean
        synchronized(favoriteLock) {
            if (favoriteIdList.contains(id)) {
                favoriteIdList.remove(id)
                isAdded = false
            } else {
                favoriteIdList.add(0, id) // 最新收藏排在最前面
                isAdded = true
            }
            _favoriteIdsFlow.value = favoriteIdList.toSet()
            saveFavoritesToPrefsLocked()
        }
        AppLogger.i("Repository", "切换收藏状态: 作品 ID=$id, 状态=${if (isAdded) "已加入收藏" else "已取消收藏"}")
        return isAdded
    }

    /**
     * 从 SharedPreferences 同步读取已持久化的收藏 ID 列表
     *
     * 具备异常容错与脏数据清理：
     * 1. 捕获 JSON 解析异常，防止损坏的本地配置引起崩溃；
     * 2. 自动去重，保留最新顺序。
     */
    private fun loadFavoritesFromPrefs(): List<String> {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_FAVORITE_IDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val result = mutableListOf<String>()
            val seen = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val id = array.optString(i, "").trim()
                if (id.isNotEmpty() && seen.add(id)) {
                    result.add(id)
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 将当前收藏 ID 列表序列化为 JSON 数组并异步写入 SharedPreferences
     *
     * 必须在持有 favoriteLock 的同步块中调用。
     */
    private fun saveFavoritesToPrefsLocked() {
        try {
            val array = JSONArray()
            for (id in favoriteIdList) {
                array.put(id)
            }
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FAVORITE_IDS, array.toString()).apply()
        } catch (_: Exception) {
            // 保证异常不扩散
        }
    }

    /**
     * 在后台 IO 线程中读取并解析 assets 目录下的 JSON 文件
     *
     * 出现异常时不吞错，明确向上抛出，确保调用方获知真正的加载状态。
     */
    private suspend fun loadWorksFromAsset(): List<Work> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        AppLogger.i("Repository", "开始读取并解析 assets 诗词数据: assetPath=$assetPath")

        val jsonString = try {
            context.assets.open(assetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            val error = IllegalStateException("读取 assets 文件 [$assetPath] 失败: ${e.message}", e)
            AppLogger.e("Repository", "读取 assets 文件失败: assetPath=$assetPath", error)
            throw error
        }

        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            val error = IllegalStateException("解析 JSON 数据格式失败: ${e.message}", e)
            AppLogger.e("Repository", "解析 assets JSON 失败: assetPath=$assetPath", error)
            throw error
        }

        val parsedWorks = try {
            parseWorkListFromJsonArray(jsonArray)
        } catch (e: Exception) {
            AppLogger.e("Repository", "反序列化诗词实体失败: ${e.message}", e)
            throw e
        }

        val duration = System.currentTimeMillis() - startTime
        AppLogger.i("Repository", "诗词数据加载成功: assetPath=$assetPath, 耗时=${duration}ms, 总篇数=${parsedWorks.size}")
        parsedWorks
    }

    // =========================================================================
    // 选本 (Anthology) 核心实现
    // =========================================================================

    override suspend fun getAnthologies(): List<Anthology> {
        val current = cachedAnthologies
        if (current != null) return current
        return anthologyMutex.withLock {
            cachedAnthologies ?: loadAnthologiesFromAsset().also { cachedAnthologies = it }
        }
    }

    override suspend fun getAnthologyById(id: String): Anthology? {
        return getAnthologies().find { it.id == id }
    }

    override suspend fun getWorksByAnthology(anthologyId: String): List<AnthologyWorkItem> {
        // 1. 确保全部作品和全部篇目数据就绪
        val allWorksMap = getAllWorks().associateBy { it.id }
        val allItems = getAllAnthologyItems()

        // 2. 筛选出属于当前选本的所有篇目，并严格按 orderIndex 升序排序
        val matchingItems = allItems.filter { it.anthologyId == anthologyId }
            .sortedBy { it.orderIndex }

        val totalRawCount = matchingItems.size
        var missingCount = 0
        val resultList = ArrayList<AnthologyWorkItem>(totalRawCount)

        for (item in matchingItems) {
            val work = allWorksMap[item.workId]
            if (work != null) {
                resultList.add(AnthologyWorkItem(item, work))
            } else {
                missingCount++
                AppLogger.w(
                    "Repository",
                    "选本篇目关联作品缺失: 选本ID=$anthologyId, 篇目ID=${item.id}, 篇目序号=${item.orderIndex}, 目标作品ID=${item.workId}"
                )
            }
        }

        AppLogger.i(
            "Repository",
            "获取选本篇目完成: 选本ID=$anthologyId, 原始篇目数=$totalRawCount, 成功关联数=${resultList.size}, 缺失数=$missingCount"
        )

        return resultList
    }

    private suspend fun getAllAnthologyItems(): List<AnthologyItem> {
        val current = cachedAnthologyItems
        if (current != null) return current
        return anthologyMutex.withLock {
            cachedAnthologyItems ?: loadAnthologyItemsFromAsset().also { cachedAnthologyItems = it }
        }
    }

    private suspend fun loadAnthologiesFromAsset(): List<Anthology> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        AppLogger.i("Repository", "开始读取并解析选本定义: assetPath=$anthologiesAssetPath")

        val jsonString = try {
            context.assets.open(anthologiesAssetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            val error = IllegalStateException("读取选本定义文件 [$anthologiesAssetPath] 失败: ${e.message}", e)
            AppLogger.e("Repository", "读取选本定义文件失败: assetPath=$anthologiesAssetPath", error)
            throw error
        }

        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            val error = IllegalStateException("解析选本定义 JSON 数据格式失败: ${e.message}", e)
            AppLogger.e("Repository", "解析选本定义 JSON 失败: assetPath=$anthologiesAssetPath", error)
            throw error
        }

        val list = mutableListOf<Anthology>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
                ?: throw IllegalStateException("选本定义数组元素非 JSONObject [序号: $i]")
            val id = requireNonBlankString(obj, "id")
            val code = requireNonBlankString(obj, "code")
            val name = requireNonBlankString(obj, "name")
            val compiler = obj.optString("compiler", "")
            val dynasty = obj.optString("dynasty", "")
            val targetCount = obj.optInt("targetCount", 0)
            val editionNote = obj.optString("editionNote", "")
            list.add(
                Anthology(
                    id = id,
                    code = code,
                    name = name,
                    compiler = compiler,
                    dynasty = dynasty,
                    targetCount = targetCount,
                    editionNote = editionNote
                )
            )
        }

        val duration = System.currentTimeMillis() - startTime
        AppLogger.i("Repository", "选本定义加载成功: 耗时=${duration}ms, 数量=${list.size}")
        list
    }

    private suspend fun loadAnthologyItemsFromAsset(): List<AnthologyItem> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        AppLogger.i("Repository", "开始读取并解析选本篇目条目: assetPath=$anthologyItemsAssetPath")

        val jsonString = try {
            context.assets.open(anthologyItemsAssetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            val error = IllegalStateException("读取选本篇目文件 [$anthologyItemsAssetPath] 失败: ${e.message}", e)
            AppLogger.e("Repository", "读取选本篇目文件失败: assetPath=$anthologyItemsAssetPath", error)
            throw error
        }

        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            val error = IllegalStateException("解析选本篇目 JSON 数据格式失败: ${e.message}", e)
            AppLogger.e("Repository", "解析选本篇目 JSON 失败: assetPath=$anthologyItemsAssetPath", error)
            throw error
        }

        val list = ArrayList<AnthologyItem>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
                ?: throw IllegalStateException("选本篇目数组元素非 JSONObject [序号: $i]")
            val id = requireNonBlankString(obj, "id")
            val anthologyId = requireNonBlankString(obj, "anthologyId")
            val workId = requireNonBlankString(obj, "workId")
            val orderIndex = if (obj.has("orderIndex")) obj.getInt("orderIndex") else {
                throw IllegalArgumentException("选本篇目缺少必填整数字段 'orderIndex' [序号: $i, ID: $id]")
            }
            val volume = obj.optString("volume", "")
            list.add(
                AnthologyItem(
                    id = id,
                    anthologyId = anthologyId,
                    workId = workId,
                    orderIndex = orderIndex,
                    volume = volume
                )
            )
        }

        val duration = System.currentTimeMillis() - startTime
        AppLogger.i("Repository", "选本篇目条目加载成功: 耗时=${duration}ms, 数量=${list.size}")
        list
    }
}
