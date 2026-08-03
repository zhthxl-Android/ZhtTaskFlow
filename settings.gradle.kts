// =============================================================================
// 根工程：装配子模块并引入 build-logic 复合构建
// =============================================================================

pluginManagement {
    includeBuild("build-logic")
    repositories {
        // 阿里云 Gradle 插件镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云公共依赖镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}

rootProject.name = "ZhtTaskFlow"

include(":app")
include(":component_base")
include(":component_core")
include(":component_nav")
include(":feature_task")
include(":feature_article")
