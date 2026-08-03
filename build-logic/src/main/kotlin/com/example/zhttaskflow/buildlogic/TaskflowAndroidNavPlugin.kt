package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 路由与 Compose UI 基座：复用 library（已含 base），再应用 Compose 插件并 api 暴露 UI 栈与 lifecycle。
 * 不重复 apply taskflow.android.base。
 */
class TaskFlowAndroidNavPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("taskflow.android.library")
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val catalog = project.libsCatalog()
        val libraryExtension = project.extensions.getByType(LibraryExtension::class.java)
        project.configureCompose(libraryExtension)
        project.dependencies {
            add("api", project.project(":component_base"))
            add("api", catalog.findLibrary("androidx-lifecycle-runtime-ktx").get())
            val bom = catalog.findLibrary("androidx-compose-bom").get()
            add("api", project.dependencies.platform(bom))
            add("api", catalog.findLibrary("androidx-compose-ui").get())
            add("api", catalog.findLibrary("androidx-compose-material3").get())
            add("api", catalog.findLibrary("androidx-navigation-compose").get())
            add("api", catalog.findLibrary("androidx-activity-compose").get())
            add("api", catalog.findLibrary("androidx-compose-ui-tooling-preview").get())
        }
    }
}
