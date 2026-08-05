package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Feature 双模式约定插件：
 * - library：taskFlow.android.library + Compose（library 已含 base，禁止再 apply base）
 * - application：com.android.application → kotlin.android → kotlin.compose → taskFlow.android.base
 * - standalone 键名固定（featureStandaloneGradlePropertyKey），避免 apply 阶段读取 DSL 扩展的时序问题
 * - applicationId 默认 namespace；在 configure<ApplicationExtension> 内写入 defaultConfig（避免 AGP「已读取 applicationId」报错）；模块可通过 taskFlowFeature { applicationId.set(...) } 覆盖
 * - 公共依赖：仅 implementation core、nav（base 经 api 传递）；不重复声明 base
 */
class TaskFlowAndroidFeaturePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 注册feature专属DSL扩展 taskFlowFeature {}
        val featureExtension = project.extensions.create(
            "taskFlowFeature",
            TaskFlowFeatureExtension::class.java,
        )
        //.convention(...)：设置默认值，默认值调用computeTaskFlowNamespace()自动生成包名；业务模块可以手动覆盖applicationId
        featureExtension.applicationId.convention(project.computeTaskFlowNamespace())

        val standaloneKey = project.featureStandaloneGradlePropertyKey()
        //project.providers.gradleProperty(standaloneKey)：惰性读取gradle.properties配置；
        val isStandalone = project.providers.gradleProperty(standaloneKey)
            .map { value ->
                value.equals(
                    "true",
                    ignoreCase = true
                )
            }
            .orElse(false)
            .get()

        if (isStandalone) {
            with(project.pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("taskFlow.android.base")
            }
            project.configure<ApplicationExtension> {
                sourceSets.getByName("main") {
                    java.srcDir("src/standalone/kotlin")
                    manifest.srcFile("src/standalone/AndroidManifest.xml")
                    res.srcDir("src/standalone/res")
                }
                defaultConfig.applicationId = featureExtension.applicationId.get()
                project.configureCompose(this)
            }
        } else {
            project.pluginManager.apply("taskFlow.android.library")
            project.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            project.configure<LibraryExtension> {
                project.configureCompose(this)
            }
        }

        project.dependencies {
            add(
                "implementation",
                project.project(":component_core")
            )
            add(
                "implementation",
                project.project(":component_nav")
            )
        }
    }
}
