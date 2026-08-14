package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 基础设施约定插件：复用 [TaskFlowAndroidLibraryPlugin]，在 **component_core** 内装配第三方能力。
 *
 * ## 依赖隔离（防腐层）
 * - **api**：仅 [component_base]（基础能力向上传递）
 * - **implementation**：Retrofit、OkHttp、Room、DataStore、Coil 等具体技术栈，**不向 feature / app 透传**
 *
 * 业务模块仅通过 core 包下的封装类访问能力（如 [com.example.zhttaskflow.core.network.ApiResult]、
 * [com.example.zhttaskflow.core.network.safeApiCall]、[com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate]），
 * 禁止直接 import 第三方库。
 *
 * ## Room KSP
 * 本模块若接入 `@Database`，由 [configureTaskFlowRoomKsp] 统一注入 KSP；业务 Feature 由 [TaskFlowAndroidFeaturePlugin] 注入。
 */
class TaskFlowAndroidCorePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("taskFlow.android.library")
        project.injectCoreDependencies()
        project.configureTaskFlowRoomKsp()
    }

    private fun Project.injectCoreDependencies() {
        val catalog = libsCatalog()
        dependencies {
            add("api", project(":component_base"))

            add("implementation", catalog.findLibrary("retrofit").get())
            add("implementation", catalog.findLibrary("retrofit-converter-gson").get())
            add("implementation", catalog.findLibrary("okhttp").get())
            add("implementation", catalog.findLibrary("okhttp-logging").get())
            add("implementation", catalog.findLibrary("room-runtime").get())
            add("implementation", catalog.findLibrary("room-ktx").get())
            add("implementation", catalog.findLibrary("androidx-datastore-preferences").get())
            add("implementation", catalog.findLibrary("coil-compose").get())
        }
    }
}
