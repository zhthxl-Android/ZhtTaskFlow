package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Android Library 约定插件：仅组装插件链 Library → Kotlin Android → Common。
 *
 * SDK、Compose、Lint、协程、core-ktx 等均由 [TaskFlowAndroidCommonPlugin] 统一配置与注入。
 */
class TaskFlowAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
            apply("taskFlow.android.common")
        }
    }
}
