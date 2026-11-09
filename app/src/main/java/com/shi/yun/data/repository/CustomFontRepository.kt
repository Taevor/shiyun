package com.shi.yun.data.repository

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import com.shi.yun.common.log.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * 用户从本地导入的自定义字体实体
 *
 * @property id 唯一标识符（如 "font_xxx"）
 * @property name 字体显示名称（优先从文件名或字体表中提取）
 * @property fileName 内部私有存储目录下的文件名
 * @property fileSize 字体文件字节大小
 */
data class CustomFontItem(
    val id: String,
    val name: String,
    val fileName: String,
    val fileSize: Long,
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("fileName", fileName)
        obj.put("fileSize", fileSize)
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): CustomFontItem? {
            val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
            val name = json.optString("name").takeIf { it.isNotBlank() } ?: "未命名字体"
            val fileName = json.optString("fileName").takeIf { it.isNotBlank() } ?: return null
            val fileSize = json.optLong("fileSize", 0L)
            return CustomFontItem(id, name, fileName, fileSize)
        }
    }
}

/**
 * 自定义字体管理仓储接口
 */
interface CustomFontRepository {
    fun getCustomFonts(): List<CustomFontItem>
    fun getFontFile(fontId: String): File?
    fun getFontFamily(fontId: String): FontFamily?
    suspend fun importFont(uri: Uri): Result<CustomFontItem>
    suspend fun deleteFont(fontId: String): Boolean
}

/**
 * 自定义字体本地持久化仓储实现
 *
 * 安全规范：
 * 1. 拷贝至应用私有存储目录 `files/fonts/`，绝不修改用户原始文件；
 * 2. 校验文件扩展名与 Skia Typeface 解析有效性，损坏或不支持格式安全阻断；
 * 3. 内存缓存已解析的 FontFamily，避免重复调用 JNI 解析文件；
 * 4. 线程安全与异常隔离。
 */
class LocalCustomFontRepository(
    private val context: Context,
    private val prefsName: String = "shiyun_custom_fonts",
) : CustomFontRepository {

    private val lock = Any()
    private val fontsDir: File by lazy {
        File(context.filesDir, "fonts").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val prefs by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    // 内存 Typeface / FontFamily 缓存，避免重复开销
    private val fontFamilyCache = mutableMapOf<String, FontFamily>()

    override fun getCustomFonts(): List<CustomFontItem> {
        synchronized(lock) {
            val raw = prefs.getString(KEY_FONTS_JSON, null) ?: return emptyList()
            return try {
                val array = JSONArray(raw)
                val list = mutableListOf<CustomFontItem>()
                for (i in 0 until array.length()) {
                    val item = CustomFontItem.fromJson(array.getJSONObject(i))
                    if (item != null) {
                        val file = File(fontsDir, item.fileName)
                        if (file.exists() && file.length() > 0) {
                            list.add(item)
                        }
                    }
                }
                list
            } catch (e: Exception) {
                AppLogger.e("CustomFontRepository", "读取字体列表失败: ${e.message}", e)
                emptyList()
            }
        }
    }

    override fun getFontFile(fontId: String): File? {
        val item = getCustomFonts().firstOrNull { it.id == fontId } ?: return null
        val file = File(fontsDir, item.fileName)
        return if (file.exists()) file else null
    }

    override fun getFontFamily(fontId: String): FontFamily? {
        synchronized(lock) {
            fontFamilyCache[fontId]?.let { return it }

            val file = getFontFile(fontId) ?: return null
            return try {
                val typeface = Typeface.createFromFile(file)
                if (typeface != null) {
                    val family = FontFamily(typeface)
                    fontFamilyCache[fontId] = family
                    family
                } else {
                    null
                }
            } catch (e: Exception) {
                AppLogger.e("CustomFontRepository", "解析字体 FontFamily 失败: id=$fontId, error=${e.message}", e)
                null
            }
        }
    }

    override suspend fun importFont(uri: Uri): Result<CustomFontItem> {
        return try {
            val rawName = resolveFileName(uri)
            val extension = rawName.substringAfterLast('.', "").lowercase()

            if (extension != "ttf" && extension != "otf") {
                return Result.failure(IllegalArgumentException("仅支持 TTF 或 OTF 格式的字体文件"))
            }

            val fontId = "font_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val targetFileName = "$fontId.$extension"
            val tempFile = File(fontsDir, "temp_$targetFileName")

            // 1. 复制流至私有临时文件
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("无法打开所选字体文件"))

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.length() <= 0L) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("所选字体文件为空"))
            }

            // 2. 校验 Typeface 是否可正常被底层 Skia 引擎解析
            val testTypeface = try {
                Typeface.createFromFile(tempFile)
            } catch (e: Exception) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("字体文件损坏或格式不兼容: ${e.message}"))
            }

            if (testTypeface == null) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("系统无法解析该字体格式"))
            }

            // 3. 重命名为正式文件
            val finalFile = File(fontsDir, targetFileName)
            if (!tempFile.renameTo(finalFile)) {
                // 如果 renameTo 失败，采用备用拷贝
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val displayName = rawName.substringBeforeLast('.')
                .takeIf { it.isNotBlank() } ?: "自定义字体"
            val newItem = CustomFontItem(
                id = fontId,
                name = displayName,
                fileName = targetFileName,
                fileSize = finalFile.length(),
            )

            // 4. 持久化元数据
            synchronized(lock) {
                val currentList = getCustomFonts().toMutableList()
                currentList.add(newItem)
                saveFontsListInternal(currentList)
            }

            AppLogger.i("CustomFontRepository", "成功导入自定义字体: 名称=${newItem.name}, 大小=${newItem.fileSize} bytes")
            Result.success(newItem)
        } catch (e: Exception) {
            AppLogger.e("CustomFontRepository", "导入字体发生异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFont(fontId: String): Boolean {
        synchronized(lock) {
            val list = getCustomFonts().toMutableList()
            val targetIndex = list.indexOfFirst { it.id == fontId }
            if (targetIndex < 0) return false

            val item = list.removeAt(targetIndex)
            val file = File(fontsDir, item.fileName)
            if (file.exists()) {
                file.delete()
            }
            fontFamilyCache.remove(fontId)
            saveFontsListInternal(list)
            AppLogger.i("CustomFontRepository", "已删除自定义字体: ID=$fontId, 名称=${item.name}")
            return true
        }
    }

    private fun resolveFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (result.isNullOrBlank()) {
            result = uri.path?.substringAfterLast('/')
        }
        return result?.takeIf { it.isNotBlank() } ?: "font.ttf"
    }

    private fun saveFontsListInternal(list: List<CustomFontItem>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_FONTS_JSON, array.toString()).apply()
    }

    companion object {
        private const val KEY_FONTS_JSON = "key_custom_fonts_json"
    }
}
