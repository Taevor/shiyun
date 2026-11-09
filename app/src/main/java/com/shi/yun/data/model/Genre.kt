package com.shi.yun.data.model

/**
 * 诗词与古典文献体裁受控枚举
 *
 * @property code 序列化标识（小写），如 "poem", "ci", "qu", "wen"
 * @property displayName 人类可读名称，如 "诗", "词", "曲", "文"
 */
enum class Genre(val code: String, val displayName: String) {
    POEM("poem", "诗"),
    CI("ci", "词"),
    QU("qu", "曲"),
    WEN("wen", "文");

    companion object {
        /**
         * 严格解析：遇到任何未定义编码必须抛出明确异常，禁止静默降级
         */
        fun fromCode(code: String): Genre {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
                ?: throw IllegalArgumentException("未知的体裁编码: '$code'，合法编码为: ${entries.map { it.code }}")
        }

        fun fromCodeOrNull(code: String?): Genre? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}
