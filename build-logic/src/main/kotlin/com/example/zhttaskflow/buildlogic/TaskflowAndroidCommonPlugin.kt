package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * 通用约定插件底座：DSL 扩展注册、Compose 插件、Android 通用配置与全模块基础依赖。
 *
 * 基础依赖（api）：协程、core-ktx、Lifecycle、Compose BOM/UI/M3/activity-compose。
 * 上层 library / application 插件仅负责 apply 插件链，不再重复注入上述依赖。
 */
class TaskFlowAndroidCommonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.registerTaskFlowExtensionIfAbsent()
        if (!project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose")) {
            project.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        }

        project.pluginManager.withPlugin("com.android.library") {
            project.applyAndroidComposeConfig<LibraryExtension>()
        }
        project.pluginManager.withPlugin("com.android.application") {
            project.applyAndroidComposeConfig<ApplicationExtension>()
        }
    }

    /**
     * Library / Application 统一配置：扩展配置 → Kotlin → 依赖注入。
     */
    private inline fun <reified T : CommonExtension<*, *, *, *, *, *>> Project.applyAndroidComposeConfig() {
        val resourcePrefix =
            extensions.findByType(TaskFlowExtension::class.java)?.resourcePrefix?.get().orEmpty()
        extensions.configure<T> {
            configureCompose(this)
            configureAndroidCommonOn(this, resourcePrefix)
        }
        configureKotlinAndroid()
        injectTaskFlowCommonDependencies()
    }

    private fun Project.injectTaskFlowCommonDependencies() {
        val catalog = libsCatalog()
        dependencies {
            add("api", catalog.findLibrary("kotlinx-coroutines-core").get())
            add("api", catalog.findLibrary("kotlinx-coroutines-android").get())
            add("api", catalog.findLibrary("androidx-core-ktx").get())
            add("api", catalog.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
            add("api", catalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("api", catalog.findLibrary("androidx-lifecycle-runtime-ktx").get())
            val bom = catalog.findLibrary("androidx-compose-bom").get()
            add("api", platform(bom))
            add("api", catalog.findLibrary("androidx-compose-ui").get())
            add("api", catalog.findLibrary("androidx-compose-material3").get())
            add("api", catalog.findLibrary("androidx-activity-compose").get())
        }
    }
}
