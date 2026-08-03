package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** 统一 Kotlin JVM 17，与 Java compileOptions 对齐 */
internal fun Project.configureKotlinAndroid() {
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
