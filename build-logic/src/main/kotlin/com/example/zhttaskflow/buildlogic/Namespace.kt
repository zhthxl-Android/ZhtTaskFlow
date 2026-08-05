package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project

/**
 * 根据 Gradle 模块 path 自动生成 namespace，与源码包路径对齐。
 * 规则：去掉前导冒号 → 剔除 component_ 前缀 → 下划线替换为点 → 拼接 com.example.zhttaskflow
 * :app 特例为根包 com.example.zhttaskflow
 */
internal fun Project.computeTaskFlowNamespace(): String {
    //path：Gradle 模块完整路径，例如 :app、:feature_task，开头带冒号
    //.removePrefix(":") 去掉开头冒号，得到纯模块名：app、feature_task
    val modulePath = path.removePrefix(":")
    if (modulePath == "app") {
        return "com.example.zhttaskflow"
    }
    val suffix = modulePath.removePrefix("component_").replace('_', '.')
    //com.example.zhttaskflow.base
    return "com.example.zhttaskflow.$suffix"
}

/**
 * Feature 独立运行开关在 gradle.properties 中的固定键名（约定优于配置）。
 * 例：:feature_task → feature.task.standalone
 */
internal fun Project.featureStandaloneGradlePropertyKey(): String {
    val modulePath = path.removePrefix(":")
    val dotted = modulePath.removePrefix("feature_").replace('_', '.')
    //feature.task.standalone
    return "feature.$dotted.standalone"
}

/**
 * 与各模块 `taskFlow { resourcePrefix.set(...) }` 命名一致；在 plugins 块执行前即可供 AGP 读取。
 */
internal fun Project.computeDefaultResourcePrefix(): String {
    val modulePath = path.removePrefix(":")
    when {
        modulePath == "app" -> return "app_"
        modulePath.startsWith("component_") ->
            return modulePath.removePrefix("component_") + "_"
        modulePath.startsWith("feature_") ->
            return modulePath.removePrefix("feature_") + "_"
        else -> return ""
    }
}
