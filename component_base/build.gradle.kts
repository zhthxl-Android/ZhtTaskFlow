plugins {
    alias(libs.plugins.taskFlow.android.library)
}

taskFlow {
    resourcePrefix.set("base_")
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.ktx)
}
