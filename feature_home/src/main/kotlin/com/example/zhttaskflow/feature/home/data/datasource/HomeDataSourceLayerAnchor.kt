package com.example.zhttaskflow.feature.home.data.datasource

/**
 * 首页模块 **数据源层**（data.datasource）。
 *
 * ## 扩展方向
 * - **remote**：Retrofit API、远程 DTO 拉取（如 Banner、公告接口）。
 * - **local**：Room/DataStore 等本地缓存（入口配置、离线展示数据）。
 *
 * ## 依赖约束（防腐）
 * - 仅实现 IO 与协议细节，不暴露给 `presentation`。
 * - 禁止被 `domain` 直接依赖；数据经 `data.repository` 实现汇总后，通过 [com.example.zhttaskflow.feature.home.domain.HomeRepository] 对外提供领域语义。
 * - 网络/数据库运行时能力由 `component_core` 统一提供；本模块若仅使用 Retrofit 注解，在 `build.gradle.kts` 使用 `compileOnly`，禁止 `implementation` 引入运行时网络/DB 依赖。
 */
internal object HomeDataSourceLayerAnchor
