package com.childe.shi.data.model

/**
 * 经典文学排版指引枚举
 *
 * @property code 序列化排版编码
 * @property displayName 人类可读排版说明
 */
enum class LayoutType(val code: String, val displayName: String) {
    FIVE_GUSHI("five_gushi", "五言古诗"),
    SEVEN_GUSHI("seven_gushi", "七言古诗"),
    FIVE_LVSHI("five_lvshi", "五言律诗"),
    SEVEN_LVSHI("seven_lvshi", "七言律诗"),
    FIVE_JUEJU("five_jueju", "五言绝句"),
    SEVEN_JUEJU("seven_jueju", "七言绝句"),
    CI_SINGLE("ci_single", "单调词"),
    CI_DOUBLE("ci_double", "双调词"),
    PROSE("prose", "古文散文");

    companion object {
        /**
         * 严格解析：遇到未定义编码抛出明确异常，禁止静默回退
         */
        fun fromCode(code: String): LayoutType {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
                ?: throw IllegalArgumentException("未知的排版编码: '$code'，合法编码为: ${entries.map { it.code }}")
        }

        fun fromCodeOrNull(code: String?): LayoutType? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}
