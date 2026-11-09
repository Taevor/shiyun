package com.shi.yun.data.model

/**
 * 诗词作品实体模型
 *
 * 升级至阶段四规范：引入强类型受控枚举、排版标识、结构化底本来源、作者归属争议及辅助内容。
 * 同时保留 source、sourceId、licenseNote 等旧字段以保证向后兼容性。
 *
 * @property id 稳定唯一 ID（如 tang_000001, song_000001, work_000001）
 * @property title 作品标准标题（如 "静夜思"）
 * @property author 署名作者姓名（如 "李白"，允许为空字符串）
 * @property dynasty 朝代（如 "唐"、"宋"，允许为空字符串）
 * @property genre 体裁受控枚举（如 Genre.POEM, Genre.CI）
 * @property content 采用的唯一权威通行本完整原文（严格保留换行 \n 与标点符号）
 * @property excerpt 首页展示的推荐短句（出自该作品正文，保留完整句意）
 * @property cipai 词牌名/曲牌名（可选，如 "水调歌头"，非词曲作品为 null）
 * @property subtitle 词题或子标题（可选，如 "明月几时有"）
 * @property layout 文学排版指引枚举（可选，如 LayoutType.FIVE_JUEJU, LayoutType.CI_DOUBLE）
 * @property preface 词前小序或题记（可选，如 "丙辰中秋，欢饮达旦，大醉，作此篇，兼怀子由。"）
 * @property textSource 规范底本来源与版权状态实体（可选）
 * @property attribution 作者归属争议考订实体（可选，普通作品恒为 null）
 * @property auxiliary 辅助内容集合（可选，含白话译文、条目注释、文学赏析、创作背景及其来源）
 * @property source 旧数据来源标识（向后兼容保留）
 * @property sourceId 旧原始数据源中的追溯 ID（向后兼容保留）
 * @property licenseNote 旧许可与版权核查备注（向后兼容保留）
 */
data class Work(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val genre: Genre,
    val content: String,
    val excerpt: String,
    val cipai: String? = null,
    val subtitle: String? = null,
    val layout: LayoutType? = null,
    val preface: String? = null,
    val textSource: SourceInfo? = null,
    val attribution: AttributionInfo? = null,
    val auxiliary: AuxiliaryContent? = null,
    val source: String = "",
    val sourceId: String = "",
    val licenseNote: String = "",
)

/**
 * 格式化输出朝代与作者信息，避免在朝代或作者为空时渲染多余的空括号或悬挂标点
 */
fun Work.formatAuthorDynasty(): String {
    val d = dynasty.trim()
    val a = author.trim()
    return when {
        d.isNotEmpty() && a.isNotEmpty() -> "〔$d〕$a"
        a.isNotEmpty() -> a
        d.isNotEmpty() -> "〔$d〕"
        else -> ""
    }
}

/**
 * 格式化输出朝代、作者与体裁信息，供作品列表项与详情头部统一使用
 */
fun Work.formatAuthorDynastyGenre(): String {
    val authorDynasty = formatAuthorDynasty()
    return if (authorDynasty.isNotEmpty()) {
        "$authorDynasty  ·  ${genre.displayName}"
    } else {
        genre.displayName
    }
}
