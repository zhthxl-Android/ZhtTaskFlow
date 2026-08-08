plugins {
    alias(libs.plugins.taskFlow.android.library)
}

taskFlow {
    resourcePrefix.set("base_")
}
