package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project

/**
 * 根据 Gradle 模块 path 自动生成 namespace，与源码包路径对齐。
 * 规则：去掉前导冒号 → 剔除 component_ 前缀 → 下划线替换为点 → 拼接 com.example.zhttaskflow
 * :app 特例为根包 com.example.zhttaskflow
 */
internal fun Project.computeTaskFlowNamespace(): String {
    val modulePath = path.removePrefix(":")
    if (modulePath == "app") {
        return "com.example.zhttaskflow"
    }
    val suffix = modulePath.removePrefix("component_").replace('_', '.')
    return "com.example.zhttaskflow.$suffix"
}

/**
 * Feature 独立运行开关在 gradle.properties 中的固定键名（约定优于配置）。
 * 例：:feature_task → feature.task.standalone
 */
internal fun Project.featureStandaloneGradlePropertyKey(): String {
    val modulePath = path.removePrefix(":")
    val dotted = modulePath.removePrefix("feature_").replace('_', '.')
    return "feature.$dotted.standalone"
}
