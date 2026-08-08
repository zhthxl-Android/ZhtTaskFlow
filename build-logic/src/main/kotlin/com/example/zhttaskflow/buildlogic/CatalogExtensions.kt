package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * 约定插件访问 Version Catalog 的唯一入口。
 * 禁止在约定插件 .kt 中使用类型安全访问器 libs.xxx。
 */
internal fun Project.libsCatalog(): VersionCatalog =
    extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * 从 Catalog [versions] 段读取整型 SDK 级别，避免在插件源码硬编码数字。
 */
internal fun VersionCatalog.requireVersionInt(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

/** 与 [gradle/libs.versions.toml] 中 compileSdk / minSdk / targetSdk 对齐。 */
internal fun Project.taskFlowCompileSdk(): Int = libsCatalog().requireVersionInt("compileSdk")

internal fun Project.taskFlowMinSdk(): Int = libsCatalog().requireVersionInt("minSdk")

internal fun Project.taskFlowTargetSdk(): Int = libsCatalog().requireVersionInt("targetSdk")
