package com.example.zhttaskflow.buildlogic

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room KSP 编译配置统一收口：core / feature 约定插件共用。
 *
 * - 应用 KSP 插件与 `room-compiler`；
 * - `room.schemaLocation` → `{moduleProjectDir}/schemas`；
 * - 启用稳定项 `room.incremental`。
 */
internal fun Project.configureRoomKsp(
    includeRoomCompileOnlyAnnotations: Boolean = false,
) {
    val catalog = libsCatalog()
    pluginManager.apply("com.google.devtools.ksp")
    dependencies {
        add("ksp", catalog.findLibrary("room-compiler").get())
        if (includeRoomCompileOnlyAnnotations) {
            add("compileOnly", catalog.findLibrary("room-ktx").get())
        }
    }
    extensions.configure<KspExtension> {
        val schemaDir = layout.projectDirectory.dir("schemas").asFile.absolutePath
        arg("room.schemaLocation", schemaDir)
        arg("room.incremental", "true")
    }
}
