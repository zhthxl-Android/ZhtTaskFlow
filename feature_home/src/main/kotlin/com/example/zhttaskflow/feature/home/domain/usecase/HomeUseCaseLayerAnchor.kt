package com.example.zhttaskflow.feature.home.domain.usecase

/**
 * 首页模块 **领域用例层**（domain.usecase）。
 *
 * ## 模块定位
 * 首页入口聚合模块，承载首页业务与功能入口分发。
 *
 * ## 扩展方向
 * 在此包新增用例类（如 `LoadHomePageUseCase`、`RefreshHomeBannerUseCase`），封装可复用的首页领域编排逻辑。
 *
 * ## 依赖约束（防腐）
 * - 仅依赖本模块 `domain` 包内的实体、[com.example.zhttaskflow.feature.home.domain.HomeRepository] 等抽象接口。
 * - 禁止依赖 `data`、`presentation`、Android/Compose/网络/数据库 API。
 * - 调用链方向固定为：**表现层 → UseCase → Repository 接口 → 数据实现**。
 */
internal object HomeUseCaseLayerAnchor
