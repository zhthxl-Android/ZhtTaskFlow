plugins {
    alias(libs.plugins.taskFlow.android.library)
}

taskFlow {
    resourcePrefix.set("base_")
}

// coroutines、core-ktx 由 TaskFlowAndroidLibraryPlugin 统一 api 注入，本模块脚本不重复声明 dependencies。
