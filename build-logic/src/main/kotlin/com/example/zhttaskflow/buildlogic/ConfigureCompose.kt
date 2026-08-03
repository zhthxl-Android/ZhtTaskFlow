package com.example.zhttaskflow.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** 开启 Compose buildFeatures；debug 变体附加 ui-tooling 便于预览 */
internal fun Project.configureCompose(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.buildFeatures {
        compose = true
    }
    val catalog = libsCatalog()
    dependencies {
        add("debugImplementation", catalog.findLibrary("androidx-compose-ui-tooling").get())
    }
}
