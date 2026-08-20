package com.example.zhttaskflow.feature.home.domain

/**
 * 首页模块领域层扩展与防腐约定（模块级说明）。
 *
 * ## 标准依赖方向
 * `presentation` → `domain.usecase` → [HomeRepository]（接口）→ `data.repository` → `data.datasource`
 *
 * ## 红线
 * - `domain` 保持纯 Kotlin，无 Android/Compose/Retrofit/Room 引用。
 * - 导航仅通过 [com.example.zhttaskflow.nav.TaskFlowNavigator] 在表现层消费 [com.example.zhttaskflow.feature.home.presentation.HomeUiEffect] 完成，禁止持有 [androidx.navigation.NavHostController]。
 */
internal object HomeDomainLayerAnchor
