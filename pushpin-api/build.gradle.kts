plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.mpecan"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    testImplementation(kotlin("test"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar{
    enabled = false
}
kotlin {
    jvmToolchain(21)
}