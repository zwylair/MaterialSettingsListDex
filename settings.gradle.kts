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

        // the hook library is published only to the maven mirror inside the host checkout
        maven {
            url = uri("${providers.gradleProperty("host.dir").get()}/Tools/maven")
            content { includeGroupByRegex("""com\.exteragram\.hook(\..+)?""") }
        }
    }
}

rootProject.name = "MaterialSettingsListDex"
include("dex")
