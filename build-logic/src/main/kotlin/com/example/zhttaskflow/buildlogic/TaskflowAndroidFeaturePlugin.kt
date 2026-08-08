package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Feature 双模式约定插件：library / standalone 结构对称，并统一 Feature 模块专属依赖。
 *
 * - library 模式：`taskFlow.android.library` + implementation activity-ktx（library 链不携带 Activity 扩展）
 * - standalone 模式：`taskFlow.android.application`（activity-ktx 由 Application 插件注入，本插件不重复）
 * - standalone 开关键名见 [featureStandaloneGradlePropertyKey]
 */
class TaskFlowAndroidFeaturePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val featureExtension = project.registerTaskFlowFeatureExtensionIfAbsent()

        val isStandalone = project.providers.gradleProperty(project.featureStandaloneGradlePropertyKey())
            .map { value -> value.equals("true", ignoreCase = true) }
            .orElse(false)
            .get()

        if (isStandalone) {
            project.pluginManager.apply("taskFlow.android.application")
            project.pluginManager.withPlugin("com.android.application") {
                project.extensions.configure<ApplicationExtension> {
                    sourceSets.getByName("main") {
                        java.srcDir("src/standalone/kotlin")
                        manifest.srcFile("src/standalone/AndroidManifest.xml")
                        res.srcDir("src/standalone/res")
                    }
                    defaultConfig.applicationId = featureExtension.applicationId.get()
                }
            }
        } else {
            project.pluginManager.apply("taskFlow.android.library")
        }

        project.injectFeatureModuleDependencies(isStandalone)
    }

    /**
     * Feature 业务依赖：core / nav 始终注入；activity-ktx 仅在 library 模式补齐（standalone 由 Application 插件负责）。
     */
    private fun Project.injectFeatureModuleDependencies(isStandalone: Boolean) {
        val catalog = libsCatalog()
        dependencies {
            add("implementation", project.project(":component_core"))
            add("implementation", project.project(":component_nav"))
            if (!isStandalone) {
                add("implementation", catalog.findLibrary("androidx-activity-ktx").get())
            }
        }
    }
}
