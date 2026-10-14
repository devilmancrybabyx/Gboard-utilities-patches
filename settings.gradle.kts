rootProject.name = "gboard-patches"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GPR_USER")
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GPR_KEY")
                    ?: System.getenv("MORPHE_PACKAGES_TOKEN")
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "1.3.3"
}
