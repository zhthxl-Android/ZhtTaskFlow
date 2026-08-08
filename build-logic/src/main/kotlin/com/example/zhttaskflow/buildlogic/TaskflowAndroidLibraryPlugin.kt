package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 标准 Android Library 约定：Library → Kotlin Android → Kotlin Compose → Common。
 *
 * 统一 api 暴露协程、core-ktx；Compose 编译能力与 BOM/UI/M3/lifecycle-compose 由 Common 插件注入。
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
    }
}
