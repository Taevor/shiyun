package com.shi.yun.ui.favorite

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 收藏空状态每日轮播管理器单元测试
 */
class FavoriteEmptyQuoteManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
    }

    @Test
    fun testQuotesAndAuxiliaryTextIntegrity() {
        assertEquals(4, FavoriteEmptyQuoteManager.QUOTES.size)
        assertEquals("还没遇见让你心动的那一句吗？", FavoriteEmptyQuoteManager.QUOTES[0])
        assertEquals("喜欢的诗句，还在等你发现。", FavoriteEmptyQuoteManager.QUOTES[1])
        assertEquals("这里还空着，等一句诗来填满。", FavoriteEmptyQuoteManager.QUOTES[2])
        assertEquals("读过千百句，总有一句值得珍藏。", FavoriteEmptyQuoteManager.QUOTES[3])

        assertEquals(
            "去作品阅读页面，点击右上角红心或双击页面，收藏喜欢的作品。",
            FavoriteEmptyQuoteManager.AUXILIARY_TEXT,
        )
        assertEquals(
            "点击右上角红心或双击页面，收藏作品",
            FavoriteEmptyQuoteManager.FIRST_TIME_FAVORITE_HINT,
        )
    }

    @Test
    fun testFirstEverDisplayShowsQuote0() {
        val today = "2026-11-09"
        val quote = FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, today)

        assertEquals(FavoriteEmptyQuoteManager.QUOTES[0], quote)
        assertEquals(today, fakePrefs.getString(FavoriteEmptyQuoteManager.KEY_LAST_DATE, null))
        assertEquals(0, fakePrefs.getInt(FavoriteEmptyQuoteManager.KEY_QUOTE_INDEX, -1))
    }

    @Test
    fun testSameDayRepeatedAccessReturnsSameQuote() {
        val today = "2026-11-09"
        // 第一次调用初始化
        val firstCall = FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, today)
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[0], firstCall)

        // 同一天内进出多次
        for (i in 1..10) {
            val repeatedCall = FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, today)
            assertEquals("Same day call #$i must return quote 0", FavoriteEmptyQuoteManager.QUOTES[0], repeatedCall)
        }
    }

    @Test
    fun testNextDayAdvancesToNextQuote() {
        // 第一天
        val day1 = "2026-11-09"
        assertEquals(
            FavoriteEmptyQuoteManager.QUOTES[0],
            FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, day1),
        )

        // 第二天：自然日变更，顺延至下一句
        val day2 = "2026-11-10"
        assertEquals(
            FavoriteEmptyQuoteManager.QUOTES[1],
            FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, day2),
        )

        // 第二天内多次调用保持不变
        assertEquals(
            FavoriteEmptyQuoteManager.QUOTES[1],
            FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, day2),
        )
    }

    @Test
    fun testFourDaysCycleBackToQuote0() {
        val dates = listOf("2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13")

        // 0 -> 1 -> 2 -> 3 -> 0 循环
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[0], FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, dates[0]))
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[1], FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, dates[1]))
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[2], FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, dates[2]))
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[3], FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, dates[3]))
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[0], FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, dates[4]))
    }

    @Test
    fun testMultiDayGapAdvancesToNextQuoteWithoutReset() {
        val day1 = "2026-11-09"
        FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, day1)

        // 跨越 10 天未打开，下一次打开顺延到下一句
        val dayLater = "2026-11-19"
        val quote = FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, dayLater)
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[1], quote)
    }

    @Test
    fun testReEmptyOnSameDayMaintainsSameQuote() {
        val today = "2026-11-09"
        // 初始为空显示第一句
        FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, today)

        // 模拟用户收藏了诗词后又取消收藏，同一天再次变为空
        val reEmptyQuote = FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, today)
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[0], reEmptyQuote)
    }

    @Test
    fun testCorruptedIndexRecoversSafely() {
        fakePrefs.edit()
            .putString(FavoriteEmptyQuoteManager.KEY_LAST_DATE, "2026-11-09")
            .putInt(FavoriteEmptyQuoteManager.KEY_QUOTE_INDEX, 999)
            .apply()

        // 异常越界索引安全恢复为 0
        val quote = FavoriteEmptyQuoteManager.getOrUpdateDailyQuote(fakePrefs, "2026-11-09")
        assertEquals(FavoriteEmptyQuoteManager.QUOTES[0], quote)
    }
}

/**
 * 内存轻量测试用 FakeSharedPreferences 实现
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        (data[key] as? String) ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (data[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (data[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (data[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val parent: FakeSharedPreferences) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = this
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) {
                parent.data.clear()
            }
            temp.forEach { (k, v) ->
                if (v === this) {
                    parent.data.remove(k)
                } else {
                    parent.data[k] = v
                }
            }
        }
    }
}
