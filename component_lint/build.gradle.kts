plugins {
    kotlin("jvm")
}

group = "com.example.zhttaskflow.lint"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.kotlin.stdlib)
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
