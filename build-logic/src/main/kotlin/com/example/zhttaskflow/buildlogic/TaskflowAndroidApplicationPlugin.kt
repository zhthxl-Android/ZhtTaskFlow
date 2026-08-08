package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Android Application 约定插件：组装 Application → Kotlin Android → Common，并注入 Application 类型专属依赖。
 *
 * 通用基础依赖与 Compose 栈由 [TaskFlowAndroidCommonPlugin] 以 api 注入；
 * [androidx.activity:activity-ktx] 以 implementation 注入，供 app 宿主与 Feature standalone 模块使用，不向上传递。
 */
class TaskFlowAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("taskFlow.android.common")
        }
        project.injectApplicationTypeDependencies()
    }

    private fun Project.injectApplicationTypeDependencies() {
        val catalog = libsCatalog()
        dependencies {
            add("implementation", catalog.findLibrary("androidx-activity-ktx").get())
        }
    }
}
