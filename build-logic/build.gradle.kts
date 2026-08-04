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
            id = "taskFlow.android.base"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidBasePlugin"
        }
        register("TaskFlowAndroidLibrary") {
            id = "taskFlow.android.library"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidLibraryPlugin"
        }
        register("TaskFlowAndroidApplication") {
            id = "taskFlow.android.application"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidApplicationPlugin"
        }
        register("TaskFlowAndroidCore") {
            id = "taskFlow.android.core"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidCorePlugin"
        }
        register("TaskFlowAndroidNav") {
            id = "taskFlow.android.nav"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidNavPlugin"
        }
        register("TaskFlowAndroidFeature") {
            id = "taskFlow.android.feature"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskFlowAndroidFeaturePlugin"
        }
    }
}