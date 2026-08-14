package com.example.zhttaskflow.feature.article.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 文章 Feature 本地数据库 **声明**（[@Database]）。
 *
 * **防腐说明**：
 * - 本类仅用于 KSP/Room 编译期生成代码，禁止在业务代码中 `ArticleDatabase()` 或持有实例；
 * - 实例创建与缓存由 [com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate] 管理；
 * - DAO 通过 [TaskFlowRoomTemplate.openDao] 获取。
 */
@Database(
    entities = [
        ArticleEntity::class,
        ArticlePageMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ArticleDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
}
