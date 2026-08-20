package com.example.zhttaskflow.feature.home.data.repository

/**
 * 首页模块 **仓库实现层**（data.repository）。
 *
 * ## 扩展方向
 * 在此包实现 [com.example.zhttaskflow.feature.home.domain.HomeRepository]（如 `HomeRepositoryImpl`），
 * 组合 [com.example.zhttaskflow.feature.home.data.datasource] 下 Remote/Local 数据源并完成 Entity/DTO 映射。
 *
 * ## 依赖约束（防腐）
 * - 实现 `domain` 层 Repository 接口，依赖 `data.datasource` 与 Mapper，不反向依赖 `presentation`。
 * - ViewModel 仅通过 UseCase 访问 Repository 抽象，禁止直连本包实现类。
 * - 调用链：**表现层 → UseCase → Repository 接口 → 本包实现 → DataSource**。
 */
internal object HomeRepositoryLayerAnchor
