package com.example.zhttaskflow.core.persistence.room

/**
 * Room 数据库配置描述，由 Feature 在业务阶段提供具体 [RoomDatabase] 子类与 Entity。
 */
data class TaskFlowRoomConfig(
    val databaseName: String,
    val schemaExportPath: String? = null,
)

/**
 * 通用 DAO 能力约定：Feature 层 DAO 可继承并补充 Room 注解。
 */
interface BaseRoomDao
