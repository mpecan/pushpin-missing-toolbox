plugins {
    id("org.springframework.boot")
}

group = "io.github.mpecan"
version = "0.0.1-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}