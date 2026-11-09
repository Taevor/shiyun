package com.shi.yun.ui.favorite

import android.content.SharedPreferences
import java.time.LocalDate

/**
 * 收藏页面空白提示文案每日轮播管理
 *
 * 核心设计规则：
 * 1. 四句话按序循环轮换：
 *    - 0: 「还没遇见让你心动的那一句吗？」
 *    - 1: 「喜欢的诗句，还在等你发现。」
 *    - 2: 「这里还空着，等一句诗来填满。」
 *    - 3: 「读过千百句，总有一句值得珍藏。」
 * 2. 首次进入或首次为空时使用第一句（索引 0）；
 * 3. 同一自然日内，无论用户进出收藏页面多少次，均显示同一句；
 * 4. 日期变化（新自然日首次打开收藏页）时，顺延至下一句；
 * 5. 四句话循环播放，避免连续两天相同；
 * 6. 使用本地 SharedPreferences 持久化存储上次更新日期和文案序号，零外部依赖；
 * 7. 若收藏列表曾有内容后来再次变为空，展示当天对应文案，不重新随机选择或重置轮播顺序。
 */
object FavoriteEmptyQuoteManager {

    val QUOTES = listOf(
        "还没遇见让你心动的那一句吗？",
        "喜欢的诗句，还在等你发现。",
        "这里还空着，等一句诗来填满。",
        "读过千百句，总有一句值得珍藏。",
    )

    const val AUXILIARY_TEXT = "去作品阅读页面，点击右上角红心或双击页面，收藏喜欢的作品。"
    const val FIRST_TIME_FAVORITE_HINT = "点击右上角红心或双击页面，收藏作品"

    const val PREFS_NAME = "shi_preferences"
    const val KEY_QUOTE_INDEX = "key_favorite_empty_quote_index"
    const val KEY_LAST_DATE = "key_favorite_empty_last_date"

    /**
     * 根据传入的日期和 SharedPreferences 解析或更新当天的轮播文案
     *
     * @param prefs 共享配置存储实例
     * @param today 当前自然日日期字符串（默认 yyyy-MM-dd，便于单元测试任意注入）
     * @return 当天对应的收藏空白文案
     */
    fun getOrUpdateDailyQuote(
        prefs: SharedPreferences,
        today: String = LocalDate.now().toString(),
    ): String {
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val lastIndex = prefs.getInt(KEY_QUOTE_INDEX, -1)

        val currentIndex = when {
            // 首次展示或缺少历史记录：必须展示第一句 (index 0)
            lastDate.isNullOrEmpty() || (lastIndex !in QUOTES.indices) -> 0
            // 同一自然日：无论进入退出多少次，展示同一句
            lastDate == today -> lastIndex
            // 跨入新自然日：切换到下一句，四句循环
            else -> (lastIndex + 1) % QUOTES.size
        }

        // 当日期变化或首次记录时，持久化落盘
        if (lastDate != today || lastIndex != currentIndex) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_QUOTE_INDEX, currentIndex)
                .apply()
        }

        return QUOTES[currentIndex]
    }
}
