plugins {
    alias(libs.plugins.taskFlow.android.feature)
}

taskFlow {
    resourcePrefix.set("home_")
}

dependencies {
    implementation(project(":component_nav"))
    implementation(libs.androidx.compose.material.icons.extended)
}
