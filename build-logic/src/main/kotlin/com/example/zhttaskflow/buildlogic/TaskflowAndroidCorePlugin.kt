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
 * [com.example.zhttaskflow.core.network.safeApiCall]），禁止直接 import 第三方库。
 *
 * schemas/（KSP + Room 接入后）：`component_core/schemas/`，由 `room.schemaLocation` 指向。
 */
class TaskFlowAndroidCorePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("taskFlow.android.library")
        project.injectCoreDependencies()
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
        // 接入 KSP + Room 编译器后启用（版本写入 libs.versions.toml）：
        // project.pluginManager.apply("com.google.devtools.ksp")
        // project.dependencies.add("ksp", catalog.findLibrary("room-compiler").get())
        // project.extensions.configure(KspExtension::class.java) {
        //     arg("room.schemaLocation", "${project.projectDir}/schemas")
        // }
    }
}
