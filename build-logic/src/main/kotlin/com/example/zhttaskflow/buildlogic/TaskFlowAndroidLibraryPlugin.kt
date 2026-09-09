package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 标准 Android Library 约定：Library → Kotlin Android → Kotlin Compose → Common。
 *
 * 统一 api 暴露协程、core-ktx；Compose 编译能力与 BOM/UI/M3/lifecycle-compose 由 Common 插件注入。
 * [component_base] 通过 api 透出 material-icons-extended（全工程唯一声明点，供 [AppIcons] 及下游使用）。
 */
class TaskFlowAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("taskFlow.android.common")
        }
        val catalog = project.libsCatalog()
        project.dependencies {
            add("api", catalog.findLibrary("kotlinx-coroutines-core").get())
            add("api", catalog.findLibrary("kotlinx-coroutines-android").get())
            add("api", catalog.findLibrary("androidx-core-ktx").get())
        }
        if (project.path == ":component_base") {
            project.dependencies {
                add(
                    "api",
                    catalog.findLibrary("androidx-compose-material-icons-extended").get(),
                )
            }
        }
    }
}
