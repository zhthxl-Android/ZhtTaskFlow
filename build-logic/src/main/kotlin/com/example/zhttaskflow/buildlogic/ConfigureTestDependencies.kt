package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project

/**
 * 测试依赖接入位（骨架阶段不启用，版本已写入 libs.versions.toml）。
 * 业务阶段取消注释并在 TaskFlowAndroidLibraryPlugin 中调用即可。
 */
@Suppress("unused")
internal fun Project.configureTaskFlowTestDependenciesPlaceholder() {
    // val catalog = libsCatalog()
    // dependencies {
    //     add("testImplementation", catalog.findLibrary("junit").get())
    //     add("testImplementation", catalog.findLibrary("kotlinx-coroutines-test").get())
    //     val bom = catalog.findLibrary("androidx-compose-bom").get()
    //     add("androidTestImplementation", platform(bom))
    //     add("androidTestImplementation", catalog.findLibrary("androidx-compose-ui-test-junit4").get())
    // }
}
