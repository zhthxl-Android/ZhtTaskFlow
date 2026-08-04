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

dependencies {
    // component_nav 已通过 api 传递 component_base（coroutines、core-ktx 等），无需重复声明 base
    implementation(project(":component_nav"))
    if (!taskStandalone.get()) {
        implementation(project(":feature_task"))
    }
    if (!articleStandalone.get()) {
        implementation(project(":feature_article"))
    }
}
