plugins {
    alias(libs.plugins.taskFlow.android.feature)
}

taskFlow {
    resourcePrefix.set("log_")
}

dependencies {
    // 后续接入日志数据源时可按需添加 compileOnly API 注解
}
