plugins {
    alias(libs.plugins.taskFlow.android.library)
}

taskFlow {
    resourcePrefix.set("base_")
}

dependencies {
    implementation(project(":component_core"))
    implementation(libs.androidx.activity.compose)
}
