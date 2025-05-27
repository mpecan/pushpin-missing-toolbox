plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `maven-publish`
}

dependencies {
    api(project(":pushpin-api"))
    api(project(":pushpin-client"))
    api(project(":pushpin-discovery"))
    implementation("org.springframework:spring-webflux")
}
