package com.example.zhttaskflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 路由与 Navigation Compose 基座：复用 Library 栈（已含 Common），仅叠加导航相关 api 依赖。
 */
class TaskFlowAndroidNavPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("taskFlow.android.library")
        project.injectNavDependencies()
    }

    private fun Project.injectNavDependencies() {
        val catalog = libsCatalog()
        dependencies {
            add("api", project(":component_base"))
            add("api", catalog.findLibrary("androidx-navigation-compose").get())
            add("api", catalog.findLibrary("androidx-compose-ui-tooling-preview").get())
        }
    }
}
