package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 标准 Android Library 约定：Android Library → Kotlin Android → Base。
 * 统一 api 暴露 coroutines 与 core-ktx，所有 Library 模块自动获得，模块脚本无需重复声明。
 */
class TaskFlowAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
            apply("taskflow.android.base")
        }
        val catalog = project.libsCatalog()
        project.dependencies {
            add("api", catalog.findLibrary("kotlinx-coroutines-core").get())
            add("api", catalog.findLibrary("kotlinx-coroutines-android").get())
            add("api", catalog.findLibrary("androidx-core-ktx").get())
        }
        // 骨架阶段不引入测试依赖；接入时调用 configureTaskFlowTestDependenciesPlaceholder() 并取消注释
        // project.configureTaskFlowTestDependenciesPlaceholder()
    }
}
