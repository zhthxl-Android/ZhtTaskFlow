// =============================================================================
// 约定插件工程：依赖 AGP / Kotlin Gradle Plugin（版本来自 libs Catalog）
// =============================================================================

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.example.zhttaskflow.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("TaskFlowAndroidBase") {
            id = "taskflow.android.base"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidBasePlugin"
        }
        register("TaskFlowAndroidLibrary") {
            id = "taskflow.android.library"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidLibraryPlugin"
        }
        register("TaskFlowAndroidApplication") {
            id = "taskflow.android.application"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidApplicationPlugin"
        }
        register("TaskFlowAndroidCore") {
            id = "taskflow.android.core"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidCorePlugin"
        }
        register("TaskFlowAndroidNav") {
            id = "taskflow.android.nav"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidNavPlugin"
        }
        register("TaskFlowAndroidFeature") {
            id = "taskflow.android.feature"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidFeaturePlugin"
        }
    }
}