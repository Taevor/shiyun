package com.childe.shi.data.repository

import com.childe.shi.data.model.Work

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
}
