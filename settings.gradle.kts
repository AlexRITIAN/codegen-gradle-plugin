dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/AlexRITIAN/catalog")
            credentials {
                username = providers.gradleProperty("githubPackagesUsername").get()
                password = providers.gradleProperty("githubPackagesPassword").get()
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from("io.github.alexritian:catalog:0.0.6-SNAPSHOT")
        }
    }
}

rootProject.name = "codegen-gradle-plugin"
