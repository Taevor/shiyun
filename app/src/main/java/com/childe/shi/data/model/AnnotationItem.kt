package com.childe.shi.data.model

/**
 * 注释条目模型
 *
 * @property index 注释展示序号
 * @property quote 原文字词（原文定位与划线高亮的核心依据，严禁为空）
 * @property note 注释正文说明（严禁为空）
 * @property phonetic 生僻字注音（可选）
 * @property targetLine 辅助定位行号（0-based，仅作为辅助提示，不得作为唯一依据）
 */
data class AnnotationItem(
    val index: Int,
    val quote: String,
    val note: String,
    val phonetic: String? = null,
    val targetLine: Int? = null
) {
    init {
        require(quote.isNotBlank()) { "注释条目的 quote（原文字词）不能为空或纯空格" }
        require(note.isNotBlank()) { "注释条目的 note（解释内容）不能为空或纯空格" }
    }
}
