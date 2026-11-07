package com.childe.shi.data.model

/**
 * 选本定义实体
 *
 * @property id 选本稳定唯一 ID，如 "anth_ts300", "anth_sc300"
 * @property code 选本代号，如 "ts300", "sc300"
 * @property name 选本全称，如 "《唐诗三百首》"
 * @property compiler 编选者，如 "[清] 蘅塘退士"
 * @property dynasty 编选朝代
 * @property targetCount 目标篇目数量，如 313 或 300
 * @property editionNote 采用的具体底本与核验说明
 */
data class Anthology(
    val id: String,
    val code: String,
    val name: String,
    val compiler: String,
    val dynasty: String,
    val targetCount: Int,
    val editionNote: String
)

/**
 * 选本篇目条目实体（解耦作品与选本顺序）
 *
 * @property id 篇目项唯一 ID，如 "item_000001"
 * @property anthologyId 关联选本 ID
 * @property workId 关联内部作品 ID
 * @property orderIndex 原书篇目先后序数（1..N）
 * @property volume 原书分卷/体裁分类，如 "卷一·五言古诗"
 */
data class AnthologyItem(
    val id: String,
    val anthologyId: String,
    val workId: String,
    val orderIndex: Int,
    val volume: String
)
