import org.gradle.internal.impldep.junit.runner.Version.id

plugins {
    alias(libs.plugins.plugin.publish)
}

group = "io.github.alexritian"
version = "0.0.15"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {

    implementation(libs.gradle.jooq.plugin)
    implementation(libs.testcontainers.postgresql)
    // flyway
    implementation(libs.bundles.flyway.pg)
    implementation(libs.postgresql)

    // 测试依赖
    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
}

gradlePlugin {
    website = "https://github.com/AlexRITIAN/codegen-gradle-plugin"
    vcsUrl = "https://github.com/AlexRITIAN/codegen-gradle-plugin.git"
    // Define the plugin
    val codegenGradlePlugin by plugins.creating {
        id = "io.github.alexritian.codegen-gradle-plugin"
        implementationClass = "io.github.alexritian.codegen.CodegenGradlePlugin"
        displayName = "Codegen JOOQ Gradle Plugin"
        description = "A custom Gradle plugin that extends gradle-jooq-plugin with default configurations."
        tags = listOf("jooq", "codegen")
    }
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    test {
        java {
            setSrcDirs(listOf("src/test/java"))
        }
    }
}
