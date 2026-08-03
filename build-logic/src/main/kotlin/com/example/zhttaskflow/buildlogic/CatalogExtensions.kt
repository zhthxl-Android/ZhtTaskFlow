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
