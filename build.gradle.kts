plugins {
    id("com.android.application") version "8.12.0" apply false
    id("org.springframework.boot") version "4.1.2" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
}

subprojects {
    group = "me.feseis.fish"
    version = "1.0-SNAPSHOT"

    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

