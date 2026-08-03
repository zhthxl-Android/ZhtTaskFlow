// =============================================================================
// 根工程：声明插件别名（apply false），版本统一由 libs.versions.toml 管理
// =============================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.taskflow.android.application) apply false
    alias(libs.plugins.taskflow.android.library) apply false
    alias(libs.plugins.taskflow.android.core) apply false
    alias(libs.plugins.taskflow.android.nav) apply false
    alias(libs.plugins.taskflow.android.feature) apply false
}

// =============================================================================
// 依赖红线辅助校验（指引型）：打印各模块 dependencies 命令，不嵌套启动 Gradle
// 避免 Windows 路径空格、Gradle 进程嵌套等跨平台问题
// =============================================================================

tasks.register("checkDependencyRules") {
    group = "verification"
    description = "打印依赖红线校验命令与说明（不自动执行子 Gradle 进程）"
    doLast {
        val modulePaths = listOf(
            ":app",
            ":component_core",
            ":component_nav",
            ":feature_task",
            ":feature_article",
        )
        logger.lifecycle("")
        logger.lifecycle("=== TaskflowTaskFlow 依赖红线辅助校验（请在本工程根目录手动执行）===")
        logger.lifecycle("规则摘要：")
        logger.lifecycle("  - app 仅 implementation :component_nav（base 经 nav 的 api 传递）")
        logger.lifecycle("  - feature 仅 implementation :component_core、:component_nav")
        logger.lifecycle("  - core 不得出现 Compose；feature 不得互依")
        logger.lifecycle("")
        modulePaths.forEach { modulePath ->
            val coord = modulePath.removePrefix(":")
            logger.lifecycle("./gradlew :$coord:dependencies --configuration debugCompileClasspath")
        }
        logger.lifecycle("")
        logger.lifecycle("可选：./gradlew lintDebug")
        logger.lifecycle("=== 结束 ===")
    }
}
