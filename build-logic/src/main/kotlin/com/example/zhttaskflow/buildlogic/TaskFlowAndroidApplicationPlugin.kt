package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/** App 壳约定：Application → Kotlin Android → Kotlin Compose → Common（Compose 配置与依赖由 Common 统一收口）。 */
class TaskFlowAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("taskFlow.android.common")
        }
    }
}
