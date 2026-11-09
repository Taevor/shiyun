package com.shi.yun.data.repository

import com.shi.yun.data.model.Work
import kotlinx.coroutines.flow.StateFlow

/**
 * 诗词数据仓库接口
 *
 * 遵循规范“阶段 A：将数据放入本地 JSON 资源，建立数据模型和 Repository 接口”。
 * UI 层只面向此接口获取数据，不直接依赖底层是 JSON 文件还是未来的 Room 数据库。
 */
interface WorkRepository {
    /**
     * 获取所有作品列表
     */
    suspend fun getAllWorks(): List<Work>

    /**
     * 根据唯一 ID 查询单篇作品
     */
    suspend fun getWorkById(id: String): Work?

    /**
     * 随机获取一篇作品（供推荐流使用）
     */
    suspend fun getRandomWork(): Work?

    /**
     * 根据体裁筛选作品（如 "poem" 为诗，"ci" 为词）
     */
    suspend fun getWorksByGenre(genre: String): List<Work>

    /**
     * 根据朝代筛选作品（如 "唐"、"宋"）
     */
    suspend fun getWorksByDynasty(dynasty: String): List<Work>

    /**
     * 根据关键字本地模糊搜索作品（匹配标题、作者、正文）
     */
    suspend fun searchWorks(keyword: String): List<Work>

    /**
     * 响应式已收藏作品 ID 集合流（供 UI 快速判断是否收藏）
     */
    val favoriteIdsFlow: StateFlow<Set<String>>

    /**
     * 按照收藏时间倒序返回所有已收藏的完整作品列表
     */
    suspend fun getFavoriteWorks(): List<Work>

    /**
     * 切换收藏状态：若未收藏则添加至首位，若已收藏则移除
     *
     * @param id 目标作品的唯一 ID
     * @return 切换后的状态（true 表示已收藏，false 表示已取消收藏）
     */
    suspend fun toggleFavorite(id: String): Boolean

    /**
     * 获取所有可用的经典选本列表（如《唐诗三百首》、《宋词三百首》）
     */
    suspend fun getAnthologies(): List<com.shi.yun.data.model.Anthology>

    /**
     * 根据选本 ID 获取选本实体
     */
    suspend fun getAnthologyById(id: String): com.shi.yun.data.model.Anthology?

    /**
     * 获取指定选本内的作品列表（严格按 orderIndex 升序对齐）
     */
    suspend fun getWorksByAnthology(anthologyId: String): List<com.shi.yun.data.model.AnthologyWorkItem>
}
