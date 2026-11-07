package com.childe.shi.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.childe.shi.data.model.AttributionStatus
import com.childe.shi.data.model.Genre
import com.childe.shi.data.model.LayoutType
import com.childe.shi.data.model.LicenseType
import com.childe.shi.data.repository.LocalJsonWorkRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段四 B1 设备插桩测试
 *
 * 在真实 Android 运行时环境中验证：
 * 1. 真实 Android SDK org.json 的解析与严格容错；
 * 2. 旧版 JSON 存在 source 与无 source 两种情况的处理；
 * 3. 新旧底本来源共存时的优先级及显式 null 判定；
 * 4. author 与 dynasty 非必填解析（允许缺失、显式 null、空白，但严格拦截类型错误）；
 * 5. 核心必填字段（id, title, genre, content）缺失与空白的严格拦截；
 * 6. 受控枚举（Genre, LayoutType, LicenseType）非法编码严格拦截；
 * 7. 数组元素非 JSONObject 异常定位（含序号、未知 ID/标题与原始 cause）；
 * 8. 新版规范 JSON 完整字段反序列化；
 * 9. LocalJsonWorkRepository 从真实 assets 资源加载现有 sample_works.json 的全流程；
 * 10. 真实 SharedPreferences 隔离测试（shi_preferences_test）：收藏有序（最新居首）、持久化无损、不清除未映射的历史 ID。
 */
