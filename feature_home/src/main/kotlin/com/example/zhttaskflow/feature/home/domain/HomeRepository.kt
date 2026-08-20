package com.example.zhttaskflow.feature.home.domain

/**
 * 首页模块 **领域层**（domain）。
 *
 * 模块定位：首页入口聚合模块，承载首页业务与功能入口分发。
 * 本包及 [usecase] 子包预留 UseCase 与 [HomeRepository] 抽象扩展；纯 Kotlin，无 Android 依赖。
 * 扩展规范见 [HomeDomainLayerAnchor] 与 [com.example.zhttaskflow.feature.home.domain.usecase.HomeUseCaseLayerAnchor]。
 */
interface HomeRepository {
    // 预留：首页聚合数据、入口配置等领域能力
}
