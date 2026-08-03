package com.example.zhttaskflow.buildlogic

import org.gradle.api.provider.Property

/**
 * Feature 扩展：仅 applicationId 可覆盖（默认等于 namespace）。
 * standalone 开关键名固定为 feature.<后缀>.standalone，不在此 DSL 配置。
 */
abstract class taskflowFeatureExtension {
    abstract val applicationId: Property<String>
}
