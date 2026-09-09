package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 单元测试依赖：JUnit、MockK、协程测试，供 infrastructure / 业务 library 复用。
 */
internal fun Project.configureUnitTestDependencies() {
    val catalog = libsCatalog()
    dependencies {
        add("testImplementation", catalog.findLibrary("junit").get())
        add("testImplementation", catalog.findLibrary("mockk").get())
        add("testImplementation", catalog.findLibrary("kotlinx-coroutines-test").get())
    }
}
