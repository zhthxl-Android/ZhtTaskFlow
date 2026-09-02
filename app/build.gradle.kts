plugins {
    alias(libs.plugins.taskFlow.android.application)
}

taskFlow {
    resourcePrefix.set("app_")
}

val taskStandalone = providers.gradleProperty("feature.task.standalone")
    .map { value -> value.equals("true", ignoreCase = true) }
    .orElse(false)

val articleStandalone = providers.gradleProperty("feature.article.standalone")
    .map { value -> value.equals("true", ignoreCase = true) }
    .orElse(false)

val logStandalone = providers.gradleProperty("feature.log.standalone")
    .map { value -> value.equals("true", ignoreCase = true) }
    .orElse(false)

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnit4Runner"
    }
}

dependencies {
    // 壳工程统一初始化 component_core 能力（如网络诊断开关），不承载业务逻辑
    implementation(project(":component_core"))
    // component_nav 已通过 api 传递 component_base（coroutines、core-ktx 等），无需重复声明 base
    implementation(project(":component_nav"))
    if (!logStandalone.get()) {
        implementation(project(":feature_log"))
    }
    if (!taskStandalone.get()) {
        implementation(project(":feature_task"))
    }
    if (!articleStandalone.get()) {
        implementation(project(":feature_article"))
    }

    val composeBom = platform(libs.androidx.compose.bom)
    debugImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
