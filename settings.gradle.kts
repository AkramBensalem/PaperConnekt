import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "PaperConnekt"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":shared:domain")
include(":shared:storage")
include(":shared:secrets")
include(":plugin:native-api")
include(":plugin:native-plugins:common")
include(":plugin:native-plugins:vscode")
include(":plugin:native-plugins:idea")
include(":app:composeApp")
include(":app:android")
include(":app:desktop")
