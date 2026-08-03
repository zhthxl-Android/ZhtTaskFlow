plugins {
    alias(libs.plugins.taskflow.android.library)
}

taskflow {
    resourcePrefix.set("base_")
}

// coroutines、core-ktx 由 TaskFlowAndroidLibraryPlugin 统一 api 注入，本模块脚本不重复声明 dependencies。
