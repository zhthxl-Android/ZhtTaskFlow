plugins {
    alias(libs.plugins.taskFlow.android.feature)
}

taskFlow {
    resourcePrefix.set("article_")
}

dependencies {
    // Retrofit 注解仅用于 ArticleApi 接口编译，运行时由 component_core 提供实现能力
    compileOnly(libs.retrofit)
    implementation(libs.androidx.compose.material.icons.extended)
}
