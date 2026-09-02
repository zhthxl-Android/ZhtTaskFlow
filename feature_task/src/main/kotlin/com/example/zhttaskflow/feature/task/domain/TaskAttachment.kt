package com.example.zhttaskflow.feature.task.domain

/**
 * 任务附件领域模型（示范：内存元数据，不承载真实文件 IO）。
 *
 * @param id 附件唯一标识
 * @param displayName 展示文件名
 * @param sizeBytes 文件大小（字节）
 * @param mimeType MIME 类型，用于 UI 图标或打开方式提示
 */
data class TaskAttachment(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
)
