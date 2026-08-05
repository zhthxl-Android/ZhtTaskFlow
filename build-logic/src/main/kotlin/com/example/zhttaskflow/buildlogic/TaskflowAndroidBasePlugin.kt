package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 基础约定插件：创建 taskFlow 扩展；在 Android 插件就绪后配置通用项。
 * Feature 的 library 模式勿重复 apply 本插件（taskFlow.android.library 已内含 base）。
 */
class TaskFlowAndroidBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.extensions.findByName("taskFlow") == null) {
            project.extensions.create("taskFlow", TaskFlowExtension::class.java).apply {
                resourcePrefix.convention(project.computeDefaultResourcePrefix())
            }
        }
        val configureAction = {
            project.configureAndroidCommon()
            project.configureKotlinAndroid()
        }
        project.pluginManager.withPlugin("com.android.application") { configureAction() }
        project.pluginManager.withPlugin("com.android.library") { configureAction() }
    }
}