@RunWith(AndroidJUnit4::class)
class LocalJsonWorkRepositoryInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testPrefsName = "shi_preferences_test"

    @Before
    fun setUp() {
        // 清理独立测试偏好，确保测试间互不干扰且绝不污染用户正式数据
        context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    // -------------------------------------------------------------------------
    // 1. 旧版 JSON 存在 source 与无 source 两种情况的处理
    // -------------------------------------------------------------------------

    @Test
    fun testLegacyJson_withSource_buildsCompatibleSourceInfo() {
        val json = JSONObject("""
            {
                "id": "tang_000001",
                "title": "静夜思",
                "author": "李白",
                "dynasty": "唐",
                "genre": "poem",
                "content": "床前明月光",
                "excerpt": "床前明月光",
                "source": "chinese-poetry",
                "licenseNote": "公有领域"
            }
        """.trimIndent())

        val work = LocalJsonWorkRepository.parseWorkFromJson(json, 0)
        assertNotNull(work.textSource)
        assertEquals("chinese-poetry", work.textSource?.sourceName)
        assertEquals(LicenseType.UNKNOWN, work.textSource?.licenseType)
        assertEquals("公有领域", work.textSource?.licenseNote)
    }

    @Test
    fun testLegacyJson_withoutSource_textSourceIsNull() {
        val json = JSONObject("""
            {
                "id": "tang_000001",
                "title": "静夜思",
                "author": "李白",
                "dynasty": "唐",
                "genre": "poem",
                "content": "床前明月光",
                "excerpt": "床前明月光",
                "source": "   "
            }
        """.trimIndent())

        val work = LocalJsonWorkRepository.parseWorkFromJson(json, 0)
        assertNull("空白 source 时 textSource 必须为 null，不得凭空捏造来源名称", work.textSource)
    }

    // -------------------------------------------------------------------------
    // 2. 新旧底本来源共存时的优先级及显式 null 判定
    // -------------------------------------------------------------------------

    @Test
    fun testSourcePriority_newTextSourceOverridesOldSource() {
        val json = JSONObject("""
            {
                "id": "tang_000001",
                "title": "静夜思",
                "author": "李白",
                "dynasty": "唐",
                "genre": "poem",
                "content": "床前明月光",
                "source": "old-source",
                "licenseNote": "旧声明",
                "textSource": {
                    "sourceName": "新版中华书局通行本",
                    "licenseType": "PUBLIC_DOMAIN",
                    "licenseNote": "公版确认"
                }
            }
        """.trimIndent())

        val work = LocalJsonWorkRepository.parseWorkFromJson(json, 0)
        assertNotNull(work.textSource)
        assertEquals("新版中华书局通行本", work.textSource?.sourceName)
        assertEquals(LicenseType.PUBLIC_DOMAIN, work.textSource?.licenseType)
        assertEquals("公版确认", work.textSource?.licenseNote)
    }

    @Test
    fun testSourceExplicitNull_doesNotFallbackToOldSource() {
        val json = JSONObject("""
            {
                "id": "tang_000001",
                "title": "静夜思",
                "author": "李白",
                "dynasty": "唐",
                "genre": "poem",
                "content": "床前明月光",
                "source": "old-source",
                "textSource": null
            }
        """.trimIndent())

        val work = LocalJsonWorkRepository.parseWorkFromJson(json, 0)
        assertNull("显式声明 textSource 为 null 时绝不后备回退检查旧 source", work.textSource)
    }

    // -------------------------------------------------------------------------
    // 3. author 与 dynasty 非必填解析与类型错误拦截
    // -------------------------------------------------------------------------

    @Test
    fun testAuthorAndDynasty_optionalHandling() {
        // author 与 dynasty 缺失 -> 解析为空字符串
        val missingAuthorDynastyJson = JSONObject("""
            {
                "id": "ancient_001",
                "title": "古歌谣",
                "genre": "poem",
                "content": "江南可采莲"
            }
        """.trimIndent())
        val work1 = LocalJsonWorkRepository.parseWorkFromJson(missingAuthorDynastyJson, 0)
        assertEquals("", work1.author)
        assertEquals("", work1.dynasty)

        // author 与 dynasty 显式 null -> 解析为空字符串
        val nullAuthorDynastyJson = JSONObject("""
            {
                "id": "ancient_002",
                "title": "古歌谣",
                "author": null,
                "dynasty": null,
                "genre": "poem",
                "content": "江南可采莲"
            }
        """.trimIndent())
        val work2 = LocalJsonWorkRepository.parseWorkFromJson(nullAuthorDynastyJson, 1)
        assertEquals("", work2.author)
        assertEquals("", work2.dynasty)

        // author 为错误类型（如整数 123）-> 必须抛错，严禁静默容忍
        val badAuthorTypeJson = JSONObject("""
            {
                "id": "bad_001",
                "title": "错误作者类型",
                "author": 12345,
                "genre": "poem",
                "content": "正文"
            }
        """.trimIndent())
        try {
            LocalJsonWorkRepository.parseWorkFromJson(badAuthorTypeJson, 2)
            fail("author 字段类型错误必须抛出 IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("ID: bad_001") == true)
            assertTrue(e.message?.contains("author") == true)
            assertNotNull(e.cause)
        }

        // dynasty 为错误类型（如布尔 true）-> 必须抛错
        val badDynastyTypeJson = JSONObject("""
            {
                "id": "bad_002",
                "title": "错误朝代类型",
                "dynasty": true,
                "genre": "poem",
                "content": "正文"
            }
        """.trimIndent())
        try {
            LocalJsonWorkRepository.parseWorkFromJson(badDynastyTypeJson, 3)
            fail("dynasty 字段类型错误必须抛出 IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("ID: bad_002") == true)
            assertTrue(e.message?.contains("dynasty") == true)
            assertNotNull(e.cause)
        }
    }

    // -------------------------------------------------------------------------
    // 4. 核心必填字段缺失与字段类型错误拦截
    // -------------------------------------------------------------------------

    @Test
    fun testRequiredFieldsMissing_throwsException() {
        val missingTitleJson = JSONObject("""
            {
                "id": "tang_000001",
                "author": "李白",
                "dynasty": "唐",
                "genre": "poem",
                "content": "床前明月光"
            }
        """.trimIndent())

        try {
            LocalJsonWorkRepository.parseWorkFromJson(missingTitleJson, 3)
            fail("缺少 title 必须抛出 IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("ID: tang_000001") == true)
            assertTrue(e.message?.contains("title") == true)
            assertNotNull("必须保留原始 cause", e.cause)
        }
    }

    @Test
    fun testJsonTypeMismatch_throwsException() {
        val badTextSourceJson = JSONObject("""
            {
                "id": "tang_000001",
                "title": "静夜思",
                "author": "李白",
                "dynasty": "唐",
                "genre": "poem",
                "content": "床前明月光",
                "textSource": "不是一个JSON对象"
            }
        """.trimIndent())

        try {
            LocalJsonWorkRepository.parseWorkFromJson(badTextSourceJson, 0)
            fail("textSource 为非对象类型必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("textSource") == true)
            assertNotNull(e.cause)
        }
    }

    // -------------------------------------------------------------------------
    // 5. 受控枚举非法编码拦截
    // -------------------------------------------------------------------------

    @Test
    fun testStrictEnumParsing_genre_layout_licenseType() {
        // genre 非法编码
        val badGenreJson = JSONObject("""
            {
                "id": "err_genre",
                "title": "错误体裁",
                "genre": "novel",
                "content": "正文"
            }
        """.trimIndent())
        try {
            LocalJsonWorkRepository.parseWorkFromJson(badGenreJson, 0)
            fail("未知体裁编码 'novel' 必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("未知的体裁编码") == true)
        }

        // layout 七种边界情况全覆盖测试（缺失、null、合法值、空字符串、纯空白、非法编码、数字类型）
        // 1. 缺失 -> null
        val missingLayoutJson = JSONObject("""{"id":"l1","title":"t","genre":"poem","content":"c"}""")
        assertNull(LocalJsonWorkRepository.parseWorkFromJson(missingLayoutJson, 0).layout)

        // 2. 显式 null -> null
        val nullLayoutJson = JSONObject("""{"id":"l2","title":"t","genre":"poem","content":"c","layout":null}""")
        assertNull(LocalJsonWorkRepository.parseWorkFromJson(nullLayoutJson, 0).layout)

        // 3. 合法值 -> 正常解析
        val validLayoutJson = JSONObject("""{"id":"l3","title":"t","genre":"poem","content":"c","layout":"five_jueju"}""")
        assertEquals(LayoutType.FIVE_JUEJU, LocalJsonWorkRepository.parseWorkFromJson(validLayoutJson, 0).layout)

        // 4. 空字符串 -> 报错
        val emptyLayoutJson = JSONObject("""{"id":"l4","title":"t","genre":"poem","content":"c","layout":""}""")
        try {
            LocalJsonWorkRepository.parseWorkFromJson(emptyLayoutJson, 0)
            fail("layout 为空字符串必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("layout") == true)
        }

        // 5. 纯空白字符串 -> 报错
        val blankLayoutJson = JSONObject("""{"id":"l5","title":"t","genre":"poem","content":"c","layout":"   "}""")
        try {
            LocalJsonWorkRepository.parseWorkFromJson(blankLayoutJson, 0)
            fail("layout 为纯空白必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("layout") == true)
        }

        // 6. 非法编码 -> 报错
        val invalidLayoutJson = JSONObject("""{"id":"l6","title":"t","genre":"poem","content":"c","layout":"irregular_unknown"}""")
        try {
            LocalJsonWorkRepository.parseWorkFromJson(invalidLayoutJson, 0)
            fail("layout 为未知编码必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("未知的排版编码") == true)
        }

        // 7. 数字等非字符串类型 -> 报错
        val numberLayoutJson = JSONObject("""{"id":"l7","title":"t","genre":"poem","content":"c","layout":12345}""")
        try {
            LocalJsonWorkRepository.parseWorkFromJson(numberLayoutJson, 0)
            fail("layout 为数字类型必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("layout") == true)
            assertTrue(e.cause?.message?.contains("字符串类型") == true)
        }

        // licenseType 非法编码
        val badLicenseJson = JSONObject("""
            {
                "id": "err_lic",
                "title": "错误授权",
                "genre": "poem",
                "content": "正文",
                "textSource": {
                    "sourceName": "底本",
                    "licenseType": "INVALID_LICENSE"
                }
            }
        """.trimIndent())
        try {
            LocalJsonWorkRepository.parseWorkFromJson(badLicenseJson, 0)
            fail("未知授权类型编码必须报错")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("未知的授权类型编码") == true)
        }
    }

    // -------------------------------------------------------------------------
    // 6. 数组元素非 JSONObject 异常定位测试
    // -------------------------------------------------------------------------

    @Test
    fun testParseWorkListFromJsonArray_nonJsonObjectElement_throwsDetailedException() {
        val jsonArray = JSONArray("""
            [
                {
                    "id": "work_1",
                    "title": "第一首",
                    "genre": "poem",
                    "content": "白日依山尽"
                },
                "非JSON对象的损坏字符串元素"
            ]
        """.trimIndent())

        try {
            LocalJsonWorkRepository.parseWorkListFromJsonArray(jsonArray)
            fail("数组元素为字符串时必须抛出 IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue("错误信息必须包含数组序号 1", e.message?.contains("序号: 1") == true)
            assertTrue("错误信息必须标记 ID: 未知", e.message?.contains("ID: 未知") == true)
            assertTrue("错误信息必须标记 标题: 未知", e.message?.contains("标题: 未知") == true)
            assertNotNull("必须保留原始 cause", e.cause)
            assertTrue(e.cause is IllegalArgumentException)
        }
    }

    // -------------------------------------------------------------------------
    // 7. 新版规范 JSON 完整字段反序列化
    // -------------------------------------------------------------------------

    @Test
    fun testNewSchemaJson_fullParsing() {
        val fullJson = JSONObject("""
            {
                "id": "work_000001",
                "title": "水调歌头·明月几时有",
                "cipai": "水调歌头",
                "subtitle": "明月几时有",
                "author": "苏轼",
                "dynasty": "宋",
                "genre": "ci",
                "layout": "ci_double",
                "preface": "丙辰中秋，欢饮达旦，大醉，作此篇，兼怀子由。",
                "content": "明月几时有？把酒问青天。",
                "excerpt": "明月几时有？把酒问青天。",
                "textSource": {
                    "sourceName": "宋刻东坡乐府",
                    "licenseType": "PUBLIC_DOMAIN",
                    "referenceUrl": "https://example.org",
                    "licenseNote": "公有领域底本"
                },
                "attribution": {
                    "status": "CONFIRMED",
                    "candidateAuthors": [],
                    "note": "苏轼定论名作",
                    "academicSources": ["《宋史·苏轼传》"]
                },
                "auxiliary": {
                    "translation": "明月从什么时候开始有的？举起酒杯问青天。",
                    "translationSource": {
                        "sourceName": "测试公版译本",
                        "licenseType": "PUBLIC_DOMAIN"
                    },
                    "annotations": [
                        {
                            "index": 1,
                            "quote": "把酒",
                            "note": "端起酒杯",
                            "targetLine": 0
                        }
                    ],
                    "annotationSource": {
                        "sourceName": "测试词汇解",
                        "licenseType": "PUBLIC_DOMAIN"
                    }
                }
            }
        """.trimIndent())

        val work = LocalJsonWorkRepository.parseWorkFromJson(fullJson, 0)
        assertEquals("work_000001", work.id)
        assertEquals("水调歌头", work.cipai)
        assertEquals("明月几时有", work.subtitle)
        assertEquals(Genre.CI, work.genre)
        assertEquals(LayoutType.CI_DOUBLE, work.layout)
        assertEquals("丙辰中秋，欢饮达旦，大醉，作此篇，兼怀子由。", work.preface)

        assertNotNull(work.textSource)
        assertEquals("宋刻东坡乐府", work.textSource?.sourceName)
        assertEquals(LicenseType.PUBLIC_DOMAIN, work.textSource?.licenseType)

        assertNotNull(work.attribution)
        assertEquals(AttributionStatus.CONFIRMED, work.attribution?.status)
        assertEquals(listOf("《宋史·苏轼传》"), work.attribution?.academicSources)

        assertNotNull(work.auxiliary)
        assertEquals("明月从什么时候开始有的？举起酒杯问青天。", work.auxiliary?.translation)
        assertEquals("测试公版译本", work.auxiliary?.translationSource?.sourceName)
        assertEquals(1, work.auxiliary?.annotations?.size)
        assertEquals("把酒", work.auxiliary?.annotations?.first()?.quote)
    }

    // -------------------------------------------------------------------------
    // 8. 真实 Repository 从 assets 读取 sample_works.json
    // -------------------------------------------------------------------------

    @Test
    fun testRepository_loadsSampleWorksSuccessfully() = runBlocking {
        val repository = LocalJsonWorkRepository(context, prefsName = testPrefsName)
        val allWorks = repository.getAllWorks()

        assertEquals("sample_works.json 包含 6 首作品", 6, allWorks.size)

        val jingYeSi = repository.getWorkById("tang_000001")
        assertNotNull(jingYeSi)
        assertEquals("静夜思", jingYeSi?.title)
        assertEquals(Genre.POEM, jingYeSi?.genre)
        assertEquals("诗", jingYeSi?.genre?.displayName)

        val poems = repository.getWorksByGenre("poem")
        assertEquals(3, poems.size)

        val cis = repository.getWorksByGenre("ci")
        assertEquals(3, cis.size)

        val searchResults = repository.searchWorks("明月")
        assertTrue(searchResults.any { it.title == "静夜思" })
        assertTrue(searchResults.any { it.title.contains("水调歌头") })
    }

    // -------------------------------------------------------------------------
    // 9. 真实 SharedPreferences 收藏持久化、最新居首与未映射历史 ID 完整性验证
    // -------------------------------------------------------------------------

    @Test
    fun testRepository_favoritesPersistence_orderAndUnmappedIdsPreserved() = runBlocking {
        // 先在隔离测试偏好中模拟历史遗留 ID（包括一个当前作品集中不存在的 unmapped_999）
        val initialArray = JSONArray().apply {
            put("unmapped_999")
            put("tang_000001")
        }
        context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("favorite_ids", initialArray.toString())
            .commit()

        // 初始化 Repository 实例（连接独立测试偏好）
        val repository = LocalJsonWorkRepository(context, prefsName = testPrefsName)

        // 验证 getFavoriteWorks() 只渲染当前数据集中真实存在的作品，但绝不删除未映射的 ID
        val favoriteWorks = repository.getFavoriteWorks()
        assertEquals(1, favoriteWorks.size)
        assertEquals("tang_000001", favoriteWorks[0].id)
        assertTrue(repository.favoriteIdsFlow.value.contains("unmapped_999"))

        // 新增收藏：必须排在最前面（最新收藏居首）
        val added = repository.toggleFavorite("song_000001")
        assertTrue(added)

        // 重新从 SharedPreferences 读取持久化数据，验证顺序与未映射 ID 的完整保留
        val savedPrefsJson = context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE)
            .getString("favorite_ids", null)
        assertNotNull(savedPrefsJson)

        val savedArray = JSONArray(savedPrefsJson)
        assertEquals(3, savedArray.length())
        assertEquals("最新收藏必须排在首位", "song_000001", savedArray.getString(0))
        assertEquals("历史未映射 ID 必须原样完整保留", "unmapped_999", savedArray.getString(1))
        assertEquals("较早收藏排在后面", "tang_000001", savedArray.getString(2))

        // 验证新建实例仍能精准还原状态
        val repositoryReloaded = LocalJsonWorkRepository(context, prefsName = testPrefsName)
        assertEquals(3, repositoryReloaded.favoriteIdsFlow.value.size)
    }
}
