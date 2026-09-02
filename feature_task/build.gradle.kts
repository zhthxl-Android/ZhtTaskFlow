plugins {
    alias(libs.plugins.taskFlow.android.feature)
}

taskFlow {
    resourcePrefix.set("task_")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

// applicationId 可选覆盖；默认与自动 namespace 一致（com.example.zhttaskflow.feature.task）
// taskFlowFeature {
//     applicationId.set("com.example.zhttaskflow.feature.task.debug")
// }
