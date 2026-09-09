package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 通用约定插件（common 底座）：taskFlow DSL、Android 通用配置、Compose 构建特性与 Compose 基础 api 依赖。
 *
 * 与代码层 [component_base] 模块无关；Library / Application 约定插件在 apply 本插件后获得上述能力。
 */
class TaskFlowAndroidCommonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        registerTaskFlowExtensionIfAbsent(project)

        val configureAction = {
            project.extensions.findByType(LibraryExtension::class.java)?.let { extension ->
                project.configureCompose(extension)
            }
            project.extensions.findByType(ApplicationExtension::class.java)?.let { extension ->
                project.configureCompose(extension)
            }
            project.configureAndroidCommon()
            project.configureKotlinAndroid()
        }
        project.pluginManager.withPlugin("com.android.library") { configureAction() }
        project.pluginManager.withPlugin("com.android.application") { configureAction() }

        project.injectTaskFlowComposeApiDependencies()
    }

    private fun registerTaskFlowExtensionIfAbsent(project: Project) {
        if (project.extensions.findByName("taskFlow") != null) {
            return
        }
        project.extensions.create("taskFlow", TaskFlowExtension::class.java).apply {
            resourcePrefix.convention(project.computeDefaultResourcePrefix())
        }
    }

    private fun Project.injectTaskFlowComposeApiDependencies() {
        val catalog = libsCatalog()
        dependencies {
            add("api", catalog.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
            add("api", catalog.findLibrary("androidx-lifecycle-runtime-compose").get())
            val bom = catalog.findLibrary("androidx-compose-bom").get()
            add("api", platform(bom))
            add("api", catalog.findLibrary("androidx-compose-ui").get())
            add("api", catalog.findLibrary("androidx-compose-material3").get())
        }
    }
}
