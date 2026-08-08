package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project

/**
 * 注册 [TaskFlowExtension]（幂等）。
 * 由 [TaskFlowAndroidCommonPlugin] 在 apply 阶段调用。
 */
internal fun Project.registerTaskFlowExtensionIfAbsent(): TaskFlowExtension {
    val existing = extensions.findByType(TaskFlowExtension::class.java)
    if (existing != null) {
        return existing
    }
    return extensions.create("taskFlow", TaskFlowExtension::class.java).apply {
        resourcePrefix.convention(computeDefaultResourcePrefix())
    }
}

/**
 * 注册 [TaskFlowFeatureExtension]（幂等）。
 * 由 [TaskFlowAndroidFeaturePlugin] 在 apply 阶段调用。
 */
internal fun Project.registerTaskFlowFeatureExtensionIfAbsent(): TaskFlowFeatureExtension {
    val existing = extensions.findByType(TaskFlowFeatureExtension::class.java)
    if (existing != null) {
        return existing
    }
    return extensions.create("taskFlowFeature", TaskFlowFeatureExtension::class.java).apply {
        applicationId.convention(computeTaskFlowNamespace())
    }
}
