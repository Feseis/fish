pluginManagement {

    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        gradlePluginPortal()
    }
}
rootProject.name = "fish"
include("auth-server8081")
include("app")