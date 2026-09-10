pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        // 高德 SDK（定位/地图）国内仓库
        maven("https://maven.aliyun.com/nexus/content/repositories/releases")
    }
}

rootProject.name = "spotpal-android"
include(":app")
include(":core:design", ":core:network", ":core:common", ":core:data", ":core:model")
include(
    ":feature:discover", ":feature:profile", ":feature:negotiate",
    ":feature:confirm", ":feature:squads", ":feature:me",
)
