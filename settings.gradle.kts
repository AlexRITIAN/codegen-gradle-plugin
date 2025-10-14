dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from("io.github.alexritian:catalog:0.0.10")
        }
    }
}

rootProject.name = "codegen-gradle-plugin"
