import com.vanniktech.maven.publish.SonatypeHost

plugins {
    signing
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.publish)
}

group = "io.github.alexritian"
version = "0.0.1-SNAPSHOT"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") } // Gradle Plugin Portal
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
        id = "io.github.alexritian.codegenGradlePlugin"
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

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/AlexRITIAN/codegen-gradle-plugin")
            credentials(PasswordCredentials::class)
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

