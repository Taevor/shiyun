package com.childe.shi.data.model

/**
 * 诗词作品实体模型
 *
 * 对应《古诗文阅读 App 开发规范与 Gemini 实施说明 V1.0》第 7.2 节的数据定义。
 *
 * @property id 稳定唯一 ID（如 tang_000001, song_000001）
 * @property title 作品标题（如 静夜思）
 * @property author 作者姓名（如 李白）
 * @property dynasty 朝代（如 唐、宋）
 * @property genre 体裁标识（如 "poem" 表示诗，"ci" 表示词）
 * @property content 完整原文（严格保留换行 \n 与标点符号）
 * @property excerpt 首页展示的推荐短句（必须出自该作品正文，保留完整句意）
 * @property source 数据来源标识（如 "chinese-poetry"）
 * @property sourceId 原始数据源中的追溯 ID
 * @property licenseNote 许可与版权核查备注（如 "公有领域"）
 */
data class Work(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val genre: String,
    val content: String,
    val excerpt: String,
    val source: String = "",
    val sourceId: String = "",
    val licenseNote: String = "",
)
