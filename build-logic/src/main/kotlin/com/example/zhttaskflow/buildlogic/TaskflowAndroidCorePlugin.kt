package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 基础设施约定插件：复用 [TaskFlowAndroidLibraryPlugin]，在模块内装配网络、存储与图片能力。
 *
 * 依赖隔离：
 * - **api**：仅 [component_base]（基础能力向上传递）
 * - **implementation**：Retrofit / OkHttp / Room / DataStore / Coil 等具体技术栈，不向业务层透传
 * - 协程、core-ktx 等由 [TaskFlowAndroidCommonPlugin] 以 api 注入，本插件不重复声明
 *
 * schemas/ 目录在接入 KSP + Room 后由 room.schemaLocation 指向 component_core/schemas/。
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
    }
}
