// =============================================================================
// build-logic：共享根目录 Version Catalog，禁止在约定插件源码中硬编码版本
// =============================================================================

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
