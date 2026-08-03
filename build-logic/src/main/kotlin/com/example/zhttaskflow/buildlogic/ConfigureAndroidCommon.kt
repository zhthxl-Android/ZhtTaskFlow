package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import java.io.File

/**
 * 统一 Android 模块通用配置：SDK、namespace、resourcePrefix、buildTypes、Lint、Library consumer 规则。
 */
internal fun Project.configureAndroidCommon() {
    val taskflowExt = extensions.findByType(TaskflowExtension::class.java)
    val prefix = taskflowExt?.resourcePrefix?.orNull ?: ""

    extensions.findByType(LibraryExtension::class.java)?.let { ext ->
        ext.compileSdk = 35
        ext.defaultConfig {
            minSdk = 24
            consumerProguardFiles("consumer-rules.pro")
        }
        applySharedAndroidSettings(ext, prefix)
        ext.buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
            }
            getByName("release") {
                isMinifyEnabled = false
            }
        }
    }
    extensions.findByType(ApplicationExtension::class.java)?.let { ext ->
        ext.compileSdk = 35
        ext.defaultConfig {
            minSdk = 24
            targetSdk = 35
        }
        applySharedAndroidSettings(ext, prefix)
        ext.buildTypes {
            getByName("debug") {
                isDebuggable = true
                isMinifyEnabled = false
            }
            getByName("release") {
                isMinifyEnabled = false
            }
        }
    }
}

private fun Project.applySharedAndroidSettings(
    ext: com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>,
    resourcePrefix: String,
) {
    ext.namespace = computeTaskflowNamespace()
    if (resourcePrefix.isNotEmpty()) {
        ext.resourcePrefix = resourcePrefix
    }
    ext.buildFeatures {
        buildConfig = false
    }
    ext.compileOptions {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
    val baselineFile: File = layout.projectDirectory.file("lint-baseline.xml").asFile
    ext.lint {
        abortOnError = true
        baseline = baselineFile
        error.add("MissingPrefix")
        disable.add("MissingTranslation")
        disable.add("IconMissingDensityFolder")
        disable.add("HardcodedText")
        checkDependencies = false
    }
}