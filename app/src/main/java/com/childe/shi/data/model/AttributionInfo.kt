package com.childe.shi.data.model

/**
 * 作者归属考订结论状态
 */
enum class AttributionStatus {
    CONFIRMED,
    DISPUTED,
    DOUBTFUL,
    ANONYMOUS;

    companion object {
        /**
         * 严格解析：遇到未定义状态编码必须抛出明确异常，禁止静默回退
         */
        fun fromCode(code: String): AttributionStatus {
            val normalized = code.trim().uppercase()
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("未知的归属状态编码: '$code'，合法编码为: ${entries.map { it.name }}")
        }

        fun fromCodeOrNull(code: String?): AttributionStatus? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().uppercase()
            return entries.firstOrNull { it.name == normalized }
        }
    }
}

/**
 * 作者归属考订实体
 *
 * @property status 考订状态枚举
 * @property candidateAuthors 其他候选作者列表
 * @property note 考订与分歧简要说明
 * @property academicSources 考证所依托的学术文献列表（允许多个学术参考来源）
 */
data class AttributionInfo(
    val status: AttributionStatus,
    val candidateAuthors: List<String> = emptyList(),
    val note: String,
    val academicSources: List<String> = emptyList()
)
