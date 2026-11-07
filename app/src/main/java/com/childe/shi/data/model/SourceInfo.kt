package com.childe.shi.data.model

/**
 * 授权类型受控枚举
 *
 * @property code 序列化标识
 */
enum class LicenseType(val code: String) {
    PUBLIC_DOMAIN("PUBLIC_DOMAIN"),
    CC_BY_SA_4_0("CC_BY_SA_4_0"),
    AUTHORIZED("AUTHORIZED"),
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 严格解析：遇到任何未定义编码必须抛出明确异常，禁止静默降级
         */
        fun fromCode(code: String): LicenseType {
            val normalized = code.trim().uppercase()
            return entries.firstOrNull { it.code == normalized }
                ?: throw IllegalArgumentException("未知的授权类型编码: '$code'，合法编码为: ${entries.map { it.code }}")
        }

        fun fromCodeOrNull(code: String?): LicenseType? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().uppercase()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}

/**
 * 数据来源与版权声明实体
 *
 * @property sourceName 来源名称（如出版物底本或古籍整理版本），必须非空白
 * @property licenseType 授权类型受控枚举，缺省为 UNKNOWN
 * @property referenceUrl 规范链接或文献著录出处（可选）
 * @property licenseNote 权利审查补充说明（默认 "待核实"）
 */
data class SourceInfo(
    val sourceName: String,
    val licenseType: LicenseType = LicenseType.UNKNOWN,
    val referenceUrl: String? = null,
    val licenseNote: String = "待核实"
) {
    init {
        require(sourceName.isNotBlank()) { "sourceName 不能为空或纯空格" }
    }
}
