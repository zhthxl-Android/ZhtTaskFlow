package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import java.io.File

/**
 * 统一 Android 模块通用配置：SDK、namespace、resourcePrefix、buildTypes、Lint、Library consumer 规则。
 */
internal fun Project.configureAndroidCommon() {
    val prefix = extensions.findByType(TaskFlowExtension::class.java)?.resourcePrefix?.get().orEmpty()

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

/**
 * library 和 app 都要执行的通用逻辑，抽成单独函数，避免重复代码
 */
private fun Project.applySharedAndroidSettings(
    ext: com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>,
    resourcePrefix: String,
) {
    //android {
    //     namespace = "com.example.zhttaskflow.nav"  // 删掉，插件自动生成
    //}
    ext.namespace = computeTaskFlowNamespace()

    if (resourcePrefix.isNotEmpty()) {
        //等价于在模块的 android {} 中手动写 resourcePrefix "xxx"
        ext.resourcePrefix = resourcePrefix
    }
    ext.buildFeatures {
        buildConfig = false
    }
    ext.compileOptions {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
    //Lint 静态检查配置
    val baselineFile: File = layout.projectDirectory.file("lint-baseline.xml").asFile
    ext.lint {
        //lint 发现 error 级别问题，构建直接失败；强制遵守代码规范
        abortOnError = true
        //启用 lint 基线
        baseline = baselineFile
        // resourcePrefix 违规：AGP 8+ 为 ResourceName；旧版/部分场景为 MissingPrefix
        error.add("ResourceName")
        error.add("MissingPrefix")
        //关闭一些不关心的 lint 规则；
        //字符串没有全部翻译，不报错；
        disable.add("MissingTranslation")
        //图标缺少各种密度文件夹，放过；
        disable.add("IconMissingDensityFolder")
        //Compose 经常硬编码字符串，关闭这个规则。
        disable.add("HardcodedText")
        //不检查依赖库内部的 lint 问题，只检查本模块源码，加快 lint 速度。
        checkDependencies = false
    }
}