package com.example.zhttaskflow.buildlogic

import org.gradle.api.provider.Property

/** 由 taskFlow.android.common 创建，用于 resourcePrefix 等模块级约定。 */
abstract class TaskFlowExtension {
    abstract val resourcePrefix: Property<String>
}
