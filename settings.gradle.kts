pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PhotoCleaner"

include(":app")
include(":core:common")
include(":core:database")
include(":feature:scanner")
include(":feature:duplicate")
include(":feature:fileops")
include(":feature:appupdate")
include(":feature:settings")
