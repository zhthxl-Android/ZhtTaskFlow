// =============================================================================
// 根工程：声明插件别名（apply false），版本统一由 libs.versions.toml 管理
// =============================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.taskFlow.android.application) apply false
    alias(libs.plugins.taskFlow.android.library) apply false
    alias(libs.plugins.taskFlow.android.core) apply false
    alias(libs.plugins.taskFlow.android.nav) apply false
    alias(libs.plugins.taskFlow.android.feature) apply false
}

// =============================================================================
// 依赖红线自动校验：解析各模块 project() 依赖，违规则失败构建
// =============================================================================

import org.gradle.api.artifacts.ProjectDependency

tasks.register("checkDependencyRules") {
    group = "verification"
    description = "校验模块间 project 依赖是否符合组件化红线（feature 不互依、底层不依赖业务等）"
    doLast {
        val violations = mutableListOf<String>()
        val featureModulePaths = rootProject.subprojects
            .map { it.path }
            .filter { path -> path.startsWith(":feature_") }
            .toSet()
        val infrastructurePaths = setOf(
            ":component_base",
            ":component_core",
            ":component_nav",
            ":component_lint",
        )
        val upwardForbiddenTargets = featureModulePaths + setOf(":app")
        val dependencyConfigurationNames = listOf(
            "implementation",
            "api",
            "compileOnly",
            "runtimeOnly",
            "lintChecks",
            "testImplementation",
            "androidTestImplementation",
        )

        fun collectProjectDependencies(projectPath: String): List<Pair<String, String>> {
            val project = rootProject.project(projectPath)
            val edges = mutableListOf<Pair<String, String>>()
            dependencyConfigurationNames.forEach { configurationName ->
                val configuration = project.configurations.findByName(configurationName) ?: return@forEach
                configuration.dependencies.withType(ProjectDependency::class.java).forEach { dependency ->
                    val targetPath = dependency.dependencyProject.path
                    edges += configurationName to targetPath
                }
            }
            return edges
        }

        rootProject.subprojects.forEach { subproject ->
            val projectPath = subproject.path
            collectProjectDependencies(projectPath).forEach { (configurationName, targetPath) ->
                when {
                    projectPath in infrastructurePaths && targetPath in upwardForbiddenTargets -> {
                        violations += "$projectPath 禁止依赖业务/壳模块 $targetPath（$configurationName）"
                    }
                    projectPath == ":component_core" && targetPath in (setOf(":component_nav") + upwardForbiddenTargets) -> {
                        violations += "$projectPath 禁止依赖 $targetPath（$configurationName）"
                    }
                    projectPath == ":component_nav" && targetPath != ":component_base" -> {
                        if (targetPath in upwardForbiddenTargets || targetPath == ":component_core") {
                            violations += "$projectPath 仅允许 api 传递 component_base，禁止 $targetPath（$configurationName）"
                        }
                    }
                    projectPath == ":component_base" && targetPath != ":component_core" -> {
                        violations += "$projectPath 仅允许依赖 :component_core，禁止 $targetPath（$configurationName）"
                    }
                    projectPath.startsWith(":feature_") -> {
                        val allowed = setOf(":component_core", ":component_nav", ":component_lint")
                        if (targetPath !in allowed) {
                            violations += "$projectPath 仅允许依赖 core/nav/lintChecks，禁止 $targetPath（$configurationName）"
                        }
                        if (targetPath in featureModulePaths && targetPath != projectPath) {
                            violations += "$projectPath 禁止依赖其他 Feature 模块 $targetPath（$configurationName）"
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            logger.error("")
            logger.error("=== ZhtTaskFlow 依赖红线校验失败（${violations.size} 项）===")
            violations.forEach { message -> logger.error("  - $message") }
            logger.error("")
            error("checkDependencyRules failed")
        } else {
            logger.lifecycle("checkDependencyRules: 全部模块 project 依赖符合红线。")
        }
    }
}
