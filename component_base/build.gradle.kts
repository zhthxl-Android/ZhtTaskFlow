// 基础层：仅使用 library 约定插件（勿用 feature，避免注入 core/nav 与业务层依赖）
plugins {
    alias(libs.plugins.taskFlow.android.library)
}

taskFlow {
    resourcePrefix.set("base_")
}
