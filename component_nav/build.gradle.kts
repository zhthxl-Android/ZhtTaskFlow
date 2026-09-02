plugins {
    alias(libs.plugins.taskFlow.android.nav)
}

taskFlow {
    resourcePrefix.set("nav_")
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
