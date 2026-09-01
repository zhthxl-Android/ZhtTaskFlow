plugins {
    alias(libs.plugins.taskFlow.android.feature)
}

taskFlow {
    resourcePrefix.set("home_")
}

dependencies {
    // Retrofit 注解仅用于后续 API 接口编译（compileOnly）；运行时网络实现由 component_core 统一提供，符合防腐层约定
    compileOnly(libs.retrofit)
}
