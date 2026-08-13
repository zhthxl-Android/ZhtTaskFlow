package com.example.zhttaskflow.core.persistence.room

/**
 * Room 数据库配置，由 Feature 在业务阶段提供数据库名与 schema 导出路径。
 */
data class TaskFlowRoomConfig(
    val databaseName: String,
    val schemaExportPath: String? = null,
)

/**
 * 通用 DAO 标记接口，Feature 层 DAO 可继承并补充 Room 注解。
 */
interface BaseRoomDao
