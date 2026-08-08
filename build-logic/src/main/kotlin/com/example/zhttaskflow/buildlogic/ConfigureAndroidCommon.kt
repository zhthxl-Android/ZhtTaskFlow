package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import java.io.File

/**
 * 在已就绪的 [CommonExtension] 上写入 SDK、namespace、资源前缀、Lint 等通用 Android 配置。
 * 由 [TaskFlowAndroidCommonPlugin] 在 `withPlugin` 回调内调用，扩展实例由 `extensions.configure` 提供。
 */
internal fun Project.configureAndroidCommonOn(
    extension: CommonExtension<*, *, *, *, *, *>,
    resourcePrefix: String,
) {
    when (extension) {
        is LibraryExtension -> configureLibraryAndroidDefaults(extension)
        is ApplicationExtension -> configureApplicationAndroidDefaults(extension)
    }
    applySharedAndroidSettings(extension, resourcePrefix)
}

private fun Project.configureLibraryAndroidDefaults(extension: LibraryExtension) {
    extension.compileSdk = taskFlowCompileSdk()
    extension.defaultConfig {
        minSdk = taskFlowMinSdk()
        consumerProguardFiles("consumer-rules.pro")
    }
    extension.buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

private fun Project.configureApplicationAndroidDefaults(extension: ApplicationExtension) {
    extension.compileSdk = taskFlowCompileSdk()
    extension.defaultConfig {
        minSdk = taskFlowMinSdk()
        targetSdk = taskFlowTargetSdk()
    }
    extension.buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

private fun Project.applySharedAndroidSettings(
    extension: CommonExtension<*, *, *, *, *, *>,
    resourcePrefix: String,
) {
    extension.namespace = computeTaskFlowNamespace()
    if (resourcePrefix.isNotEmpty()) {
        extension.resourcePrefix = resourcePrefix
    }
    extension.buildFeatures {
        buildConfig = false
    }
    extension.compileOptions {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
    val baselineFile: File = layout.projectDirectory.file("lint-baseline.xml").asFile
    extension.lint {
        abortOnError = true
        baseline = baselineFile
        error.add("ResourceName")
        error.add("MissingPrefix")
        disable.add("MissingTranslation")
        disable.add("IconMissingDensityFolder")
        disable.add("HardcodedText")
        checkDependencies = false
    }
}
