plugins {
    alias(libs.plugins.taskflow.android.feature)
}

taskflow {
    resourcePrefix.set("task_")
}

// applicationId 可选覆盖；默认与自动 namespace 一致（com.example.zhttaskflow.feature.task）
// taskflowFeature {
//     applicationId.set("com.example.zhttaskflow.feature.task.debug")
// }
