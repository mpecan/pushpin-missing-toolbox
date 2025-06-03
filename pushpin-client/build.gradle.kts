plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-client",
    moduleDescription =
        "Spring Boot client library for Pushpin - provides message serialization, " +
            "transport abstraction, and auto-configuration for WebSocket, HTTP Streaming, SSE, and long polling",
)

dependencies {
    api(project(":pushpin-api"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor:reactor-core")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))
}
