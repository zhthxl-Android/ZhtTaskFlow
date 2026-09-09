package com.example.zhttaskflow.observability

/**
 * 应用壳对 [com.example.zhttaskflow.core.observability.LocalLogStore] 的类型别名，
 * 保持历史引用与 Release 校验类名路径稳定。
 *
 * 分页与侧车索引 API 见 core 模块：
 * - [com.example.zhttaskflow.core.observability.LocalLogStore.queryPaged]
 * - [com.example.zhttaskflow.core.observability.LocalLogStore.PagedQueryFilter]
 */
typealias LocalLogStore = com.example.zhttaskflow.core.observability.LocalLogStore
