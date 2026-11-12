pluginManagement {

    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        gradlePluginPortal()
    }
}
rootProject.name = "fish"
include("server8081-auth")
include("server8082-logistics")
include("server8083-payment")
include("server8084-sms")
include("auth-api")
include("payment-api")
include("sms-api")
include("app")
include("contract")