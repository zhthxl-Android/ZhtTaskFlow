package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** App 壳约定：Application → Kotlin Android → Compose → Base */
class TaskflowAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("taskflow.android.base")
        }
        val appExtension = project.extensions.getByType(ApplicationExtension::class.java)
        project.configureCompose(appExtension)
    }
}
