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
        register("TaskflowAndroidBase") {
            id = "taskflow.android.base"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskflowAndroidBasePlugin"
        }
        register("TaskflowAndroidLibrary") {
            id = "taskflow.android.library"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskflowAndroidLibraryPlugin"
        }
        register("TaskflowAndroidApplication") {
            id = "taskflow.android.application"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskflowAndroidApplicationPlugin"
        }
        register("TaskflowAndroidCore") {
            id = "taskflow.android.core"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskflowAndroidCorePlugin"
        }
        register("TaskflowAndroidNav") {
            id = "taskflow.android.nav"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskflowAndroidNavPlugin"
        }
        register("TaskflowAndroidFeature") {
            id = "taskflow.android.feature"
            implementationClass = "com.example.zhttaskflow.buildlogic.TaskflowAndroidFeaturePlugin"
        }
    }
}