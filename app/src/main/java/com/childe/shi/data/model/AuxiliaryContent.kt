package com.childe.shi.data.model

/**
 * 作品辅助内容集合实体
 *
 * 规范强一致性约束：只要某种辅助内容正文不为空，就必须存在对应的 SourceInfo 来源记录。
 * 杜绝出现有译文或注释正文却没有来源记录的情况。
 */
data class AuxiliaryContent(
    val translation: String? = null,
    val translationSource: SourceInfo? = null,
    val annotations: List<AnnotationItem>? = null,
    val annotationSource: SourceInfo? = null,
    val appreciation: String? = null,
    val appreciationSource: SourceInfo? = null,
    val background: String? = null,
    val backgroundSource: SourceInfo? = null
) {
    init {
        if (!translation.isNullOrBlank()) {
            require(translationSource != null) { "存在白话译文正文，但未提供 translationSource 来源记录" }
        }
        if (!annotations.isNullOrEmpty()) {
            require(annotationSource != null) { "存在注释条目列表，但未提供 annotationSource 来源记录" }
        }
        if (!appreciation.isNullOrBlank()) {
            require(appreciationSource != null) { "存在文学赏析正文，但未提供 appreciationSource 来源记录" }
        }
        if (!background.isNullOrBlank()) {
            require(backgroundSource != null) { "存在创作背景正文，但未提供 backgroundSource 来源记录" }
        }
    }
}
