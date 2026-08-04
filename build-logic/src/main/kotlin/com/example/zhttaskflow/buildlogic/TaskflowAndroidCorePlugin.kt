package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 基础设施约定：复用 taskFlow.android.library，叠加 Retrofit/Room/OkHttp。
 *
 * schemas/ 目录（KSP+Room 接入后）：
 * - 路径：component_core/schemas/，由 room.schemaLocation 指向
 * - 规则：Entity/@Database 变更时编译器自动生成/更新 JSON；须纳入 Git 做迁移评审
 * - 版本：room-compiler 版本仅来自 libs.versions.toml，与 room-runtime 对齐
 */
class TaskFlowAndroidCorePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("taskFlow.android.library")
        val catalog = project.libsCatalog()
        project.dependencies {
            add(
                "api",
                project.project(":component_base")
            )
            add(
                "api",
                catalog.findLibrary("retrofit").get()
            )
            add(
                "api",
                catalog.findLibrary("room-runtime").get()
            )
            add(
                "api",
                catalog.findLibrary("room-ktx").get()
            )
            add(
                "implementation",
                catalog.findLibrary("okhttp").get()
            )
            add(
                "implementation",
                catalog.findLibrary("okhttp-logging").get()
            )
        }
        // 接入 KSP + Room 编译器后启用（版本写入 libs.versions.toml）：
        // project.pluginManager.apply("com.google.devtools.ksp")
        // project.dependencies.add("ksp", catalog.findLibrary("room-compiler").get())
        // project.extensions.configure(KspExtension::class.java) {
        //     arg("room.schemaLocation", "${project.projectDir}/schemas")
        // }
    }
}
